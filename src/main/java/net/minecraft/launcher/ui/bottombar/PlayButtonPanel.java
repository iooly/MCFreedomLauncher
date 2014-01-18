package net.minecraft.launcher.ui.bottombar;

import net.minecraft.launcher.updater.VersionManager;
import com.mojang.authlib.UserAuthentication;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.LayoutManager;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import javax.swing.JPanel;

public class PlayButtonPanel extends JPanel implements RefreshedProfilesListener, RefreshedVersionsListener
{
    private final Launcher launcher;
    private final JButton playButton;
    
    public PlayButtonPanel(final Launcher launcher) {
        super();
        this.playButton = new JButton("Play");
        this.launcher = launcher;
        launcher.getProfileManager().addRefreshedProfilesListener(this);
        this.checkState();
        this.createInterface();
        this.playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                PlayButtonPanel.this.getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
                    @Override
                    public void run() {
                        PlayButtonPanel.this.getLauncher().getGameLauncher().playGame();
                    }
                });
            }
        });
    }
    
    protected void createInterface() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridy = 0;
        constraints.gridx = 0;
        this.add(this.playButton, constraints);
        this.playButton.setFont(this.playButton.getFont().deriveFont(1, this.playButton.getFont().getSize() + 2));
    }
    
    @Override
    public void onProfilesRefreshed(final ProfileManager manager) {
        this.checkState();
    }
    
    public void checkState() {
        final Profile profile = this.launcher.getProfileManager().getProfiles().isEmpty() ? null : this.launcher.getProfileManager().getSelectedProfile();
        final UserAuthentication auth = (profile == null) ? null : this.launcher.getProfileManager().getAuthDatabase().getByUUID(profile.getPlayerUUID());
        if (auth == null || !auth.isLoggedIn() || this.launcher.getVersionManager().getVersions(profile.getVersionFilter()).isEmpty()) {
            this.playButton.setEnabled(false);
            this.playButton.setText("Play");
        }
        else if (auth.getSelectedProfile() == null) {
            this.playButton.setEnabled(true);
            this.playButton.setText("Play Demo");
        }
        else if (auth.canPlayOnline()) {
            this.playButton.setEnabled(true);
            this.playButton.setText("Play");
        }
        else {
            this.playButton.setEnabled(true);
            this.playButton.setText("Play Offline");
        }
        if (this.launcher.getGameLauncher().isWorking()) {
            this.playButton.setEnabled(false);
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
