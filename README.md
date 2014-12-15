## Seeking Maintainer

Want commit access? If you are using irccat and want to help reviewing the occasional pull request/issues, please message me for commit access. I don't do any java dev atm, so testing changes is a bit painful for me.


IRCcat
======

As in `cat` to IRC. 

IRCcat does 2 things:

1) Listens on a specific ip:port and writes incoming data to an IRC channel.
   This is useful for sending various announcements and log messages to irc
   from shell scripts, Nagios and other services.

2) Hands off commands issued on irc to a handler program (eg: shell script)
   and responds to irc with the output of the handler script. This only 
   happens for commands addressed to irccat: or prefixed with ?.
   (easily extend irccat functionality with your own scripts)


Installation
------------
Edit the irccat.xml so it knows which irc server, channel etc..
Check you have sun jvm >=1.5 (java -version)

This will build and package irccat as a jar:

$ ant dist

Then, using the appropriate config file:

$ ant -Dconfigfile=./examples/irccat.xml run

Also, you'll probably want to install netcat.


Sending text to IRC
-------------------
By default IRCCat listens on 127.0.0.1:12345, and sends data to the 
default (first) channels.  In a networked server environment you will p
robably want to make it listen on the private LAN interface.

Examples: 

$ echo "Hello World" | netcat -q0 localhost 12345
$ tail -f /var/log/www/error.log | netcat localhost 12345

In a server environment, consider adding a hostname to your internal DNS or 
using a virtual IP for irccat to listen on, in case you want to move irccat
to another box later.


Sending to specific channels
----------------------------
To send to a specific channel rather than the default, put the channel name
as the first word in the data (the channel name will be stripped):

$ echo "#mychannel hello world" | netcat -q0 machinename 12345

This sends "hello world" to #mychannel

First word defines the recipients, you can use comma separated list. 
# prefix indicates channel, @ indicates a nick.

Examples:
	
	- "#chan blah blah"
	  just sends to #chan
	
	- "@RJ psst, this is a pm"
	  just sends via PM to user RJ
	
	- "#channel1,#channel2,@RJ,@Russ blah blah this is the message"
	  this sends to 2 channels, and 2 users (RJ, Russ)
	
	- "#* Attention, something important"
	  this sends to all channels the bot is in	
	
	- "#chan %REDroses are red%NORMAL, grass is #GREENgreen#NORMAL"
	  you can use colors too, see IRCCat.java for the full list;
	  Either "#" or "%" will work.



Changing topics in channels
---------------------------
To change the topic in a channel (rather than sending a message), use
the prefix %TOPIC.

$ echo "%TOPIC #mychannel hello world" | netcat -q0 machinename 12345

This changes the topic of #mychannel to "hello world" (assuming the bot
has permission to set the topic)

Second word defines the recipient channels. You can use a comma separated
list. Nick recipients will be accepted, but silently ignored.

Examples:

  - "%TOPIC #chan new topic"
    just changes the topic of #chan

  - "%TOPIC #channel1,#channel2 this channel is great"
    this changes the topics of two channels

  - "%TOPIC #* Important Topic"
    this changes the topics in all channels the bot is in


Built-in commands
-----------------
There are a handful of built-in commands for instructing the bot at runtime.

Built-in commands are prefixed with a !

	!join #chan pass		- joins another channel. pass is optional
	!part #chan				- parts chan
	!channels				- lists channels the bot is in
	!spam blah blah..		- repeats your message in all joined channels
	!exit					- System.exit()

Trust
-----

Any command (?.. !..) uttered in a channel with irccat in is executed and 
implicitly trusted. 

Any command PMed to irccat is ignored unless the user 
is joined to the default (first) channel in the config file.
	

SVN commit notifications
------------------------
svn hooks let you announce commits etc.
For example, try this in your SVN repo/hooks/post-commit file:

REPOS="$1"
REV="$2"
LOG=`/usr/bin/svnlook log -r $REV $REPOS`
AUTHOR=`/usr/bin/svnlook author -r $REV $REPOS`
echo "SVN commit by $AUTHOR (r$REV) '$LOG' http://web-svn-interface.example.com/?rev=$REV" | netcat -q0 machinename 12345


Nagios alerts to irc
--------------------
Buried in our nagios config is this, from misccommands.cfg:

define command {
    command_name    host-notify-by-irc
    command_line    /bin/echo "#sys Nagios: Host '$HOSTALIAS$' is $HOSTSTATE$ - Info: $OUTPUT$" | /bin/netcat -q 1 irccathost 12345
}

define command {
    command_name    service-notify-by-irc
    command_line    /bin/echo "#sys Nagios: Service $SERVICEDESC$ on '$HOSTALIAS$' is $SERVICESTATE$ - Info: $OUTPUT$" | /bin/netcat -q 1 irccathost 12345
}

And in contacts.cfg:

define contact{
        contact_name                    irccat
        alias                   irccat
        service_notification_period     24x7
        host_notification_period        24x7
        service_notification_options    w,c,r
        host_notification_options       d,r
        service_notification_commands service-notify-by-irc
        host_notification_commands  host-notify-by-irc
        email   blah@blah
}

Feedback
--------
Email:  rj@metabrew.com
Web:    http://www.last.fm/user/RJ (work, and the reason irccat exists)
Web:    http://www.metabrew.com/   (blog)
Irc:    irc://irc.audioscrobbler.com/audioscrobbler
