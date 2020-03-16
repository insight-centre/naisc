package org.insightcentre.uld.naisc.meas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Methods that download appropriate datasets
 * @author John McCrae
 */
public class Install {
    
    private static void verifyOrDownload(String name) throws IOException {
        if(!new File("models").exists()) {
            new File("models").mkdir();
        }
        if(!new File("models/" + name).exists()) {
            System.err.println("Downloading resource: " + name);
            URL website = new URL("http://server1.nlp.insight-centre.org/naisc-models/" + name + ".gz");
            ReadableByteChannel rbc = Channels.newChannel(new GZIPInputStream(website.openStream()));
            FileOutputStream fos = new FileOutputStream("models/" + name);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }
    
    private static void getConfigs() throws IOException {
        if(!new File("configs").exists()) {
            new File("configs").mkdir();
            System.err.println("Downloading configurations");
            URL website = new URL("http://server1.nlp.insight-centre.org/naisc-models/configs.zip");
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream("configs.zip");
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            unzip("configs.zip", "configs");
            new File("configs.zip").delete();
            
        }
    }
    
    private static void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        if(!dir.exists()) dir.mkdirs();
        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipFilePath)) {
            try (ZipInputStream zis = new ZipInputStream(fis)) {
                ZipEntry ze = zis.getNextEntry();
                while(ze != null){
                    String fileName = ze.getName();
                    if(fileName.startsWith("configs/")) {
                        fileName = fileName.substring(8);
                    }
                    if (fileName.equals("")) {
                        ze = zis.getNextEntry();
                        continue;
                    }
                    File newFile = new File(destDir + File.separator + fileName);
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    public static void verify() throws IOException {
        getConfigs();
        verifyOrDownload("idf");
        verifyOrDownload("ngidf");
        verifyOrDownload("glove.6B.100d.txt");
        verifyOrDownload("jaccard.libsvm");
        verifyOrDownload("basic.libsvm");
        verifyOrDownload("default.libsvm");
        
    }

}
