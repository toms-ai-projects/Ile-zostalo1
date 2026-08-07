import urllib.request
import os

os.makedirs('app/src/main/res/font', exist_ok=True)
fonts = {
    'quicksand': 'https://raw.githubusercontent.com/google/fonts/main/ofl/quicksand/Quicksand%5Bwght%5D.ttf',
    'roboto': 'https://raw.githubusercontent.com/google/fonts/main/ofl/roboto/Roboto%5Bwdth%2Cwght%5D.ttf',
    'roboto_slab': 'https://raw.githubusercontent.com/google/fonts/main/apache/robotoslab/RobotoSlab%5Bwght%5D.ttf',
    'roboto_mono': 'https://raw.githubusercontent.com/google/fonts/main/ofl/robotomono/RobotoMono%5Bwght%5D.ttf'
}

for name, url in fonts.items():
    print(f"Downloading {name}...")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            with open(f'app/src/main/res/font/{name}.ttf', 'wb') as f:
                f.write(response.read())
        print(f"Downloaded {name}")
    except Exception as e:
        print(f"Error {name}: {e}")

