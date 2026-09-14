using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using CmlLib.Core.Auth;

namespace CowClient.Launcher;
public partial class MainWindow : Window {
    private readonly SettingsStore store=new();
    private readonly MicrosoftLogin auth=new();
    private readonly GameInstaller installer=new();
    private Settings settings=new();
    private MSession? session;
    private CancellationTokenSource? operation;
    private Process? game;
    private bool busy;
    private DateTime signedInAt;
    public MainWindow() {
        InitializeComponent();
        settings=store.Load();RamSlider.Value=settings.MemoryMb;
        PerformanceCheck.IsChecked=settings.PerformanceMods;MinimizeCheck.IsChecked=settings.MinimizeOnLaunch;
        ClientIdBox.Text=settings.MicrosoftClientId;UpdateSessionInfo();
        if(store.Error.Length>0)StatusText.Text=store.Error;
    }
    private void UpdateSessionInfo()=>SessionInfo.Text="Minecraft 1.21.11  ·  "+settings.Profile+"  ·  "+(settings.MemoryMb/1024.0).ToString("0.#")+" GB RAM";
    private void Status(string message,double progress=0) {
        Dispatcher.Invoke(()=>{StatusText.Text=message;Progress.Value=Math.Clamp(progress,0,1);});
    }
    private void SetBusy(bool value) {
        busy=value;LaunchButton.IsEnabled=!value&&(game==null||SafeExited());LoginButton.IsEnabled=!value;
        CancelButton.Visibility=value?Visibility.Visible:Visibility.Collapsed;Progress.Visibility=value?Visibility.Visible:Visibility.Collapsed;
    }
    private void ShowPage(string page) {
        HomePanel.Visibility=page=="home"?Visibility.Visible:Visibility.Collapsed;
        ProfilesPanel.Visibility=page=="profiles"?Visibility.Visible:Visibility.Collapsed;
        SettingsPanel.Visibility=page=="settings"?Visibility.Visible:Visibility.Collapsed;
    }
    private async Task<MSession> SignIn(CancellationToken ct) {
        Status("Waiting for Microsoft sign-in…");
        var result=await auth.LoginAsync(settings.MicrosoftClientId,p=>Dispatcher.Invoke(()=>{
            DeviceCodeBox.Text=p.Code;LoginOverlay.Visibility=Visibility.Visible;
        }),ct);
        LoginOverlay.Visibility=Visibility.Collapsed;session=result;signedInAt=DateTime.UtcNow;
        AccountName.Text=result.Username??"Minecraft account";LoginButton.Content="Switch account";
        return result;
    }
    private async void Login_Click(object sender,RoutedEventArgs e) {
        if(busy)return;
        SetBusy(true);operation=new CancellationTokenSource();
        try {
            if(session!=null){auth.SignOut();session=null;}
            await SignIn(operation.Token);Status("Signed in. Ready when you are.");
        } catch(OperationCanceledException){Status("Sign-in cancelled.");}
        catch(Exception ex){Status(SafeError(ex));if(string.IsNullOrWhiteSpace(settings.MicrosoftClientId))ShowPage("settings");}
        finally {LoginOverlay.Visibility=Visibility.Collapsed;operation.Dispose();operation=null;SetBusy(false);}
    }
    private async void Launch_Click(object sender,RoutedEventArgs e) {
        if(busy || (game!=null&&!game.HasExited))return;
        SetBusy(true);operation=new CancellationTokenSource();
        try {
            if(session==null || DateTime.UtcNow-signedInAt>TimeSpan.FromMinutes(30))await SignIn(operation.Token);
            var launchSettings=new Settings{MicrosoftClientId=settings.MicrosoftClientId,Profile=settings.Profile,MemoryMb=settings.MemoryMb,PerformanceMods=settings.PerformanceMods,MinimizeOnLaunch=settings.MinimizeOnLaunch};
            game=await installer.PrepareAsync(launchSettings,session!,Status,operation.Token);
            operation.Token.ThrowIfCancellationRequested();
            game.EnableRaisingEvents=true;
            game.Exited+=(_,_)=>Dispatcher.InvokeAsync(()=>{
                int code=game?.ExitCode??-1;
                WindowState=WindowState.Normal;Activate();LaunchButton.IsEnabled=true;LaunchButton.Content="Launch Minecraft  →";
                Status(code==0?"Minecraft closed. See you next session.":"Minecraft exited with code "+code+". Open the game folder and check logs/latest.log.");
            });
            if(!game.Start())throw new InvalidOperationException("Minecraft could not be started.");
            Status("Minecraft is running.");LaunchButton.Content="Minecraft is running";
            if(settings.MinimizeOnLaunch)WindowState=WindowState.Minimized;
        } catch(OperationCanceledException){Status("Cancelled. Verified downloads are kept for your next launch.");}
        catch(Exception ex){Status(SafeError(ex));if(string.IsNullOrWhiteSpace(settings.MicrosoftClientId))ShowPage("settings");}
        finally {LoginOverlay.Visibility=Visibility.Collapsed;operation.Dispose();operation=null;SetBusy(false);if(game==null||SafeExited())LaunchButton.Content="Launch Minecraft  →";}
    }
    private bool SafeExited(){try{return game!.HasExited;}catch(InvalidOperationException){game=null;return true;}}
    private string SafeError(Exception ex) {
        string message=ex is HttpRequestException?"A network request failed. Check your connection and try again.":ex.Message;
        if(!string.IsNullOrEmpty(session?.AccessToken))message=message.Replace(session.AccessToken,"[redacted]");
        return message.Length>340?message[..340]:message;
    }
    private void Save_Click(object sender,RoutedEventArgs e) {
        if(busy){Status("Wait for the current operation before changing settings.");return;}
        try {
            string newId=ClientIdBox.Text.Trim();
            if(newId.Length>0&&!Guid.TryParse(newId,out _)){Status("The application ID must be a GUID, not a password or secret.");return;}
            if(newId!=settings.MicrosoftClientId){auth.SignOut();session=null;AccountName.Text="Microsoft account";LoginButton.Content="Sign in to play";}
            settings.MicrosoftClientId=newId;settings.MemoryMb=(int)RamSlider.Value;
            settings.PerformanceMods=PerformanceCheck.IsChecked==true;settings.MinimizeOnLaunch=MinimizeCheck.IsChecked==true;
            store.Save(settings);UpdateSessionInfo();Status("Settings saved.");
        } catch(Exception ex){Status(SafeError(ex));}
    }
    private void Profile_Click(object sender,RoutedEventArgs e) {
        if(busy){Status("Wait for the current operation before changing profiles.");return;}
        try{settings.Profile=(string)((Button)sender).Tag;store.Save(settings);UpdateSessionInfo();ShowPage("home");Status(settings.Profile+" selected. Your saved in-game customizations are preserved.");}
        catch(Exception ex){Status(SafeError(ex));}
    }
    private void Logout_Click(object sender,RoutedEventArgs e) {
        if(busy)return;
        try{auth.SignOut();session=null;AccountName.Text="Microsoft account";LoginButton.Content="Sign in to play";Status("Signed out. The locally saved refresh token was removed.");}
        catch(Exception ex){Status(SafeError(ex));}
    }
    private void Ram_Changed(object sender,RoutedPropertyChangedEventArgs<double> e){if(RamLabel!=null)RamLabel.Text=((int)e.NewValue)+" MB";}
    private void Home_Click(object sender,RoutedEventArgs e)=>ShowPage("home");
    private void Profiles_Click(object sender,RoutedEventArgs e)=>ShowPage("profiles");
    private void Settings_Click(object sender,RoutedEventArgs e)=>ShowPage("settings");
    private void Notes_Click(object sender,RoutedEventArgs e)=>Open("https://github.com/AutoCodix/CowClient");
    private void AuthDocs_Click(object sender,RoutedEventArgs e)=>Open("https://github.com/AutoCodix/CowClient/blob/main/docs/AUTHENTICATION.md");
    private void Folder_Click(object sender,RoutedEventArgs e){try{Directory.CreateDirectory(GameInstaller.GameDirectory);Open(GameInstaller.GameDirectory);}catch(Exception ex){Status(SafeError(ex));}}
    private void Browser_Click(object sender,RoutedEventArgs e)=>Open("https://www.microsoft.com/link");
    private void Open(string target){try{Process.Start(new ProcessStartInfo(target){UseShellExecute=true});}catch(Exception ex){Status(SafeError(ex));}}
    private void Cancel_Click(object sender,RoutedEventArgs e){operation?.Cancel();Status("Cancelling… Minecraft's installer may finish its current step first.");LoginOverlay.Visibility=Visibility.Collapsed;}
    private void Minimize_Click(object sender,RoutedEventArgs e)=>WindowState=WindowState.Minimized;
    private void Close_Click(object sender,RoutedEventArgs e)=>Close();
    private void Title_Drag(object sender,MouseButtonEventArgs e){if(e.OriginalSource is FrameworkElement fe && fe.TemplatedParent is Button)return;if(e.LeftButton==MouseButtonState.Pressed)DragMove();}
    protected override void OnClosing(System.ComponentModel.CancelEventArgs e) {
        if(busy){operation?.Cancel();Status("Cancelling the current operation. Close again after it stops.");e.Cancel=true;return;}
        base.OnClosing(e);
    }
    protected override void OnClosed(EventArgs e){auth.Dispose();installer.Dispose();base.OnClosed(e);}
}
