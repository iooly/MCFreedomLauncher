package net.minecraft.launcher.ui.popups.version;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.minecraft.launcher.versions.ReleaseType;
import javax.swing.Box;
import javax.swing.JComponent;
import java.awt.Container;
import javax.swing.BoxLayout;
import java.awt.Component;
import javax.swing.JLabel;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import java.awt.LayoutManager;
import java.awt.GridBagLayout;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerDateModel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JPanel;

public class VersionInfoPanel extends JPanel
{
    private final VersionEditorPopup editor;
    private final JTextField versionName;
    private final JSpinner releaseTime;
    private final JButton resetReleaseTime;
    private final JSpinner updateTime;
    private final JButton resetUpdateTime;
    private final JComboBox releaseType;
    
    public VersionInfoPanel(final VersionEditorPopup editor) {
        super();
        this.versionName = new JTextField();
        this.releaseTime = new JSpinner(new SpinnerDateModel());
        this.resetReleaseTime = new JButton("Now");
        this.updateTime = new JSpinner(new SpinnerDateModel());
        this.resetUpdateTime = new JButton("Now");
        this.releaseType = new JComboBox();
        this.editor = editor;
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createTitledBorder("Version Info"));
        this.createInterface();
        this.fillDefaultValues();
        this.addEventHandlers();
    }
    
    protected void createInterface() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = 17;
        constraints.gridy = 0;
        this.add(new JLabel("Version ID:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(this.versionName, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints = constraints;
        ++gridBagConstraints.gridy;
        final JPanel releaseDatePanel = new JPanel();
        releaseDatePanel.setLayout(new BoxLayout(releaseDatePanel, 0));
        this.releaseTime.setEditor(new JSpinner.DateEditor(this.releaseTime, "yyyy-MM-dd HH:mm:ss z"));
        releaseDatePanel.add(this.releaseTime);
        releaseDatePanel.add(Box.createHorizontalStrut(5));
        releaseDatePanel.add(this.resetReleaseTime);
        this.add(new JLabel("Release Date:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(releaseDatePanel, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints2 = constraints;
        ++gridBagConstraints2.gridy;
        final JPanel updateTimePanel = new JPanel();
        updateTimePanel.setLayout(new BoxLayout(updateTimePanel, 0));
        this.updateTime.setEditor(new JSpinner.DateEditor(this.updateTime, "yyyy-MM-dd HH:mm:ss z"));
        updateTimePanel.add(this.updateTime);
        updateTimePanel.add(Box.createHorizontalStrut(5));
        updateTimePanel.add(this.resetUpdateTime);
        this.add(new JLabel("Last Updated:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(updateTimePanel, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints3 = constraints;
        ++gridBagConstraints3.gridy;
        this.add(new JLabel("Release Type:"), constraints);
        constraints.fill = 2;
        constraints.weightx = 1.0;
        this.add(this.releaseType, constraints);
        constraints.weightx = 0.0;
        constraints.fill = 0;
        final GridBagConstraints gridBagConstraints4 = constraints;
        ++gridBagConstraints4.gridy;
        for (final ReleaseType type : ReleaseType.values()) {
            this.releaseType.addItem(type);
        }
    }
    
    protected void fillDefaultValues() {
        this.versionName.setText(this.editor.getVersion().getId());
        this.releaseTime.setValue(this.editor.getVersion().getReleaseTime());
        this.updateTime.setValue(this.editor.getVersion().getUpdatedTime());
        this.releaseType.setSelectedItem(this.editor.getVersion().getType());
    }
    
    protected void addEventHandlers() {
        this.versionName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                VersionInfoPanel.this.updateVersionName();
            }
            
            @Override
            public void removeUpdate(final DocumentEvent e) {
                VersionInfoPanel.this.updateVersionName();
            }
            
            @Override
            public void changedUpdate(final DocumentEvent e) {
                VersionInfoPanel.this.updateVersionName();
            }
        });
        this.releaseTime.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                VersionInfoPanel.this.editor.getVersion().setReleaseTime((Date)VersionInfoPanel.this.releaseTime.getValue());
            }
        });
        this.resetReleaseTime.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VersionInfoPanel.this.releaseTime.setValue(new Date());
            }
        });
        this.updateTime.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                VersionInfoPanel.this.editor.getVersion().setUpdatedTime((Date)VersionInfoPanel.this.updateTime.getValue());
            }
        });
        this.resetUpdateTime.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                VersionInfoPanel.this.updateTime.setValue(new Date());
            }
        });
        this.releaseType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                VersionInfoPanel.this.editor.getVersion().setType((ReleaseType)VersionInfoPanel.this.releaseType.getSelectedItem());
            }
        });
    }
    
    private void updateVersionName() {
        if (this.versionName.getText().length() > 0) {
            return;
        }
    }
}
