package dev.cowclient.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class LocalAccount {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("spatialclient").resolve("account.json");
    private static final int ITERATIONS=120_000;
    private static Data data;
    private static boolean unlocked;

    private LocalAccount(){}

    public static synchronized void init(){
        if(data!=null)return;
        data=read();
        unlocked=data!=null && data.remembered;
    }
    public static synchronized boolean hasAccount(){init();return data!=null && data.username!=null && data.salt!=null && data.hash!=null;}
    public static synchronized boolean unlocked(){init();return unlocked;}
    public static synchronized String username(){init();return hasAccount()?data.username:"Not registered";}

    public static synchronized Result register(String username,String password,boolean remember){
        init();
        String clean=username==null?"":username.trim();
        if(clean.length()<3||clean.length()>20)return new Result(false,"Username must be 3-20 characters.");
        if(!clean.matches("[A-Za-z0-9_]+"))return new Result(false,"Use letters, numbers or underscore.");
        if(password==null||password.length()<6)return new Result(false,"Password must be at least 6 characters.");
        try{
            byte[] salt=new byte[16];RANDOM.nextBytes(salt);
            byte[] hash=derive(password.toCharArray(),salt);
            data=new Data();data.username=clean;data.salt=Base64.getEncoder().encodeToString(salt);
            data.hash=Base64.getEncoder().encodeToString(hash);data.remembered=remember;
            write();unlocked=true;return new Result(true,"Profile created.");
        }catch(Exception e){return new Result(false,"Could not create local profile.");}
    }

    public static synchronized Result login(String username,String password,boolean remember){
        init();
        if(!hasAccount())return new Result(false,"No local profile exists yet.");
        if(username==null||!data.username.equalsIgnoreCase(username.trim())||password==null)
            return new Result(false,"Username or password is incorrect.");
        try{
            byte[] actual=derive(password.toCharArray(),Base64.getDecoder().decode(data.salt));
            if(!MessageDigest.isEqual(actual,Base64.getDecoder().decode(data.hash)))
                return new Result(false,"Username or password is incorrect.");
            data.remembered=remember;write();unlocked=true;return new Result(true,"Signed in.");
        }catch(Exception e){return new Result(false,"Could not read local profile.");}
    }

    public static synchronized void lock(){
        init();unlocked=false;
        if(data!=null){data.remembered=false;write();}
    }

    private static byte[] derive(char[] password,byte[] salt)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        finally{spec.clearPassword();java.util.Arrays.fill(password,'\0');}
    }
    private static Data read(){
        try{
            if(!Files.isRegularFile(FILE))return null;
            Data v=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Data.class);
            return v!=null&&v.username!=null&&v.salt!=null&&v.hash!=null?v:null;
        }catch(Exception e){return null;}
    }
    private static void write(){
        try{Files.createDirectories(FILE.getParent());Files.writeString(FILE,JSON.toJson(data),StandardCharsets.UTF_8);}
        catch(Exception ignored){}
    }
    private static final class Data{String username;String salt;String hash;boolean remembered;}
    public record Result(boolean ok,String message){}
}
