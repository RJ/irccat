package fm.last.jira.plugins;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.event.issue.AbstractIssueEventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.IssueEventListener;
import com.atlassian.jira.issue.Issue;

public class IrccatListener extends AbstractIssueEventListener implements IssueEventListener {

	private String[] acceptedParams = {"irccat.projectkeyregex", "irccat.host", "irccat.port", "irccat.channel"};
	private Pattern projectKeyPattern = null;
	private String host = null;
	private int port;
	private String channel = null;
	
	public void init(Map params) {
		if (params.get("irccat.projectkeyregex") != null) {
			this.projectKeyPattern = Pattern.compile((String)params.get("irccat.projectkeyregex"), Pattern.CASE_INSENSITIVE);
		}
		this.host = (String)params.get("irccat.host");
		this.port = Integer.parseInt((String)params.get("irccat.port"));
		this.channel = (String)params.get("irccat.channel");
		ApplicationProperties a = ManagerFactory.getApplicationProperties();
		Iterator keys =  a.getKeys().iterator();
		while (keys.hasNext()) {
			String b = (String)keys.next();
			System.err.println(b + " = " + a.getString(b));
		}
	}
	
	protected boolean isEventMonitored(IssueEvent event) {
		return projectKeyPattern != null && projectKeyPattern.matcher(event.getIssue().getProjectObject().getKey()).matches();
	}
	
	protected void sendNotification(String message) {
		Socket so = null;
		try {
			so = new Socket(host, port);
			OutputStreamWriter osw = new OutputStreamWriter(so.getOutputStream());
			osw.write(channel + " " + message + "\n");
			osw.close();
		} catch (IOException e) {
			System.err.println("Unable to send irccat message to " + host + ":" + port + ", due to: " + e.getMessage());
		} finally {
			try {
				if (so != null)
					so.close();
			} catch (Exception e) {}
		}
	}
	
	public void workflowEvent(IssueEvent event) {
		if (!isEventMonitored(event))
			return;
		Issue issue = event.getIssue();
		StringBuilder sb = new StringBuilder();
		sb.append("JIRA issue ");
		sb.append(ManagerFactory.getApplicationProperties().getString(APKeys.JIRA_BASEURL));
		sb.append("/browse/");
		sb.append(issue.getKey());
		sb.append(" ");
		sb.append(ComponentManager.getInstance().getEventTypeManager().getEventType(event.getEventTypeId()).getName().toLowerCase());
		sb.append(" by ");
		sb.append(event.getRemoteUser().getName());
		if (event.getComment() != null && event.getComment().getBody().length() > 0) {
			sb.append(" \"");
			sb.append(StringUtils.abbreviate(event.getComment().getBody(), 100));
			sb.append("\"");
		}

		sendNotification(sb.toString());
	}

	public String[] getAcceptedParams() {
		return acceptedParams;
	}

	public String getDescription() {
		return "IRCCat listener";
	}

	public boolean isInternal() {
		return false;
	}

	public boolean isUnique() {
		return false;
	}
}
