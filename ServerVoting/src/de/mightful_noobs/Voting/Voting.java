package de.mightful_noobs.Voting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Vector;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.FunctionExceptionLog;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleClientList;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import de.stefan1200.util.ArrangedPropertiesWriter;

public class Voting implements HandleBotEvents, LoadConfiguration, HandleClientList {

	public static void main(String[] args) {
	}

	private JTS3ServerMod_Interface modClass = null;
	private JTS3ServerQuery queryLib = null;
	private String configPrefix = "";
	private boolean pluginEnabled = false;

	private Vector<Integer> clientList = new Vector<Integer>();
	private FunctionExceptionLog fel = new FunctionExceptionLog();
	private String sgid = "";
	private String key = "";
	private String message = "";
	private boolean firstInit = true;

	@Override
	public void initClass(JTS3ServerMod_Interface modClass, JTS3ServerQuery queryLib, String prefix) {
		this.modClass = modClass;
		this.queryLib = queryLib;
		configPrefix = prefix.trim();
	}

	public void initConfig(ArrangedPropertiesWriter config) {
		config.addKey(configPrefix + "_sgid", "Servergroup-ID?");
		config.addKey(configPrefix + "_key", "API-Key");
		config.addKey(configPrefix + "_message", "Voting Message");
	}

	@Override
	public void handleOnBotConnect() {
		if (!pluginEnabled) {
			return;
		}
	}

	@Override
	public void handleAfterCacheUpdate() {
		if (!pluginEnabled) {
			return;
		}
	}

	@Override
	public void activate() {
		if (!pluginEnabled) {
			return;
		}
	}

	@Override
	public void disable() {
		if (!pluginEnabled) {
			return;
		}
	}

	@Override
	public void unload() {
		message = null;
		clientList = null;
	}

	@Override
	public boolean multipleInstances() {
		return false;
	}

	@Override
	public int getAPIBuild() {
		return 4;
	}

	@Override
	public String getCopyright() {
		return "Voting function v1.4 (29.06.2019) created by Sascha Moser (Xopez), [url]https://mightful-noobs.de[/url]";
	}

