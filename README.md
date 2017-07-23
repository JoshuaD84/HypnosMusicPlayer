# Hypnos Music Player

## A cross-platform music player and library. Simple, clean, powerful, and easy to use.

Released under the GNU General Public License (v3). [Git Hub Project Page](https://github.com/JoshuaD84/HypnosMusicPlayer)

[![](hypnos-2017-06-04.png)](http://hypnosplayer.org/hypnos-2017-06-04.png)

## Status

Hypnos is still under early development. In my limited testing, it has been very stable and functional, so feel free to try it out and let me know if you have any problems.

**Codec Support:**

*   FLAC
*   mp3
*   m4a
*   wav
*   ogg - coming soon
*   ALAC - coming soon

## Download

Visit [HypnosPlayer.org](https://github.com/JoshuaD84/HypnosMusicPlayer) to download for Windows, Linux, or OSX. 

## Setup and Running

### Windows

Download the zip, extract the folder to a location of your choosing, open the folder, and double click "Hypnos.exe".

Windows 7+ supported. It will probably work on Vista / XP, but I haven't tested there yet.

There are some small display issues in Windows 7 (some buttons won't have proper icons). This will be fixed in an upcoming version.

### Linux

Download the .tgz, extract to a location of your chooosing. Launch using the `hypnos` script in the folder.

*   I've included a Hypnos.desktop file, which you can edit and put in the appropriate location for shell integration.
*   You can create a link in /usr/bin or ~/.local/bin to `hypnos` so hypnos is in your path
*   Hypnos is fully controllable at the console, so you can set global hotkeys using commands like `hypnos --next`. (hypnos --help for the full list of options).

Please note that Hypnos requires Java 8 and JavaFX. On Debian/Ubuntu/Mint, you can install these packages like this:

> sudo apt-get install openjdk-8-jre  
> sudo apt-get install openjfx

For other distributions, you'll have to google how to install java 8 and javafx. I recommend using the OpenJDK, but Oracle's java will work as well.

### OSX

Coming soon. I just need to get a testing environment up and running to see if I did the packaging right.