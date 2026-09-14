package dev.cowclient.ui;

import dev.cowclient.CowClient;
import dev.cowclient.core.Config;
import dev.cowclient.core.Module;
import dev.cowclient.core.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class CowScreen extends Screen {
    private static final int W=1080,H=680;
    private final Surface surface=new Surface("menu",W,H);
    private final List<Hit> hits=new ArrayList<>();
    private String category="All",query="",detail="";
    private int scroll,hover=-1,focus=-1;
    private boolean searchFocused;
    private long opened=System.nanoTime();
    private double scale=1,originX,originY;
    private record Hit(int x,int y,int w,int h,Runnable click,Runnable right) {
        boolean contains(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    }
    public CowScreen(){super(Component.literal("CowClient modules"));}
    @Override public boolean isPauseScreen(){return false;}
    private void button(Graphics2D g,String label,int x,int y,int w,int h,Runnable click) {
        boolean hot=hover==hits.size()||focus==hits.size();
        Theme.box(g,x,y,w,h,12,hot?new Color(0x35432b):new Color(0x252e20));
        Theme.text(g,label,x+15,y+(h-18)/2,15,Theme.INK,true);
        hits.add(new Hit(x,y,w,h,click,click));
    }
    private void changed(){CowClient.config.revision++;CowClient.config.save();}
    private void paint(Graphics2D g) {
        hits.clear();
        Theme.box(g,0,0,W,H,24,Theme.BG);
        Theme.box(g,0,0,212,H,24,new Color(0x191f16));
        Theme.cow(g,20,20,64);Theme.text(g,"cowclient",83,33,24,Theme.INK,true);
        Theme.text(g,"MAKE IT YOURS",26,116,11,Theme.MUTED,true);
        int y=156;
        for(String c:List.of("All","HUD","Visual","Utility")) {
            final String selected=c;
            button(g,c.equals("All")?"All modules":c,18,y,175,46,()->{category=selected;query="";scroll=0;detail="";focus=-1;});
            if(category.equals(c)) Theme.box(g,22,y+13,4,20,2,Theme.ACCENT);y+=58;
        }
        Theme.text(g,"YOUR PROFILE",26,446,11,Theme.MUTED,true);
        button(g,CowClient.config.profile+"  ›",18,476,175,44,()->{
            var cfg=CowClient.config;cfg.save();cfg.load(Config.PROFILES.get((Config.PROFILES.indexOf(cfg.profile)+1)%3));
            CowClient.sprintLatched=false;
        });
        button(g,"Arrange HUD",18,534,175,44,()->{if(minecraft.player!=null)minecraft.setScreen(new HudEditor());});
        button(g,"Close",18,604,175,44,this::onClose);
        Theme.text(g,"Modules",244,33,31,Theme.INK,true);
        Theme.text(g,"The little things that make it yours.",246,80,15,Theme.MUTED,false);
        Theme.box(g,685,33,367,44,13,searchFocused?new Color(0x303b28):new Color(0x252e20));
        Theme.text(g,query.isEmpty()?"Search modules…":query,701,45,16,query.isEmpty()?Theme.MUTED:Theme.INK,false);
        hits.add(new Hit(685,33,367,44,()->{searchFocused=true;detail="";},()->{query="";scroll=0;}));
        if(!detail.isEmpty()) {paintDetail(g);return;}
        List<Module> modules=Modules.search(category,query);
        int max=Math.max(0,(modules.size()+2)/3-2);scroll=Math.min(max,Math.max(0,scroll));
        for(int n=scroll*3;n<Math.min(modules.size(),scroll*3+6);n++) {
            Module m=modules.get(n);int i=n-scroll*3;
            int x=244+(i%3)*273,cy=142+(i/3)*205;
            boolean hot=hover==hits.size()||focus==hits.size();
            Theme.box(g,x,cy,258,188,16,hot?new Color(0x2e3827):Theme.CARD);
            Theme.text(g,m.name(),x+18,cy+18,20,Theme.INK,true);
            Theme.text(g,m.description(),x+18,cy+51,11,Theme.MUTED,false);
            Theme.box(g,x+16,cy+83,226,58,10,new Color(0x151b12));
            Theme.text(g,m.preview(),x+27,cy+102,15,Theme.ACCENT,true);
            boolean enabled=CowClient.active(m.id());
            Theme.text(g,enabled?"Enabled":"Disabled",x+18,cy+158,12,Theme.MUTED,false);
            Theme.toggle(g,x+199,cy+153,enabled);
            hits.add(new Hit(x,cy,258,188,()->CowClient.config.toggle(m.id()),()->{detail=m.id();searchFocused=false;focus=-1;}));
        }
        if(modules.isEmpty()) Theme.text(g,"No modules found. Try another search.",270,220,21,Theme.MUTED,false);
        button(g,"‹",245,575,45,38,()->scroll=Math.max(0,scroll-1));
        Theme.text(g,(scroll+1)+" / "+(max+1),310,584,14,Theme.MUTED,false);
        button(g,"›",378,575,45,38,()->scroll=Math.min(max,scroll+1));
        button(g,CowClient.config.data.blur?"Blur on":"Blur off",686,575,118,38,()->{CowClient.config.data.blur=!CowClient.config.data.blur;changed();});
        button(g,"HUD −",816,575,112,38,()->{CowClient.config.data.hudScale=Math.max(.65,CowClient.config.data.hudScale-.1);changed();});
        button(g,"HUD +",940,575,112,38,()->{CowClient.config.data.hudScale=Math.min(1.75,CowClient.config.data.hudScale+.1);changed();});
        Theme.text(g,CowClient.config.error.isEmpty()?"Right-click for settings · Tab / Enter to navigate · Scroll for more":CowClient.config.error,246,643,12,Theme.MUTED,false);
    }
    private void paintDetail(Graphics2D g) {
        Module m=Modules.ALL.stream().filter(a->a.id().equals(detail)).findFirst().orElseThrow();
        Theme.text(g,m.name(),250,152,30,Theme.INK,true);
        Theme.text(g,m.description(),250,203,16,Theme.MUTED,false);
        button(g,CowClient.active(m.id())?"Disable module":"Enable module",250,261,248,48,()->CowClient.config.toggle(m.id()));
        if(m.id().equals("zoom")) {
            Theme.text(g,"Magnification: "+String.format(java.util.Locale.ROOT,"%.1f×",CowClient.config.data.zoomFactor),250,349,21,Theme.INK,false);
            button(g,"−",250,399,70,44,()->{CowClient.config.data.zoomFactor=Math.max(2,CowClient.config.data.zoomFactor-1);changed();});
            button(g,"+",333,399,70,44,()->{CowClient.config.data.zoomFactor=Math.min(12,CowClient.config.data.zoomFactor+1);changed();});
        } else if(m.isHud()) {
            Theme.text(g,"Use Arrange HUD to move this panel.",250,349,18,Theme.MUTED,false);
            Theme.text(g,"HUD size applies to all panels on this profile.",250,384,16,Theme.MUTED,false);
        } else {
            Theme.text(g,"Keybinds are in Options → Controls → CowClient.",250,349,17,Theme.MUTED,false);
        }
        button(g,"Back to modules",250,568,242,46,()->{detail="";focus=-1;});
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) {
        if(CowClient.config.data.blur) super.renderBackground(g,mx,my,delta);
        else g.fill(0,0,width,height,0x88101310);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        double elapsed=(System.nanoTime()-opened)/1e9;
        double progress=CowClient.active("motion")?1:Math.min(1,elapsed/.22);
        double ease=1-Math.pow(1-progress,3);
        scale=Math.min((width-18.0)/W,(height-18.0)/H)*(.96+.04*ease);
        originX=(width-W*scale)/2;originY=(height-H*scale)/2+(1-ease)*8;
        double lx=(mx-originX)/scale,ly=(my-originY)/scale;hover=-1;
        for(int i=0;i<hits.size();i++)if(hits.get(i).contains(lx,ly))hover=i;
        String state=category+"|"+query+"|"+scroll+"|"+hover+"|"+focus+"|"+searchFocused+"|"+detail+"|"+CowClient.config.revision;
        surface.paint(state,this::paint);
        surface.draw(g,(int)originX,(int)originY,(int)(W*scale),(int)(H*scale));
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        double x=(e.x()-originX)/scale,y=(e.y()-originY)/scale;
        searchFocused=false;
        for(Hit h:List.copyOf(hits)) if(h.contains(x,y)){if(e.button()==1)h.right.run();else if(e.button()==0)h.click.run();return true;}
        return false;
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        scroll=Math.max(0,scroll+(vertical<0?1:-1));return true;
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if(e.key()==GLFW.GLFW_KEY_ESCAPE || e.key()==GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if(!detail.isEmpty())detail="";else onClose();return true;
        }
        if(e.key()==GLFW.GLFW_KEY_TAB) {
            searchFocused=false;focus=(focus+1)%Math.max(1,hits.size());return true;
        }
        if((e.key()==GLFW.GLFW_KEY_ENTER || e.key()==GLFW.GLFW_KEY_SPACE) && focus>=0 && focus<hits.size()) {hits.get(focus).click.run();return true;}
        if(searchFocused && e.key()==GLFW.GLFW_KEY_BACKSPACE) {
            if(!query.isEmpty())query=query.substring(0,query.offsetByCodePoints(query.length(),-1));
            scroll=0;return true;
        }
        return super.keyPressed(e);
    }
    @Override public boolean charTyped(CharacterEvent e) {
        if(searchFocused && e.isAllowedChatCharacter() && query.length()<40) {query+=e.codepointAsString();scroll=0;return true;}
        return false;
    }
    @Override public void removed(){surface.close();}
    @Override public void onClose(){CowClient.config.save();minecraft.setScreen(null);}
}
