using System.IO;
using System.Text.Json;

namespace CowClient.Launcher;
public sealed class Settings {
    public string MicrosoftClientId {get;set;}="";
    public string Profile {get;set;}="Default";
    public int MemoryMb {get;set;}=4096;
    public bool PerformanceMods {get;set;}=true;
    public bool MinimizeOnLaunch {get;set;}=true;
    public bool ShowIntro {get;set;}=true;
    public bool ReducedMotion {get;set;}=false;
    public void Validate() {
        MemoryMb=Math.Clamp(MemoryMb,2048,8192);
        if(!new[]{"Default","Competitive","Chill"}.Contains(Profile)) Profile="Default";
        MicrosoftClientId=MicrosoftClientId.Trim();
    }
}
public sealed class SettingsStore {
    public static string Root => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"CowClient");
    public string Error {get;private set;}="";
    private readonly string path;
    public SettingsStore(string? path=null) {this.path=path??Path.Combine(Root,"settings.json");}
    public Settings Load() {
        if(!File.Exists(path)) return new Settings();
        try {
            var value=JsonSerializer.Deserialize<Settings>(File.ReadAllText(path))??throw new JsonException();
            value.Validate();return value;
        } catch(Exception ex) when(ex is IOException or JsonException or UnauthorizedAccessException) {
            Error="Existing settings could not be read; the original file was preserved.";return new Settings();
        }
    }
    public void Save(Settings settings) {
        if(Error.Length>0) throw new InvalidOperationException(Error);
        settings.Validate();Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        string temp=path+"."+Guid.NewGuid().ToString("N")+".tmp";
        try {File.WriteAllText(temp,JsonSerializer.Serialize(settings,new JsonSerializerOptions{WriteIndented=true}));File.Move(temp,path,true);}
        finally {if(File.Exists(temp))File.Delete(temp);}
    }
}
