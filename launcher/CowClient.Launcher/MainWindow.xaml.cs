using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;
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
    private DevicePrompt? devicePrompt;
    private readonly DispatcherTimer codeTimer=new(){Interval=TimeSpan.FromSeconds(1)};
    private int introVersion;
    internal bool SmokeMode {get;set;}

    public MainWindow() {
        InitializeComponent();
        settings=store.Load();RamSlider.Value=settings.MemoryMb;
        PerformanceCheck.IsChecked=settings.PerformanceMods;MinimizeCheck.IsChecked=settings.MinimizeOnLaunch;
        ClientIdBox.Text=settings.MicrosoftClientId;UpdateSessionInfo();
        IntroCheck.IsChecked=settings.ShowIntro;ReducedMotionCheck.IsChecked=settings.ReducedMotion;
        ShowPage("home");codeTimer.Tick+=(_,_)=>UpdateCodeCountdown();
        Loaded+=async (_,_)=>{if(settings.ShowIntro&&!SmokeMode)await PlayIntroAsync();};
        PreviewKeyDown+=(_,e)=>{if(e.Key==Key.Escape){if(LoginOverlay.Visibility==Visibility.Visible)Cancel_Click(this,new RoutedEventArgs());else SkipIntro_Click(this,new RoutedEventArgs());e.Handled=true;}};
        if(store.Error.Length>0)StatusText.Text=store.Error;
    }
    private void UpdateSessionInfo(){SessionInfo.Text="Minecraft 1.21.11  ·  Fabric  ·  "+(settings.MemoryMb/1024.0).ToString("0.#")+" GB RAM";SelectedProfile.Text=settings.Profile+" profile";}
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
        HomeNav.Background=new SolidColorBrush(page=="home"?Color.FromRgb(58,40,77):Colors.Transparent);
        ProfilesNav.Background=new SolidColorBrush(page=="profiles"?Color.FromRgb(58,40,77):Colors.Transparent);
        SettingsNav.Background=new SolidColorBrush(page=="settings"?Color.FromRgb(58,40,77):Colors.Transparent);
        if(!settings.ReducedMotion && SystemParameters.ClientAreaAnimation) {
            UIElement panel=page=="home"?HomePanel:page=="profiles"?ProfilesPanel:SettingsPanel;
            panel.BeginAnimation(OpacityProperty,new DoubleAnimation(0,1,TimeSpan.FromMilliseconds(190)));
        }
    }
    private async Task<MSession> SignIn(CancellationToken ct) {
        devicePrompt=null;codeTimer.Stop();LoginOverlay.Visibility=Visibility.Visible;
        AuthHeading.Text="Let's get you signed in.";AuthDescription.Text="Use this one-time code on Microsoft's website. Your password stays with Microsoft.";
        CodePanel.Visibility=AuthSteps.Visibility=Visibility.Visible;AuthFeedback.Text="";
        RetryLoginButton.Visibility=AuthSetupButton.Visibility=Visibility.Collapsed;
        OpenMicrosoftButton.IsEnabled=CopyCodeButton.IsEnabled=false;DeviceCodeBox.Text="Getting code…";CodeExpiry.Text="Waiting for Microsoft";
        Status("Waiting for Microsoft sign-in…");
        var result=await auth.LoginAsync(settings.MicrosoftClientId,p=>Dispatcher.Invoke(()=>{
            devicePrompt=p;DeviceCodeBox.Text=p.Code;OpenMicrosoftButton.IsEnabled=CopyCodeButton.IsEnabled=true;
            UpdateCodeCountdown();codeTimer.Start();OpenMicrosoftButton.Focus();
        }),ct);
        codeTimer.Stop();devicePrompt=null;LoginOverlay.Visibility=Visibility.Collapsed;session=result;signedInAt=DateTime.UtcNow;
        AccountName.Text=result.Username??"Minecraft account";LoginButton.Content="Switch account";
        return result;
    }
    private async void Login_Click(object sender,RoutedEventArgs e) {
        if(busy)return;
        SetBusy(true);operation=new CancellationTokenSource();
        try {
            if(session!=null){auth.SignOut();session=null;}
            await SignIn(operation.Token);Status("Signed in. Ready when you are.");
        } catch(OperationCanceledException){LoginOverlay.Visibility=Visibility.Collapsed;Status("Sign-in cancelled.");}
        catch(Exception ex){ShowAuthError(ex);}
        finally {codeTimer.Stop();operation.Dispose();operation=null;SetBusy(false);}
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
        } catch(OperationCanceledException){LoginOverlay.Visibility=Visibility.Collapsed;Status("Cancelled. Verified downloads are kept for your next launch.");}
        catch(Exception ex){ShowAuthError(ex);}
        finally {codeTimer.Stop();operation.Dispose();operation=null;SetBusy(false);if(game==null||SafeExited())LaunchButton.Content="Launch Minecraft  →";}
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
            settings.ShowIntro=IntroCheck.IsChecked==true;settings.ReducedMotion=ReducedMotionCheck.IsChecked==true;
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
    private void Browser_Click(object sender,RoutedEventArgs e) {
        if(devicePrompt==null||DateTime.UtcNow>=devicePrompt.ExpiresAt)return;
        CopyCode_Click(sender,e);Open(devicePrompt.Website);
        AuthFeedback.Text="Enter the code in your browser, then review the Microsoft permissions screen.";
    }
    private void CopyCode_Click(object sender,RoutedEventArgs e) {
        if(devicePrompt==null||DateTime.UtcNow>=devicePrompt.ExpiresAt)return;
        try{Clipboard.SetText(devicePrompt.Code);AuthFeedback.Text="Code copied. Paste it on Microsoft's verification page.";}
        catch(System.Runtime.InteropServices.ExternalException){AuthFeedback.Text="Clipboard is busy. Select the code above and copy it manually.";}
    }
    private void UpdateCodeCountdown() {
        if(devicePrompt==null)return;
        var left=devicePrompt.ExpiresAt-DateTime.UtcNow;
        if(left<=TimeSpan.Zero){codeTimer.Stop();OpenMicrosoftButton.IsEnabled=CopyCodeButton.IsEnabled=false;CodeExpiry.Text="Code expired. Request a new code.";return;}
        CodeExpiry.Text="Expires in "+left.ToString(@"mm\:ss")+"  ·  Waiting for you";
    }
    private void ShowAuthError(Exception ex) {
        string error=SafeError(ex);Status(error);codeTimer.Stop();devicePrompt=null;
        if(LoginOverlay.Visibility!=Visibility.Visible)return;
        OpenMicrosoftButton.IsEnabled=CopyCodeButton.IsEnabled=false;CodePanel.Visibility=AuthSteps.Visibility=Visibility.Collapsed;
        bool setup=!Guid.TryParse(settings.MicrosoftClientId,out _);
        AuthHeading.Text=setup?"Sign-in needs one setup step.":"Let's try that again.";
        AuthDescription.Text=setup?"This alpha needs CowClient's registered Microsoft application before it can request a code.":"Microsoft sign-in did not complete. You can request a new code.";
        AuthFeedback.Text=error;RetryLoginButton.Visibility=setup?Visibility.Collapsed:Visibility.Visible;AuthSetupButton.Visibility=setup?Visibility.Visible:Visibility.Collapsed;
    }
    private void AuthSetup_Click(object sender,RoutedEventArgs e){LoginOverlay.Visibility=Visibility.Collapsed;ShowPage("settings");ClientIdBox.Focus();}
    internal async Task PlayIntroAsync() {
        int version=++introVersion;IntroOverlay.BeginAnimation(OpacityProperty,null);IntroOverlay.Opacity=1;IntroOverlay.Visibility=Visibility.Visible;
        if(!settings.ReducedMotion&&SystemParameters.ClientAreaAnimation) {
            IntroContent.BeginAnimation(OpacityProperty,new DoubleAnimation(0,1,TimeSpan.FromMilliseconds(450)));
            var scale=(ScaleTransform)IntroContent.RenderTransform;
            var animation=new DoubleAnimation(0.92,1,TimeSpan.FromMilliseconds(650)){EasingFunction=new CubicEase{EasingMode=EasingMode.EaseOut}};
            scale.BeginAnimation(ScaleTransform.ScaleXProperty,animation);scale.BeginAnimation(ScaleTransform.ScaleYProperty,animation);
        }
        await Task.Delay(settings.ReducedMotion?500:1600);
        if(version!=introVersion)return;
        if(!settings.ReducedMotion&&SystemParameters.ClientAreaAnimation){IntroOverlay.BeginAnimation(OpacityProperty,new DoubleAnimation(1,0,TimeSpan.FromMilliseconds(230)));await Task.Delay(240);}
        if(version==introVersion)IntroOverlay.Visibility=Visibility.Collapsed;
    }
    private async void ReplayIntro_Click(object sender,RoutedEventArgs e){if(!busy)await PlayIntroAsync();}
    private void SkipIntro_Click(object sender,RoutedEventArgs e){introVersion++;IntroOverlay.Visibility=Visibility.Collapsed;}
    internal void ShowSmokeView(string view) {
        SkipIntro_Click(this,new RoutedEventArgs());LoginOverlay.Visibility=Visibility.Collapsed;
        if(view=="intro"){IntroOverlay.BeginAnimation(OpacityProperty,null);IntroOverlay.Opacity=1;IntroOverlay.Visibility=Visibility.Visible;}
        else if(view=="login"){LoginOverlay.Visibility=Visibility.Visible;AuthHeading.Text="Sign-in layout preview";DeviceCodeBox.Text="PREVIEW";CodeExpiry.Text="Preview only — no account session";AuthFeedback.Text="A real code is requested from Microsoft when you sign in.";OpenMicrosoftButton.IsEnabled=CopyCodeButton.IsEnabled=false;}
        else ShowPage(view);
    }
    private void Open(string target){try{Process.Start(new ProcessStartInfo(target){UseShellExecute=true});}catch(Exception ex){Status(SafeError(ex));}}
    private void Cancel_Click(object sender,RoutedEventArgs e){codeTimer.Stop();devicePrompt=null;operation?.Cancel();Status("Cancelling… Minecraft's installer may finish its current step first.");LoginOverlay.Visibility=Visibility.Collapsed;}
    private void Minimize_Click(object sender,RoutedEventArgs e)=>WindowState=WindowState.Minimized;
    private void Close_Click(object sender,RoutedEventArgs e)=>Close();
    private void Title_Drag(object sender,MouseButtonEventArgs e){if(e.OriginalSource is FrameworkElement fe && fe.TemplatedParent is Button)return;if(e.LeftButton==MouseButtonState.Pressed)DragMove();}
    protected override void OnClosing(System.ComponentModel.CancelEventArgs e) {
        if(busy){operation?.Cancel();Status("Cancelling the current operation. Close again after it stops.");e.Cancel=true;return;}
        base.OnClosing(e);
    }
    protected override void OnClosed(EventArgs e){introVersion++;codeTimer.Stop();auth.Dispose();installer.Dispose();base.OnClosed(e);}
}
