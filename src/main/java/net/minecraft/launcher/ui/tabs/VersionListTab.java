package net.minecraft.launcher.ui.tabs;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.ui.popups.version.VersionEditorPopup;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.Version;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class VersionListTab extends JScrollPane implements RefreshedVersionsListener
{
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_TYPE = 1;
    private static final int COLUMN_RELEASE_DATE = 2;
    private static final int COLUMN_UPDATE_DATE = 3;
    private static final int COLUMN_LIBRARIES = 4;
    private static final int COLUMN_STATUS = 5;
    private static final int NUM_COLUMNS = 6;
    private final Launcher launcher;
    private final VersionTableModel dataModel;
    private final JTable table;
    private final JPopupMenu popupMenu;
    private final JMenuItem browseVersionFolder;
    
    public VersionListTab(final Launcher launcher) {
        super();
        this.dataModel = new VersionTableModel();
        this.table = new JTable(this.dataModel);
        this.popupMenu = new JPopupMenu();
        this.browseVersionFolder = new JMenuItem("Open Versions Folder");
        this.launcher = launcher;
        this.setViewportView(this.table);
        this.createInterface();
        launcher.getVersionManager().addRefreshedVersionsListener(this);
    }
    
    protected void createInterface() {
        this.popupMenu.add(this.browseVersionFolder);
        this.table.setComponentPopupMenu(this.popupMenu);
        this.table.setFillsViewportHeight(true);
        this.browseVersionFolder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                OperatingSystem.openFolder(new File(VersionListTab.this.launcher.getWorkingDirectory(), "/versions/"));
            }
        });
        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final int row = VersionListTab.this.table.getSelectedRow();
                    if (row >= 0 && row < VersionListTab.this.dataModel.versions.size() && VersionListTab.this.dataModel.versions.get(row) instanceof CompleteVersion) {
                        VersionEditorPopup.showEditVersionDialog(VersionListTab.this.getLauncher(),(CompleteVersion) VersionListTab.this.dataModel.versions.get(row));
                    }
                }
            }
            
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    final int r = VersionListTab.this.table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < VersionListTab.this.table.getRowCount()) {
                        VersionListTab.this.table.setRowSelectionInterval(r, r);
                    }
                    else {
                        VersionListTab.this.table.clearSelection();
                    }
                    VersionListTab.this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    final int r = VersionListTab.this.table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < VersionListTab.this.table.getRowCount()) {
                        VersionListTab.this.table.setRowSelectionInterval(r, r);
                    }
                    else {
                        VersionListTab.this.table.clearSelection();
                    }
                    VersionListTab.this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    @Override
    public void onVersionsRefreshed(final VersionManager manager) {
        this.dataModel.setVersions(manager.getLocalVersionList().getVersions());
    }
    
    @Override
    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }
    
    private class VersionTableModel extends AbstractTableModel
    {
        private final List<Version> versions;
        
        private VersionTableModel() {
            super();
            this.versions = new ArrayList<Version>();
        }
        
        @Override
        public int getRowCount() {
            return this.versions.size();
        }
        
        @Override
        public int getColumnCount() {
            return 6;
        }
        
        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            if (columnIndex == 3 || columnIndex == 2) {
                return Date.class;
            }
            return String.class;
        }
        
        @Override
        public String getColumnName(final int column) {
            switch (column) {
                case 3: {
                    return "Last modified";
                }
                case 1: {
                    return "Version type";
                }
                case 4: {
                    return "Library count";
                }
                case 0: {
                    return "Version name";
                }
                case 5: {
                    return "Sync status";
                }
                case 2: {
                    return "Release Date";
                }
                default: {
                    return super.getColumnName(column);
                }
            }
        }
        
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final Version version = (Version)this.versions.get(rowIndex);
            switch (columnIndex) {
                case 0: {
                    return version.getId();
                }
                case 3: {
                    return version.getUpdatedTime();
                }
                case 4: {
                    if (!(version instanceof CompleteVersion)) {
                        return "?";
                    }
                    final CompleteVersion complete = (CompleteVersion)version;
                    final int total = complete.getLibraries().size();
                    final int relevant = complete.getRelevantLibraries().size();
                    if (total == relevant) {
                        return total;
                    }
                    return String.format("%d (%d relevant to %s)", total, relevant, OperatingSystem.getCurrentPlatform().getName());
                }
                case 5: {
                    final VersionSyncInfo syncInfo = VersionListTab.this.launcher.getVersionManager().getVersionSyncInfo(version);
                    if (!syncInfo.isOnRemote()) {
                        return "Local only";
                    }
                    if (syncInfo.isUpToDate()) {
                        return "Up to date with remote";
                    }
                    return "Update avail from remote";
                }
                case 1: {
                    return version.getType().getName();
                }
                case 2: {
                    return version.getReleaseTime();
                }
                default: {
                    return null;
                }
            }
        }
        
        public void setVersions(final Collection<Version> versions) {
            this.versions.clear();
            this.versions.addAll(versions);
            this.fireTableDataChanged();
        }
    }
}
