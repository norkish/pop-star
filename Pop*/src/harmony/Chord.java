package harmony;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pitch.Pitch;

public class Chord implements Serializable{

	public static class ChordQuality implements Serializable{
//		SortedMap<Short,Short> notes = new TreeMap<Short,Short>();
		private String quality;
		private boolean isMinor;
		
		public ChordQuality()
		{
			quality = "";
		}
		
		private ChordQuality(String input) {
			quality = input;
			isMinor = quality.matches("m([^aj].*)?");
		}

		public static ChordQuality parseChordQuality(String input, boolean confidentSource) {
			if (input == null)
				return null;
			else if  (input.length() == 0)
				return new ChordQuality();

			String token = input;
			int tokenLen = token.length();
			int prevLen = Integer.MAX_VALUE;
			
			// If we want to keep track of all the qualities, just create an object and pass it in to the consume function
			while (tokenLen > 0 && tokenLen < prevLen) {
				prevLen = tokenLen;
				token = consume(token);
				tokenLen = token.length();
			}
			
			if (tokenLen > 0 && !token.equals("-") && !token.equals("th")){
				if (!confidentSource || tokenLen > 1)
					return null;
			}
				
			return new ChordQuality(input.substring(0,input.length()-tokenLen));
		}
		
		

		private static Matcher matcher;
		
		private static String consume(String token) {
			if (augDimSymbols.contains(token.substring(0, 1))) {
				token = token.substring(1);
			}
			if (token.matches("(?i)(maj|min|dim|aug|dom|sus|add).*")){
				token = token.substring(3);
			} else if (token.matches("(?i)(ma|mj).*")){
				token = token.substring(3);
			} else if (token.matches("^[mM].*")){
				token = token.substring(1);
			}
			if (token.matches("\\/.*")){
				token = token.substring(1);
			}
			matcher = numericPitch.matcher(token);
			if (matcher.find()) {
				token = token.substring(matcher.end());
			}
			matcher = alphaPitch.matcher(token);
			if (matcher.find()) {
				token = token.substring(matcher.end());
			}
			
			if (token.length() > 0 && token.charAt(0) == '(') {
					token = token.substring(1);
			}
			if (token.length() > 0 && token.charAt(0) == ')') {
					token = token.substring(1);
			}
			
			return token;
		}

		public String toString() {
			return quality;
		}

		public boolean isMinor() {
			return isMinor;
		}
	}
	
	private static Pattern numericPitch = Pattern.compile("^[b\\-#]?([2-79]|1[13]?)"); 
	private static Pattern alphaPitch = Pattern.compile("^[A-G][b#]?"); 
	private static String[] symbs = new String[]{"Δ","+","°","ø","Ø","o"};
	private static Set<String> augDimSymbols = new HashSet<String>(Arrays.asList(symbs));

	String chordName;
	private int root;
	private ChordQuality quality;
	
	public Chord(String chord) {
		this.chordName = chord;
		this.root = -1;
		this.quality = new ChordQuality();
	}

	public Chord(int root, ChordQuality quality) {
		this.chordName = "";
		this.root = root;
		this.quality = quality;
	}

	public String getChordName() {
		return chordName;
	}

	public String toString()
	{
		return chordName + Pitch.getPitchName(root) + quality;
	}

	
	public static Chord parse(String input, boolean confidentSource) {
		if(input == null || input.length() == 0)
			return null;
		
		Matcher matcher = alphaPitch.matcher(input);
		if(!matcher.find()){
			return null;
		}

		int root = Pitch.getPitchValue(matcher.group());
		
		input = input.substring(matcher.end());
		
		ChordQuality quality = ChordQuality.parseChordQuality(input, confidentSource);
		
		if (quality == null){
			return null;
		}
		
		return new Chord(root, quality);
	}

	public int getRoot() {
		return root;
	}

	public boolean isMinor() {
		return this.quality.isMinor();
	}
}
