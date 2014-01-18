package net.minecraft.launcher.ui.bottombar;

import org.apache.logging.log4j.LogManager;
import java.util.Iterator;
import net.minecraft.launcher.Http;
import java.net.URL;
import java.util.Map;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import java.awt.GridBagConstraints;
import com.google.gson.TypeAdapterFactory;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import javax.swing.JLabel;
import net.minecraft.launcher.Launcher;
import org.apache.logging.log4j.Logger;

public class StatusPanelForm extends SidebarGridForm
{
    private static final Logger LOGGER;
    private static final String SERVER_SESSION = "session.minecraft.net";
    private static final String SERVER_LOGIN = "login.minecraft.net";
    private final Launcher launcher;
    private final JLabel sessionStatus;
    private final JLabel loginStatus;
    private final Gson gson;
    
    public StatusPanelForm(final Launcher launcher) {
        super();
        this.sessionStatus = new JLabel("???");
        this.loginStatus = new JLabel("???");
        this.gson = new GsonBuilder().registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();
        this.launcher = launcher;
        this.createInterface();
        this.refreshStatuses();
    }
    
    @Override
    protected void populateGrid(final GridBagConstraints constraints) {
        this.<JLabel>add(new JLabel("Multiplayer:", 2), constraints, 0, 0, 0, 1, 17);
        this.<JLabel>add(this.sessionStatus, constraints, 1, 0, 1, 1);
        this.<JLabel>add(new JLabel("Login:", 2), constraints, 0, 1, 0, 1, 17);
        this.<JLabel>add(this.loginStatus, constraints, 1, 1, 1, 1);
    }
    
    public JLabel getSessionStatus() {
        return this.sessionStatus;
    }
    
    public JLabel getLoginStatus() {
        return this.loginStatus;
    }
    
    public void refreshStatuses() {
        this.launcher.getVersionManager().getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final TypeToken<List<Map<String, ServerStatus>>> token = new TypeToken<List<Map<String, ServerStatus>>>() {};
                    final List<Map<String, ServerStatus>> statuses = (List<Map<String, ServerStatus>>)StatusPanelForm.this.gson.<List<Map<String, ServerStatus>>>fromJson(Http.performGet(new URL("http://status.mojang.com/check"), StatusPanelForm.this.launcher.getProxy()), token.getType());
                    for (final Map<String, ServerStatus> serverStatusInformation : statuses) {
                        if (serverStatusInformation.containsKey("login.minecraft.net")) {
                            StatusPanelForm.this.loginStatus.setText(((ServerStatus)serverStatusInformation.get("login.minecraft.net")).title);
                        }
                        else {
                            if (!serverStatusInformation.containsKey("session.minecraft.net")) {
                                continue;
                            }
                            StatusPanelForm.this.sessionStatus.setText(((ServerStatus)serverStatusInformation.get("session.minecraft.net")).title);
                        }
                    }
                }
                catch (Exception e) {
                    StatusPanelForm.LOGGER.error("Couldn't get server status", e);
                }
            }
        });
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
    
    public enum ServerStatus
    {
        GREEN("Online, no problems detected."), 
        YELLOW("May be experiencing issues."), 
        RED("Offline, experiencing problems.");
        
        private final String title;
        
        private ServerStatus(final String title) {
            this.title = title;
        }
    }
}
