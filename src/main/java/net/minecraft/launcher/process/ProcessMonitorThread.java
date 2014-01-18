package net.minecraft.launcher.process;

import org.apache.logging.log4j.LogManager;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.logging.log4j.Logger;

public class ProcessMonitorThread extends Thread
{
    private static final Logger LOGGER;
    private final JavaProcess process;
    
    public ProcessMonitorThread(final JavaProcess process) {
        super();
        this.process = process;
    }
    
    @Override
    public void run() {
        final InputStreamReader reader = new InputStreamReader(this.process.getRawProcess().getInputStream());
        final BufferedReader buf = new BufferedReader(reader);
        String line = null;
        while (this.process.isRunning()) {
            try {
                while ((line = buf.readLine()) != null) {
                    ProcessMonitorThread.LOGGER.info("Client> " + line);
                    this.process.getSysOutLines().add(line);
                }
            }
            catch (IOException ex) {
                ProcessMonitorThread.LOGGER.error(ex);
            }
            finally {
                IOUtils.closeQuietly(reader);
            }
        }
        final JavaProcessRunnable onExit = this.process.getExitRunnable();
        if (onExit != null) {
            onExit.onJavaProcessEnded(this.process);
        }
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
