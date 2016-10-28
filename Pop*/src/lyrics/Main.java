package lyrics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class Main {

	public static void main(String[] args) {
		TreeMap<Integer, String> map = new TreeMap<Integer, String>();
		try {
			BufferedReader br;
			br = new BufferedReader(new FileReader("/Users/Benjamin/Documents/workspace/BibleWordThing/src/biblewords.txt"));
			
		    String line = br.readLine();
		
		    while (line != null) {
		    	StringBuilder originalLine = new StringBuilder(line);
		    	int i = Integer.parseInt(line.replaceAll("[^0-9]", ""));
		    	map.put(i, originalLine.toString());
		    	
		        line = br.readLine();
		    }
		    br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
				
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			System.out.print(entry.getKey() + ": ");
			System.out.println(entry.getValue());

		}

	}

}
