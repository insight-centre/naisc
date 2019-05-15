package org.insightcentre.uld.naisc.meas;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author John McCrae
 */
public class DatasetDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String datasetName = req.getParameter("dataset");
        if (datasetName != null && datasetName.matches("[A-Za-z\\-_]+")) {
            try {
                File directory = new File(new File("datasets"), datasetName);
                if (!directory.mkdirs() && !directory.exists() && !directory.isDirectory()) {
                    throw new ServletException("Could not create directoy");
                }
                downloadRDF("http://server1.nlp.insight-centre.org/naisc-datasets/" + datasetName + "/left",
                        directory, "left");
                downloadRDF("http://server1.nlp.insight-centre.org/naisc-datasets/" + datasetName + "/right",
                        directory, "right");
                try {
                    downloadRDF("http://server1.nlp.insight-centre.org/naisc-datasets/" + datasetName + "/align",
                            directory, "align");
                } catch(IOException x) {
                    // Ignore: not all datasets have a gold standard
                }
                try {
                    downloadURL("http://server1.nlp.insight-centre.org/naisc-datasets/" + datasetName + "/blocks.nt",
                            new File(directory, "blocks.nt"));
                } catch(IOException x) {
                    // Ignore: not all datasets have a blocking
                }
                Meas.data.datasetNames.add(datasetName);
            } catch(IOException x) {
                throw new ServletException(x);
            }
        }
    }

    private void downloadRDF(String urlBase, File directory, String name) throws IOException {
        try {
            downloadURL(urlBase + ".nt", new File(directory, name + ".nt"));
        } catch (IOException x) {
            try {
                downloadURL(urlBase + ".rdf", new File(directory, name + ".rdf"));
            } catch (IOException x2) {
                try {
                    downloadURL(urlBase + ".xml", new File(directory, name + ".xml"));
                } catch (IOException x3) {
                    downloadURL(urlBase + ".ttl", new File(directory, name + ".ttl"));
                }
            }
        }
    }

    private void downloadURL(String url, File file) throws IOException {
        System.err.println("Downloading " + url);
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }

}
