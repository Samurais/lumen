# How to Convert .mp3 / .3gpp to .ogg properly

Some mp3s (from Forvo) includes PNG image, which ffmpeg/avconv will convert to video in .ogg.
So you need `-vn`.

    for i in *.mp3 *.3gpp; do ffmpeg -i $i -vn -acodec libvorbis $i.ogg; done
    rename -fv 's/.(mp3|3gpp)//' *.mp3.ogg *.3gpp.ogg
	