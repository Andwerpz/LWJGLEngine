package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {

	public static String loadAsString(String file) {
		StringBuilder result = new StringBuilder();
		try {
			BufferedReader fin = new BufferedReader(new FileReader(file));
			String buffer = "";
			while((buffer = fin.readLine()) != null) {
				result.append(buffer + '\n');
			}
			fin.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return result.toString();
	}
	
}
