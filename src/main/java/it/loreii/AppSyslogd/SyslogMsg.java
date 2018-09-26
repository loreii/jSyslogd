package it.loreii.AppSyslogd;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Simple match
 */
public final class SyslogMsg {
	/**
	 * Simple regexp for matching the first part of protocol definition RFC3164 This
	 * version SHALL not be anymore used but still present into the web
	 * https://tools.ietf.org/html/rfc3164
	 * 
	 * PRI VERSION TIMESTAMP HOSTNAME MSG
	 */
	private final static String regex_rfc3164 = "^\\<([0-9]{1,3})\\>([A-Za-z]{3}[\\s]+[0-9]{1,2} \\d{2}:\\d{2}:\\d{2}) ([\\S]+) ([\\S\\s]+)";
	private final static FastDateFormat dateFormat_rfc3164 = FastDateFormat.getInstance("MMM dd HH:mm:ss"); //TODO use something smarter and parse directly it
	private final static FastDateFormat dateFormat_rfc3164_2 = FastDateFormat.getInstance("MMM  d HH:mm:ss");
	private final static Pattern pattern_rfc3164 = Pattern.compile(regex_rfc3164);
	/**
	 * Simple matcher for RFC-5424, identify the main parts as groups
	 * https://tools.ietf.org/html/rfc5424
	 * 
	 * PRI VERSION TIMESTAMP HOSTNAME APP-NAME PROCID MSGID STRUCTURED-DATA [MSG]
	 */
	private final static String regex_rfc5424 = "^\\<([0-9]{1,3})\\>(\\d{0,2}) (\\-|[\\S]+) (\\-|[\\S]{1,255}) (\\-|[\\S]{1,48}) (\\-|[\\S]{1,128}) (\\-|[\\S]{1,32}) (\\-|\\[[\\S\\s]+\\]) ([\\S\\s]+){0,1}";
	private final static FastDateFormat dateFormat_rfc5424 = FastDateFormat.getInstance("yyyy-MM-DD'T'HH:mm:ss.SSS'Z'"); //TODO handle timezone
	private final static Pattern pattern_rfc5424 = Pattern.compile(regex_rfc5424);

	public static Data match(String input) throws NumberFormatException, ParseException {
		try {
			Matcher m = pattern_rfc5424.matcher(input);
			m.find();

			return Data.makeDataRFC5424(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
					dateFormat_rfc5424.parse(m.group(3)), m.group(4), m.group(5), m.group(6), m.group(7), m.group(8),
					m.group(9));
		} catch (java.lang.IllegalStateException e) {
			Matcher m = pattern_rfc3164.matcher(input);
			m.find();
			Data data;
			try {
				data = Data.makeDataRFC3164(Integer.parseInt(m.group(1)), dateFormat_rfc3164.parse(m.group(2)), m.group(3), m.group(4));
			}catch (Exception ex) {
				data = Data.makeDataRFC3164(Integer.parseInt(m.group(1)), dateFormat_rfc3164_2.parse(m.group(2)), m.group(3), m.group(4));
			}
			return data;
		}
	}
	
	/**
	 * Syslog data container
	 * */
	static class Data {

		private InetAddress address;
		int port;
		
		Integer pri;
		Integer version;
		Date timestamp;
		Date createdAt;
		String hostname;
		String appName;
		String procId;
		String msgId;
		String structuredData;
		String msg;

		public static Data makeDataRFC3164(Integer pri, Date timestamp, String hostname, String msg) {
			return new Data(pri, timestamp, hostname, msg);
		}

		public static Data makeDataRFC5424(Integer pri, Integer version, Date timestamp, String hostname,
				String appName, String procId, String msgId, String structuredData, String msg) {
			return new Data(pri, version, timestamp, hostname, appName, procId, msgId, structuredData, msg);
		}

		public Data() {
		//for JPA	
		}
		
		private Data(Integer pri, Integer version, Date timestamp, String hostname, String appName, String procId,
				String msgId, String structuredData, String msg) {
			super();
			this.pri = pri;
			this.version = version;
			this.timestamp = timestamp;
			this.hostname = hostname;
			this.appName = appName;
			this.procId = procId;
			this.msgId = msgId;
			this.structuredData = structuredData;
			this.msg = msg;
			this.createdAt = new Date();
		}

		private Data(Integer pri, Date timestamp, String hostname, String msg) {
			this(pri, null, timestamp, hostname, null, null, null, null, msg);
		}

		public Integer getPri() {
			return pri;
		}

		public Integer getVersion() {
			return version;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public String getHostname() {
			return hostname;
		}

		public String getAppName() {
			return appName;
		}

		public String getProcId() {
			return procId;
		}

		public String getMsgId() {
			return msgId;
		}

		public String getStructuredData() {
			return structuredData;
		}

		public String getMsg() {
			return msg;
		}

		public InetAddress getAddress() {
			return address;
		}

		public void setAddress(InetAddress address) {
			this.address = address;
		}
		
		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public Date getCreatedAt() {
			return createdAt;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Data [");
			if (address != null)
				builder.append("address=").append(address).append(", ");
			builder.append("port=").append(port).append(", ");
			if (pri != null)
				builder.append("pri=").append(pri).append(", ");
			if (version != null)
				builder.append("version=").append(version).append(", ");
			if (timestamp != null)
				builder.append("timestamp=").append(timestamp).append(", ");
			if (hostname != null)
				builder.append("hostname=").append(hostname).append(", ");
			if (appName != null)
				builder.append("appName=").append(appName).append(", ");
			if (procId != null)
				builder.append("procId=").append(procId).append(", ");
			if (msgId != null)
				builder.append("msgId=").append(msgId).append(", ");
			if (structuredData != null)
				builder.append("structuredData=").append(structuredData).append(", ");
			if (msg != null)
				builder.append("msg=").append(msg);
			builder.append("]");
			return builder.toString();
		}

		

	}
}
