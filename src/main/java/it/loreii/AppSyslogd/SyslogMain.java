package it.loreii.AppSyslogd;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import it.loreii.AppSyslogd.SyslogMsg.Data;

@Component
public class SyslogMain {
	@Value("${keystore.password}")
	private String keystorePassword;
	
	static final int PORT_NUMBER = 5140;// default is 514 TODO remove the 0
	static final int PORT_NUMBER_TLS = 6514;

	static final Logger log = LoggerFactory.getLogger(SyslogMsg.class);
	static final ExecutorService executorService = Executors.newFixedThreadPool(16);

	@Autowired
	private SyslogDataRepository repo;
	@Autowired
	private DictionaryRepository dict;

	public SyslogMain() throws InterruptedException, IOException {
		SyslogUDP udp = new SyslogUDP();
		udp.start();
		SyslogTCP tcp = new SyslogTCP();
		tcp.start();
		SyslogTLS tls = new SyslogTLS();
		tls.start();
	}

	private void process(InetAddress address, int port, String received) {
		try {
			if (StringUtils.isEmpty(received)) {
				log.debug("empty payload");
				return;
			}

			Data data = SyslogMsg.match(received);
			data.setAddress(address);
			data.setPort(port);

			addDictionary(data.getHostname(), "hostname");
			addDictionary(data.getPri().toString(), "pri");

			// System.out.println(data.toString());
			repo.save(data);
		} catch (NumberFormatException | ParseException e) {
			log.error("Unparsable Syslog", e);
		}
	}

	// keep a dictionary with unique entries
	private void addDictionary(String value, String type) {
		try {
			dict.save(new Dictionary(type, value));
		} catch (Exception e) {
			log.error("Dictionary update ", e);
		}
	}

	class SyslogTCP extends Thread {
		private boolean running;

		public SyslogTCP() throws IOException {

		}

		@Override
		public void run() {
			running = true;
			log.info("TCP Server ready..");
			while (running) {
				try (ServerSocket serverSocket = new ServerSocket(PORT_NUMBER)) {
					Socket clientSocket = serverSocket.accept();

					Runnable worker = new Runnable() {
						@Override
						public void run() {

							try (BufferedReader in = new BufferedReader(
									new InputStreamReader(clientSocket.getInputStream()))) {
								InetAddress address = clientSocket.getInetAddress();
								int port = clientSocket.getPort();

								String received;
								while ((received = in.readLine()) != null) {
									log.trace("TCP:" + received);
									process(address, port, received);
								}
							} catch (IOException e) {
								log.error("TCP server", e);
							}
						}
					};
					executorService.execute(worker);
				} catch (IOException e) {
					log.error("TCP server", e);
				}
			}
		}
	}

	/**
	 * https://tools.ietf.org/html/rfc5425
	 *
	 */

	class SyslogTLS extends Thread {
		private boolean running;
		private SSLContext sslContext;

		public SyslogTLS() throws IOException {
			KeyStore keystore = loadKeyStore("keystore.jks", keystorePassword);
			KeyManagerFactory kmf = loadKeyManager(keystore, keystorePassword);

			try {
				sslContext = SSLContext.getInstance("TLSv1.2");
				sslContext.init(kmf == null ? null : kmf.getKeyManagers(), null, null);
			} catch (NoSuchAlgorithmException | KeyManagementException e) {
				throw new RuntimeException("SSLContext cannot be loaded ..");
			}

		}

		@Override
		public void run() {
			running = true;
			log.info("TLS Server ready..");
			while (running) {
				try (ServerSocket serverSocket = sslContext.getServerSocketFactory()
						.createServerSocket(PORT_NUMBER_TLS)) {
					((SSLServerSocket) serverSocket).setEnabledProtocols(new String[] { "TLSv1.2" });

					Socket clientSocket = serverSocket.accept();

					Runnable worker = new Runnable() {
						@Override
						public void run() {

							try (BufferedReader in = new BufferedReader(
									new InputStreamReader(clientSocket.getInputStream()))) {
								InetAddress address = clientSocket.getInetAddress();
								int port = clientSocket.getPort();

								String received;
								while ((received = in.readLine()) != null) {
									log.trace("TLS:" + received);
									System.err.println("TLS:" + received);
									process(address, port, received);
								}
							} catch (IOException e) {
								e.printStackTrace();
								log.error("TLS server", e);
							}
						}
					};
					executorService.execute(worker);

				} catch (IOException e) {
					log.error("TLS handling", e);
				}
			}
		}

		private KeyManagerFactory loadKeyManager(KeyStore keystore, String password) {
			KeyManagerFactory kmf;
			try {
				kmf = KeyManagerFactory.getInstance("PKIX");
				kmf.init(keystore, password.toCharArray());
			} catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
				throw new RuntimeException("KeyManagerFactory cannot be loaded ..");
			}
			return kmf;
		}

		private KeyStore loadKeyStore(String filename, String password) {
			KeyStore keystore = null;

			try {
				keystore = KeyStore.getInstance("JKS");
				char[] psw = password.toCharArray();
				if (psw != null) {
					try (FileInputStream fis = new FileInputStream(new ClassPathResource(filename).getFile())) {
						keystore.load(fis, psw);
					}
					;
				}
			} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
				throw new RuntimeException("Keystore cannot be loaded ..");
			}
			return keystore;
		}
	}

	class SyslogUDP extends Thread {

		private DatagramSocket socket;
		private boolean running;
		// circular buffer reused for handling incoming messages but need semaphore to
		// avoid concurrent write
		private final int BUFFER_SIZE = 1_024 * 1_024 * 100; // MB
		private byte[] buffer = new byte[BUFFER_SIZE];
		private final BlockingQueue<Integer> free = new ArrayBlockingQueue<>(BUFFER_SIZE / 1_024);
		private final byte[] zero= new byte[1024];

		public SyslogUDP() throws SocketException, InterruptedException {
			socket = new DatagramSocket(PORT_NUMBER); // TODO handle server shutdown and socket close
		}

		public void run() {
			running = true;
			try {
				for (int idx = 0; idx < BUFFER_SIZE; idx += 1024)
					free.put(idx);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.info("UDP Server ready..");
			while (running) {

				try {
					DatagramPacket packet ;
					if(free.size()>0) {
						packet = new DatagramPacket(buffer, free.take(), 1024);
					}else {
						//avoid drop packet when running out of space
						packet = new DatagramPacket(zero.clone(), 0, 1024);
					}
					socket.receive(packet);
					Runnable worker = new Runnable() {
						@Override
						public void run() {

							try {
								int offset = packet.getOffset();
								int length = packet.getLength();
								String received = new String(packet.getData(), offset, length);
								if(offset>0) {
									System.arraycopy(zero, 0, buffer, offset, length);
									free.put(offset);
								}

								log.trace("UDP:" + received);
								System.err.println("UDP:" + received);

								InetAddress address = packet.getAddress();
								int port = packet.getPort();
								process(address, port, received);
							} catch (InterruptedException e) {
								log.error("UDP server", e);
							}

						}

					};
					executorService.execute(worker);

				} catch (IOException | InterruptedException e) {
					log.error("UDP server", e);
				}
			}
			socket.close();
		}
	}
}
