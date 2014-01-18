package net.minecraft.launcher.ui.tabs;

import java.util.Collection;
import com.mojang.authlib.UserAuthentication;
import net.minecraft.launcher.authentication.AuthenticationDatabase;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.logging.log4j.LogManager;
import net.minecraft.launcher.profile.ProfileManager;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.ui.popups.profile.ProfileEditorPopup;
import net.minecraft.launcher.profile.Profile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Component;
import javax.swing.table.TableModel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.Logger;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import javax.swing.JScrollPane;

public class ProfileListTab extends JScrollPane implements RefreshedProfilesListener
{
    private static final Logger LOGGER;
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_VERSION = 1;
    private static final int COLUMN_AUTHENTICATION = 2;
    private static final int NUM_COLUMNS = 3;
    private final Launcher launcher;
    private final ProfileTableModel dataModel;
    private final JTable table;
    private final JPopupMenu popupMenu;
    private final JMenuItem addProfileButton;
    private final JMenuItem copyProfileButton;
    private final JMenuItem deleteProfileButton;
    private final JMenuItem browseGameFolder;
    
    public ProfileListTab(final Launcher launcher) {
        super();
        this.dataModel = new ProfileTableModel();
        this.table = new JTable(this.dataModel);
        this.popupMenu = new JPopupMenu();
        this.addProfileButton = new JMenuItem("Add Profile");
        this.copyProfileButton = new JMenuItem("Copy Profile");
        this.deleteProfileButton = new JMenuItem("Delete Profile");
        this.browseGameFolder = new JMenuItem("Open Game Folder");
        this.launcher = launcher;
        this.setViewportView(this.table);
        this.createInterface();
        launcher.getProfileManager().addRefreshedProfilesListener(this);
    }
    
