/*******************************************************************************
 * Copyright 2017 by the Department of Informatics (University of Oslo)
 * 
 *    This file is part of the Ontology Services Toolkit 
 *
 *******************************************************************************/
package org.insightcentre.uld.naisc.main;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.NodeIterator;
import org.hobbit.core.Commands;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.rabbit.SimpleFileReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.ShutdownSignalException;
import org.insightcentre.uld.naisc.util.None;


/**
 * HOBBIT system adaptor for LogMap
 * 
 * @author ernesto
 * Created on 15 Jan 2018
 *
 */
public class NaiscSystemAdapter extends AbstractSystemAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(NaiscSystemAdapter.class);
	
	//Not used in OAEI related tasks. Format should be any ontology format accepted by OWL API, Jena API, etc.
	private String format;
	
	private String sourceName;
	private String targetName;
	
	private String sourcePath;
	private String targetPath;
	
	private String results_directory = "./results/";
	
	private String resultsPath = System.getProperty("user.dir") + File.separator + "results";
	
	//output file name given to the output manager, which add an additional ".rdf" to the file
	private String resultsFile = resultsPath + File.separator + "logmap-alignment" ;
	
	
	private ExecutorService executor;
	private Map<String, FileReceiverCallableState> receivers = Collections.synchronizedMap(new HashMap<String, FileReceiverCallableState>());
	
	
	private String queueName;
	
	
	//Input parameter to allow the use of backgorund knowledge from bioportal: LogMap and LogMapBio
	//protected static final String BIOPORTAL_BACKGROUND_KNOWLEDGE = "http://w3id.org/system#isBioportalActive";
	//private Boolean isBioportalActive;
	
	
	@Override
	public void init() throws Exception
	{
		super.init();
		LOGGER.info("LogMap initialized.");
		
		//Input parameter: not used, but could potentially be used to parametrise a system the same way a benchmark is parametrised
		//isBioportalActive = (Boolean) getProperty(BIOPORTAL_BACKGROUND_KNOWLEDGE, false);
		
		
		executor = Executors.newCachedThreadPool();
		
	}

	/**
	 * 
	 * It is expected from the DataGenerator of a benchmark the format (not used) and one or more queue names
	 * 
	 */
	public void receiveGeneratedData(byte[] data){
		try
		{
			//TODO For the OAEI we just receive some "dummy" source data
			LOGGER.info("Starting receiveGeneratedData...");
			ByteBuffer dataBuffer = ByteBuffer.wrap(data);
			String format = RabbitMQUtils.readString(dataBuffer);
			
			
			//We receive queue names sent from data generator
			while (dataBuffer.hasRemaining()){
				queueName = RabbitMQUtils.readString(dataBuffer);
				
				SimpleFileReceiver receiver = SimpleFileReceiver.create(this.incomingDataQueueFactory, queueName);
			    FileReceiverCallable callable = new FileReceiverCallable(receiver, results_directory);
			    
			    // Start a parallel thread that receives the data for us
			    receivers.put(queueName, new FileReceiverCallableState(executor.submit(callable),callable));
			}
			
			LOGGER.info("Received '"+ receivers.size() + "' queue names for the matching tasks");
			
			
		}
		//catch (IOException | ShutdownSignalException | ConsumerCancelledException | InterruptedException ex){
		catch (Exception ex){
			LOGGER.error(ex.toString());
		}
	}

	
	/**
	 * This method receives the task: source and target datasets
	 */
	public void receiveGeneratedTask(String taskId, byte[] data) {
		 LOGGER.info("Starting receiveGeneratedTask..");
	        long time = System.currentTimeMillis();
	        
	        
	        Set<String> allowed_instance_types = new HashSet<String>();
	        
	        
	        ByteBuffer taskBuffer = ByteBuffer.wrap(data);
            //read the buffer in order (8 elements)
            //1. Format
            format = RabbitMQUtils.readString(taskBuffer);
            //2. Source file name
            sourceName =RabbitMQUtils.readString(taskBuffer);
            //3. Target file name
            targetName =RabbitMQUtils.readString(taskBuffer);
            //4. If class matching is required
            boolean isMatchingClassesRequired = Boolean.valueOf(RabbitMQUtils.readString(taskBuffer));
            //5. If data property matching is required
            boolean isMatchingDataPropertiesRequired = Boolean.valueOf(RabbitMQUtils.readString(taskBuffer));
            //6. If object property matching is required
            boolean isMatchingObjectPropertiesRequired = Boolean.valueOf(RabbitMQUtils.readString(taskBuffer));
            //7. If instance matching is required
            boolean isMatchingInstancesRequired = Boolean.valueOf(RabbitMQUtils.readString(taskBuffer));
            //8. Queue name (task name and id to receive the files)
            //We should have defined above a Thread to receive the files in that queue, otherwise the task will not be processed (see below) 
            String queueName=RabbitMQUtils.readString(taskBuffer);
            
            //9+ Allowed instance types (i.e., class URIs)
            if (isMatchingInstancesRequired){	
	           while (taskBuffer.hasRemaining()){
	        	   //Update allowed_instance_types
	        	   allowed_instance_types.add(RabbitMQUtils.readString(taskBuffer));
	           }
            }
            
            
            LOGGER.info("Parsed task " + taskId + ". Queue name: " + queueName +  ". Source: " + sourceName + ". Target: " + targetName + ". It took {}ms.", System.currentTimeMillis() - time);
            
            time = System.currentTimeMillis();
            
            
            
	        try {
	        	
	        	if(receivers.containsKey(queueName)) {
	                FileReceiverCallableState status = receivers.get(queueName);
	                // First, tell the receiver that it should have received all data
	                status.callable.terminateReceiver();
	                
	                // Second, wait until the receiver has stopped
	                try {
                        String files[] = status.result.get(); //to be stored in results_directory
                    } catch (InterruptedException e) {
                    	LOGGER.error("Exception while trying to receive data in queue "+ queueName + ". Aborting.", e);
                    } catch (ExecutionException e) {
                    	LOGGER.error("Exception while trying to receive data in queue "+ queueName + ". Aborting.", e);
                    }
	            } 
	        	else {
	                System.err.println("The given queue name does not exist: " + queueName);
	                
	            }
	        	
	        	LOGGER.info("Received data for task " + taskId + ". Queue/task name: " + queueName + ". It took {}ms.", System.currentTimeMillis() - time);
	            time = System.currentTimeMillis();
	            

	        	//SourceName and targetName play an important role as the order of files in receivedFiles 
            	File file_source = new File(resultsPath + File.separator + sourceName);	            	
				File file_target = new File(resultsPath + File.separator + targetName);
				
				LOGGER.info("Received source file "+ file_source.getAbsolutePath() + " exists? " + file_source.exists());
				LOGGER.info("Received target file "+ file_target.getAbsolutePath() + " exists? " + file_target.exists());
	
				
				//LogMap requires URIs of input ontologies
	            sourcePath = getURIPath(file_source.getAbsolutePath());
	            targetPath = getURIPath(file_target.getAbsolutePath());
            	
	            

	            LOGGER.info("Task " + taskId + " received from task generator");
	            LOGGER.info("Files in queue '" + queueName + "' received from task generator");
	            LOGGER.info("Source " + sourcePath);
	            LOGGER.info("Target " + targetPath);
	            LOGGER.info("Flags: isMatchingClassesRequired " + isMatchingClassesRequired + 
	            		",  isMatchingDataPropertiesRequired " + isMatchingDataPropertiesRequired  + 
	            		",  isMatchingObjectPropertiesRequired " + isMatchingObjectPropertiesRequired  + 
	            		",  isMatchingInstancesRequired " + isMatchingInstancesRequired +
	            		",  restricted_instance_types " + allowed_instance_types.size());
				
	            //Runs LogMap and saves results file
	            String resultsFileTask = resultsFile +"-"+ queueName;//taskId
	            naiscController(sourcePath, targetPath, resultsFileTask, isMatchingClassesRequired, isMatchingDataPropertiesRequired, isMatchingObjectPropertiesRequired, isMatchingInstancesRequired, allowed_instance_types);
	            
				byte[][] resultsArray = new byte[1][];
			    resultsArray[0] = FileUtils.readFileToByteArray(new File(resultsFileTask + ".rdf"));
			    byte[] results = RabbitMQUtils.writeByteArrays(resultsArray);
			    
			    try {
			
			        sendResultToEvalStorage(taskId, results);
			        LOGGER.info("LogMap results sent to evaluation storage. Task " + taskId + ". Queue/Task name: " + queueName);
			    } catch (IOException e) {
			        LOGGER.error("Exception while sending storage space cost to evaluation storage (LogMap results). Task " + taskId, e);
			    }
			} catch (IOException ex) {
				LOGGER.error(ex.toString());
			}
		
		
	}
	
	
	/**
	 * @param targetPath
	 * @return
	 */
	private String getURIPath(String file_path) {
		
		if (file_path.startsWith("file:") || file_path.startsWith("http://")){
			return file_path;
		}
		
		if (file_path.startsWith("/")){
			return "file:" + file_path; //linux
		}
		return "file:/" + file_path; //windows
		
	}

        public void naiscController(String source, String target, String resultsFileTask,
			boolean isMatchingClassesRequired, boolean isMatchingDataPropertiesRequired, 
			boolean isMatchingObjectPropertiesRequired, boolean isMatchingInstancesRequired,
			Set<String> allowed_instance_types) {
            
            Main.execute("tempName", new File(source), new File(target), 
                    new File("configs/default.json"), new File(resultsFileTask), 
                    new None<>(), true, ExecuteListeners.STDERR, new DefaultDatasetLoader());
        }
	
	/*public void logMapController(String source, String target, String resultsFileTask,
			boolean isMatchingClassesRequired, boolean isMatchingDataPropertiesRequired, 
			boolean isMatchingObjectPropertiesRequired, boolean isMatchingInstancesRequired,
			Set<String> allowed_instance_types) {
		
		OracleManager.allowOracle(false);
		
		//LogMap default parameters
		Parameters.readParameters();
		
		//Task parameters
		Parameters.output_class_mappings = isMatchingClassesRequired;
		Parameters.output_prop_mappings = (isMatchingDataPropertiesRequired || isMatchingObjectPropertiesRequired);
		Parameters.output_instance_mappings = isMatchingInstancesRequired;
		Parameters.perform_instance_matching = isMatchingInstancesRequired;
		
		//Allowed instance types
		if (allowed_instance_types.size()>0){
			Parameters.setRestrictInstanceTypes(true);
			Parameters.allowed_instance_types.addAll(allowed_instance_types);
		}
		else{
			Parameters.setRestrictInstanceTypes(false);
			Parameters.allowed_instance_types.clear();
		}
		
		//System parameters to run LogMap or LogMapBio: not used for now
		//Parameters.allow_bioportal = isBioportalActive.booleanValue();
		
		try {
			LOGGER.info("Running LogMap");
			long time = System.currentTimeMillis();
			LogMap2Core logmap2 = new LogMap2Core(source, target);			
			LOGGER.info("Running LogMap completed. It took {}ms.", System.currentTimeMillis() - time);
			
			LOGGER.info("Saving alignments. Approx. size: {} mappings", logmap2.getClassMappings().size());
			saveAlignmentFile(resultsFileTask, logmap2);
			LOGGER.info("LogMap alignment saved in " + resultsFileTask + ".rdf");
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			LOGGER.error("Error running LogMap.", e);
		}
		
		
	}
	*/
	
	
	public void saveAlignmentFile(String output_file, LogMap2Core logmap2) throws Exception{
		
		OutPutFilesManager output_manager = new OutPutFilesManager();
		
		output_manager.createOutFiles(output_file, OutPutFilesManager.OAEIFormat, logmap2.getIRIOntology1(), logmap2.getIRIOntology2());

		
		int dir_mapping;
		
		if (Parameters.output_class_mappings){
		
			for (int ide1 : logmap2.getClassMappings().keySet()){
				
				if (ignoreMapping(logmap2.getIRI4ConceptIdentifier(ide1)))
					continue;
								
				for (int ide2 : logmap2.getClassMappings().get(ide1)){
					
					dir_mapping = logmap2.getDirClassMapping(ide1, ide2);
					
					if (ignoreMapping(logmap2.getIRI4ConceptIdentifier(ide2)))
						continue;	
					
					
					if (dir_mapping!=Utilities.NoMap){
						
						if (dir_mapping!=Utilities.R2L){						
						
							//GSs in OAIE only contains, in general, equivalence mappings
							if (Parameters.output_equivalences_only){
								dir_mapping=Utilities.EQ;
							}
								
							output_manager.addClassMapping2Files(
									logmap2.getIRI4ConceptIdentifier(ide1),
									logmap2.getIRI4ConceptIdentifier(ide2),
									dir_mapping,
									logmap2.getConfidence4ConceptMapping(ide1, ide2)
									);
						}
						else{
							
							if (Parameters.output_equivalences_only){
								dir_mapping=Utilities.EQ;
							}
							
							output_manager.addClassMapping2Files(				
									logmap2.getIRI4ConceptIdentifier(ide2),
									logmap2.getIRI4ConceptIdentifier(ide1),
									dir_mapping,
									logmap2.getConfidence4ConceptMapping(ide1, ide2)
									);
						}
					}
				}
			}
		}
		
		
		if (Parameters.output_prop_mappings){
			
					
			for (int ide1 : logmap2.getDataPropMappings().keySet()){
				
				//ignore mappings involving entities containing these uris
				if (ignoreMapping(logmap2.getIRI4DataPropIdentifier(ide1))||
					ignoreMapping(logmap2.getIRI4DataPropIdentifier(logmap2.getDataPropMappings().get(ide1))))
					continue;
				
				
				output_manager.addDataPropMapping2Files(
							logmap2.getIRI4DataPropIdentifier(ide1),
							logmap2.getIRI4DataPropIdentifier(logmap2.getDataPropMappings().get(ide1)),
							Utilities.EQ,  
							logmap2.getConfidence4DataPropConceptMapping(ide1, logmap2.getDataPropMappings().get(ide1))//1.0
							);
			}
			
			for (int ide1 : logmap2.getObjectPropMappings().keySet()){
			
				//ignore mappings involving entities containing these uris
				if (ignoreMapping(logmap2.getIRI4ObjectPropIdentifier(ide1))||
					ignoreMapping(logmap2.getIRI4ObjectPropIdentifier(logmap2.getObjectPropMappings().get(ide1))))
					continue;
				
				output_manager.addObjPropMapping2Files(
							logmap2.getIRI4ObjectPropIdentifier(ide1),
							logmap2.getIRI4ObjectPropIdentifier(logmap2.getObjectPropMappings().get(ide1)),
							Utilities.EQ, 
							logmap2.getConfidence4ObjectPropConceptMapping(ide1, logmap2.getObjectPropMappings().get(ide1))//1.0
							);
			}
		}
		

		//Output for individuals
		if (Parameters.perform_instance_matching && Parameters.output_instance_mappings){
			
			if (Parameters.output_instance_mapping_files){
				int type;
				for (int ide1 : logmap2.getInstanceMappings4OutputType().keySet()) {
					
					for (int ide2 : logmap2.getInstanceMappings4OutputType().get(ide1).keySet()){
					
						
						type = logmap2.getInstanceMappings4OutputType().get(ide1).get(ide2);
												
						if (type<=1){
							output_manager.addInstanceMapping2Files(
									logmap2.getIRI4InstanceIdentifier(ide1), 
									logmap2.getIRI4InstanceIdentifier(ide2), 
									logmap2.getConfidence4InstanceMapping(ide1, ide2)
								);					
						}
					}
				}
			}
			else{
				for (int ide1 : logmap2.getInstanceMappings().keySet()){
					for (int ide2 : logmap2.getInstanceMappings().get(ide1)){
					
						output_manager.addInstanceMapping2Files(
								logmap2.getIRI4InstanceIdentifier(ide1), 
								logmap2.getIRI4InstanceIdentifier(ide2), 
								logmap2.getConfidence4InstanceMapping(ide1, ide2)
							);					
					}
				}
			}
			
			
		}
	
		output_manager.closeAndSaveFiles();
		logmap2.clearIndexStructures();
		
		
	}

	
	
	private boolean ignoreMapping(String uri_entity){
		//ignore mappings involving entities containing these uris
		for (String uri : Parameters.filter_entities)
			if (uri_entity.contains(uri))
				return true;		
		return false;
	}
	
	
	
	@Override
	public void receiveCommand(byte command, byte[] data)
	{
		//if (Commands.DATA_GENERATION_FINISHED == command)
		//{
		//	LOGGER.info("receiveCommand for source - DATA_GENERATION_FINISHED");
		//	sourceReceiver.terminate();
		//}
		/*if (Commands.TASK_GENERATION_FINISHED == command)
		{
			LOGGER.info("receiveCommand for target - TASK_GENERATION_FINISHED");
			taskReceiver.terminate();
		}
		*/
		super.receiveCommand(command, data);
		
	}

	@Override
	public void close() throws IOException
	{
		super.close();
		LOGGER.info("LogMap System Adapter closed successfully.");
	}

	
	
	
	
	/**
     * A generic method for loading parameters from the system parameter
     * model defined in the system.ttl file
     *
     * @param property the property that we want to load
     * @param defaultValue the default value that will be used in case of an
     * error while loading the property
     */
    @SuppressWarnings("unchecked")
    private <T> T getProperty(String property, T defaultValue) {
        T propertyValue = null;
        NodeIterator iterator = systemParamModel
                .listObjectsOfProperty(systemParamModel
                        .getProperty(property));
        if (iterator.hasNext()) {
            try {
     
            	if (defaultValue instanceof Boolean) {
            		return (T) ((Boolean) iterator.next().asLiteral().getBoolean());
            	} else if (defaultValue instanceof String) {
                    return (T) iterator.next().asLiteral().getString();
                } else if (defaultValue instanceof Integer) {
                    return (T) ((Integer) iterator.next().asLiteral().getInt());
                } else if (defaultValue instanceof Long) {
                    return (T) ((Long) iterator.next().asLiteral().getLong());
                } else if (defaultValue instanceof Double) {
                    return (T) ((Double) iterator.next().asLiteral().getDouble());
                }
            } catch (Exception e) {
                LOGGER.error("Exception while parsing parameter.");
            }
        } else {
            LOGGER.info("Couldn't get property '" + property + "' from the parameter model. Using '" + defaultValue + "' as a default value.");
            propertyValue = defaultValue;
        }
        return propertyValue;
    }
	
	
}