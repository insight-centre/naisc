package org.insightcentre.uld.naisc.meas;

import java.io.File;
import java.util.Arrays;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * A dataset that consists of
 * 
 * <ul>
 * <li>A left dataset</li>
 * <li>A right dataset</li>
 * <li>(Optional) A gold standard alignment</li>
 * <ul>
 * 
 * @author John McCrae
 */
public class Dataset {
    private final File folder;

    /**
     * Create a dataset from a folder
     * @param folder The folder contains left.rdf and right.rdf
     */
    public Dataset(File folder) {
        this.folder = folder;
        if(!folder.exists() && !folder.isDirectory())
            throw new DatasetException("Dataset folder not available");
    }
    
    /**
     * The left dataset
     * @return The left dataset file
     * @throws DatasetException If no left dataset is available
     */
    public File left() {
        for(String suffix : Arrays.asList(".rdf",".nt",".ttl",".xml")) {
            final File f = new File(folder, "left" + suffix);
            if(f.exists())
                return f;
        }
        throw new DatasetException("No left file");
    }
    
    /**
     * The right dataset
     * @return The right dataset file
     * @throws DatasetException If no right dataset is available
     */
    public File right() {
        for(String suffix : Arrays.asList(".rdf",".nt",".ttl",".xml")) {
            final File f = new File(folder, "right" + suffix);
            if(f.exists())
                return f;
        }
        throw new DatasetException("No left file");
    }
    
    /**
     * Get the alignment file
     * @return Some if the file exists or none if there is no alignment in this dataset
     */
    public Option<File> align() {
        for(String suffix : Arrays.asList(".rdf",".nt",".ttl",".xml")) {
            final File f = new File(folder, "align" + suffix);
            if(f.exists())
                return new Some<>(f);
        }
        return new None<>();
    }

    /**
     * Checks if the required dataset files are present
     * @param f The folder to check
     * @return True if this folder is a dataset
     */
    public static boolean isDataset(File f) {
        return f.exists() && f.isDirectory() &&
                (new File(f, "left.rdf").exists() || new File(f, "left.nt").exists() || new File(f, "left.ttl").exists() || new File(f, "left.xml").exists()) &&
                (new File(f, "right.rdf").exists() || new File(f, "right.nt").exists() || new File(f, "right.ttl").exists() || new File(f, "right.xml").exists());
    }
}
