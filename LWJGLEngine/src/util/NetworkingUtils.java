package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class NetworkingUtils {

	public static String getLocalIP() {
		InetAddress localhost = null;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		return localhost.getHostAddress().trim();
	}

	public static String getPublicIP() {
		String publicIPAddress = "";
		try {
			URL url_name = new URL("https://v4.ident.me/");

			BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));

			// reads system IPAddress
			publicIPAddress = sc.readLine().trim();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return publicIPAddress;
	}

}
