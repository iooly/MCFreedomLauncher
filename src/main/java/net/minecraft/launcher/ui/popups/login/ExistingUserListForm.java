package net.minecraft.launcher.ui.popups.login;

import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.authentication.AuthenticationService;
import net.minecraft.launcher.authentication.exceptions.AuthenticationException;
import net.minecraft.launcher.locale.LocaleHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ResourceBundle;

public class ExistingUserListForm extends JPanel
        implements ActionListener {
    private ResourceBundle resourceBundle = LocaleHelper.getMessages();
    private final LogInPopup popup;
    private final JComboBox userDropdown = new JComboBox();
    private final AuthenticationDatabase authDatabase;
    private final JButton playButton = new JButton(resourceBundle.getString("play"));
    private final JButton logOutButton = new JButton(resourceBundle.getString("log.out"));

    public ExistingUserListForm(LogInPopup popup) {
        this.popup = popup;
        this.authDatabase = popup.getLauncher().getProfileManager().getAuthDatabase();

        fillUsers();
        createInterface();

        this.playButton.addActionListener(this);
        this.logOutButton.addActionListener(this);
    }

    private void fillUsers() {
        for (String user : this.authDatabase.getKnownNames())
            this.userDropdown.addItem(user);
    }

    protected void createInterface() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = 2;
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0D;

        add(Box.createGlue());

        String currentUser = this.authDatabase.getKnownNames().size() + resourceBundle.getString("different.users");
        String thisOrThese = this.authDatabase.getKnownNames().size() == 1 ? resourceBundle.getString("this.account") : resourceBundle.getString("one.of.these.accounts");
        add(new JLabel(resourceBundle.getString("you.re.already.logged.in.as") + currentUser + resourceBundle.getString("in.another.profile")), constraints);
        add(new JLabel(resourceBundle.getString("you.may.use") + thisOrThese + resourceBundle.getString("and.skip.authentication")), constraints);

        add(Box.createVerticalStrut(5), constraints);

        JLabel usernameLabel = new JLabel(resourceBundle.getString("existing.user"));
        Font labelFont = usernameLabel.getFont().deriveFont(1);

        usernameLabel.setFont(labelFont);
        add(usernameLabel, constraints);

        constraints.gridwidth = 1;
        add(this.userDropdown, constraints);

        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.0D;
        constraints.insets = new Insets(0, 5, 0, 0);
        add(this.playButton, constraints);
        constraints.gridx = 2;
        add(this.logOutButton, constraints);
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.weightx = 1.0D;
        constraints.gridx = 0;
        constraints.gridy = -1;

        constraints.gridwidth = 2;

        add(Box.createVerticalStrut(5), constraints);
        add(new JLabel(resourceBundle.getString("alternatively.log.in.with.a.new.account.below")), constraints);
        add(new JPopupMenu.Separator(), constraints);
    }


    public void actionPerformed(ActionEvent e) {
        final Object selected = this.userDropdown.getSelectedItem();

        final AuthenticationService auth;
        final String uuid;
        if ((selected != null) && ((selected instanceof String))) {
            auth = this.authDatabase.getByName((String) selected);

            if (auth.getSelectedProfile() == null)
                uuid = "demo-" + auth.getUsername();
            else
                uuid = auth.getSelectedProfile().getId();
        } else {
            auth = null;
            uuid = null;
        }

        if (e.getSource() == this.playButton) {
            this.popup.setCanLogIn(false);

            this.popup.getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {
                public void run() {
                    if ((auth != null) && (uuid != null))
                        try {
                            auth.logIn();
                            ExistingUserListForm.this.popup.setLoggedIn(uuid);
                        } catch (AuthenticationException ex) {
                            ExistingUserListForm.this.popup.getErrorForm().displayError(new String[]{resourceBundle.getString("we.couldn.t.log.you.back.in.as") + selected + ".", resourceBundle.getString("please.try.to.log.in.again")});


                            ExistingUserListForm.this.removeUser((String) selected, uuid);

                            ExistingUserListForm.this.popup.setCanLogIn(true);
                        }
                    else
                        ExistingUserListForm.this.popup.setCanLogIn(true);
                }
            });
        } else if (e.getSource() == this.logOutButton) {
            removeUser((String) selected, uuid);
        }
    }

    protected void removeUser(final String name, final String uuid) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ExistingUserListForm.this.removeUser(name, uuid);
                }
            });
        } else {
            this.userDropdown.removeItem(name);
            this.authDatabase.removeUUID(uuid);
            try {
                this.popup.getLauncher().getProfileManager().saveProfiles();
            } catch (IOException e) {
                this.popup.getLauncher().println("Couldn't save profiles whilst removing " + name + " / " + uuid + " from database", e);
            }

            if (this.userDropdown.getItemCount() == 0)
                this.popup.remove(this);
        }
    }
}
