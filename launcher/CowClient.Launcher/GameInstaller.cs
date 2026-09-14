using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text.Json.Nodes;
using CmlLib.Core;
using CmlLib.Core.Auth;
using CmlLib.Core.ModLoaders.FabricMC;
using CmlLib.Core.ProcessBuilder;

namespace CowClient.Launcher;
public sealed class GameInstaller : IDisposable {
    public const string GameVersion="1.21.11";
    public const string LoaderVersion="0.18.1";
    public static string GameDirectory=>Path.Combine(SettingsStore.Root,"instances","1.21.11");
    private readonly HttpClient http=new(){Timeout=TimeSpan.FromMinutes(5)};
    public GameInstaller() {http.DefaultRequestHeaders.UserAgent.ParseAdd("CowClient/0.1.0 (github.com/AutoCodix/CowClient)");}
    public async Task<Process> PrepareAsync(Settings settings,MSession session,Action<string,double> progress,CancellationToken ct) {
        if(session==null || !session.CheckIsValid())throw new InvalidOperationException("Sign in with an owned Minecraft Java account first.");
        settings.Validate();Directory.CreateDirectory(GameDirectory);
        var path=new MinecraftPath(GameDirectory);
        var launcher=new MinecraftLauncher(path);
        launcher.FileProgressChanged+=(_,e)=>progress("Preparing Minecraft: "+e.Name,e.TotalTasks>0?(double)e.ProgressedTasks/e.TotalTasks:0);
        progress("Preparing Fabric "+LoaderVersion,0);
        var fabric=new FabricInstaller(http);
        string id=await fabric.Install(GameVersion,LoaderVersion,path);
        ct.ThrowIfCancellationRequested();
        await launcher.InstallAsync(id);
        ct.ThrowIfCancellationRequested();
        Directory.CreateDirectory(Path.Combine(GameDirectory,"mods"));
        await InstallBundledClient(ct);
        await EnsureMod("fabric-api",progress,ct);
        foreach(string slug in new[]{"sodium","lithium"}) {
            if(settings.PerformanceMods)await EnsureMod(slug,progress,ct);
            else DisableManaged(slug);
        }
        await File.WriteAllTextAsync(Path.Combine(GameDirectory,"cowclient-profile.txt"),settings.Profile,ct);
        progress("Building Minecraft launch",.99);
        return await launcher.BuildProcessAsync(id,new MLaunchOption {
            Session=session,MaximumRamMb=settings.MemoryMb,MinimumRamMb=1024,
            ScreenWidth=1280,ScreenHeight=720,GameLauncherName="CowClient",GameLauncherVersion="0.1.0"
        });
    }
    private async Task InstallBundledClient(CancellationToken ct) {
        string payload=Path.Combine(AppContext.BaseDirectory,"Payload","CowClient.jar");
        string hashFile=Path.Combine(AppContext.BaseDirectory,"Payload","CowClient.sha512");
        if(!File.Exists(payload)||!File.Exists(hashFile))throw new InvalidOperationException("CowClient.jar is missing from Payload. Extract the complete launcher ZIP, not just the executable.");
        byte[] bytes=await File.ReadAllBytesAsync(payload,ct);
        if(!HashMatches(bytes,(await File.ReadAllTextAsync(hashFile,ct)).Trim()))throw new InvalidOperationException("The bundled client failed its integrity check. Re-download the build.");
        string target=Path.Combine(GameDirectory,"mods","cowclient.jar");
        if(File.Exists(target)) {
            byte[] existing=await File.ReadAllBytesAsync(target,ct);
            if(HashMatches(existing,Convert.ToHexString(SHA512.HashData(bytes))))return;
            Backup(target);
        }
        await File.WriteAllBytesAsync(target+".tmp",bytes,ct);File.Move(target+".tmp",target,true);
    }
    private async Task EnsureMod(string slug,Action<string,double> progress,CancellationToken ct) {
        string target=Path.Combine(GameDirectory,"mods","cowclient-managed-"+slug+".jar");
        string manifest=target+".json";
        if(File.Exists(target)&&File.Exists(manifest)) {
            var saved=JsonNode.Parse(await File.ReadAllTextAsync(manifest,ct));
            if(saved?["sha512"]?.GetValue<string>() is string hash && HashMatches(await File.ReadAllBytesAsync(target,ct),hash))return;
        }
        progress("Installing "+slug+" for Minecraft "+GameVersion,0);
        string query="?loaders="+Uri.EscapeDataString("[\"fabric\"]")+"&game_versions="+Uri.EscapeDataString("[\""+GameVersion+"\"]");
        var versions=JsonNode.Parse(await http.GetStringAsync("https://api.modrinth.com/v2/project/"+slug+"/version"+query,ct))?.AsArray();
        var version=versions?.FirstOrDefault(v=>v?["version_type"]?.GetValue<string>()=="release")
            ??throw new InvalidOperationException("No stable "+slug+" build supports Minecraft "+GameVersion+".");
        var files=version["files"]!.AsArray();
        var entry=files.FirstOrDefault(v=>v?["primary"]?.GetValue<bool>()==true)??files.First();
        string url=entry!["url"]!.GetValue<string>();
        var uri=new Uri(url);
        if(uri.Scheme!="https"||uri.Host!="cdn.modrinth.com")throw new InvalidOperationException("Unexpected mod download host.");
        byte[] bytes=await http.GetByteArrayAsync(uri,ct);
        string expected=entry["hashes"]!["sha512"]!.GetValue<string>();
        if(!HashMatches(bytes,expected))throw new InvalidOperationException(slug+" failed its SHA-512 integrity check.");
        if(File.Exists(target))Backup(target);
        await File.WriteAllBytesAsync(target+".tmp",bytes,ct);File.Move(target+".tmp",target,true);
        await File.WriteAllTextAsync(manifest,new JsonObject{["project"]=slug,["version"]=version["id"]!.GetValue<string>(),["sha512"]=expected}.ToJsonString(),ct);
    }
    private static void DisableManaged(string slug) {
        string target=Path.Combine(GameDirectory,"mods","cowclient-managed-"+slug+".jar");
        if(File.Exists(target))Backup(target);
    }
    private static void Backup(string target) {
        string folder=Path.Combine(SettingsStore.Root,"backups");Directory.CreateDirectory(folder);
        File.Move(target,Path.Combine(folder,DateTime.UtcNow.ToString("yyyyMMddHHmmssfff")+"-"+Guid.NewGuid().ToString("N")[..8]+"-"+Path.GetFileName(target)));
    }
    public static bool HashMatches(byte[] bytes,string hash)=>hash.Length==128 && Convert.ToHexString(SHA512.HashData(bytes)).Equals(hash,StringComparison.OrdinalIgnoreCase);
    public void Dispose()=>http.Dispose();
}
