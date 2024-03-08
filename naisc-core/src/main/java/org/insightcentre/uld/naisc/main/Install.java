package org.insightcentre.uld.naisc.main;

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
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Methods that download appropriate datasets
 * @author John McCrae
 */
public class Install {
    
    private static List<String> verifyOrDownload(String name) throws IOException {
        try {
            if(!new File("models").exists()) {
                new File("models").mkdir();
            }
            if(!new File("models/" + name).exists()) {
                System.err.println("Downloading resource: " + name);
                URL website = new URL("https://server1.nlp.insight-centre.org/naisc-models/" + name + ".gz");
                ReadableByteChannel rbc = Channels.newChannel(new GZIPInputStream(website.openStream()));
                try(FileOutputStream fos = new FileOutputStream("models/" + name)) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                }
            }
            return Collections.emptyList();
        } catch(IOException x) {
            x.printStackTrace();
            return Collections.singletonList(name);
       }

    }
    
    private static void getConfigs() throws IOException {
        try {
            if(!new File("configs").exists()) {
                new File("configs").mkdir();
                System.err.println("Downloading configurations");
                URL website = new URL("https://server1.nlp.insight-centre.org/naisc-models/configs.zip");
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                try(FileOutputStream fos = new FileOutputStream("configs.zip")) {
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    unzip("configs.zip", "configs");
                    new File("configs.zip").delete();
                }

            }
        } catch(IOException x) {
            throw new IOException("Could not download a resource. You can fix this by manually downloading and installing it\n" +
                    "For example on a Linux machine use the following commands:\n" +
                    "    wget https://server1.nlp.insight-centre.org/naisc-models/configs.zip\n" +
                    "    unzip configs.zip -d configs/\n", x);
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
        if(System.getProperty("naisc.skip.install") != null)
            return;
        getConfigs();
        List<String> missing = new ArrayList<>();
        missing.addAll(verifyOrDownload("idf"));
        missing.addAll(verifyOrDownload("ngidf"));
        missing.addAll(verifyOrDownload("glove.6B.100d.txt"));
        missing.addAll(verifyOrDownload("jaccard.libsvm"));
        missing.addAll(verifyOrDownload("basic.libsvm"));
        missing.addAll(verifyOrDownload("default.libsvm"));
        if(!missing.isEmpty()) {
            System.err.println("Could not download a resource. You can fix this by manually downloading and installing it\n" +
            "For example on a Linux machine use the following commands:");
            for (String name : missing) {
                System.err.println("    wget https://server1.nlp.insight-centre.org/naisc-models/" + name + ".gz");
            }
            for (String name : missing) {                System.err.println("    gunzip " + name + ".gz");
                System.err.println("    mv " + name + " models/");
            }
            for (String name : missing) {
                System.err.println("    rm " + name + ".gz");
            }
            throw new IOException("Missing resources: " + missing);
        }
    }

}
