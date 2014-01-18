package net.minecraft.launcher.ui.tabs;

import javax.swing.JScrollBar;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.SwingUtilities;
import com.mojang.util.QueueLogAppender;
import java.awt.Component;
import java.awt.Insets;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.minecraft.launcher.Launcher;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import java.awt.Font;
import javax.swing.JScrollPane;

public class ConsoleTab extends JScrollPane
{
    private static final Font MONOSPACED;
    private final JTextPane console;
    private final JPopupMenu popupMenu;
    private final JMenuItem copyTextButton;
    private final Launcher launcher;
    
    public ConsoleTab(final Launcher launcher) {
        super();
        this.console = new JTextPane();
        this.popupMenu = new JPopupMenu();
        this.copyTextButton = new JMenuItem("Copy All Text");
        this.launcher = launcher;
        this.popupMenu.add(this.copyTextButton);
        this.console.setComponentPopupMenu(this.popupMenu);
        this.copyTextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    final StringSelection ss = new StringSelection(ConsoleTab.this.console.getText());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                }
                catch (Exception ex) {}
            }
        });
        this.console.setFont(ConsoleTab.MONOSPACED);
        this.console.setEditable(false);
        this.console.setMargin(null);
        this.setViewportView(this.console);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String line;
                while ((line = QueueLogAppender.getNextLogEvent("DevelopmentConsole")) != null) {
                    ConsoleTab.this.print(line);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    public void print(final String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ConsoleTab.this.print(line);
                }
            });
            return;
        }
        final Document document = this.console.getDocument();
        final JScrollBar scrollBar = this.getVerticalScrollBar();
        boolean shouldScroll = false;
        if (this.getViewport().getView() == this.console) {
            shouldScroll = (scrollBar.getValue() + scrollBar.getSize().getHeight() + ConsoleTab.MONOSPACED.getSize() * 4 > scrollBar.getMaximum());
        }
        try {
            document.insertString(document.getLength(), line, null);
        }
        catch (BadLocationException ex) {}
        if (shouldScroll) {
            scrollBar.setValue(Integer.MAX_VALUE);
        }
    }
    
    static {
        MONOSPACED = new Font("Monospaced", 0, 12);
    }
}
