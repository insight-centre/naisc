package org.insightcentre.uld.naisc.meas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author John McCrae
 */
public class DatasetUploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Configure a repository (to ensure a secure temp location is used)
        ServletContext servletContext = this.getServletConfig().getServletContext();
        File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        factory.setRepository(repository);

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        try {
            // Parse the request
            List<FileItem> items = upload.parseRequest(request);
            String datasetName = null;
            File leftFile = null, rightFile = null, alignFile = null;
            String leftSuffix = ".rdf", rightSuffix = ".rdf";
            for (FileItem item : items) {
                switch (item.getFieldName()) {
                    case "name":
                        datasetName = item.getString();
                        if (!datasetName.matches("[A-Za-z][A-Za-z0-9-_]*")) {
                            throw new ServletException("Bad input for dataset name");
                        }
                        break;
                    case "left":
                        leftFile = File.createTempFile("left", leftSuffix = guessSuffix(item));
                        leftFile.deleteOnExit();
                        writeFile(leftFile, item.getInputStream());
                        break;
                    case "right":
                        rightFile = File.createTempFile("right", rightSuffix = guessSuffix(item));
                        rightFile.deleteOnExit();
                        writeFile(rightFile, item.getInputStream());
                        break;
                    case "align":
                        if (item.getName() != null && !item.getName().equals("")) {
                            alignFile = File.createTempFile("align", ".rdf");
                            alignFile.deleteOnExit();
                            writeFile(alignFile, item.getInputStream());
                        }
                        break;
                    default:
                        System.err.println("Not recognized: " + item.getFieldName());
                        break;
                }
            }
            if (datasetName != null && leftFile != null && rightFile != null) {
                File directory = new File(new File("datasets"), datasetName);
                if(directory.exists() && directory.isDirectory()) {
                    // Remove any existing files
                    for(File f : directory.listFiles()) {
                        f.delete();
                    }
                }
                if (!directory.mkdirs() && !directory.exists() && !directory.isDirectory()) {
                    throw new ServletException("Could not create directoy");
                }
                Files.move(leftFile.toPath(), new File(directory, "left" + leftSuffix).toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.move(rightFile.toPath(), new File(directory, "right" + rightSuffix).toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (alignFile != null) {
                    Files.move(alignFile.toPath(), new File(directory, "align.rdf").toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                if (leftFile != null) {
                    leftFile.delete();
                }
                if (rightFile != null) {
                    rightFile.delete();
                }
                if (alignFile != null) {
                    alignFile.delete();
                }
                Meas.data.datasetNames.add(datasetName);
            }
        } catch (FileUploadException x) {
            throw new ServletException(x);
        }
    }

    private String guessSuffix(FileItem item) {
        String suffix = ".rdf";
        if(item.getName().endsWith(".nt")) {
            suffix = ".nt";
        } else if(item.getName().endsWith(".xml")) {
            suffix = ".xml";
        } else if(item.getName().endsWith(".ttl")) {
            suffix = ".ttl";
        }
        return suffix;
    }

    private static int BUF_SIZE = 4096;

    private void writeFile(File file, InputStream inputStream) throws IOException {

        try (FileOutputStream out = new FileOutputStream(file)) {
            byte[] buf = new byte[BUF_SIZE];
            int read;
            while ((read = inputStream.read(buf)) >= 0) {
                out.write(buf, 0, read);
            }
        }
    }

}
