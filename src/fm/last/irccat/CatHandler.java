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

import java.util.List;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Utility class to handle IRC cat output text.
 * 1) Handle parsing of initial line of the outgoing text
 * which can contain metadata about the recipients of
 * the message (channels or users) and whether its a /topic msg.
 *
 * 2) call the IRC cat bot the caller passes in, sending the text lines.
 */
class CatHandler implements Runnable {
  public static final String TOPIC_TOKEN = "%TOPIC";
  public static final String ALL_CHAN_TOKEN = "#*";

  private final List<String> lines = new LinkedList<String>();
  private final IRCCat bot;
  private final Socket socket;

  private boolean isBroadcastMessage;
  private boolean isTopicMessage;
  private String[] recipients = new String[0];

  public CatHandler(Socket s, IRCCat b) {
    this.socket = s;
    this.bot = b;
  }
 
  @Override
  public void run() {
    try {
      getLinesFromStream();
      checkForTopicMessage();
      populateRecipientList();  
      sendLines();
      //System.out.println("Handler finished.");
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
 
  private void sendLines() {
    for ( String line : lines ) {
      if ( isBroadcastMessage && isTopicMessage ) {
        bot.catTopicToAll(line);
      } else if ( isTopicMessage ) {
        bot.catTopic(line, recipients);
      } else if ( isBroadcastMessage ) {
        bot.catStuffToAll(line);
      } else { // neither topic or broadcast
        bot.catStuff(line, recipients);
      }
    }
 }

 private void getLinesFromStream() throws Exception {
   try {
     BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream(), "UTF-8")
      );
      String line;
      while ( null != (line = in.readLine()) ) {
        lines.add( line.trim() );
      }
    } finally {
      if ( null != socket ) {
        socket.close();
      }
    } 
 }

 private void checkForTopicMessage() {
    if ( lines.get(0).startsWith(TOPIC_TOKEN) ) {
      isTopicMessage = true;
      lines.set( 0, lines.get(0).substring(7) );
    }
 }

 private void populateRecipientList() {
    String firstLine = lines.get(0);
    if( firstLine.startsWith(ALL_CHAN_TOKEN) ) {
      isBroadcastMessage = true;
      lines.set( 0, firstLine.substring(3) );
    } else if ( firstLine.startsWith("#") || firstLine.startsWith("@") ) {
      int length = 0, index = 0;
      recipients = firstLine.split(",");
      while ( index < recipients.length ) {
        length += recipients[index].length(); // track size of text block 
        recipients[index] = recipients[index].trim();
        if ( recipients[index].startsWith("@") ) {
          // to a user, strip the @ for sendMessage()
          recipients[index] = recipients[index].substring(1); 
        }
        ++index;
      }
      lines.set( 0, lines.get(0).substring(length + index) );
    } else {
      recipients = new String[1];
      recipients[0] = bot.getDefaultChannel();
    }
  }
}

