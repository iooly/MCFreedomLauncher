package net.minecraft.launcher.ui.popups.version;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import javax.swing.JLabel;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.awt.LayoutManager;
import java.awt.GridBagLayout;
import javax.swing.JTextField;
import javax.swing.JPanel;

public class VersionLaunchInfoPanel extends JPanel
{
    private final VersionEditorPopup editor;
    private final JTextField mainClass;
    private final JTextField processArguments;
    
    public VersionLaunchInfoPanel(final VersionEditorPopup editor) {
        super();
        this.mainClass = new JTextField();
        this.processArguments = new JTextField(50);
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Launch Environment"));
        this.createInterface();
        this.fillDefaultValues();
        this.addEventHandlers();
    }
    
    protected void createInterface() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;
        constraints.gridy = 0;
        this.add(new JLabel("Main Class:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(this.mainClass, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints = constraints;
        ++gridBagConstraints.gridy;
        this.add(new JLabel("Process Arguments:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(this.processArguments, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints2 = constraints;
        ++gridBagConstraints2.gridy;
    }
    
    protected void fillDefaultValues() {
        this.mainClass.setText(this.editor.getVersion().getMainClass());
        this.processArguments.setText(this.editor.getVersion().getMinecraftArguments());
    }
    
    protected void addEventHandlers() {
        this.mainClass.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                VersionLaunchInfoPanel.this.updateMainClass();
            }
            
            @Override
            public void removeUpdate(final DocumentEvent e) {
                VersionLaunchInfoPanel.this.updateMainClass();
            }
            
            @Override
            public void changedUpdate(final DocumentEvent e) {
                VersionLaunchInfoPanel.this.updateMainClass();
            }
        });
        this.processArguments.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                VersionLaunchInfoPanel.this.updateProcessArguments();
            }
            
            @Override
            public void removeUpdate(final DocumentEvent e) {
                VersionLaunchInfoPanel.this.updateProcessArguments();
            }
            
            @Override
            public void changedUpdate(final DocumentEvent e) {
                VersionLaunchInfoPanel.this.updateProcessArguments();
            }
        });
    }
    
    private void updateMainClass() {
        if (this.mainClass.getText().length() > 0) {
            this.editor.getVersion().setMainClass(this.mainClass.getText());
        }
    }
    
    private void updateProcessArguments() {
        this.editor.getVersion().setMinecraftArguments(this.processArguments.getText());
    }
}
