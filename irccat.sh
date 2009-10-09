#!/bin/sh
#
# Run irccat.
#

cp="irccat.jar"
for jar in libs/*.jar
do
    cp="$cp:$jar"
done

exec java -cp "build/:$cp" fm.last.irccat.IRCCat "${1:-irccat.xml}"
