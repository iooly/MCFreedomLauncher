package net.minecraft.launcher.ui.tabs;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.locale.LocaleHelper;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class LauncherTabPanel extends JTabbedPane {
    private final Launcher launcher;
    private final WebsiteTab blog;
    private final ConsoleTab console;
    private final ReadmeTab readme;
    private CrashReportTab crashReportTab;
    private ResourceBundle resourceBundle = LocaleHelper.getMessages();

    public LauncherTabPanel(Launcher launcher) {
        super(1);

        this.launcher = launcher;
        this.blog = new WebsiteTab(launcher);
        this.console = new ConsoleTab(launcher);
        this.readme = new ReadmeTab(launcher);

        createInterface();
    }

    protected void createInterface() {
        addTab(resourceBundle.getString("update.notes"), this.blog);
        addTab(resourceBundle.getString("development.console"), this.console);
        addTab(resourceBundle.getString("profile.editor"), new ProfileListTab(this.launcher));
        addTab(resourceBundle.getString("local.version.editor.nyi"), new VersionListTab(this.launcher));
        addTab(resourceBundle.getString("readme"), this.readme);
    }

    public Launcher getLauncher() {
        return this.launcher;
    }

    public WebsiteTab getBlog() {
        return this.blog;
    }

    public ReadmeTab getReadme() {
        return readme;
    }

    public ConsoleTab getConsole() {
        return this.console;
    }

    public void showConsole() {
        setSelectedComponent(this.console);
    }

    public void setCrashReport(CrashReportTab newTab) {
        if (this.crashReportTab != null) removeTab(this.crashReportTab);
        this.crashReportTab = newTab;
        addTab(resourceBundle.getString("crash.report"), this.crashReportTab);
        setSelectedComponent(newTab);
    }

    protected void removeTab(Component tab) {
        for (int i = 0; i < getTabCount(); i++)
            if (getTabComponentAt(i) == tab) {
                removeTabAt(i);
                break;
            }
    }
}


