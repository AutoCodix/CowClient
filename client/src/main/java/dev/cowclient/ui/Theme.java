package dev.cowclient.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class Theme {
    public static final Color BG=new Color(0x050814),CARD=new Color(0x0d1328),INK=new Color(0xf3f6ff),MUTED=new Color(0x7f8caf),ACCENT=new Color(0x4fa8ff);
    private static final Font base=new Font("SansSerif",Font.PLAIN,16);
    private static BufferedImage logo;
    static {
        try(var in=Theme.class.getResourceAsStream("/assets/cowclient/spatial_logo.png")) {
            if(in!=null) logo=ImageIO.read(in);
        } catch(Exception ignored) {}
    }

    private Theme() {}

    public static void box(Graphics2D g,int x,int y,int w,int h,int radius,Color color) {
        g.setColor(color);g.fillRoundRect(x,y,w,h,radius*2,radius*2);
    }
    public static void text(Graphics2D g,String s,int x,int y,int size,Color color,boolean bold) {
        g.setFont(base.deriveFont(bold?Font.BOLD:Font.PLAIN,(float)size));g.setColor(color);g.drawString(s,x,y+g.getFontMetrics().getAscent());
    }
    public static void spatial(Graphics2D g,int x,int y,int size) {
        int s=Math.max(28,size);
        box(g,x,y,s,s,12,new Color(0x071023));
        if(logo!=null) {
            Object old=g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(logo,x+2,y+2,s-4,s-4,null);
            if(old!=null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,old);
            return;
        }
        g.setColor(new Color(0x8d63ff));g.fillOval(x+5,y+8,s/3,s/3);
        g.setColor(new Color(0x39a7ff));g.drawString("S",x+s/2,y+s-7);
    }

    public static void starfield(Graphics2D g,int w,int h,int driftX,int driftY) {
        g.setColor(BG);g.fillRect(0,0,w,h);
        for(int i=0;i<110;i++) {
            int px=Math.floorMod(i*173+47+driftX*(1+i%3),Math.max(1,w));
            int py=Math.floorMod(i*97+31+driftY*(1+i%2),Math.max(1,h));
            int sz=i%19==0?2:1;
            g.setColor(i%13==0?new Color(0xb9dbff):i%9==0?new Color(0x8f7cff):new Color(0x41577f));
            g.fillRect(px,py,sz,sz);
        }
    }
    public static void toggle(Graphics2D g,int x,int y,boolean enabled) {
        box(g,x,y,40,22,11,enabled?new Color(0x235fbb):new Color(0x283149));
        box(g,x+(enabled?21:3),y+3,16,16,8,enabled?new Color(0xe9f4ff):new Color(0x8793af));
    }
}
