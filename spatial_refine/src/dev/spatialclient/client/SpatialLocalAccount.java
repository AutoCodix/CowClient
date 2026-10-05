package dev.spatialclient.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class SpatialLocalAccount {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("spatialclient").resolve("account.json");
    private static final int ITERATIONS = 120_000;
    private static AccountData data;
    private static boolean unlocked;

    private SpatialLocalAccount() {}

    public static synchronized void init() {
        if (data != null) return;
        data = read();
        unlocked = data != null && data.remembered;
    }

    public static synchronized boolean hasAccount() {
        init();
        return data != null && data.username != null && data.hash != null && data.salt != null;
    }

    public static synchronized boolean isUnlocked() {
        init();
        return unlocked;
    }

    public static synchronized String username() {
        init();
        return hasAccount() ? data.username : "Not registered";
    }

    public static synchronized Result register(String username, String password, boolean remember) {
        init();
        String clean = username == null ? "" : username.trim();
        if (clean.length() < 3 || clean.length() > 20) return new Result(false, "Username must be 3-20 characters.");
        if (!clean.matches("[A-Za-z0-9_]+")) return new Result(false, "Use letters, numbers or underscore.");
        if (password == null || password.length() < 6) return new Result(false, "Password must be at least 6 characters.");

        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            byte[] hash = derive(password.toCharArray(), salt);
            data = new AccountData();
            data.username = clean;
            data.salt = Base64.getEncoder().encodeToString(salt);
            data.hash = Base64.getEncoder().encodeToString(hash);
            data.remembered = remember;
            write(data);
            unlocked = true;
            return new Result(true, "Account created.");
        } catch (Exception e) {
            return new Result(false, "Could not create local account.");
        }
    }

    public static synchronized Result login(String username, String password, boolean remember) {
        init();
        if (!hasAccount()) return new Result(false, "No local account exists yet.");
        if (username == null || !data.username.equalsIgnoreCase(username.trim())) return new Result(false, "Username or password is incorrect.");
        if (password == null) return new Result(false, "Username or password is incorrect.");

        try {
            byte[] salt = Base64.getDecoder().decode(data.salt);
            byte[] expected = Base64.getDecoder().decode(data.hash);
            byte[] actual = derive(password.toCharArray(), salt);
            if (!MessageDigest.isEqual(expected, actual)) return new Result(false, "Username or password is incorrect.");
            data.remembered = remember;
            write(data);
            unlocked = true;
            return new Result(true, "Signed in.");
        } catch (Exception e) {
            return new Result(false, "Could not read the local account.");
        }
    }

    public static synchronized void lock() {
        init();
        unlocked = false;
        if (data != null && data.remembered) {
            data.remembered = false;
            write(data);
        }
    }

    public static synchronized void forget() {
        data = null;
        unlocked = false;
        try {
            Files.deleteIfExists(FILE);
        } catch (Exception ignored) {
        }
    }

    private static byte[] derive(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static AccountData read() {
        try {
            if (!Files.isRegularFile(FILE)) return null;
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            AccountData read = GSON.fromJson(json, AccountData.class);
            if (read == null || read.username == null || read.salt == null || read.hash == null) return null;
            return read;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void write(AccountData value) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(value), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static final class AccountData {
        String username;
        String salt;
        String hash;
        boolean remembered;
    }

    public record Result(boolean ok, String message) {}
}
