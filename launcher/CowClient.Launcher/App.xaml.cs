using System.IO;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;
namespace CowClient.Launcher;
public partial class App : Application {
    protected override void OnStartup(StartupEventArgs e) {
        base.OnStartup(e);
        bool smoke=e.Args.Length==2&&e.Args[0]=="--smoke-test";
        var window=new MainWindow{SmokeMode=smoke};MainWindow=window;window.Show();
        if(smoke)window.Dispatcher.InvokeAsync(async ()=>{
            void Capture(string path){
                window.UpdateLayout();
                var bitmap=new RenderTargetBitmap((int)window.ActualWidth,(int)window.ActualHeight,96,96,PixelFormats.Pbgra32);
                bitmap.Render(window);var encoder=new PngBitmapEncoder();encoder.Frames.Add(BitmapFrame.Create(bitmap));
                using var stream=File.Create(path);encoder.Save(stream);
            }
            try {
                await Task.Delay(400);
                string prefix=Path.Combine(Path.GetDirectoryName(e.Args[1])!,Path.GetFileNameWithoutExtension(e.Args[1]));
                var intro=window.PlayIntroAsync();await Task.Delay(600);Capture(prefix+"-intro.png");await intro;
                if(window.IntroOverlay.Visibility!=Visibility.Collapsed)throw new InvalidOperationException("Intro did not finish.");
                window.ShowSmokeView("home");await Task.Delay(300);Capture(e.Args[1]);
                foreach(string view in new[]{"profiles","settings","login"}){window.ShowSmokeView(view);await Task.Delay(300);Capture(prefix+"-"+view+".png");}
                Shutdown(0);
            }catch(Exception ex){File.WriteAllText(e.Args[1]+".error.txt",ex.ToString());Shutdown(1);}
        },System.Windows.Threading.DispatcherPriority.ApplicationIdle);
    }
}
