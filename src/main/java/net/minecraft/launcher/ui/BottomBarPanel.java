package net.minecraft.launcher.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.GridLayout;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import net.minecraft.launcher.ui.bottombar.PlayButtonPanel;
import net.minecraft.launcher.ui.bottombar.PlayerInfoPanel;
import net.minecraft.launcher.ui.bottombar.ProfileSelectionPanel;
import net.minecraft.launcher.Launcher;
import javax.swing.JPanel;

public class BottomBarPanel extends JPanel
{
    private final Launcher launcher;
    private final ProfileSelectionPanel profileSelectionPanel;
    private final PlayerInfoPanel playerInfoPanel;
    private final PlayButtonPanel playButtonPanel;
    
    public BottomBarPanel(final Launcher launcher) {
        super();
        this.launcher = launcher;
        final int border = 4;
        this.setBorder(new EmptyBorder(border, border, border, border));
        this.profileSelectionPanel = new ProfileSelectionPanel(launcher);
        this.playerInfoPanel = new PlayerInfoPanel(launcher);
        this.playButtonPanel = new PlayButtonPanel(launcher);
        this.createInterface();
    }
    
    protected void createInterface() {
        this.setLayout(new GridLayout(1, 3));
        this.add(this.wrapSidePanel(this.profileSelectionPanel, 17));
        this.add(this.playButtonPanel);
        this.add(this.wrapSidePanel(this.playerInfoPanel, 13));
    }
    
    protected JPanel wrapSidePanel(final JPanel target, final int side) {
        final JPanel wrapper = new JPanel(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = side;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        wrapper.add(target, constraints);
        return wrapper;
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    public ProfileSelectionPanel getProfileSelectionPanel() {
        return this.profileSelectionPanel;
    }
    
    public PlayerInfoPanel getPlayerInfoPanel() {
        return this.playerInfoPanel;
    }
    
    public PlayButtonPanel getPlayButtonPanel() {
        return this.playButtonPanel;
    }
}
