#!/usr/bin/python
import os, sys, re, subprocess, difflib

# If this script is set as your command handler, when any ?command is run in IRC it will
# look in the path defined below for a script matching that command name and run it.
#
# e.g. ?uptime would look in "/usr/share/irccat/" (the default) for any script called
# "uptime", with any extension. It would happily run both uptime.sh and uptime.py, or
# a script in whatever language you like. Command names are limited to [0-9a-z].

path = '/usr/share/irccat/'

args = sys.argv[1]
bits = args.split(' ')
command = bits[3].lower()

found = False
commands = []

if re.match('^[a-z0-9]+$', command):
    for file in os.listdir(path):

        # Build the index, in case we never find a command
        m = re.match('^[a-z0-9]+', file)
        if m:
            commands.append(m.group(0))

        if re.match('^%s\.[a-z]+$' % command, file):
            found = True
            
            procArgs = [path + file]
            procArgs.extend(bits)
            proc = subprocess.Popen(procArgs, stdout=subprocess.PIPE)
            stdout = proc.stdout

            while True:

                # We do this to avoid buffering from the subprocess stdout
                data = os.read(stdout.fileno(), 65536)

                # Kill useless lines in the output
                for line in re.split("[\r\n]+", data):
                    line = line.strip()
                    if line:
                        print line

                sys.stdout.flush()

                if proc.poll() != None:
                    break


if found == False:

    # Didn't find a command to run. Maybe can we help.
    matches = difflib.get_close_matches(command, commands, 1)
    if matches:
        print "%s, I don't understand '%s'. Did you mean '%s'?" % (bits[0], command, matches[0])
