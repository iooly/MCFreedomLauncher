package net.minecraft.launcher.ui.popups.version;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JLabel;
import javax.swing.Box;
import java.awt.Component;
import java.awt.Container;
import javax.swing.BoxLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.awt.LayoutManager;
import java.awt.GridBagLayout;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.JPanel;

public class VersionRequirementsPanel extends JPanel
{
    private final VersionEditorPopup editor;
    private final JSpinner launcherVersion;
    private final JTextField incompatibilityWarning;
    
    public VersionRequirementsPanel(final VersionEditorPopup editor) {
        super();
        this.launcherVersion = new JSpinner();
        this.incompatibilityWarning = new JTextField();
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Compatibility Requirements"));
        this.createInterface();
        this.fillDefaultValues();
        this.addEventHandlers();
    }
    
    protected void createInterface() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;
        constraints.gridy = 0;
        final JPanel launcherPanel = new JPanel();
        launcherPanel.setLayout(new BoxLayout(launcherPanel, 0));
        launcherPanel.add(this.launcherVersion);
        launcherPanel.add(Box.createHorizontalStrut(5));
        launcherPanel.add(new JLabel("(Current version, 1.3.8, is numerical 13)"));
        this.add(new JLabel("Required Launcher:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(launcherPanel, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints = constraints;
        ++gridBagConstraints.gridy;
        this.add(new JLabel("Incompatibility Reason:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(this.incompatibilityWarning, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints2 = constraints;
        ++gridBagConstraints2.gridy;
    }
    
    protected void fillDefaultValues() {
        this.launcherVersion.setValue(this.editor.getVersion().getMinimumLauncherVersion());
        this.incompatibilityWarning.setText(this.editor.getVersion().getIncompatibilityReason());
    }
    
    protected void addEventHandlers() {
        this.launcherVersion.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                VersionRequirementsPanel.this.editor.getVersion().setMinimumLauncherVersion((Integer)VersionRequirementsPanel.this.launcherVersion.getValue());
            }
        });
        this.incompatibilityWarning.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                VersionRequirementsPanel.this.updateIncompatibilityWarning();
            }
            
            @Override
            public void removeUpdate(final DocumentEvent e) {
                VersionRequirementsPanel.this.updateIncompatibilityWarning();
            }
            
            @Override
            public void changedUpdate(final DocumentEvent e) {
                VersionRequirementsPanel.this.updateIncompatibilityWarning();
            }
        });
    }
    
    private void updateIncompatibilityWarning() {
        this.editor.getVersion().setIncompatibilityReason((this.incompatibilityWarning.getText().length() == 0) ? null : this.incompatibilityWarning.getText());
    }
}
