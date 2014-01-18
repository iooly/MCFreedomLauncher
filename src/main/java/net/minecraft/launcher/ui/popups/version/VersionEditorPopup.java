package net.minecraft.launcher.ui.popups.version;

import org.apache.logging.log4j.LogManager;
import java.awt.Frame;
import javax.swing.JDialog;
import java.awt.AWTEvent;
import java.awt.event.WindowEvent;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import java.awt.Component;
import java.awt.Container;
import javax.swing.BoxLayout;
import java.awt.LayoutManager;
import java.awt.BorderLayout;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.Logger;
import java.awt.event.ActionListener;
import javax.swing.JPanel;

public class VersionEditorPopup extends JPanel implements ActionListener
{
    private static final Logger LOGGER;
    private final Launcher launcher;
    private final CompleteVersion originalVersion;
    private final CompleteVersion version;
    private final JButton saveButton;
    private final JButton cancelButton;
    private final JButton browseButton;
    private final VersionInfoPanel versionInfoPanel;
    private final VersionLaunchInfoPanel versionLaunchInfoPanel;
    private final VersionRequirementsPanel versionRequirementsPanel;
    private final VersionLibrariesPanel versionLibrariesPanel;
    
    public VersionEditorPopup(final Launcher launcher, final CompleteVersion version) {
        super(true);
        this.saveButton = new JButton("Save Version");
        this.cancelButton = new JButton("Cancel");
        this.browseButton = new JButton("Open Version Dir");
        this.launcher = launcher;
        this.originalVersion = version;
        this.version = new CompleteVersion(version);
        this.versionInfoPanel = new VersionInfoPanel(this);
        this.versionLaunchInfoPanel = new VersionLaunchInfoPanel(this);
        this.versionRequirementsPanel = new VersionRequirementsPanel(this);
        this.versionLibrariesPanel = new VersionLibrariesPanel(this);
        this.saveButton.setEnabled(false);
        this.browseButton.setEnabled(false);
        this.saveButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        this.browseButton.addActionListener(this);
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.setLayout(new BorderLayout(0, 5));
        this.createInterface();
    }
    
    protected void createInterface() {
        final JPanel standardPanels = new JPanel(true);
        standardPanels.setLayout(new BoxLayout(standardPanels, 1));
        standardPanels.add(this.versionInfoPanel);
        standardPanels.add(this.versionLaunchInfoPanel);
        standardPanels.add(this.versionRequirementsPanel);
        standardPanels.add(this.versionLibrariesPanel);
        this.add(standardPanels, "Center");
        final JPanel buttonPannel = new JPanel();
        buttonPannel.setLayout(new BoxLayout(buttonPannel, 0));
        buttonPannel.add(this.cancelButton);
        buttonPannel.add(Box.createGlue());
        buttonPannel.add(this.browseButton);
        buttonPannel.add(Box.createHorizontalStrut(5));
        buttonPannel.add(this.saveButton);
        this.add(buttonPannel, "South");
    }
    
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.saveButton) {
            this.closeWindow();
        }
        else if (e.getSource() != this.browseButton) {
            this.closeWindow();
        }
    }
    
    private void closeWindow() {
        final Window window = (Window)this.getTopLevelAncestor();
        window.dispatchEvent(new WindowEvent(window, 201));
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    public CompleteVersion getVersion() {
        return this.version;
    }
    
    public static void showEditVersionDialog(final Launcher launcher, final CompleteVersion version) {
        final JDialog dialog = new JDialog(launcher.getFrame(), "Version Editor", true);
        final VersionEditorPopup editor = new VersionEditorPopup(launcher, version);
        dialog.add(editor);
        dialog.pack();
        dialog.setLocationRelativeTo(launcher.getFrame());
        dialog.setVisible(true);
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
