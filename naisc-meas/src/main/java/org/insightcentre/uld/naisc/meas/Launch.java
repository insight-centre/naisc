package org.insightcentre.uld.naisc.meas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.Configuration;

public class Launch {

    private static final String WEBROOT_INDEX = "/webapp/";

    private final int port;

    public Launch(int port) {
        this.port = port;
    }

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("p", "Port to start the server on").withRequiredArg().ofType(Integer.class);
                    accepts("b", "The Base URL of the server").withRequiredArg().ofType(String.class);
                }
            };
            
            
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            
            int port = os.valueOf("p") == null ? 8080 : (Integer)os.valueOf("p");

            if(os.has("b")) {
                System.setProperty("base.url", os.valueOf("b").toString());
            }
            
            Install.verify();
            Launch main = new Launch(port);
            main.start();
            System.err.println("Naisc started at http://localhost:" + port + "/");
            main.waitForInterrupt();
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }
    }

    private Server server;

    public void start() throws Exception {
        server = new Server();

        // Define ServerConnector
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        // Add annotation scanning (for WebAppContexts)
        Configuration.ClassList classlist = Configuration.ClassList
                .setServerDefault(server);
        classlist.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");

        // Base URI for servlet context
        URI baseUri = getWebRootResourceUri();
        //LOG.info("Base URI: " + baseUri);

        // Create Servlet context
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");
        servletContextHandler.setResourceBase(baseUri.toASCIIString());

        // Since this is a ServletContextHandler we must manually configure JSP support.
        enableEmbeddedJspSupport(servletContextHandler);

        // Add Application Servlets
        servletContextHandler.addServlet(DatasetUploadServlet.class, "/upload_dataset");
        servletContextHandler.addServlet(DatasetDownloadServlet.class, "/download_dataset");
        servletContextHandler.addServlet(ExecuteServlet.class, "/execute/*");
        servletContextHandler.addServlet(ManageServlet.class, "/manage/*");
        servletContextHandler.addServlet(SPARQLEndpointServlet.class, "/sparql/*");
        // Create Example of mapping jsp to path spec
        ServletHolder holderAltMapping = new ServletHolder();
        holderAltMapping.setName("foo.jsp");
        holderAltMapping.setForcedPath("/test/foo/foo.jsp");
        servletContextHandler.addServlet(holderAltMapping, "/test/foo/");

        // Default Servlet (always last, always named "default")
        ServletHolder holderDefault = new ServletHolder("default", DefaultServlet.class);
        holderDefault.setInitParameter("resourceBase", baseUri.toASCIIString());
        holderDefault.setInitParameter("dirAllowed", "true");
        servletContextHandler.addServlet(holderDefault, "/");
        server.setHandler(servletContextHandler);

        // Start Server
        server.start();

    }

    /**
     * Setup JSP Support for ServletContextHandlers.
     * <p>
     * NOTE: This is not required or appropriate if using a WebAppContext.
     * </p>
     *
     * @param servletContextHandler the ServletContextHandler to configure
     * @throws IOException if unable to configure
     */
    private void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException {
        // Establish Scratch directory for the servlet context (used by JSP compilation)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "embedded-jetty-jsp");

        if (!scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);

        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        servletContextHandler.setClassLoader(jspClassLoader);

        // Manually call JettyJasperInitializer on context startup
        servletContextHandler.addBean(new JspStarter(servletContextHandler));

        // Create / Register JSP Servlet (must be named "jsp" per spec)
        ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
        holderJsp.setInitParameter("fork", "false");
        holderJsp.setInitParameter("xpoweredBy", "false");
        holderJsp.setInitParameter("compilerTargetVM", "1.8");
        holderJsp.setInitParameter("compilerSourceVM", "1.8");
        holderJsp.setInitParameter("keepgenerated", "true");
        servletContextHandler.addServlet(holderJsp, "*.jsp");
    }

    public static class JspStarter extends AbstractLifeCycle implements ServletContextHandler.ServletContainerInitializerCaller {

        JettyJasperInitializer sci;
        ServletContextHandler context;

        public JspStarter(ServletContextHandler context) {
            this.sci = new JettyJasperInitializer();
            this.context = context;
            this.context.setAttribute("org.apache.tomcat.JarScanner", new StandardJarScanner());
        }

        @Override
        protected void doStart() throws Exception {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(context.getClassLoader());
            try {
                sci.onStartup(null, context.getServletContext());
                super.doStart();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }

    private URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException {
        URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
        if (indexUri == null) {
            throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
        }
        // Points to wherever /webroot/ (the resource) is
        return indexUri.toURI();
    }

    public void stop() throws Exception {
        server.stop();
    }

    /**
     * Cause server to keep running until it receives a Interrupt.
     * <p>
     * Interrupt Signal, or SIGINT (Unix Signal), is typically seen as a result
     * of a kill -TERM {pid} or Ctrl+C
     *
     * @throws InterruptedException if interrupted
     */
    public void waitForInterrupt() throws InterruptedException {
        server.join();
    }
}
