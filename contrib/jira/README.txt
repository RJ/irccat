This is a JIRA listener plugin which sends notifications to irccat on JIRA ticket changes.

For information on how to build this:

http://confluence.atlassian.com/display/DEVNET/How+to+Build+an+Atlassian+Plugin

To install and configure:

- Copy the .jar file to the WEB-INF/lib directory in your JIRA install
- Restart JIRA
- The plugin should now show up under Administration->Plugins
- Under Administration->Listeners, add a new listener with the class fm.last.jira.plugins.IrccatListener
- Edit the config for that listener:
 - irccat.channel is the channel that you want the notifications sent to
 - irccat.host/port is the host/port where irccat is running
 - irccat.projectkeyregex is a regular expression matching the project key you want this listener to affect
- You can add as many listeners as you require (one per channel)


Russ Garrett
russ@last.fm
