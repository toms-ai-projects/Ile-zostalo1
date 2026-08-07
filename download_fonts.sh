mkdir -p app/src/main/res/font
cd app/src/main/res/font

# Quicksand
curl -sL "https://fonts.google.com/download?family=Quicksand" -o quicksand.zip
unzip -o -q quicksand.zip -d quicksand_dir
cp quicksand_dir/static/Quicksand-Regular.ttf quicksand.ttf
cp quicksand_dir/static/Quicksand-Bold.ttf quicksand_bold.ttf
rm -rf quicksand.zip quicksand_dir

# Roboto
curl -sL "https://fonts.google.com/download?family=Roboto" -o roboto.zip
unzip -o -q roboto.zip -d roboto_dir
cp roboto_dir/Roboto-Regular.ttf roboto.ttf
rm -rf roboto.zip roboto_dir

# Roboto Slab
curl -sL "https://fonts.google.com/download?family=Roboto%20Slab" -o robotoslab.zip
unzip -o -q robotoslab.zip -d robotoslab_dir
cp robotoslab_dir/static/RobotoSlab-Regular.ttf roboto_slab.ttf
rm -rf robotoslab.zip robotoslab_dir

# Roboto Mono
curl -sL "https://fonts.google.com/download?family=Roboto%20Mono" -o robotomono.zip
unzip -o -q robotomono.zip -d robotomono_dir
cp robotomono_dir/static/RobotoMono-Regular.ttf roboto_mono.ttf
rm -rf robotomono.zip robotomono_dir

echo "Fonts downloaded"
ls -l
