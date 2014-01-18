package net.minecraft.launcher.versions;

import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import net.minecraft.launcher.OperatingSystem;
import java.util.Map;
import java.util.List;
import org.apache.commons.lang3.text.StrSubstitutor;

public class Library
{
    private static final StrSubstitutor SUBSTITUTOR;
    private String name;
    private List<Rule> rules;
    private Map<OperatingSystem, String> natives;
    private ExtractRules extract;
    private String url;
    
    public Library() {
        super();
    }
    
    public Library(final String name) {
        super();
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Library name cannot be null or empty");
        }
        this.name = name;
    }
    
    public Library(final Library library) {
        super();
        this.name = library.name;
        this.url = library.url;
        if (library.extract != null) {
            this.extract = new ExtractRules(library.extract);
        }
        if (library.rules != null) {
            this.rules = new ArrayList<Rule>();
            for (final Rule rule : library.rules) {
                this.rules.add(new Rule(rule));
            }
        }
        if (library.natives != null) {
            this.natives = new LinkedHashMap<OperatingSystem, String>();
            for (final Map.Entry<OperatingSystem, String> entry : library.getNatives().entrySet()) {
                this.natives.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public String getName() {
        return this.name;
    }
    
    public Library addNative(final OperatingSystem operatingSystem, final String name) {
        if (operatingSystem == null || !operatingSystem.isSupported()) {
            throw new IllegalArgumentException("Cannot add native for unsupported OS");
        }
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Cannot add native for null or empty name");
        }
        if (this.natives == null) {
            this.natives = new EnumMap<OperatingSystem, String>(OperatingSystem.class);
        }
        this.natives.put(operatingSystem, name);
        return this;
    }
    
    public List<Rule> getRules() {
        return this.rules;
    }
    
    public boolean appliesToCurrentEnvironment() {
        if (this.rules == null) {
            return true;
        }
        Rule.Action lastAction = Rule.Action.DISALLOW;
        for (final Rule rule : this.rules) {
            final Rule.Action action = rule.getAppliedAction();
            if (action != null) {
                lastAction = action;
            }
        }
        return lastAction == Rule.Action.ALLOW;
    }
    
    public Map<OperatingSystem, String> getNatives() {
        return this.natives;
    }
    
    public ExtractRules getExtractRules() {
        return this.extract;
    }
    
    public Library setExtractRules(final ExtractRules rules) {
        this.extract = rules;
        return this;
    }
    
    public String getArtifactBaseDir() {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact dir of empty/blank artifact");
        }
        final String[] parts = this.name.split(":", 3);
        return String.format("%s/%s/%s", parts[0].replaceAll("\\.", "/"), parts[1], parts[2]);
    }
    
    public String getArtifactPath() {
        return this.getArtifactPath(null);
    }
    
    public String getArtifactPath(final String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact path of empty/blank artifact");
        }
        return String.format("%s/%s", this.getArtifactBaseDir(), this.getArtifactFilename(classifier));
    }
    
    public String getArtifactFilename(final String classifier) {
        if (this.name == null) {
            throw new IllegalStateException("Cannot get artifact filename of empty/blank artifact");
        }
        final String[] parts = this.name.split(":", 3);
        final String result = String.format("%s-%s%s.jar", parts[1], parts[2], StringUtils.isEmpty(classifier) ? "" : ("-" + classifier));
        return Library.SUBSTITUTOR.replace(result);
    }
    
    @Override
    public String toString() {
        return "Library{name='" + this.name + '\'' + ", rules=" + this.rules + ", natives=" + this.natives + ", extract=" + this.extract + '}';
    }
    
    public boolean hasCustomUrl() {
        return this.url != null;
    }
    
    public String getDownloadUrl() {
        if (this.url != null) {
            return this.url;
        }
        return "https://libraries.minecraft.net/";
    }
    
    static {
        SUBSTITUTOR = new StrSubstitutor((Map<String, String>)new HashMap<String, String>() {
            {
                this.put("arch", System.getProperty("os.arch").contains("64") ? "64" : "32");
            }
        });
    }
}
