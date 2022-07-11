package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileUtils {

	public static String loadAsString(String file) {
		StringBuilder result = new StringBuilder();
		InputStream is;
		try {
			System.out.println("LOADING: " + file);
			is = FileUtils.class.getResourceAsStream(file);
			BufferedReader fin = new BufferedReader(new InputStreamReader(is));
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
