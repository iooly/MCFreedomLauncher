package net.minecraft.launcher.ui.bottombar;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.apache.logging.log4j.LogManager;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.awt.event.ItemEvent;
import java.util.Iterator;
import java.util.Collection;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.awt.Component;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.LayoutManager;
import java.awt.GridBagLayout;
import javax.swing.ListCellRenderer;
import net.minecraft.launcher.Launcher;
import javax.swing.JButton;
import javax.swing.JComboBox;
import org.apache.logging.log4j.Logger;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import javax.swing.JPanel;

public class ProfileSelectionPanel extends JPanel implements ActionListener, ItemListener, RefreshedProfilesListener
{
    private static final Logger LOGGER;
    private final JComboBox profileList;
    private final JButton newProfileButton;
    private final JButton editProfileButton;
    private final Launcher launcher;
    private boolean skipSelectionUpdate;
    
    public ProfileSelectionPanel(final Launcher launcher) {
        super();
        this.profileList = new JComboBox();
        this.newProfileButton = new JButton("New Profile");
        this.editProfileButton = new JButton("Edit Profile");
        this.launcher = launcher;
        this.profileList.setRenderer(new ProfileListRenderer());
        this.profileList.addItemListener(this);
        this.profileList.addItem("Loading profiles...");
        this.newProfileButton.addActionListener(this);
        this.editProfileButton.addActionListener(this);
        this.createInterface();
        launcher.getProfileManager().addRefreshedProfilesListener(this);
    }
    
    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.weightx = 0.0;
        constraints.gridy = 0;
        this.add(new JLabel("Profile: "), constraints);
        constraints.gridx = 1;
        this.add(this.profileList, constraints);
        constraints.gridx = 0;
        final GridBagConstraints gridBagConstraints = constraints;
        ++gridBagConstraints.gridy;
        final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
        buttonPanel.add(this.newProfileButton);
        buttonPanel.add(this.editProfileButton);
        constraints.gridwidth = 2;
        this.add(buttonPanel, constraints);
        constraints.gridwidth = 1;
        final GridBagConstraints gridBagConstraints2 = constraints;
        ++gridBagConstraints2.gridy;
    }
    
    @Override
    public void onProfilesRefreshed(final ProfileManager manager) {
        this.populateProfiles();
    }
    
    @Override
    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
    
    public void populateProfiles() {
        final String previous = this.launcher.getProfileManager().getSelectedProfile().getName();
        Profile selected = null;
        final Collection<Profile> profiles = this.launcher.getProfileManager().getProfiles().values();
        this.profileList.removeAllItems();
        this.skipSelectionUpdate = true;
        for (final Profile profile : profiles) {
            if (previous.equals(profile.getName())) {
                selected = profile;
            }
            this.profileList.addItem(profile);
        }
        if (selected == null) {
            if (profiles.isEmpty()) {
                selected = this.launcher.getProfileManager().getSelectedProfile();
                this.profileList.addItem(selected);
            }
            selected = profiles.iterator().next();
        }
        this.profileList.setSelectedItem(selected);
        this.skipSelectionUpdate = false;
    }
    
    @Override
    public void itemStateChanged(final ItemEvent e) {
        if (e.getStateChange() != 1) {
            return;
        }
        if (!this.skipSelectionUpdate && e.getItem() instanceof Profile) {
            final Profile profile = (Profile)e.getItem();
            this.launcher.getProfileManager().setSelectedProfile(profile.getName());
            try {
                this.launcher.getProfileManager().saveProfiles();
            }
            catch (IOException e2) {
                ProfileSelectionPanel.LOGGER.error("Couldn't save new selected profile", e2);
            }
            this.launcher.ensureLoggedIn();
        }
    }
    
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.newProfileButton) {
            final Profile profile = new Profile(this.launcher.getProfileManager().getSelectedProfile());
            profile.setName("Copy of " + profile.getName());
            while (this.launcher.getProfileManager().getProfiles().containsKey(profile.getName())) {
                profile.setName(profile.getName() + "_");
            }
            ProfileEditorPopup.showEditProfileDialog(this.getLauncher(), profile);
            this.launcher.getProfileManager().setSelectedProfile(profile.getName());
        }
        else if (e.getSource() == this.editProfileButton) {
            final Profile profile = this.launcher.getProfileManager().getSelectedProfile();
            ProfileEditorPopup.showEditProfileDialog(this.getLauncher(), profile);
        }
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
    
    private static class ProfileListRenderer extends BasicComboBoxRenderer
    {
        @Override
        public Component getListCellRendererComponent(final JList list, Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            if (value instanceof Profile) {
                value = ((Profile)value).getName();
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }
}
