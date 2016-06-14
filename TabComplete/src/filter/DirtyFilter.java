package filter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class DirtyFilter {

	//Source: https://gist.github.com/ryanlewis/a37739d710ccdb4b406d
	private static String filePath = "/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/dirtystopwords.txt";
	private static Set<String> stopWords = loadStopWords();
	
	public static boolean isProfane(String content) {
		content = content.toLowerCase();
		for(String stopWord: stopWords) {
			if(content.contains(stopWord))
			{
				return true;
			}
		}
		
		return false;
	}

	private static Set<String> loadStopWords() {
		File file = new File(filePath);
		Set<String> stopWords = null;
		try {
			stopWords = new HashSet<String>();
			Scanner scan = new Scanner(new FileReader(file));
			while(scan.hasNextLine()) {
				stopWords.add(scan.nextLine().toLowerCase());
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
