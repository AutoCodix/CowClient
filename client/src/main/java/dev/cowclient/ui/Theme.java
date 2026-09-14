package dev.cowclient.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class Theme {
    public static final Color BG=new Color(0x141914),CARD=new Color(0x20271d),INK=new Color(0xefefdf),MUTED=new Color(0x959f8c),ACCENT=new Color(0xc3ed91);
    private static Font base=new Font("SansSerif",Font.PLAIN,16);
    private static BufferedImage cow;
    static {
        try(var in=Theme.class.getResourceAsStream("/assets/cowclient/ui.ttf")) {
            if(in!=null) base=Font.createFont(Font.TRUETYPE_FONT,in);
        } catch(Exception ignored) {}
        try(var in=Theme.class.getResourceAsStream("/assets/cowclient/icon.png")) {
            if(in!=null) cow=ImageIO.read(in);
        } catch(Exception ignored) {}
    }
    public static void box(Graphics2D g,int x,int y,int w,int h,int radius,Color color) {
        g.setColor(color);g.fillRoundRect(x,y,w,h,radius*2,radius*2);
    }
    public static void text(Graphics2D g,String s,int x,int y,int size,Color color,boolean bold) {
        g.setFont(base.deriveFont(bold?Font.BOLD:Font.PLAIN,(float)size));g.setColor(color);g.drawString(s,x,y+g.getFontMetrics().getAscent());
    }
    public static void cow(Graphics2D g,int x,int y,int width) {
        if(cow!=null) g.drawImage(cow,x,y,width,width*200/256,null);
    }
    public static void toggle(Graphics2D g,int x,int y,boolean enabled) {
        box(g,x,y,40,22,11,enabled?ACCENT:new Color(0x46523e));
        box(g,x+(enabled?21:3),y+3,16,16,8,enabled?new Color(0x263521):MUTED);
    }
}