	public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode)
			throws BotConfigurationException, NumberFormatException {
		String temp = null;
		pluginEnabled = false;
		String entry="";
		entry = "====================";
		addLogEntry(entry, "Info");
		entry = "Voting is loading...";
		addLogEntry(entry, "Info");
		try {
			entry = "Configuration:";
			addLogEntry(entry, "Info");

			temp = null;
			clientList.clear();
			temp = config.getValue(configPrefix + "_sgid");
			if (temp == null) {
				throw new NumberFormatException();
			}
			sgid = temp;
			entry="Servergroup-ID: " + sgid.toString();
			addLogEntry(entry, "Info");
			
			temp = config.getValue(configPrefix + "_key");
			if (temp == null) {
				throw new NullPointerException();
			}
			key = temp;
			entry="API-Key: " + key.toString();
			addLogEntry(entry, "Info");
			
			temp = config.getValue(configPrefix + "_message");
			if (temp == null) {
				throw new NullPointerException();
			}
			message = temp;
			entry="Message: \"" + message.toString() + "\"";
			addLogEntry(entry, "Info");
			
			pluginEnabled = true;
			entry="Started successfully!";
			addLogEntry(entry, "Info");
			entry = "====================";
			addLogEntry(entry, "Info");
		} catch (NumberFormatException e) {
			NumberFormatException nfe = new NumberFormatException("Config value of \"" + temp
					+ "\" is not a number! Current value: " + config.getValue(temp, "not set"));
			nfe.setStackTrace(e.getStackTrace());
			throw nfe;
		}
		return pluginEnabled;
	}

	public void handleClientCheck(Vector<HashMap<String, String>> clientList) {
		if (!pluginEnabled) {
			return;
		}
		if (firstInit) {
			firstInit = false;
		}

		// Go through the whole client list
		for (HashMap<String, String> clientInfo : clientList) {
			// Check real Teamspeak 3 clients only, ignore query clients
			String clientNickname = clientInfo.get("client_nickname");
			String clientDbId = clientInfo.get("client_database_id");
			int clientId = Integer.parseInt(clientInfo.get("clid"));
			if (clientInfo.get("client_type").equals("0")) {
				// Parse the client id as int
				int hasVoted = 0;
				try {
					hasVoted = checkVote(clientNickname);
					switch (hasVoted) {
					case 0:
						break;
					case 1:
						try {
							int claim = setClaim(clientNickname, clientDbId, clientId);
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
					case 2:
						break;
					default:
						break;
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
			}
		}
	}

	public int checkVote(String clientNickname) throws Exception {
		int check = 0;
		try {
			String url = "https://teamspeak-servers.org/api/?object=votes&element=claim&key=" + key + "&username="
					+ clientNickname.replace(" ", "%20");
			check = Integer.valueOf(URLReader(url));
		} catch (Exception e) {
			Exception nfe = new Exception("Couldn't open URL");
			nfe.setStackTrace(e.getStackTrace());
			throw nfe;
		}
		return check;
	}

	public int setClaim(String clientNickname, String clientDBId, int clientId) throws Exception {
		int claim = 0;
		try {
			String url = "https://teamspeak-servers.org/api/?action=post&object=votes&element=claim&key=" + key + "&username="
					+ clientNickname.replace(" ", "%20");
			claim = Integer.valueOf(URLReader(url));
			setServerGroup(clientNickname, clientDBId, clientId);
		} catch (Exception e) {
			Exception nfe = new Exception("Couldn't open URL");
			nfe.setStackTrace(e.getStackTrace());
			throw nfe;
		}
		return claim;
	}

	public void setServerGroup(String clientNickname, String clientDBId, int clientId) {

		try {
			queryLib.doCommand("servergroupaddclient sgid=" + sgid + " cldbid=" + clientDBId);
			String msg = "Servergruppe " + sgid + " zu " + clientNickname + " hinzugefügt";
			queryLib.sendTextMessage(clientId, JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, message);
			addLogEntry(msg,"Info");
			fel.clearException(Integer.parseInt(clientDBId));
		} catch (TS3ServerQueryException sqe) {
			// This prevents flooding the log file with error messages, if a missing
			// permission of the bot account on the Teamspeak 3 server is missing.
			if (!fel.existsException(sqe, Integer.parseInt(clientDBId))) {
				// Since there is no saved TS3ServerQueryException for this client, save it now.
				fel.addException(sqe, Integer.parseInt(clientDBId));

				// Adding error message to the bot log.
				String entry="Cannot send chat command answer to client \"" + clientNickname + "\" (db id: " + clientId
						+ "), an error occurred while sending message to client!";
				addLogEntry(entry,"Error");
				modClass.addLogEntry(configPrefix, sqe, false);
			}
		}
	}

	public String URLReader(String strUrl) throws Exception {

		URLConnection connection = new URL(strUrl).openConnection();
		connection.setRequestProperty("User-Agent", "Java/8+(compatible; Voting-Addon; https://mightful-noobs.de/)");
		connection.connect();

		BufferedReader r = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));

		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}
	
	@Override
	public void setListModes(BitSet listOptions) {
		listOptions.set(1);
		listOptions.set(4);
	}
	
	public void addLogEntry(String entry, String level) {
		if (level == "Info") {
			modClass.addLogEntry(configPrefix, JTS3ServerMod_Interface.ERROR_LEVEL_INFO, entry, true);}
		else if (level == "Error") {
			modClass.addLogEntry(configPrefix, JTS3ServerMod_Interface.ERROR_LEVEL_ERROR, entry, false);	
		}
		else {
			modClass.addLogEntry(configPrefix, JTS3ServerMod_Interface.ERROR_LEVEL_INFO, "UNDEF", true);}
		}
	
	
}