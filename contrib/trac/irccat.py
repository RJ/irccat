import socket
from trac.core import *
from trac.ticket.api import ITicketChangeListener

class IrcCatListener(Component):
    implements(ITicketChangeListener)

    def _sendText(self, ticketid, text):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect(("1.2.3.4",12345))
            s.send("#last.fm Trac: ticket #%i (http://www.example.com/trac/ticket/%i) %s" % (ticketid, ticketid, text))
            s.close()
        except:
            return

    def ticket_created(self, ticket):
        self._sendText(ticket.id, "\"%s\" created by %s." % (ticket.values['summary'][0:100], ticket.values['reporter']))

    def ticket_changed(self, ticket, comment, author, old_values):
        self._sendText(ticket.id, "changed by %s, Comment: %s." % (author, comment[0:100]))
