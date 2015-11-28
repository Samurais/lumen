# How to Convert .mp3 to .ogg properly

Some mp3s (from Forvo) includes PNG image, which ffmpeg/avconv will convert to video in .ogg.
So you need `-vn`.

    for i in *.mp3; do ffmpeg -i $i -vn -acodec libvorbis $i.ogg; done
    rename -fv 's/.mp3//' *.mp3.ogg
