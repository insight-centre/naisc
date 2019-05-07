package org.insightcentre.uld.naisc.launch;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * A launcher that starts Naisc anywhere
 *
 * @author John McCrae
 */
public class Launch {

    public static void startServer(JLabel label, int port) throws Exception {
        Server server = new Server(port);
        ResourceHandler resourceHandler = new ResourceHandler();

        // This is the path on the server
        // This is the local directory that is used to 
        resourceHandler.setResourceBase("static");
        if (!new File("static/index.html").exists()) {
            System.err.println("No static folder, please run the command in the right folder.");
            System.exit(-1);
        }
        //scontextHandler.setHandler(resourceHandler);
        HandlerList handlers = new HandlerList();
        /*Browser browser = new Browser(directory);
            Executor executor = new Executor(browser.saffron, directory, (File)os.valueOf("l"));
            NewRun welcome = new NewRun(executor);
            Home home = new Home(browser.saffron, directory);*/
        Handler handler = new ServletHandler();
        handlers.setHandlers(new Handler[]{handler,resourceHandler });
        server.setHandler(handlers);

        try {
            server.start();
        } catch (BindException x) {
            for (int i = port + 1; i < port + 20; i++) {
                try {
                    server.stop();
                    System.err.println(String.format("##### WARNING: Could not bind at port %d, incrementing to %d #####", port, i));
                    server = new Server(i);
                    server.setHandler(handlers);
                    server.start();
                    port = i;
                    break;
                } catch (BindException x2) {
                }

            }
        }
        // Get current size of heap in bytes
        String hostname = InetAddress.getLocalHost().getHostAddress();
        label.setText(String.format("Started server at http://localhost:%d/ (or http://%s:%d/)", port, hostname, port));
        server.join();
    }

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
    public static void main(String[] args) throws Exception {

        // Parse command line arguments
        final OptionParser p = new OptionParser() {
            {
                //accepts("d", "The directory containing the output or where to write the output to").withRequiredArg().ofType(File.class);
                accepts("p", "The port to run on").withRequiredArg().ofType(Integer.class);
                //accepts("l", "The log file").withOptionalArg().ofType(File.class);
            }
        };
        final OptionSet os;

        try {
            os = p.parse(args);
        } catch (Exception x) {
            badOptions(p, x.getMessage());
            return;
        }

        int port = os.valueOf("p") == null ? 8080 : (Integer) os.valueOf("p");
        
        JFrame frame = new JFrame("Naisc - Nearly Automatic Integration of Schema");
        frame.setLayout(new BorderLayout());
        frame.setSize(400, 200);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                System.exit(0);
            }
        });
        JLabel label = new JLabel("Welcome to Naisc");
        frame.add(label, BorderLayout.CENTER);
        JButton button = new JButton("Click me");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                label.setText("You touched me");
            }
        });
        frame.add(button, BorderLayout.SOUTH);
        frame.setVisible(true);
        startServer(label, port);

    }

}
