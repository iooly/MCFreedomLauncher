package net.minecraft.launcher;

import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.ui.LauncherPanel;
import net.minecraft.launcher.ui.popups.login.LogInPopup;
import net.minecraft.launcher.updater.ExceptionalThreadPoolExecutor;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.RemoteVersionList;
import net.minecraft.launcher.updater.VersionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Launcher
{
    private static final Logger LOGGER;
    private static Launcher instance;
    private final VersionManager versionManager;
    private final JFrame frame;
    private final LauncherPanel launcherPanel;
    private final GameLauncher gameLauncher;
    private final File workingDirectory;
    private final Proxy proxy;
    private final PasswordAuthentication proxyAuth;
    private final String[] additionalArgs;
    private final Integer bootstrapVersion;
    private final ProfileManager profileManager;
    private final ThreadPoolExecutor downloaderExecutorService;
    private UUID clientToken;
    private static boolean onlineMode=false;
    
    public Launcher(final JFrame frame, final File workingDirectory, final Proxy proxy, final PasswordAuthentication proxyAuth, final String[] args) {
        this(frame, workingDirectory, proxy, proxyAuth, args, 0);
    }
    
    public Launcher(final JFrame frame, final File workingDirectory, final Proxy proxy, final PasswordAuthentication proxyAuth, final String[] args, final Integer bootstrapVersion) {
        super();
        this.downloaderExecutorService = new ExceptionalThreadPoolExecutor(16, 16, 30L, TimeUnit.SECONDS);
        this.clientToken = UUID.randomUUID();
        this.bootstrapVersion = bootstrapVersion;
        Launcher.instance = this;
        setLookAndFeel();
        this.proxy = proxy;
        this.proxyAuth = proxyAuth;
        this.additionalArgs = args;
        this.workingDirectory = workingDirectory;
        this.frame = frame;
        this.gameLauncher = new GameLauncher(this);
        this.profileManager = new ProfileManager(this);
        this.versionManager = new VersionManager(new LocalVersionList(workingDirectory), new RemoteVersionList(proxy));
        this.launcherPanel = new LauncherPanel(this);
        this.downloaderExecutorService.allowCoreThreadTimeOut(true);
        this.initializeFrame();
        if (bootstrapVersion < 4) {
            this.showOutdatedNotice();
            return;
        }
        this.refreshVersionsAndProfiles();
        Launcher.LOGGER.info(this.getFrame().getTitle() + " (through bootstrap " + bootstrapVersion + ") started on " + OperatingSystem.getCurrentPlatform().getName() + "...");
        Launcher.LOGGER.info("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));
        if (!OperatingSystem.getCurrentPlatform().isSupported()) {
            Launcher.LOGGER.fatal("This operating system is unknown or unsupported, we cannot guarantee that the game will launch successfully.");
        }
        Launcher.LOGGER.info("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
        Launcher.LOGGER.info("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
        Launcher.LOGGER.info("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
        Launcher.LOGGER.info("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
        Launcher.LOGGER.info("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
        Launcher.LOGGER.info("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
    }
    
    private void showOutdatedNotice() {
        final String error = "Sorry, but your launcher is outdated! Please redownload it at https://mojang.com/2013/06/minecraft-1-6-pre-release/";
        this.frame.getContentPane().removeAll();
        final int result = JOptionPane.showOptionDialog(this.frame, error, "Outdated launcher", 0, 0, null, LauncherConstants.BOOTSTRAP_OUT_OF_DATE_BUTTONS, LauncherConstants.BOOTSTRAP_OUT_OF_DATE_BUTTONS[0]);
        if (result == 0) {
            try {
                OperatingSystem.openLink(new URI("https://mojang.com/2013/06/minecraft-1-6-pre-release/"));
            }
            catch (URISyntaxException e) {
                Launcher.LOGGER.error("Couldn't open bootstrap download link. Please visit https://mojang.com/2013/06/minecraft-1-6-pre-release/ manually.", e);
            }
        }
        this.closeLauncher();
    }
    
    public static void setLookAndFeel() {
        final JFrame frame = new JFrame();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Throwable ignored) {
            try {
                Launcher.LOGGER.error("Your java failed to provide normal look and feel, trying the old fallback now");
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch (Throwable t) {
                Launcher.LOGGER.error("Unexpected exception setting look and feel", t);
            }
        }
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("test"));
        frame.add(panel);
        try {
            frame.pack();
        }
        catch (Throwable ignored2) {
            Launcher.LOGGER.error("Custom (broken) theme detected, falling back onto x-platform theme");
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch (Throwable ex) {
                Launcher.LOGGER.error("Unexpected exception setting look and feel", ex);
            }
        }
        frame.dispose();
    }
    
    public void refreshVersionsAndProfiles() {
        this.versionManager.getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Launcher.this.versionManager.refreshVersions();
                }
                catch (Throwable e) {
                    Launcher.LOGGER.error("Unexpected exception refreshing version list", e);
                }
                try {
                    Launcher.this.profileManager.loadProfiles();
                    Launcher.LOGGER.info("Loaded " + Launcher.this.profileManager.getProfiles().size() + " profile(s); selected '" + Launcher.this.profileManager.getSelectedProfile().getName() + "'");
                }
                catch (Throwable e) {
                    Launcher.LOGGER.error("Unexpected exception refreshing profile list", e);
                }
                Launcher.this.ensureLoggedIn();
            }
        });
    }
    
    public void ensureLoggedIn() {
        final Profile selectedProfile = this.profileManager.getSelectedProfile();
        final UserAuthentication auth = this.profileManager.getAuthDatabase().getByUUID(selectedProfile.getPlayerUUID());
        if (auth == null) {
            this.showLoginPrompt();
        }
        else if (!auth.isLoggedIn()) {
            if (auth.canLogIn()) {
                try {
                    auth.logIn();
                    try {
                        this.profileManager.saveProfiles();
                    }
                    catch (IOException e) {
                        Launcher.LOGGER.error("Couldn't save profiles after refreshing auth!", e);
                    }
                    this.profileManager.fireRefreshEvent();
                }
                catch (AuthenticationException e2) {
                    Launcher.LOGGER.error("Exception whilst logging into profile", e2);
                    this.showLoginPrompt();
                }
            }
            else {
                this.showLoginPrompt();
            }
        }
        else if (!auth.canPlayOnline()) {
            try {
                Launcher.LOGGER.info("Refreshing auth...");
                auth.logIn();
                try {
                    this.profileManager.saveProfiles();
                }
                catch (IOException e) {
                    Launcher.LOGGER.error("Couldn't save profiles after refreshing auth!", e);
                }
                this.profileManager.fireRefreshEvent();
            }
            catch (InvalidCredentialsException e3) {
                Launcher.LOGGER.error("Exception whilst logging into profile", e3);
                this.showLoginPrompt();
            }
            catch (AuthenticationException e2) {
                Launcher.LOGGER.error("Exception whilst logging into profile", e2);
            }
        }
    }
    
    public void showLoginPrompt() {
        try {
            this.profileManager.saveProfiles();
        }
        catch (IOException e) {
            Launcher.LOGGER.error("Couldn't save profiles before logging in!", e);
        }
        for (final Profile profile : this.profileManager.getProfiles().values()) {
            final Map<String, String> credentials = profile.getAuthentication();
            if (credentials != null) {
                final UserAuthentication auth = this.profileManager.getAuthDatabase().getAuthenticationService().createUserAuthentication(Agent.MINECRAFT);
                auth.loadFromStorage(credentials);
                if (auth.isLoggedIn()) {
                    final String uuid = (auth.getSelectedProfile() == null) ? ("demo-" + auth.getUserID()) : auth.getSelectedProfile().getId();
                    if (this.profileManager.getAuthDatabase().getByUUID(uuid) == null) {
                        this.profileManager.getAuthDatabase().register(uuid, auth);
                    }
                }
                profile.setAuthentication(null);
            }
        }
        final Profile selectedProfile = this.profileManager.getSelectedProfile();
        LogInPopup.showLoginPrompt(this, new LogInPopup.Callback() {
            @Override
            public void onLogIn(final String uuid) {
                final UserAuthentication auth = Launcher.this.profileManager.getAuthDatabase().getByUUID(uuid);
                selectedProfile.setPlayerUUID(uuid);
                if (selectedProfile.getName().equals("(Default)") && auth.getSelectedProfile() != null) {
                    final String playerName = auth.getSelectedProfile().getName();
                    String profileName = auth.getSelectedProfile().getName();
                    for (int count = 1; Launcher.this.profileManager.getProfiles().containsKey(profileName); profileName = playerName + " " + ++count) {}
                    final Profile newProfile = new Profile(selectedProfile);
                    newProfile.setName(profileName);
                    Launcher.this.profileManager.getProfiles().put(profileName, newProfile);
                    Launcher.this.profileManager.getProfiles().remove("(Default)");
                    Launcher.this.profileManager.setSelectedProfile(profileName);
                }
                try {
                    Launcher.this.profileManager.saveProfiles();
                }
                catch (IOException e) {
                    Launcher.LOGGER.error("Couldn't save profiles after logging in!", e);
                }
                if (uuid == null) {
                    Launcher.this.closeLauncher();
                }
                else {
                    Launcher.this.profileManager.fireRefreshEvent();
                }
                Launcher.this.launcherPanel.setCard("launcher", null);
            }
        });
    }
    
    public void closeLauncher() {
        this.frame.dispatchEvent(new WindowEvent(this.frame, 201));
    }
    
    protected void initializeFrame() {
        this.frame.getContentPane().removeAll();
        this.frame.setTitle("Minecraft Launcher 1.3.7");
        this.frame.setPreferredSize(new Dimension(900, 580));
        this.frame.setDefaultCloseOperation(2);
        this.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Launcher.this.frame.setVisible(false);
                Launcher.this.frame.dispose();
                Launcher.this.versionManager.getExecutorService().shutdown();
            }
        });
        try {
            final InputStream in = Launcher.class.getResourceAsStream("/favicon.png");
            if (in != null) {
                this.frame.setIconImage(ImageIO.read(in));
            }
        }
        catch (IOException ex) {}
        this.frame.add(this.launcherPanel);
        this.frame.pack();
        this.frame.setVisible(true);
    }
    
    public VersionManager getVersionManager() {
        return this.versionManager;
    }
    
    public JFrame getFrame() {
        return this.frame;
    }
    
    public LauncherPanel getLauncherPanel() {
        return this.launcherPanel;
    }
    
    public GameLauncher getGameLauncher() {
        return this.gameLauncher;
    }
    
    public File getWorkingDirectory() {
        return this.workingDirectory;
    }
    
    public Proxy getProxy() {
        return this.proxy;
    }
    
    public PasswordAuthentication getProxyAuth() {
        return this.proxyAuth;
    }
    
    public String[] getAdditionalArgs() {
        return this.additionalArgs;
    }
    
    public int getBootstrapVersion() {
        return this.bootstrapVersion;
    }
    
    public static Launcher getInstance() {
        return Launcher.instance;
    }
    
    public ProfileManager getProfileManager() {
        return this.profileManager;
    }
    
    public UUID getClientToken() {
        return this.clientToken;
    }
    
    public void setClientToken(final UUID clientToken) {
        this.clientToken = clientToken;
    }
    
    public ThreadPoolExecutor getDownloaderExecutorService() {
        return this.downloaderExecutorService;
    }

    public static boolean isOnlineMode() {
        return Launcher.onlineMode;
    }

    public static void setOnlineMode(boolean onlineMode) {
        Launcher.onlineMode = onlineMode;
    }

    static {
        Thread.currentThread().setContextClassLoader(Launcher.class.getClassLoader());
        LOGGER = LogManager.getLogger();
    }
}
