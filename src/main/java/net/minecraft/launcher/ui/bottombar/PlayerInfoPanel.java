package net.minecraft.launcher.ui.bottombar;

import net.minecraft.launcher.updater.VersionManager;
import java.util.List;
import com.mojang.authlib.UserAuthentication;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.profile.ProfileManager;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.LayoutManager;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import javax.swing.JPanel;

public class PlayerInfoPanel extends JPanel implements RefreshedProfilesListener, RefreshedVersionsListener
{
    private final Launcher launcher;
    private final JLabel welcomeText;
    private final JLabel versionText;
    private final JButton logOutButton;
    
    public PlayerInfoPanel(final Launcher launcher) {
        super();
        this.welcomeText = new JLabel("", 0);
        this.versionText = new JLabel("", 0);
        this.logOutButton = new JButton("Log Out");
        this.launcher = launcher;
        launcher.getProfileManager().addRefreshedProfilesListener(this);
        this.checkState();
        this.createInterface();
        this.logOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                launcher.getProfileManager().getSelectedProfile().setPlayerUUID(null);
                launcher.getProfileManager().trimAuthDatabase();
                launcher.showLoginPrompt();
            }
        });
    }
    
    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        this.add(this.welcomeText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        final GridBagConstraints gridBagConstraints = constraints;
        ++gridBagConstraints.gridy;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        this.add(this.versionText, constraints);
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        final GridBagConstraints gridBagConstraints2 = constraints;
        ++gridBagConstraints2.gridy;
        constraints.weightx = 0.5;
        constraints.fill = 0;
        this.add(this.logOutButton, constraints);
        constraints.weightx = 0.0;
        final GridBagConstraints gridBagConstraints3 = constraints;
        ++gridBagConstraints3.gridy;
    }
    
    @Override
    public void onProfilesRefreshed(final ProfileManager manager) {
        this.checkState();
    }
    
    public void checkState() {
        final Profile profile = this.launcher.getProfileManager().getProfiles().isEmpty() ? null : this.launcher.getProfileManager().getSelectedProfile();
        final UserAuthentication auth = (profile == null) ? null : this.launcher.getProfileManager().getAuthDatabase().getByUUID(profile.getPlayerUUID());
        final List<VersionSyncInfo> versions = (profile == null) ? null : this.launcher.getVersionManager().getVersions(profile.getVersionFilter());
        VersionSyncInfo version = (profile == null || versions.isEmpty()) ? null : ((VersionSyncInfo)versions.get(0));
        if (profile != null && profile.getLastVersionId() != null) {
            final VersionSyncInfo requestedVersion = this.launcher.getVersionManager().getVersionSyncInfo(profile.getLastVersionId());
            if (requestedVersion != null && requestedVersion.getLatestVersion() != null) {
                version = requestedVersion;
            }
        }
        if (auth == null || !auth.isLoggedIn()) {
            this.welcomeText.setText("Welcome, guest! Please log in.");
            this.logOutButton.setEnabled(false);
        }
        else if (auth.getSelectedProfile() == null) {
            this.welcomeText.setText("<html>Welcome, player!</html>");
            this.logOutButton.setEnabled(true);
        }
        else {
            this.welcomeText.setText("<html>Welcome, <b>" + auth.getSelectedProfile().getName() + "</b></html>");
            this.logOutButton.setEnabled(true);
        }
        if (version == null) {
            this.versionText.setText("Loading versions...");
        }
        else if (version.isUpToDate()) {
            this.versionText.setText("Ready to play Minecraft " + version.getLatestVersion().getId());
        }
        else if (version.isInstalled()) {
            this.versionText.setText("Ready to update & play Minecraft " + version.getLatestVersion().getId());
        }
        else if (version.isOnRemote()) {
            this.versionText.setText("Ready to download & play Minecraft " + version.getLatestVersion().getId());
        }
    }
    
    @Override
    public void onVersionsRefreshed(final VersionManager manager) {
        this.checkState();
    }
    
    @Override
    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
}
