package dev.cowclient.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Config {
    private static final Logger LOG = LoggerFactory.getLogger("CowClient");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path directory;
    public String profile = "Default";
    public Data data = new Data();
    public String error = "";
    public long revision;
    public static final List<String> PROFILES = List.of("Default","Competitive","Chill");

    public static final class Data {
        public Map<String,Boolean> enabled = new HashMap<>();
        public Map<String,Position> positions = new HashMap<>();
        public double hudScale = 1.0;
        public double zoomFactor = 4.0;
        public boolean blur = true;
    }
    public static final class Position {
        public double x, y;
        public Position(double x, double y) { this.x=x; this.y=y; }
    }
    public Config(Path directory) { this.directory=directory; }
    public boolean enabled(String id) {
        return data.enabled.getOrDefault(id, Modules.ALL.stream().filter(m->m.id().equals(id)).findFirst().map(Module::defaultEnabled).orElse(false));
    }
    public void toggle(String id) { data.enabled.put(id,!enabled(id)); revision++; save(); }
    public void load(String name) {
        if (!PROFILES.contains(name)) throw new IllegalArgumentException("Unknown profile");
        profile=name; data=new Data(); error="";
        Path file=directory.resolve(name.toLowerCase(Locale.ROOT)+".json");
        if(Files.exists(file)) {
            try (var r=Files.newBufferedReader(file)) {
                Data read=JSON.fromJson(r,Data.class);
                if(read==null) throw new IOException("Empty configuration");
                data=read;
                if(data.enabled==null) data.enabled=new HashMap<>();
                if(data.positions==null) data.positions=new HashMap<>();
                data.hudScale=finiteClamp(data.hudScale,.65,1.75,1);
                data.zoomFactor=finiteClamp(data.zoomFactor,2,12,4);
                data.positions.entrySet().removeIf(e->e.getValue()==null || !Double.isFinite(e.getValue().x) || !Double.isFinite(e.getValue().y));
            } catch(Exception ex) { data=new Data(); error="Profile could not be read; original file preserved."; LOG.warn(error,ex); }
        } else if(name.equals("Competitive")) {
            for(String id:List.of("armor","speed","steady","hurt")) data.enabled.put(id,true);
        } else if(name.equals("Chill")) {
            for(String id:List.of("cps","keys")) data.enabled.put(id,false);
            for(String id:List.of("coords","clock")) data.enabled.put(id,true);
        }
        revision++;
    }
    public static double finiteClamp(double value,double min,double max,double fallback) {
        return Double.isFinite(value) ? Math.max(min,Math.min(max,value)) : fallback;
    }
    public void save() {
        if(!error.isEmpty()) return;
        try {
            Files.createDirectories(directory);
            Path target=directory.resolve(profile.toLowerCase(Locale.ROOT)+".json");
            Path temp=Files.createTempFile(directory,"profile-",".tmp");
            try {
                Files.writeString(temp,JSON.toJson(data));
                try { Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                catch(AtomicMoveNotSupportedException e) { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temp); }
        } catch(IOException ex) { error="Settings could not be saved."; LOG.warn(error,ex); }
    }
}
