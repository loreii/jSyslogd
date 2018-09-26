package it.loreii.AppSyslogd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.loreii.AppSyslogd.Application;

public class SyslogProducer {

	static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) throws InterruptedException, IOException {
		long i = 1;
		long th = 1000;

		while (true) {
			long st = System.currentTimeMillis();
			Date time = Calendar.getInstance().getTime();
			int r = 0;
			for (; r < i; ++r) {
//				 logger.debug("debug message slf4j " + time);
//				 logger.info("info message slf4j " + time);
//				 logger.warn("warn message slf4j " + time);
//				 logger.error("error message slf4j " + time);
				push(hostnames[(int) (Math.random() * 100 % hostnames.length)] + "."
						+ domains[(int) (Math.random() * 100 % domains.length)],
						(int) (150 + Math.random() * 1000 % 100));
				push(hostnames[(int) (Math.random() * 100 % hostnames.length)] + "."
						+ domains[(int) (Math.random() * 100 % domains.length)],
						(int) (150 + Math.random() * 1000 % 100));
				push(hostnames[(int) (Math.random() * 100 % hostnames.length)] + "."
						+ domains[(int) (Math.random() * 100 % domains.length)],
						(int) (150 + Math.random() * 1000 % 100));
				push(hostnames[(int) (Math.random() * 100 % hostnames.length)] + "."
						+ domains[(int) (Math.random() * 100 % domains.length)],
						(int) (150 + Math.random() * 1000 % 100));
			}
			long en = System.currentTimeMillis();

			long elapsed = (en - st);
			if (elapsed > 1000) {
				// System.out.println("reached 1000 ms");
			} else {
				Thread.sleep(th - elapsed);
			}
			System.err.println(i + ")" + th + " ms     " + elapsed + " ms     " + (elapsed / (r * 4)) + " ms   "
					+ (r * 4) + " msg/sec");

			if ((en - st) < 950) { // rump fast
				i += 100;
			} else if ((en - st) < 1000) { // slow down
				i += 10;
			} else if ((en - st) > 1000) { // back
				i -= 10;
			}

		}

	}

	static String[] hostnames = { "aqueduct", "bar", "bay", "beach", "brook", "cape", "coast", "creek", "culvert",
			"dam", "dock", "gate", "harbor", "hook", "inlet", "island", "jetty", "kill", "marina", "pier", "point",
			"river", "seaside", "shoal", "shore", "sound", "stream", "trench", "wash", "zanja", "almont", "alpine",
			"alta", "ambassador", "arden", "bedford", "benedict", "beverley", "brighton", "camden", "canon", "chalette",
			"clark", "clifton", "coldwater", "cove", "crescent", "dabney", "dayton", "della", "doheny", "elcamino",
			"elm", "evelyn", "foothill", "greenacres", "hartford", "hillcrest", "lapeer", "leona", "lindacrest",
			"linden", "lomavista", "maple", "mountain", "oakhurst", "oxford", "palm", "pamela", "peck", "pine",
			"reeves", "rexford", "robertson", "rodeo", "roxbury", "shadowhill", "sierra", "spaulding", "stonewood",
			"summit", "sunset", "swall", "tower", "wallace", "wetherly" };
	static String[] domains = { "it", "com", "co.uk" };

	static InetAddress address ;
	static DatagramSocket socket;
	static {
		try {
			address = InetAddress.getByName("localhost");
			socket = new DatagramSocket();
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	static DateFormat sdf = new SimpleDateFormat("YYYY-MM-DD'T'HH:mm:ss.000'Z'");
	
	public static void push(String hostname, int pri) throws IOException {
		
			String time = sdf.format(new Date((long) (System.currentTimeMillis() + 	(Math.random() * 10055000))));

			StringBuffer string = new StringBuffer("<").append(pri).append(">1 ").append(time).append(" ").append(hostname).append(" evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] An application event log entry...");
			byte[] buf = string.toString().getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 5140);
			socket.send(packet);
		
	}

}
