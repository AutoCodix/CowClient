using Xunit;
using CowClient.Launcher;
using System.Security.Cryptography;

public class LauncherTests {
 [Fact] public void RamAndProfilesAreBounded() {
  var s=new Settings{MemoryMb=int.MaxValue,Profile="../escape"};s.Validate();
  Assert.Equal(8192,s.MemoryMb);Assert.Equal("Default",s.Profile);
 }
 [Fact] public void DownloadsMustMatchHash() {
  byte[] content={1,2,3};string hash=Convert.ToHexString(SHA512.HashData(content));
  Assert.True(GameInstaller.HashMatches(content,hash));Assert.False(GameInstaller.HashMatches(new byte[]{1,2,4},hash));
  Assert.False(GameInstaller.HashMatches(content,""));
 }
 [Fact] public void SettingsRoundTrip() {
  string dir=System.IO.Path.Combine(System.IO.Path.GetTempPath(),"cowclient-test-"+Guid.NewGuid());
  System.IO.Directory.CreateDirectory(dir);
  string path=System.IO.Path.Combine(dir,"settings.json");
  try {var store=new SettingsStore(path);store.Save(new Settings{Profile="Chill",MemoryMb=3072});Assert.Equal("Chill",store.Load().Profile);Assert.Equal(3072,store.Load().MemoryMb);}
  finally {System.IO.Directory.Delete(dir,true);}
 }
}
