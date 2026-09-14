package dev.cowclient.ui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.function.Consumer;

public final class Surface implements AutoCloseable {
    private final Identifier id;
    private final int width,height;
    private final BufferedImage bitmap;
    private DynamicTexture texture;
    private Object key;
    public Surface(String name,int width,int height) {
        this.id=Identifier.fromNamespaceAndPath("cowclient","dynamic/"+name);
        this.width=width;this.height=height;
        bitmap=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
    }
    public void paint(Object state,Consumer<Graphics2D> painter) {
        if(texture!=null && java.util.Objects.equals(key,state)) return;
        Graphics2D g=bitmap.createGraphics();
        g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,width,height);
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        try { painter.accept(g); } finally { g.dispose(); }
        if(texture==null) {
            texture=new DynamicTexture(()->"CowClient UI",new NativeImage(width,height,false)) {{ sampler=RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE,AddressMode.CLAMP_TO_EDGE,FilterMode.LINEAR,FilterMode.LINEAR,false); }};
            Minecraft.getInstance().getTextureManager().register(id,texture);
        }
        int[] pixels=((DataBufferInt)bitmap.getRaster().getDataBuffer()).getData();
        NativeImage nativeImage=texture.getPixels();
        if(nativeImage==null) return;
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) nativeImage.setPixel(x,y,pixels[y*width+x]);
        texture.upload();key=state;
    }
    public void draw(GuiGraphics g,int x,int y,int w,int h) {
        if(texture!=null) g.blit(RenderPipelines.GUI_TEXTURED,id,x,y,0,0,w,h,width,height,width,height);
    }
    @Override public void close() {
        if(texture!=null) Minecraft.getInstance().getTextureManager().release(id);
        texture=null;key=null;
    }
}
