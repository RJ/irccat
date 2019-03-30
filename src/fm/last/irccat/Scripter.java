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

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.LinkedList;

// hands off cmd to shell script and returns stdout to the requester
class Scripter implements Runnable {
  private final IRCCat bot;
  private final String nick, channel, returnName, cmd;

  public Scripter( String nk, String ch, String r, String c, IRCCat b ) {
    this.nick = nk;
    this.channel = ch;
    this.cmd = c;
    this.returnName = r;
    this.bot = b;
  }

  private Process startProcess() throws Exception {
    String message = nick + " " + channel + " " + returnName + " " + " " + cmd;
    return new ProcessBuilder(bot.getCmdScript(), message).start();
  }

  @Override
  public void run() {
    BufferedReader reader = null;
    Process process = null;
    try {
      process = startProcess();
      reader = new BufferedReader( new InputStreamReader(process.getInputStream(), "UTF-8") );
      String line;
      int lineCount = 0;
      while ( (line = reader.readLine()) != null) {
        bot.sendMsg(returnName, line);
        if ( ++lineCount == bot.getCmdMaxResponseLines() ) {
          bot.sendMsg(returnName, "<truncated, too many lines>");
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if ( null != reader ) {
        try { reader.close(); } catch (Exception ignored) { }
      }
      if ( null != process ) {
        try { process.destroy(); } catch (Exception ignored) { }
      }
    }
  }
}

