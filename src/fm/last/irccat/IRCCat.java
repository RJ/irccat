/*
 irccat - a development support irc bot
 Copyright (C) 2006-2008 Richard Jones <rj@last.fm>

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; version 2 of the GPL only, not 3 :P

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package fm.last.irccat;

import org.apache.commons.configuration.*;
import org.jibble.pircbot.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class IRCCat extends PircBot {

  // we could use this for situations where an ordered sequence of tasks should
  // be run on a single worker thread in serial order of task submission.
  //static final ExecutorService threadPoolManager = Executors.newSingleThreadedExecutor();
  // a thread pool manager to handle our thread lifecycles
  static final ExecutorService threadPoolManager = Executors.newCachedThreadPool();

	private String nick;
	private String cmdScript;
	private String defaultChannel = null;
	private int maxCmdResponseLines = 26;
	private XMLConfiguration config;


	public static void main(String[] args) throws Exception {
		try {
			if (args.length == 0) {
				System.out.println("first param should be config file");
				System.exit(-1);
			}
			XMLConfiguration c = null;
			try {
				c = new XMLConfiguration(args[0]);
			} catch (ConfigurationException cex) {
				System.err.println("Configuration error, check config file");
				cex.printStackTrace();
				System.exit(1);
			}

			IRCCat bot = new IRCCat(c);

			// listen for stuff and send it to irc:
			ServerSocket serverSocket = null;
			InetAddress inet = null;
			try {
				if (bot.getCatIP() != null)
					inet = InetAddress.getByName(bot.getCatIP());
			} catch (UnknownHostException ex) {
				System.out
						.println("Could not resolve config cat.ip, fix your config");
				ex.printStackTrace();
				System.exit(2);
			}

			try {
				serverSocket = new ServerSocket(bot.getCatPort(), 0, inet);
			} catch (IOException e) {
				System.err.println("Could not listen on port: "
						+ bot.getCatPort());
				System.exit(1);
			}

			System.out.println("Listening on " + bot.getCatIP() + " : "
					+ bot.getCatPort());

			while (true) {
				try {
					Socket clientSocket = serverSocket.accept();
					// System.out.println("Connection on catport from: "
					// + clientSocket.getInetAddress().toString());
				  threadPoolManager.submit( new CatHandler(clientSocket, bot) );
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
      if (null != threadPoolManager) {
        threadPoolManager.shutdown();
      }
    }

	}

	public IRCCat(XMLConfiguration c) throws Exception {
		this.config = c;
		setEncoding("UTF8");
		cmdScript = config.getString("script.cmdhandler");
		maxCmdResponseLines = config.getInt("script.maxresponselines", 26);
		nick = config.getString("bot.nick");
		setName(nick);
		setLogin(nick);
		setMessageDelay(config.getLong("bot.messagedelay", 1000));
		setFinger(config.getString("bot.finger",
				"IRCCat - a development support bot, used by Last.fm"));

		try {
			// connect to server
			int tries =0 ;
			while (!isConnected()) {
				tries++;
				System.out.println("Connecting to server [try "+tries+"]: "+ config.getString("server.address"));
				connect(config.getString("server.address"), config.getInt(
						"server.port", 6667), config.getString(
						"server.password", ""));
				if(tries>1) Thread.sleep(10000);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}

	public String getCmdScript() {
		return cmdScript;
	}

	public int getCmdMaxResponseLines() {
		return maxCmdResponseLines;
	}

	protected void onDisconnect(){
      while (!isConnected()) {
         try {
            reconnect();
         }
         catch (Exception ex) {
            try {
               Thread.sleep(10000);
            } catch (InterruptedException e) {
               break;
            }
         }
      }
	}

	@SuppressWarnings("unchecked")
	protected void onConnect() {

		// join channels
		List<HierarchicalConfiguration> chans = config
				.configurationsAt("channels.channel");
		for (HierarchicalConfiguration chan : chans) {
			System.out.println("/join #" + chan.getString("name"));
			joinChannel("#" + chan.getString("name") + " "
					+ chan.getString("password", ""));
			// first one in the list considered default:
			if (defaultChannel == null)
				defaultChannel = "#"+chan.getString("name");
		}
        String nickpass = config.getString("server.identify","");
        if(nickpass != "")  identify(nickpass);
		
		System.out.println("Default channel: "+defaultChannel);
	}

	public int getCatPort() {
		return config.getInt("cat.port", 12345);
	}

	public String getCatIP() {
		return config.getString("cat.ip", "127.0.0.1");
	}

	public String getDefaultChannel() {
		return defaultChannel;
	}

	// PM was sent to us on irc
	public void onPrivateMessage(String sender, String login, String hostname,
			String message) {
		handleMessage(null, sender, message);
	}

    public void changeTopic(String target, String topic) {
            super.setTopic(target, topic);
    }

    public void sendMsg(String t, String m) {
            m = mIRCify(m);
            super.sendMessage(t, m);
    }

    public String mIRCify(String m) {
            Map<String, String> colorReplacementMap = new HashMap<String, String>();
            colorReplacementMap.put("NORMAL", Colors.NORMAL);
            colorReplacementMap.put("BOLD", Colors.BOLD);
            colorReplacementMap.put("UNDERLINE", Colors.UNDERLINE);
            colorReplacementMap.put("REVERSE", Colors.REVERSE);
            colorReplacementMap.put("WHITE", Colors.WHITE);
            colorReplacementMap.put("BLACK", Colors.BLACK);
            colorReplacementMap.put("DBLUE", Colors.DARK_BLUE);
            colorReplacementMap.put("DGREEN", Colors.DARK_GREEN);
            colorReplacementMap.put("RED", Colors.RED);
            colorReplacementMap.put("BROWN", Colors.BROWN);
            colorReplacementMap.put("PURPLE", Colors.PURPLE);
            colorReplacementMap.put("ORANGE", Colors.OLIVE);
            colorReplacementMap.put("YELLOW", Colors.YELLOW);
            colorReplacementMap.put("GREEN", Colors.GREEN);
            colorReplacementMap.put("TEAL", Colors.TEAL);
            colorReplacementMap.put("CYAN", Colors.CYAN);
            colorReplacementMap.put("BLUE", Colors.BLUE);
            colorReplacementMap.put("PINK", Colors.MAGENTA);
            colorReplacementMap.put("DGRAY", Colors.DARK_GRAY);
            colorReplacementMap.put("GRAY", Colors.LIGHT_GRAY);

            for(Map.Entry<String, String> e : colorReplacementMap.entrySet()) {
                    // Support #COLOR or %COLOR
                    // either format can be confusing, depending on context.
                    m = m.replaceAll( "%" + e.getKey(), e.getValue());
                    m = m.replaceAll( "#" + e.getKey(), e.getValue());
            }
            return m;
    }

	// message sent to our channel
	public void onMessage(String channel_, String sender, String login,
			String hostname, String message) {
		handleMessage(channel_, sender, message);
	}

	public void onPart(String _channel, String _sender, String _login,
			String _hostname) {
		if (!_sender.equals(nick))
			return;
		// System.out.println("Exiting due to onPart()");
		// System.exit(-1);
	}

	public void onQuit(String _sourceNick, String _sourceLogin,
			String _sourceHostname, String _reason) {
		if (!_sourceNick.equals(nick))
			return;
		System.out.println("Exiting due to onQuit()");
		System.exit(-1);
	}

	public void onKick(String channel_, String kickerNick, String kickerLogin,
			String kickerHostname, String recipientNick, String reason) {
		if (!recipientNick.equals(nick))
			return;

		// we were kicked
	}

	// is this nick trusted? (are they in the default channel)
	private boolean isTrusted(String n){
		//return true
		User[] users = getUsers(getDefaultChannel());
		for(int j =0; j<users.length; j++)
			if(n.equalsIgnoreCase(users[j].getNick()))  return true;
		
		return false;
	}
	
	public void handleMessage(String channel_, String sender, String message) {
		String cmd;
		String respondTo = channel_ == null ? sender : channel_;
		
		
		
		if (message.startsWith(nick)) {
			// someone said something to us.
			// we don't care.
			return;
		}

		if (message.startsWith("!")) {
			if(!isTrusted(sender)) {
                System.out.println("UNTRUSTED (ignoring): ["+respondTo+"] <"+sender+"> "+message);
                return;
            }
			// irccat builtin command processing:
			String resp = handleBuiltInCommand(message.substring(1).trim(),
					sender);
			if (!(resp == null || resp.equals("")))
				sendMessage(respondTo, resp);
            
            System.out.println("Built-in: ["+respondTo+"] <"+sender+"> "+message);
			return;
		}

		if (message.startsWith("?")) {
			// external script command.
			cmd = message.substring(1).trim();
		} else {
			// just a normal message which we ignore
			return;
		}

		if (cmd.trim().length() < 1)
			return;
		
		// if a PM, you gotta be trusted.
		if(channel_ == null && !isTrusted(sender)) {
            System.out.println("UNTRUSTED (ignoring): ["+respondTo+"] <"+sender+"> "+message);
            return;
        }
		
		// now "cmd" contains the message, minus the address prefix (eg: ?)
		// hand off msg to thread that executes shell script
    System.out.println("Scripter: ["+respondTo+"] <"+sender+"> "+message);
    threadPoolManager.submit( new Scripter(sender, channel_, respondTo, cmd, this) );
	}

	/*
	 * Basic built-in command processing allows you to instruct the bot at
	 * runtime to join/leave channels etc
	 */
	protected String handleBuiltInCommand(String cmd, String sender) {
		String toks[] = cmd.split(" ");
		String method = toks[0];

		// JOIN A CHANNEL
		if (method.equals("join") && toks.length >= 2) {
			if (toks.length == 3)
				joinChannel(toks[1], toks[2]);
			else
				joinChannel(toks[1]);

			sendMessage(toks[1], "<" + sender + "> !" + cmd);
			return "Joining: " + toks[1];
		}

		// PART A CHANNEL
		if (method.equals("part") && toks.length == 2) {
			sendMessage(toks[1], "<" + sender + "> !" + cmd);
			partChannel(toks[1]);
			return "Leaving: " + toks[1];
		}

		// BROADCAST MSG TO ALL CHANNELS
		if (method.equals("spam")) {
			this.catStuffToAll("<" + sender + "> " + cmd.substring(5));
		}

		// LIST CHANNELS THE BOT IS IN
		if (method.equals("channels")) {
			String[] c = getChannels();
			StringBuffer sb = new StringBuffer("I am in " + c.length
					+ " channels: ");
			for (int i = 0; i < c.length; ++i)
				sb.append(c[i] + " ");
			return sb.toString();
		}

		// EXIT()
		if (method.equals("exit"))
			System.exit(0);

		return "";
	}

	public void catTopic(String stuff, String[] recips) {
		for (int ci = 0; ci < recips.length; ci++) {
			changeTopic(recips[ci], stuff);
		}
	}

	public void catTopicToAll(String stuff) {
		String[] channels = getChannels();
		for (int i = 0; i < channels.length; i++) {
			changeTopic(channels[i], stuff);
		}
	}

	public void catStuffToAll(String stuff) {
		String[] channels = getChannels();
		for (int i = 0; i < channels.length; i++) {
			sendMsg(channels[i], stuff);
		}
	}

	public void catStuff(String stuff, String[] recips) {
		for (int ci = 0; ci < recips.length; ci++) {
			sendMsg(recips[ci], stuff);
		}
	}

}
