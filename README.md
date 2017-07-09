# Irista Downloader

Bothered by friends that shared ablums on Canon Irista cloud photo service (irista.com) that you cannot download as Google Photos? Then this tool is for you.

## Install

It is a Java program, the build system is gradle.

```
$ gradle build
```

## Run

Then to run:

```
$ java fr.leyrdhin.tools.IristaDownloader -a 'https://www.irista.com/gallery/ab12cd34ef56#/' -d /home/user/Download/album123
```

with 

* `-a` the url to the album
* `-d` the destination folder

Enjoy!
