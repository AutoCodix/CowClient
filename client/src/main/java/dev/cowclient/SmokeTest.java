package dev.cowclient;

import dev.cowclient.ui.CowScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.slf4j.LoggerFactory;

final class SmokeTest {
    static void install() {
        if(!Boolean.getBoolean("cowclient.smoke"))return;
        final int[] ticks={0};
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            ticks[0]++;
            if(ticks[0]==140)mc.setScreen(new CowScreen());
            if(ticks[0]==210) {
                try {
                    if(!(mc.screen instanceof CowScreen))throw new IllegalStateException("CowScreen did not open");
                    var out=mc.gameDirectory.toPath().resolve("cowclient-smoke.png");
                    Files.createDirectories(out.getParent());
                    var image=new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                    ImageIO.write(image,"png",out.toFile());
                    LoggerFactory.getLogger("CowClient").info("COWCLIENT_SMOKE_OK");
                } catch(Exception e) {throw new RuntimeException("CowClient smoke test failed",e);}
            }
            if(ticks[0]==220)mc.stop();
        });
    }
}
