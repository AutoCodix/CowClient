using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json.Nodes;
using CmlLib.Core.Auth;

namespace CowClient.Launcher;

public sealed record DevicePrompt(string Code,string Website,DateTime ExpiresAt);
public sealed class MicrosoftLogin : IDisposable {
    private readonly HttpClient http=new(){Timeout=TimeSpan.FromSeconds(45)};
    private readonly string tokenPath=Path.Combine(SettingsStore.Root,"account.dpapi");
    private const string Authority="https://login.microsoftonline.com/consumers/oauth2/v2.0/";
    private const string Scope="XboxLive.signin offline_access";
    private sealed record StoredToken(string ClientId,string RefreshToken);
    private static string Str(JsonNode node,string key)=>node[key]?.GetValue<string>()??throw new InvalidOperationException("Authentication response is missing "+key+".");
    public void SignOut() {if(File.Exists(tokenPath))File.Delete(tokenPath);}
    public async Task<MSession> LoginAsync(string clientId,Action<DevicePrompt> prompt,CancellationToken ct) {
        if(!Guid.TryParse(clientId,out _)) throw new InvalidOperationException("Microsoft sign-in needs CowClient's registered application ID. Add it in Settings. See docs/AUTHENTICATION.md in the repository.");
        JsonNode? oauth=null;
        if(File.Exists(tokenPath)) {
            try {
                byte[] plain=ProtectedData.Unprotect(await File.ReadAllBytesAsync(tokenPath,ct),null,DataProtectionScope.CurrentUser);
                var saved=System.Text.Json.JsonSerializer.Deserialize<StoredToken>(plain);
                CryptographicOperations.ZeroMemory(plain);
                if(saved?.ClientId==clientId) {
                    var r=await Form("token",new(){["client_id"]=clientId,["grant_type"]="refresh_token",["refresh_token"]=saved.RefreshToken,["scope"]=Scope},ct);
                    if(r["access_token"]!=null)oauth=r;
                }
            } catch(CryptographicException) { }
            catch(System.Text.Json.JsonException) { }
        }
        if(oauth==null) {
            var device=await Form("devicecode",new(){["client_id"]=clientId,["scope"]=Scope},ct);
            if(device["error"]!=null)throw new InvalidOperationException("Microsoft rejected this application configuration ("+Str(device,"error")+").");
            string website=Str(device,"verification_uri");
            if(!IsOfficialVerificationUri(website))throw new InvalidOperationException("Microsoft returned an unexpected verification address.");
            int seconds=device["expires_in"]?.GetValue<int>()??900;
            int interval=Math.Max(device["interval"]?.GetValue<int>()??5,5);
            DateTime deadline=DateTime.UtcNow.AddSeconds(seconds);
            prompt(new DevicePrompt(Str(device,"user_code"),website,deadline));
            while(DateTime.UtcNow<deadline) {
                await Task.Delay(TimeSpan.FromSeconds(interval),ct);
                if(DateTime.UtcNow>=deadline)break;
                var result=await Form("token",new(){["client_id"]=clientId,["grant_type"]="urn:ietf:params:oauth:grant-type:device_code",["device_code"]=Str(device,"device_code")},ct);
                if(result["access_token"]!=null){oauth=result;break;}
                string error=result["error"]?.GetValue<string>()??"unknown_error";
                if(error=="authorization_pending")continue;
                if(error=="slow_down"){interval=checked(interval+5);continue;}
                throw new InvalidOperationException("Microsoft sign-in stopped ("+error+").");
            }
            if(oauth==null)throw new InvalidOperationException("The sign-in code expired. Try again.");
        }
        var xbox=await Post("https://user.auth.xboxlive.com/user/authenticate",new {
            Properties=new{AuthMethod="RPS",SiteName="user.auth.xboxlive.com",RpsTicket="d="+Str(oauth,"access_token")},
            RelyingParty="http://auth.xboxlive.com",TokenType="JWT"
        },ct);
        var xsts=await Post("https://xsts.auth.xboxlive.com/xsts/authorize",new {
            Properties=new{SandboxId="RETAIL",UserTokens=new[]{Str(xbox,"Token")}},
            RelyingParty="rp://api.minecraftservices.com/",TokenType="JWT"
        },ct);
        string uhs=xsts["DisplayClaims"]?["xui"]?[0]?["uhs"]?.GetValue<string>()??throw new InvalidOperationException("Xbox profile is unavailable.");
        var minecraft=await Post("https://api.minecraftservices.com/authentication/login_with_xbox",new {identityToken="XBL3.0 x="+uhs+";"+Str(xsts,"Token")},ct);
        string access=Str(minecraft,"access_token");
        var ownership=await GetMinecraft("entitlements/mcstore",access,ct);
        if(ownership["items"] is not JsonArray items || items.Count==0)
            throw new InvalidOperationException("This account does not have a Minecraft Java entitlement.");
        var profile=await GetMinecraft("minecraft/profile",access,ct);
        if(oauth["refresh_token"]!=null) {
            byte[] plain=System.Text.Json.JsonSerializer.SerializeToUtf8Bytes(new StoredToken(clientId,Str(oauth,"refresh_token")));
            byte[] encrypted=ProtectedData.Protect(plain,null,DataProtectionScope.CurrentUser);
            CryptographicOperations.ZeroMemory(plain);
            Directory.CreateDirectory(SettingsStore.Root);
            string tmp=tokenPath+".tmp";
            try { await File.WriteAllBytesAsync(tmp,encrypted,ct);File.Move(tmp,tokenPath,true); }
            finally { if(File.Exists(tmp))File.Delete(tmp); }
        }
        return new MSession(Str(profile,"name"),access,Str(profile,"id")){UserType="msa"};
    }
    private async Task<JsonNode> Form(string endpoint,Dictionary<string,string> data,CancellationToken ct) {
        using var content=new FormUrlEncodedContent(data);
        using var response=await http.PostAsync(Authority+endpoint,content,ct);
        return JsonNode.Parse(await response.Content.ReadAsStringAsync(ct))??throw new InvalidOperationException("Invalid Microsoft response.");
    }
    private async Task<JsonNode> Post(string url,object payload,CancellationToken ct) {
        using var response=await http.PostAsJsonAsync(url,payload,ct);
        if(!response.IsSuccessStatusCode) {
            string host=new Uri(url).Host;
            throw new InvalidOperationException(host+" declined sign-in (HTTP "+(int)response.StatusCode+"). Check the app's Minecraft API approval, account ownership and Xbox family/privacy settings. CowClient cannot override account restrictions.");
        }
        return JsonNode.Parse(await response.Content.ReadAsStringAsync(ct))??throw new InvalidOperationException("Invalid authentication response.");
    }
    private async Task<JsonNode> GetMinecraft(string path,string token,CancellationToken ct) {
        using var request=new HttpRequestMessage(HttpMethod.Get,"https://api.minecraftservices.com/"+path);
        request.Headers.Authorization=new AuthenticationHeaderValue("Bearer",token);
        using var response=await http.SendAsync(request,ct);
        if(!response.IsSuccessStatusCode)throw new InvalidOperationException("Minecraft account check failed (HTTP "+(int)response.StatusCode+"). Verify Java ownership and the application registration.");
        return JsonNode.Parse(await response.Content.ReadAsStringAsync(ct))??throw new InvalidOperationException("Invalid Minecraft response.");
    }
    public static bool IsOfficialVerificationUri(string value) {
        return Uri.TryCreate(value,UriKind.Absolute,out var uri) && uri.Scheme==Uri.UriSchemeHttps
            && uri.IsDefaultPort && string.IsNullOrEmpty(uri.UserInfo)
            && new[]{"microsoft.com","www.microsoft.com","login.microsoftonline.com","login.live.com","aka.ms"}.Contains(uri.IdnHost,StringComparer.OrdinalIgnoreCase);
    }
    public void Dispose()=>http.Dispose();
}
