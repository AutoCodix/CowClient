from pathlib import Path
from urllib.request import urlopen
import cairosvg
from PIL import Image
import io

ROOT = Path(__file__).resolve().parent.parent
svg = (ROOT / "branding/cow.svg").read_bytes()
png = cairosvg.svg2png(bytestring=svg, output_width=512, output_height=400)
assets = ROOT / "client/src/main/resources/assets/cowclient"
assets.mkdir(parents=True, exist_ok=True)
(assets / "icon.png").write_bytes(png)
brand = ROOT / "branding"
(brand / "cow.png").write_bytes(png)
im = Image.open(io.BytesIO(png)).convert("RGBA")
square = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
square.alpha_composite(im, (0, 56))
square.save(brand / "cow.ico", sizes=[(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)])
base = "https://raw.githubusercontent.com/notofonts/noto-fonts/main/"
for remote, local in [("hinted/ttf/NotoSans/NotoSans-Regular.ttf", assets / "ui.ttf"), ("LICENSE", assets / "FONT-LICENSE.txt")]:
    with urlopen(base + remote, timeout=60) as response:
        local.write_bytes(response.read())
assert (assets / "ui.ttf").stat().st_size > 10000
print("Cow icon, Windows ICO, and Noto Sans font prepared.")
