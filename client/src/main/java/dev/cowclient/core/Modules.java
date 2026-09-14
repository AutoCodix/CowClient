package dev.cowclient.core;

import java.util.List;
import java.util.Locale;

public final class Modules {
    private Modules() {}
    public static final List<Module> ALL = List.of(
        new Module("fps","FPS","Know how your game is running.","HUD","144 FPS",true),
        new Module("cps","CPS","Actual left and right click counts.","HUD","0 / 0 CPS",true),
        new Module("keys","Keystrokes","Your movement inputs on display.","HUD","W A S D",true),
        new Module("ping","Ping","Your connection, at a glance.","HUD","32 ms",true),
        new Module("coords","Coordinates","Keep your bearings.","HUD","-128  72  304",false),
        new Module("armor","Armor","Remaining equipment durability.","HUD","HEAD 92%  CHEST 85%",false),
        new Module("speed","Speed","Horizontal blocks per second.","HUD","4.32 b/s",false),
        new Module("direction","Direction","Know which way you are facing.","HUD","North",false),
        new Module("session","Session","Time spent in the current world.","HUD","00:24:18",false),
        new Module("memory","Memory","Java heap usage, not system RAM.","HUD","2048 / 4096 MB",false),
        new Module("clock","Clock","Your local time.","HUD","18:42",false),
        new Module("zoom","Zoom","Hold C. Rebind in Minecraft controls.","Visual","4.0× magnification",true),
        new Module("steady","Steady camera","Disable walking camera bob.","Visual","Calmer movement",false),
        new Module("hurt","Hurt tilt","Disable the damage camera tilt.","Visual","Steady on impact",false),
        new Module("sprint","Toggle sprint","Tap G to keep sprint held.","Utility","G · toggle",false),
        new Module("motion","Reduced motion","Skip menu opening animations.","Utility","Less motion",false)
    );
    public static List<Module> search(String category, String query) {
        String needle = query.toLowerCase(Locale.ROOT).strip();
        return ALL.stream().filter(m -> category.equals("All") || m.category().equals(category))
            .filter(m -> (m.name()+" "+m.description()).toLowerCase(Locale.ROOT).contains(needle)).toList();
    }
}
