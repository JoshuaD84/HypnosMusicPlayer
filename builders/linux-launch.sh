#!/bin/bash

#make the script's directory the current directory
DIR="$(dirname "$(readlink -f "$0")")"
cd $DIR
echo "Executing from $DIR"
mkdir -p ../bin-native/linux-launch/

set -o xtrace

g++ -I"../packaging/jres/linux-full/include/" \
-I"../packaging/jres/linux-full/include/linux" \
-L/usr/lib/jvm/java-11-oracle \
-L/usr/lib/jvm/java-11-oracle/lib/server/ \
-fno-pic \
../src/linux-launch/launch.cpp \
-ldl \
-ljvm \
-no-pie \
-o ../bin-native/linux-launch/hypnos


#why we do -fno-pic and -no-pie:
#https://askubuntu.com/questions/1071374/how-do-i-make-an-executable-instead-of-a-shared-library-under-18-04-1/1087680
