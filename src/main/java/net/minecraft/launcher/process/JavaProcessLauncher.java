package net.minecraft.launcher.process;

import java.util.Arrays;
import java.util.Collection;
import java.io.IOException;
import java.util.ArrayList;
import net.minecraft.launcher.OperatingSystem;
import java.io.File;
import java.util.List;

public class JavaProcessLauncher
{
    private final String jvmPath;
    private final List<String> commands;
    private File directory;
    
    public JavaProcessLauncher(String jvmPath, final String... commands) {
        super();
        if (jvmPath == null) {
            jvmPath = OperatingSystem.getCurrentPlatform().getJavaDir();
        }
        this.jvmPath = jvmPath;
        this.commands = new ArrayList<String>(commands.length);
        this.addCommands(commands);
    }
    
    public JavaProcess start() throws IOException {
        final List<String> full = this.getFullCommands();
        return new JavaProcess(full, new ProcessBuilder(full).directory(this.directory).redirectErrorStream(true).start());
    }
    
    public List<String> getFullCommands() {
        final List<String> result = new ArrayList<String>(this.commands);
        result.add(0, this.getJavaPath());
        return result;
    }
    
    public List<String> getCommands() {
        return this.commands;
    }
    
    public void addCommands(final String... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }
    
    public void addSplitCommands(final String commands) {
        this.addCommands(commands.split(" "));
    }
    
    public JavaProcessLauncher directory(final File directory) {
        this.directory = directory;
        return this;
    }
    
    public File getDirectory() {
        return this.directory;
    }
    
    protected String getJavaPath() {
        return this.jvmPath;
    }
    
    @Override
    public String toString() {
        return "JavaProcessLauncher[commands=" + this.commands + ", java=" + this.jvmPath + "]";
    }
}
