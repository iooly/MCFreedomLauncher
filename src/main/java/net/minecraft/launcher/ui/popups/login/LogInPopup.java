package net.minecraft.launcher.ui.popups.login;

import java.awt.Window;
import javax.swing.SwingUtilities;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.LauncherConstants;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.GridLayout;
import java.io.IOException;
import javax.swing.Box;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.LayoutManager;
import java.awt.Container;
import javax.swing.BoxLayout;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import net.minecraft.launcher.Launcher;
import java.awt.event.ActionListener;
import javax.swing.JPanel;

public class LogInPopup extends JPanel implements ActionListener
{
    private final Launcher launcher;
    private final Callback callback;
    private final AuthErrorForm errorForm;
    private final ExistingUserListForm existingUserListForm;
    private final LogInForm logInForm;
    private final JButton loginButton;
    private final JButton registerButton;
    private final JProgressBar progressBar;
    
    public LogInPopup(final Launcher launcher, final Callback callback) {
        super(true);
        this.loginButton = new JButton("Log In");
        this.registerButton = new JButton("Register");
        this.progressBar = new JProgressBar();
        this.launcher = launcher;
        this.callback = callback;
        this.errorForm = new AuthErrorForm(this);
        this.existingUserListForm = new ExistingUserListForm(this);
        this.logInForm = new LogInForm(this);
        this.createInterface();
        this.loginButton.addActionListener(this);
        this.registerButton.addActionListener(this);
    }
    
    protected void createInterface() {
        this.setLayout(new BoxLayout(this, 1));
        this.setBorder(new EmptyBorder(5, 15, 5, 15));
        try {
            final InputStream stream = LogInPopup.class.getResourceAsStream("/minecraft_logo.png");
            if (stream != null) {
                final BufferedImage image = ImageIO.read(stream);
                final JLabel label = new JLabel(new ImageIcon(image));
                final JPanel imagePanel = new JPanel();
                imagePanel.add(label);
                this.add(imagePanel);
                this.add(Box.createVerticalStrut(10));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (!this.launcher.getProfileManager().getAuthDatabase().getKnownNames().isEmpty()) {
            this.add(this.existingUserListForm);
        }
        this.add(this.errorForm);
        this.add(this.logInForm);
        this.add(Box.createVerticalStrut(15));
        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2, 10, 0));
        buttonPanel.add(this.registerButton);
        buttonPanel.add(this.loginButton);
        this.add(buttonPanel);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setVisible(false);
        this.add(this.progressBar);
    }
    
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.loginButton) {
            this.logInForm.tryLogIn();
        }
        else if (e.getSource() == this.registerButton) {
            OperatingSystem.openLink(LauncherConstants.URL_REGISTER);
        }
    }
    
    public static void showLoginPrompt(final Launcher launcher, final Callback callback) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final LogInPopup popup = new LogInPopup(launcher, callback);
                launcher.getLauncherPanel().setCard("login", popup);
            }
        });
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    public void setCanLogIn(final boolean enabled) {
        if (SwingUtilities.isEventDispatchThread()) {
            this.loginButton.setEnabled(enabled);
            this.progressBar.setIndeterminate(false);
            this.progressBar.setIndeterminate(true);
            this.progressBar.setVisible(!enabled);
            this.repack();
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    LogInPopup.this.setCanLogIn(enabled);
                }
            });
        }
    }
    
    public LogInForm getLogInForm() {
        return this.logInForm;
    }
    
    public AuthErrorForm getErrorForm() {
        return this.errorForm;
    }
    
    public ExistingUserListForm getExistingUserListForm() {
        return this.existingUserListForm;
    }
    
    public void setLoggedIn(final String uuid) {
        this.callback.onLogIn(uuid);
    }
    
    public void repack() {
        final Window window = SwingUtilities.windowForComponent(this);
        if (window != null) {
            window.pack();
        }
    }
    
    public interface Callback
    {
        void onLogIn(String p0);
    }
}
