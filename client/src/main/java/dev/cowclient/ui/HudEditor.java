package dev.cowclient.ui;

import dev.cowclient.CowClient;
import dev.cowclient.core.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditor extends Screen {
    private Hud.Bounds dragging;
    private double dx,dy;
    public HudEditor(){super(Component.literal("CowClient HUD editor"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        g.drawCenteredString(font,"Drag your HUD panels · Esc to save",width/2,height-24,0xffefefdf);
        for(var b:Hud.bounds()) g.renderOutline(b.x()-1,b.y()-1,b.width()+2,b.height()+2,b.contains(mx,my)?0xffc3ed91:0x666b7b60);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        if(e.button()!=0) return false;
        var list=Hud.bounds();
        for(int i=list.size()-1;i>=0;i--) if(list.get(i).contains(e.x(),e.y())) {
            dragging=list.get(i);dx=e.x()-dragging.x();dy=e.y()-dragging.y();return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double mx,double my) {
        if(dragging==null) return false;
        double x=Math.max(0,Math.min(width-dragging.width(),e.x()-dx));
        double y=Math.max(0,Math.min(height-dragging.height(),e.y()-dy));
        CowClient.config.data.positions.put(dragging.module().id(),new Config.Position(x/width,y/height));return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {dragging=null;CowClient.config.save();return true;}
    @Override public void onClose(){CowClient.config.save();minecraft.setScreen(new CowScreen());}
}
