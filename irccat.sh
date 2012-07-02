#!/bin/sh
#
# Run irccat.
#
set -e
cd `dirname $0`
cp="dist/irccat.jar"
for jar in libs/*.jar
do
    cp="$cp:$jar"
done

exec java -cp "build/:$cp" fm.last.irccat.IRCCat "${1:-irccat.xml}"
