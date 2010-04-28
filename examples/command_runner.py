#!/usr/bin/python
import os, sys, re, subprocess

# If this script is set as your command handler, when any ?command is run in IRC it will
# look in the path defined below for a script matching that command name and run it.
#
# e.g. ?uptime would look in "/usr/share/irccat/" (the default) for any script called
# "uptime", with any extension. It would happily run both uptime.sh and uptime.py, or
# a script in whatever language you like. Command names are limited to [0-9a-z].

path = '/usr/share/irccat/'

args = sys.argv[1]
bits = args.split(' ')
command = bits[3]

found = False
if re.match('^[a-z0-9]+$', command):
    for file in os.listdir(path):

        if re.match('^%s\.[a-z]+$' % command, file):
            found = True
            proc = subprocess.Popen([path+file, args], shell=True, stdout=subprocess.PIPE)
            stdout = proc.stdout

            while True:
                # We do this to avoid buffering from the subprocess stdout
                print os.read(stdout.fileno(), 65536),
                sys.stdout.flush()

                if proc.poll() != None:
                    break

if found == False:
    print "Unknown command '%s'" % command
