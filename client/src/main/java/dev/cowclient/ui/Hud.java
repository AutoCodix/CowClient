package dev.cowclient.ui;

import dev.cowclient.CowClient;
import dev.cowclient.core.Config;
import dev.cowclient.core.Module;
import dev.cowclient.core.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import java.awt.Color;
import java.util.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Hud {
    private static final Map<String,Surface> surfaces=new HashMap<>();
    private static final Map<String,String> values=new HashMap<>();
    private static long nextUpdate;
    public record Bounds(Module module,int x,int y,int width,int height) {
        public boolean contains(double mx,double my) { return mx>=x && mx<x+width && my>=y && my<y+height; }
    }
    public static List<Bounds> bounds() {
        Minecraft mc=Minecraft.getInstance();var cfg=CowClient.config;
        int sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        int w=(int)(132*cfg.data.hudScale),h=(int)(39*cfg.data.hudScale),i=0;
        List<Bounds> out=new ArrayList<>();
        for(Module m:Modules.ALL) if(m.isHud() && cfg.enabled(m.id())) {
            int row=i%6,col=i/6;i++;
            var p=cfg.data.positions.get(m.id());
            int x=p==null?10+col*(w+7):(int)(p.x*sw);
            int y=p==null?10+row*(h+5):(int)(p.y*sh);
            x=Math.max(0,Math.min(sw-w,x));y=Math.max(0,Math.min(sh-h,y));
            out.add(new Bounds(m,x,y,w,h));
        }
        return out;
    }
    public static void render(GuiGraphics g) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.options.hideGui || CowClient.config==null || mc.screen instanceof CowScreen) return;
        long now=System.currentTimeMillis();
        if(now>=nextUpdate) {
            nextUpdate=now+100;
            for(Module m:Modules.ALL) if(m.isHud() && CowClient.active(m.id())) values.put(m.id(),value(m.id(),now));
        }
        for(Bounds b:bounds()) {
            String value=values.getOrDefault(b.module.id(),"—");
            Surface surface=surfaces.computeIfAbsent(b.module.id(),id->new Surface("hud_"+id,396,117));
            surface.paint(value,g2->{
                Theme.box(g2,0,0,396,117,24,new Color(20,25,20,218));
                Theme.text(g2,b.module.name(),23,14,24,Theme.MUTED,false);
                Theme.text(g2,value,23,52,value.length()>25?20:29,Theme.INK,true);
            });
            surface.draw(g,b.x,b.y,b.width,b.height);
        }
    }
    private static String value(String id,long now) {
        Minecraft mc=Minecraft.getInstance();var p=mc.player;
        if(p==null) return "—";
        return switch(id) {
            case "fps" -> mc.getFps()+" FPS";
            case "cps" -> CowClient.clicks.left(now)+" / "+CowClient.clicks.right(now)+" CPS";
            case "ping" -> {
                var info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(p.getUUID());
                yield info==null?"Local world":info.getLatency()+" ms";
            }
            case "coords" -> p.blockPosition().getX()+"  "+p.blockPosition().getY()+"  "+p.blockPosition().getZ();
            case "speed" -> String.format(Locale.ROOT,"%.2f b/s",Math.hypot(p.getDeltaMovement().x,p.getDeltaMovement().z)*20);
            case "direction" -> new String[]{"South","West","North","East"}[Math.floorMod((int)Math.floor(p.getYRot()/90+.5),4)];
            case "session" -> {long s=Math.max(0,(now-CowClient.worldStarted)/1000);yield String.format(Locale.ROOT,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
            case "memory" -> {Runtime r=Runtime.getRuntime();yield (r.totalMemory()-r.freeMemory())/1048576+" / "+r.maxMemory()/1048576+" MB";}
            case "clock" -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            case "keys" -> (mc.options.keyUp.isDown()?"[W]":" W ")+" "+(mc.options.keyLeft.isDown()?"[A]":" A ")+" "+(mc.options.keyDown.isDown()?"[S]":" S ")+" "+(mc.options.keyRight.isDown()?"[D]":" D ");
            case "armor" -> {
                StringBuilder s=new StringBuilder();String[] labels={"H","C","L","B"};int i=0;
                for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
                    var stack=p.getItemBySlot(slot);
                    s.append(labels[i++]).append(":").append(stack.isEmpty()?"—":stack.isDamageableItem()?Math.round(100f*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage())+"%":"100%").append(" ");
                }yield s.toString().strip();
            }
            default -> "";
        };
    }
}
