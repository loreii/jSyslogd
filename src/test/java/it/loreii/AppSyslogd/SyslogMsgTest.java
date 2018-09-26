package it.loreii.AppSyslogd;

import java.text.ParseException;
import org.junit.Test;

import it.loreii.AppSyslogd.SyslogMsg;
import it.loreii.AppSyslogd.SyslogMsg.Data;

import org.junit.Assert;

public class SyslogMsgTest {
	static final String test_5424 = "<34>1 2003-10-11T22:14:15.003Z mymachine.example.com su - ID47 - BOM'su root' failed for lonvick on /dev/pts/8";
	static final String test_5424_1 = "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] An application event log entry...";
	static final String test_3164 = "<131>Jan 23 17:05:05 rdmlpavesi1d gs-rest-service error message slf4j thread:http-nio-8080-exec-1 priority:ERROR category:it.loreii.AppSyslogd.Application exception:";
	static final String[] tests = { test_5424, test_5424_1, test_3164 };

	@Test
	public void testSomeRealMessage() throws NumberFormatException, ParseException {
		for (String i : tests) {
			Data data = SyslogMsg.match(i);
			Assert.assertNotNull(data);
		}
	}

}
