package tabcomplete.filter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class DirtyFilter {

	
	private static final boolean DEBUG = false;
	//Source: https://gist.github.com/ryanlewis/a37739d710ccdb4b406d
	private static String filePath = "/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/dirtystopwords.txt";
	private static Set<String> stopWords = loadStopWords();
	private static String patternString = "\\b(" + StringUtils.join(stopWords, "|").replaceAll("\\.", "\\\\.") + ")\\b";
	private static Pattern pattern = Pattern.compile(patternString);
	
	public static boolean isProfane(String content) {
		Matcher matcher = pattern.matcher(content);
		boolean found = matcher.find();
		if (DEBUG && found) {
			System.out.println("Found the word \""+ matcher.group(1) +"\"" );
		}
		return found;
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
			scan.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return stopWords;
	}

	public static void main(String[] args) {
		String[] tests = new String[]{"ass","bass","kick-ass","ass113","assume","as","my ass is"};
		for (String test : tests) {
			System.out.println(test + " is " + (isProfane(test)?"":"not ") + "profane");
		}
	}
}
