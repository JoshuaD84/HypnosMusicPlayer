#!/bin/bash
# A simple script which replaces 'javaw.exe' with 'hypno.exe' in the specified file

set -o xtrace
infile=$1
temp=$2
hexdump -ve '1/1 "%.2X"' $1 | sed -e 's/6A61766177/6879706E6F/g' | xxd -r -p > $2/fixed.exe 
rm $1
mv $2/fixed.exe $1