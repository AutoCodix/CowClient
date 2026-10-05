package dev.cowclient.ui;

import dev.cowclient.core.LocalAccount;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

public final class AccountScreen extends Screen {
    private EditBox username;
    private EditBox password;
    private boolean registerMode;
    private boolean remember=true;
    private String status="";

    public AccountScreen(){
        super(Component.literal("Spatial Client"));
        LocalAccount.init();
        registerMode=!LocalAccount.hasAccount();
    }

    @Override protected void init(){
        int cardW=Math.min(420,width-36);
        int x=(width-cardW)/2;
        int y=Math.max(56,height/2-160);
        username=new EditBox(font,x+44,y+102,cardW-88,24,Component.literal("Username"));
        username.setMaxLength(20);
        if(!registerMode&&LocalAccount.hasAccount())username.setValue(LocalAccount.username());
        addRenderableWidget(username);

        password=new EditBox(font,x+44,y+150,cardW-88,24,Component.literal("Password"));
        password.setMaxLength(64);
        addRenderableWidget(password);

        addRenderableWidget(Button.builder(Component.literal("Remember this device: ON"),b->{
            remember=!remember;
            b.setMessage(Component.literal("Remember this device: "+(remember?"ON":"OFF")));
        }).bounds(x+44,y+190,cardW-88,24).build());

        addRenderableWidget(Button.builder(Component.literal(registerMode?"Create local profile":"Sign in"),b->submit())
            .bounds(x+44,y+228,cardW-88,28).build());

        addRenderableWidget(Button.builder(Component.literal(registerMode?"I already have a profile":"Use another local profile"),b->{
            registerMode=!LocalAccount.hasAccount();
            status="";
        }).bounds(x+44,y+264,cardW-88,22).build());

        setInitialFocus(registerMode?username:password);
    }

    private void submit(){
        LocalAccount.Result r=registerMode
            ?LocalAccount.register(username.getValue(),password.getValue(),remember)
            :LocalAccount.login(username.getValue(),password.getValue(),remember);
        status=r.message();password.setValue("");
        if(r.ok()&&minecraft!=null)minecraft.setScreen(new CowScreen());
    }

    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){
        g.fill(0,0,width,height,0xff01040d);
        for(int i=0;i<86;i++){
            int x=Math.floorMod(i*173+47,Math.max(1,width));
            int y=Math.floorMod(i*97+31,Math.max(1,height));
            int c=i%13==0?0xffb8d8ff:i%9==0?0xff8b78ff:0xff40577f;
            g.fill(x,y,x+(i%17==0?2:1),y+(i%17==0?2:1),c);
        }
    }

    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g,mx,my,delta);
        int cardW=Math.min(420,width-36),cardH=330;
        int x=(width-cardW)/2,y=Math.max(40,height/2-cardH/2);
        g.fill(x,y,x+cardW,y+cardH,0xee070b18);
        g.renderOutline(x,y,cardW,cardH,0xff273b6d);
        g.drawString(font,registerMode?"Create Spatial profile":"Welcome back",x+44,y+30,0xfff3f6ff,false);
        g.drawString(font,"Local only - credentials never leave this device.",x+44,y+52,0xff7f8caf,false);
        g.drawString(font,"USERNAME",x+44,y+88,0xff7180a5,false);
        g.drawString(font,"PASSWORD",x+44,y+136,0xff7180a5,false);
        if(!status.isBlank())g.drawCenteredString(font,status,width/2,y+304,status.toLowerCase().contains("incorrect")?0xffff92a0:0xff9cc5ff);
        super.render(g,mx,my,delta);
    }

    @Override public boolean isPauseScreen(){return false;}
}