    protected void createInterface() {
        this.popupMenu.add(this.addProfileButton);
        this.popupMenu.add(this.copyProfileButton);
        this.popupMenu.add(this.deleteProfileButton);
        this.popupMenu.add(this.browseGameFolder);
        this.table.setFillsViewportHeight(true);
        this.table.setSelectionMode(0);
        this.popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                final int[] selection = ProfileListTab.this.table.getSelectedRows();
                final boolean hasSelection = selection != null && selection.length > 0;
                ProfileListTab.this.copyProfileButton.setEnabled(hasSelection);
                ProfileListTab.this.deleteProfileButton.setEnabled(hasSelection);
                ProfileListTab.this.browseGameFolder.setEnabled(hasSelection);
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            }
            
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
            }
        });
        this.addProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Profile profile = new Profile();
                profile.setName("New Profile");
                while (ProfileListTab.this.launcher.getProfileManager().getProfiles().containsKey(profile.getName())) {
                    profile.setName(profile.getName() + "_");
                }
                ProfileEditorPopup.showEditProfileDialog(ProfileListTab.this.getLauncher(), profile);
            }
        });
        this.copyProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final int selection = ProfileListTab.this.table.getSelectedRow();
                if (selection < 0 || selection >= ProfileListTab.this.table.getRowCount()) {
                    return;
                }
                final Profile current = (Profile)ProfileListTab.this.dataModel.profiles.get(selection);
                final Profile copy = new Profile(current);
                copy.setName("Copy of " + current.getName());
                while (ProfileListTab.this.launcher.getProfileManager().getProfiles().containsKey(copy.getName())) {
                    copy.setName(copy.getName() + "_");
                }
                ProfileEditorPopup.showEditProfileDialog(ProfileListTab.this.getLauncher(), copy);
            }
        });
        this.browseGameFolder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final int selection = ProfileListTab.this.table.getSelectedRow();
                if (selection < 0 || selection >= ProfileListTab.this.table.getRowCount()) {
                    return;
                }
                final Profile profile = (Profile)ProfileListTab.this.dataModel.profiles.get(selection);
                OperatingSystem.openFolder((profile.getGameDir() == null) ? ProfileListTab.this.launcher.getWorkingDirectory() : profile.getGameDir());
            }
        });
        this.deleteProfileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final int selection = ProfileListTab.this.table.getSelectedRow();
                if (selection < 0 || selection >= ProfileListTab.this.table.getRowCount()) {
                    return;
                }
                final Profile current = (Profile)ProfileListTab.this.dataModel.profiles.get(selection);
                final int result = JOptionPane.showOptionDialog(ProfileListTab.this.launcher.getFrame(), "Are you sure you want to delete this profile?", "Profile Confirmation", 0, 2, null, LauncherConstants.CONFIRM_PROFILE_DELETION_OPTIONS, LauncherConstants.CONFIRM_PROFILE_DELETION_OPTIONS[0]);
                if (result == 0) {
                    ProfileListTab.this.launcher.getProfileManager().getProfiles().remove(current.getName());
                    try {
                        ProfileListTab.this.launcher.getProfileManager().saveProfiles();
                        ProfileListTab.this.launcher.getProfileManager().fireRefreshEvent();
                    }
                    catch (IOException ex) {
                        ProfileListTab.LOGGER.error("Couldn't save profiles whilst deleting '" + current.getName() + "'", ex);
                    }
                }
            }
        });
        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final int row = ProfileListTab.this.table.getSelectedRow();
                    if (row >= 0 && row < ProfileListTab.this.dataModel.profiles.size()) {
                        ProfileEditorPopup.showEditProfileDialog(ProfileListTab.this.getLauncher(), ProfileListTab.this.dataModel.profiles.get(row));
                    }
                }
            }
            
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    final int r = ProfileListTab.this.table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < ProfileListTab.this.table.getRowCount()) {
                        ProfileListTab.this.table.setRowSelectionInterval(r, r);
                    }
                    else {
                        ProfileListTab.this.table.clearSelection();
                    }
                    ProfileListTab.this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    final int r = ProfileListTab.this.table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < ProfileListTab.this.table.getRowCount()) {
                        ProfileListTab.this.table.setRowSelectionInterval(r, r);
                    }
                    else {
                        ProfileListTab.this.table.clearSelection();
                    }
                    ProfileListTab.this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    @Override
    public void onProfilesRefreshed(final ProfileManager manager) {
        this.dataModel.setProfiles(manager.getProfiles().values());
    }
    
    @Override
    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
    
    private class ProfileTableModel extends AbstractTableModel
    {
        private final List<Profile> profiles;
        
        private ProfileTableModel() {
            super();
            this.profiles = new ArrayList<Profile>();
        }
        
        @Override
        public int getRowCount() {
            return this.profiles.size();
        }
        
        @Override
        public int getColumnCount() {
            return 3;
        }
        
        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return String.class;
        }
        
        @Override
        public String getColumnName(final int column) {
            switch (column) {
                case 2: {
                    return "Username";
                }
                case 1: {
                    return "Version";
                }
                case 0: {
                    return "Version name";
                }
                default: {
                    return super.getColumnName(column);
                }
            }
        }
        
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final Profile profile = (Profile)this.profiles.get(rowIndex);
            final AuthenticationDatabase authDatabase = ProfileListTab.this.launcher.getProfileManager().getAuthDatabase();
            final UserAuthentication auth = authDatabase.getByUUID(profile.getPlayerUUID());
            switch (columnIndex) {
                case 0: {
                    return profile.getName();
                }
                case 2: {
                    if (auth != null && auth.getSelectedProfile() != null) {
                        return auth.getSelectedProfile().getName();
                    }
                    return "(Not logged in)";
                }
                case 1: {
                    if (profile.getLastVersionId() == null) {
                        return "(Latest version)";
                    }
                    return profile.getLastVersionId();
                }
                default: {
                    return null;
                }
            }
        }
        
        public void setProfiles(final Collection<Profile> profiles) {
            this.profiles.clear();
            this.profiles.addAll(profiles);
            this.fireTableDataChanged();
        }
    }
}
