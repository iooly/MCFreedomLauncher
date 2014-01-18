package net.minecraft.launcher.ui.popups.version;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import net.minecraft.launcher.OperatingSystem;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.launcher.versions.Rule;
import java.util.ArrayList;
import net.minecraft.launcher.versions.Library;
import javax.swing.table.AbstractTableModel;
import java.awt.Component;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.table.TableModel;
import javax.swing.JTable;
import javax.swing.JScrollPane;

public class VersionLibrariesPanel extends JScrollPane
{
    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_RULES = 1;
    private static final int COLUMN_NATIVES = 2;
    private static final int NUM_COLUMNS = 3;
    private final VersionEditorPopup editor;
    private final LibraryDataModel dataModel;
    private final JTable table;
    
    public VersionLibrariesPanel(final VersionEditorPopup editor) {
        super();
        this.dataModel = new LibraryDataModel();
        this.table = new JTable(this.dataModel);
        this.editor = editor;
        this.setBorder(BorderFactory.createTitledBorder("Libraries & Natives"));
        this.setViewportView(this.table);
        this.table.getColumn("Library").setMinWidth(250);
    }
    
    private class LibraryDataModel extends AbstractTableModel
    {
        @Override
        public int getRowCount() {
            return VersionLibrariesPanel.this.editor.getVersion().getLibraries().size();
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
                    return "Has natives?";
                }
                case 1: {
                    return "Has rules?";
                }
                case 0: {
                    return "Library";
                }
                default: {
                    return super.getColumnName(column);
                }
            }
        }
        
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final Library library = VersionLibrariesPanel.this.editor.getVersion().getLibraries().get(rowIndex);
            if (columnIndex == 1) {
                if (library.getRules() == null || library.getRules().isEmpty()) {
                    return "-";
                }
                final List<Rule> allowedRules = new ArrayList<Rule>();
                for (final Rule rule : library.getRules()) {
                    if (rule.getAction() == Rule.Action.ALLOW) {
                        allowedRules.add(rule);
                    }
                }
                if (allowedRules.size() == 1) {
                    final Rule rule2 = allowedRules.get(0);
                    if (rule2.getOs() != null && rule2.getOs().getName() != null) {
                        if (StringUtils.isNotEmpty(rule2.getOs().getVersion())) {
                            return rule2.getOs().getName().getName() + " " + rule2.getOs().getVersion() + " only";
                        }
                        return rule2.getOs().getName().getName() + " only";
                    }
                }
                return "Yes";
            }
            else {
                if (columnIndex != 2) {
                    return library.getName();
                }
                if (library.getNatives() == null || library.getNatives().isEmpty()) {
                    return "-";
                }
                final StringBuilder result = new StringBuilder();
                for (final Map.Entry<OperatingSystem, String> entry : library.getNatives().entrySet()) {
                    if (result.length() > 0) {
                        result.append("/");
                    }
                    result.append(entry.getKey());
                }
                return result;
            }
        }
    }
}
