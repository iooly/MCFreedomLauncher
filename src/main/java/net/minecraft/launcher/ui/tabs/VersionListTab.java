package net.minecraft.launcher.ui.tabs;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.locale.LocaleHelper;
import net.minecraft.launcher.updater.VersionManager;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.Version;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.*;

public class VersionListTab extends JScrollPane
        implements RefreshedVersionsListener {
    private ResourceBundle resourceBundle= LocaleHelper.getMessages();
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_TYPE = 1;
    private static final int COLUMN_RELEASE_DATE = 2;
    private static final int COLUMN_UPDATE_DATE = 3;
    private static final int COLUMN_LIBRARIES = 4;
    private static final int COLUMN_STATUS = 5;
    private static final int NUM_COLUMNS = 6;
    private final Launcher launcher;
    private final VersionTableModel dataModel = new VersionTableModel();
    private final JTable table = new JTable(this.dataModel);


    public VersionListTab(Launcher launcher) {
        this.launcher = launcher;

        setViewportView(this.table);
        createInterface();

        launcher.getVersionManager().addRefreshedVersionsListener(this);
    }

    protected void createInterface() {
        this.table.setFillsViewportHeight(true);
    }

    public Launcher getLauncher() {
        return this.launcher;
    }

    public void onVersionsRefreshed(VersionManager manager) {
        this.dataModel.setVersions(manager.getLocalVersionList().getVersions());
    }

    public boolean shouldReceiveEventsInUIThread() {
        return true;
    }

    private class VersionTableModel extends AbstractTableModel {
        private final List<Version> versions = new ArrayList();

        private VersionTableModel() {
        }

        public int getRowCount() {
            return this.versions.size();
        }


        public int getColumnCount() {
            return 6;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if ((columnIndex == 3) || (columnIndex == 2)) {
                return Date.class;
            }

            return String.class;
        }

        public String getColumnName(int column) {
            switch (column) {
                case 3:

                    return resourceBundle.getString("last.modified");
                case 1:
                    return resourceBundle.getString("version.type");
                case 4:
                    return resourceBundle.getString("library.count");
                case 0:
                    return resourceBundle.getString("version.name");
                case 5:
                    return resourceBundle.getString("sync.status");
                case 2:
                    return resourceBundle.getString("release.date");
            }
            return super.getColumnName(column);
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Version version = (Version) this.versions.get(rowIndex);

            switch (columnIndex) {
                case 0:
                    return version.getId();
                case 3:
                    return version.getUpdatedTime();
                case 4:
                    if ((version instanceof CompleteVersion)) {
                        CompleteVersion complete = (CompleteVersion) version;
                        int total = complete.getLibraries().size();
                        int relevant = complete.getRelevantLibraries().size();
                        //int relevant = complete.getRelevantLibraries(OperatingSystem.getCurrentPlatform()).size();
                        if (total == relevant) {
                            return Integer.valueOf(total);
                        }
                        return String.format("%d (%d relevant to %s)", new Object[]{Integer.valueOf(total), Integer.valueOf(relevant), OperatingSystem.getCurrentPlatform().getName()});
                    }

                    return "?";
                case 5:
                    VersionSyncInfo syncInfo = VersionListTab.this.launcher.getVersionManager().getVersionSyncInfo(version);
                    if (syncInfo.isOnRemote()) {
                        if (syncInfo.isUpToDate()) {
                            return resourceBundle.getString("up.to.date.with.remote");
                        }
                        return resourceBundle.getString("update.avail.from.remote");
                    }

                    return resourceBundle.getString("local.only");
                case 1:
                    return version.getType().getName();
                case 2:
                    return version.getReleaseTime();
            }

            return null;
        }

        public void setVersions(Collection<Version> versions) {
            this.versions.clear();
            this.versions.addAll(versions);
            fireTableDataChanged();
        }
    }
}
