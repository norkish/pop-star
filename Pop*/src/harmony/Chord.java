package harmony;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pitch.Pitch;

public class Chord implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static class ChordQuality implements Serializable{
/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (isMinor ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ChordQuality))
				return false;
			ChordQuality other = (ChordQuality) obj;
			if (isMinor != other.isMinor)
				return false;
			return true;
		}
	}
	
	private static Pattern numericPitch = Pattern.compile("^[b\\-#]?([2-79]|1[13]?)"); 
	private static Pattern alphaPitch = Pattern.compile("^[A-G][b#]?"); 
	private static String[] symbs = new String[]{"Δ","+","°","ø","Ø","o"};
	private static Set<String> augDimSymbols = new HashSet<String>(Arrays.asList(symbs));

	private int root;
	private ChordQuality quality;

	public Chord(int root, ChordQuality quality) {
		this.root = root;
		this.quality = quality;
	}

	public String toString()
	{
		return Pitch.getPitchName(root) + quality;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((quality == null) ? 0 : quality.hashCode());
		result = prime * result + root;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Chord))
			return false;
		Chord other = (Chord) obj;
		if (quality == null) {
			if (other.quality != null)
				return false;
		} else if (!quality.equals(other.quality))
			return false;
		if (root != other.root)
			return false;
		return true;
	}
	
	
}
