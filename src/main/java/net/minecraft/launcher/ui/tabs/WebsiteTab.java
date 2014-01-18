package net.minecraft.launcher.ui.tabs;

import org.apache.logging.log4j.LogManager;
import java.net.URL;
import java.awt.Component;
import net.minecraft.launcher.OperatingSystem;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Color;
import java.awt.Insets;
import net.minecraft.launcher.Launcher;
import javax.swing.JTextPane;
import org.apache.logging.log4j.Logger;
import javax.swing.JScrollPane;

public class WebsiteTab extends JScrollPane
{
    private static final Logger LOGGER;
    private final JTextPane blog;
    private final Launcher launcher;
    
    public WebsiteTab(final Launcher launcher) {
        super();
        this.blog = new JTextPane();
        this.launcher = launcher;
        this.blog.setEditable(false);
        this.blog.setMargin(null);
        this.blog.setBackground(Color.DARK_GRAY);
        this.blog.setContentType("text/html");
        this.blog.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Loading page..</h1></center></font></body></html>");
        this.blog.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(final HyperlinkEvent he) {
                if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        OperatingSystem.openLink(he.getURL().toURI());
                    }
                    catch (Exception e) {
                        WebsiteTab.LOGGER.error("Unexpected exception opening link " + he.getURL(), e);
                    }
                }
            }
        });
        this.setViewportView(this.blog);
    }
    
    public void setPage(final String url) {
        final Thread thread = new Thread("Update website tab") {
            @Override
            public void run() {
                try {
                    WebsiteTab.this.blog.setPage(new URL(url));
                }
                catch (Exception e) {
                    WebsiteTab.LOGGER.error("Unexpected exception loading " + url, e);
                    WebsiteTab.this.blog.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Failed to get page</h1><br>" + e.toString() + "</center></font></body></html>");
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
