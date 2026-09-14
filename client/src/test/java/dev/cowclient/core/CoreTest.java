package dev.cowclient.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @TempDir Path dir;
    @Test void clicksUseRollingSecond() {
        var c=new ClickCounter();c.click(0,0);c.click(0,999);c.click(1,500);
        assertEquals(2,c.left(999));assertEquals(1,c.left(1000));assertEquals(0,c.right(1500));
    }
    @Test void searchIsCaseInsensitiveAndScoped() {
        assertEquals(1,Modules.search("Visual","ZOOM").size());
        assertTrue(Modules.search("HUD","ZOOM").isEmpty());
    }
    @Test void configRoundTripsAndRejectsTraversal() {
        var c=new Config(dir);c.load("Default");c.toggle("coords");c.load("Default");
        assertTrue(c.enabled("coords"));
        assertThrows(IllegalArgumentException.class,()->c.load("../other"));
    }
    @Test void malformedConfigurationIsPreserved() throws Exception {
        Path p=dir.resolve("default.json");Files.writeString(p,"not-json");
        var c=new Config(dir);c.load("Default");c.toggle("fps");
        assertEquals("not-json",Files.readString(p));assertFalse(c.error.isEmpty());
    }
    @Test void invalidScaleIsBounded() {
        assertEquals(1,Config.finiteClamp(Double.NaN,.65,1.75,1));
        assertEquals(1.75,Config.finiteClamp(100,.65,1.75,1));
    }
}
