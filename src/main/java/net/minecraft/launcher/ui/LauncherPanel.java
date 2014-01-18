package net.minecraft.launcher.ui;

import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import net.minecraft.launcher.Launcher;
import javax.swing.JProgressBar;
import net.minecraft.launcher.ui.tabs.LauncherTabPanel;
import java.awt.CardLayout;
import javax.swing.JPanel;

public class LauncherPanel extends JPanel
{
    public static final String CARD_DIRT_BACKGROUND = "loading";
    public static final String CARD_LOGIN = "login";
    public static final String CARD_LAUNCHER = "launcher";
    private final CardLayout cardLayout;
    private final LauncherTabPanel tabPanel;
    private final BottomBarPanel bottomBar;
    private final JProgressBar progressBar;
    private final Launcher launcher;
    private final JPanel loginPanel;
    
    public LauncherPanel(final Launcher launcher) {
        super();
        this.launcher = launcher;
        this.setLayout(this.cardLayout = new CardLayout());
        this.progressBar = new JProgressBar();
        this.bottomBar = new BottomBarPanel(launcher);
        this.tabPanel = new LauncherTabPanel(launcher);
        this.loginPanel = new TexturedPanel("/dirt.png");
        this.createInterface();
    }
    
    protected void createInterface() {
        this.add(this.createLauncherInterface(), "launcher");
        this.add(this.createDirtInterface(), "loading");
        this.add(this.createLoginInterface(), "login");
    }
    
    protected JPanel createLauncherInterface() {
        final JPanel result = new JPanel(new BorderLayout());
        this.tabPanel.getBlog().setPage("http://mcupdate.tumblr.com");
        final JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BorderLayout());
        topWrapper.add(this.tabPanel, "Center");
        topWrapper.add(this.progressBar, "South");
        this.progressBar.setVisible(false);
        this.progressBar.setMinimum(0);
        this.progressBar.setMaximum(100);
        result.add(topWrapper, "Center");
        result.add(this.bottomBar, "South");
        return result;
    }
    
    protected JPanel createDirtInterface() {
        return new TexturedPanel("/dirt.png");
    }
    
    protected JPanel createLoginInterface() {
        this.loginPanel.setLayout(new GridBagLayout());
        return this.loginPanel;
    }
    
    public LauncherTabPanel getTabPanel() {
        return this.tabPanel;
    }
    
    public BottomBarPanel getBottomBar() {
        return this.bottomBar;
    }
    
    public JProgressBar getProgressBar() {
        return this.progressBar;
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    public void setCard(final String card, final JPanel additional) {
        if (card.equals("login")) {
            this.loginPanel.removeAll();
            this.loginPanel.add(additional);
        }
        this.cardLayout.show(this, card);
    }
}
