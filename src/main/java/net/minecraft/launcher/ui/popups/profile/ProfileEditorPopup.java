package net.minecraft.launcher.ui.popups.profile;

import org.apache.logging.log4j.LogManager;
import java.awt.Frame;
import javax.swing.JDialog;
import java.awt.AWTEvent;
import java.awt.event.WindowEvent;
import java.awt.Window;
import java.util.Map;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.OperatingSystem;
import java.io.IOException;
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
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.Logger;
import java.awt.event.ActionListener;
import javax.swing.JPanel;

public class ProfileEditorPopup extends JPanel implements ActionListener
{
    private static final Logger LOGGER;
    private final Launcher launcher;
    private final Profile originalProfile;
    private final Profile profile;
    private final JButton saveButton;
    private final JButton cancelButton;
    private final JButton browseButton;
    private final ProfileInfoPanel profileInfoPanel;
    private final ProfileVersionPanel profileVersionPanel;
    private final ProfileJavaPanel javaInfoPanel;
    
    public ProfileEditorPopup(final Launcher launcher, final Profile profile) {
        super(true);
        this.saveButton = new JButton("Save Profile");
        this.cancelButton = new JButton("Cancel");
        this.browseButton = new JButton("Open Game Dir");
        this.launcher = launcher;
        this.originalProfile = profile;
        this.profile = new Profile(profile);
        this.profileInfoPanel = new ProfileInfoPanel(this);
        this.profileVersionPanel = new ProfileVersionPanel(this);
        this.javaInfoPanel = new ProfileJavaPanel(this);
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
        standardPanels.add(this.profileInfoPanel);
        standardPanels.add(this.profileVersionPanel);
        standardPanels.add(this.javaInfoPanel);
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
            try {
                final ProfileManager manager = this.launcher.getProfileManager();
                final Map<String, Profile> profiles = manager.getProfiles();
                final String selected = manager.getSelectedProfile().getName();
                if (!this.originalProfile.getName().equals(this.profile.getName())) {
                    profiles.remove(this.originalProfile.getName());
                    while (profiles.containsKey(this.profile.getName())) {
                        this.profile.setName(this.profile.getName() + "_");
                    }
                }
                profiles.put(this.profile.getName(), this.profile);
                if (selected.equals(this.originalProfile.getName())) {
                    manager.setSelectedProfile(this.profile.getName());
                }
                manager.saveProfiles();
                manager.fireRefreshEvent();
            }
            catch (IOException ex) {
                ProfileEditorPopup.LOGGER.error("Couldn't save profiles whilst editing " + this.profile.getName(), ex);
            }
            this.closeWindow();
        }
        else if (e.getSource() == this.browseButton) {
            OperatingSystem.openFolder((this.profile.getGameDir() == null) ? this.launcher.getWorkingDirectory() : this.profile.getGameDir());
        }
        else {
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
    
    public Profile getProfile() {
        return this.profile;
    }
    
    public static void showEditProfileDialog(final Launcher launcher, final Profile profile) {
        final JDialog dialog = new JDialog(launcher.getFrame(), "Profile Editor", true);
        final ProfileEditorPopup editor = new ProfileEditorPopup(launcher, profile);
        dialog.add(editor);
        dialog.pack();
        dialog.setLocationRelativeTo(launcher.getFrame());
        dialog.setVisible(true);
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
