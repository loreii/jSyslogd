package it.loreii.AppSyslogd;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import org.springframework.core.io.ClassPathResource;

public class ExampleTLS {

	private static final int PORT = 5555;
	private static final int BACKLOG = 100;

	
	public static void main(String[] args) throws IOException {
	
		KeyStore keystore = loadKeyStore(args[0],args[1]);
		SSLContext sslContext = null;
		
		KeyManagerFactory kmf;
		try {
		    kmf = KeyManagerFactory.getInstance("PKIX");
			kmf.init(keystore, args[1].toCharArray());
		} catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
			 throw new RuntimeException("KeyManagerFactory cannot be loaded ..");
		}
		
		try {
		    sslContext = SSLContext.getInstance("TLSv1.2");
		    sslContext.init(kmf==null?null:kmf.getKeyManagers(), null, null);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			 throw new RuntimeException("SSLContext cannot be loaded ..");
		}
		
		ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket(PORT);
		((SSLServerSocket)serverSocket).setEnabledProtocols(new String[]{"TLSv1.2"});
		for (String s: ((SSLServerSocket)serverSocket).getEnabledCipherSuites()) {
		    System.out.println(s);
		}
		
//		ServerSocketFactory tlsSocketFactory = SSLServerSocketFactory.getDefault();
//		ServerSocket serverSocket = tlsSocketFactory.createServerSocket(PORT);
		Socket clientSocket = serverSocket.accept();
		
		try (BufferedReader in = new BufferedReader(
				new InputStreamReader(clientSocket.getInputStream()))) {
			InetAddress address = clientSocket.getInetAddress();
			int port = clientSocket.getPort();

			String received;
			while ((received = in.readLine()) != null) {
				System.err.println(received);
			}
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	
	private static KeyStore loadKeyStore(String filename, String password) {
	    KeyStore keystore = null;

	    try {
	        keystore = KeyStore.getInstance("JKS");
	       
			char[] psw = password.toCharArray();
	        if (psw != null) {
	        	try(FileInputStream fis = new FileInputStream(new ClassPathResource(filename).getFile())){	        		
	        		keystore.load(fis, psw);
	        	};
	        }
	    } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
	        throw new RuntimeException("Keystore cannot be loaded ..");
	    } 
	    return keystore;
	}

}
