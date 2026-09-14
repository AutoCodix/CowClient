package dev.cowclient;

import dev.cowclient.ui.CowScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
import java.nio.file.Files;
import java.util.Base64;
import org.slf4j.LoggerFactory;

final class SmokeTest {
    static void install() {
        if(!Boolean.getBoolean("cowclient.smoke"))return;
        final int[] ticks={0};
        final boolean[] captured={false};
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            ticks[0]++;
            if(ticks[0]==140)mc.setScreen(new CowScreen());
            if(ticks[0]==210) {
                if(!(mc.screen instanceof CowScreen))throw new IllegalStateException("CowScreen did not open");
                Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{
                    try(image) {
                        var out=mc.gameDirectory.toPath().resolve("cowclient-smoke.png");
                        Files.createDirectories(out.getParent());image.writeToFile(out);
                        LoggerFactory.getLogger("CowClient").info("COWCLIENT_SMOKE_OK");
                        LoggerFactory.getLogger("CowClient").info("COWCLIENT_PNG:{}",Base64.getEncoder().encodeToString(Files.readAllBytes(out)));
                        captured[0]=true;
                    } catch(Exception e) {throw new RuntimeException("CowClient screenshot check failed",e);}
                });
            }
            if(ticks[0]>220 && captured[0])mc.stop();
            if(ticks[0]>400)throw new IllegalStateException("Screenshot callback timed out");
        });
    }
}
