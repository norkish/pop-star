package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import dbtb.data.CommandlineExecutor;
import dbtb.utils.Pair;
import inspiration.Inspiration;
import inspiration.InspirationSource;
import main.AlnNHMMSongGeneratorNoNHMMVariableStructure.HarmonyToken;
import main.AlnNHMMSongGeneratorNoNHMMVariableStructure.PitchToken;
import main.AlnNHMMSongGeneratorNoNHMMVariableStructure.RhythmToken;
import tabcomplete.main.TabDriver;

public class Muse {

	private Map<String,Double> inspiringEmpathVec;
	private Tweet inspiringTweet = null;
	private String inspiringEmotion = "Complex";
	
	public static final String[] ARRAY_OF_EMOTIONS = new String[]{"joy","surprise","anger","sadness","fear","disgust"};
	private static final Random RAND = new Random();
	private static final String inspiringEmotionFile = "inspiring_empath.txt";
	private static final String personaFile = "abbrev_persona.txt";
	private static final String matchListFile = "training_songs_sorted_by_relevance.txt";
	private static final String matchingLyricsEmpathListFile = "training_lyrics_sorted_by_relevance.txt";

	boolean useExistingEmpaths = false;
	private List<Pair<String, Map<String, Double>>> all_tweet_empath_vecs;
	private int all_tweet_empath_vecs_idx = -1;
	
	public Muse() throws IOException, InterruptedException {
		all_tweet_empath_vecs = getEmpathVectorsFromTwitter();
	}

	public boolean setTweetAndVecToNext() {
		if (all_tweet_empath_vecs_idx == all_tweet_empath_vecs.size()-1) return false;
		Pair<String, Map<String, Double>> empath_vec = all_tweet_empath_vecs.get(++all_tweet_empath_vecs_idx);
		inspiringTweet = parseTweet(empath_vec.getFirst());
		inspiringEmpathVec = empath_vec.getSecond();
		setInspiringEmotion(summarizeEmpathVec(inspiringEmpathVec));
		return true;
	}
	
	private Tweet parseTweet(String tweetString) {
		Tweet newTweet = new Tweet();
		
		int beginIndex = tweetString.indexOf("SEARCH_KEYWORD: ") + "SEARCH_KEYWORD: ".length();
		int endIndex = tweetString.indexOf("USERNAME: ");
		newTweet.setSearchKeyword(tweetString.substring(beginIndex, endIndex).trim());
		
		beginIndex = endIndex + "USERNAME: ".length();
		endIndex = tweetString.indexOf("TIMESTAMP: ");
		newTweet.setUsername(tweetString.substring(beginIndex, endIndex).trim());

		beginIndex = endIndex + "TIMESTAMP: ".length();
		endIndex = tweetString.indexOf("TWEETBODY: ");
		newTweet.setDate(tweetString.substring(beginIndex, endIndex).trim());
		
		beginIndex = endIndex + "TWEETBODY: ".length();
		endIndex = tweetString.indexOf("MATCH_SCORE: ");
		newTweet.setTweettext(tweetString.substring(beginIndex, endIndex).trim());
		
		return newTweet;
	}

	private static final int NUM_TWEETS_TO_GATHER = 30; 
	private List<Pair<String, Map<String, Double>>> getEmpathVectorsFromTwitter() throws IOException, InterruptedException {
		if (!useExistingEmpaths) CommandlineExecutor.execute("python script/computeSentimentVectorFromTweepyPersona.py " + personaFile  + " " + NUM_TWEETS_TO_GATHER, inspiringEmotionFile);
		Thread.sleep(1000);
		return readInEmpathVecFromFile(inspiringEmotionFile);
	}

	private List<Pair<String, Map<String, Double>>> readInEmpathVecFromFile(String filename) throws FileNotFoundException, IOException {
		BufferedReader bf = new BufferedReader(new FileReader(filename));
		
		String metadata;
		List<Pair<String, Map<String, Double>>> empathVecs = new ArrayList<Pair<String, Map<String, Double>>>();
		while ((metadata = bf.readLine()) != null) {
			String empath_vec = bf.readLine();
		
			Scanner sc = new Scanner(empath_vec);
			String vec_item;
			Map<String, Double> vector = new LinkedHashMap<String, Double>();
			while((vec_item = sc.findInLine("'.*?': ")) != null) {
				vector.put(vec_item.substring(1, vec_item.length()-3), Double.parseDouble(sc.findInLine("[0-9]+\\.[0-9]+")));
			}
			sc.close();
			empathVecs.add(new Pair<String,Map<String, Double>>(metadata,vector));
		}
		bf.close();
		
		return empathVecs;
	}

	private Map<String, Double> defineRandomEmpathVector() {
		Map<String, Double> vector = new LinkedHashMap<String, Double>(){{
			put("help", 0.0);
			put("office", 0.0);
			put("violence", 0.0);
			put("dance", 0.0);
			put("money", 0.0);
			put("wedding", 0.0);
			put("valuable", 0.0);
			put("domestic_work", 0.0);
			put("sleep", 0.0);
			put("medical_emergency", 0.0);
			put("cold", 0.0);
			put("hate", 0.0);
			put("cheerfulness", 0.0);
			put("aggression", 0.0);
			put("occupation", 0.0);
			put("envy", 0.0);
			put("anticipation", 0.0);
			put("family", 0.0);
			put("crime", 0.0);
			put("attractive", 0.0);
			put("masculine", 0.0);
			put("prison", 0.0);
			put("health", 0.0);
			put("pride", 0.0);
			put("dispute", 0.0);
			put("nervousness", 0.0);
			put("government", 0.0);
			put("weakness", 0.0);
			put("horror", 0.0);
			put("swearing_terms", 0.0);
			put("leisure", 0.0);
			put("suffering", 0.0);
			put("royalty", 0.0);
			put("wealthy", 0.0);
			put("white_collar_job", 0.0);
			put("tourism", 0.0);
			put("furniture", 0.0);
			put("school", 0.0);
			put("magic", 0.0);
			put("beach", 0.0);
			put("journalism", 0.0);
			put("morning", 0.0);
			put("banking", 0.0);
			put("social_media", 0.0);
			put("exercise", 0.0);
			put("night", 0.0);
			put("kill", 0.0);
			put("art", 0.0);
			put("play", 0.0);
			put("computer", 0.0);
			put("college", 0.0);
			put("traveling", 0.0);
			put("stealing", 0.0);
			put("real_estate", 0.0);
			put("home", 0.0);
			put("divine", 0.0);
			put("sexual", 0.0);
			put("fear", 0.0);
			put("monster", 0.0);
			put("irritability", 0.0);
			put("superhero", 0.0);
			put("business", 0.0);
			put("driving", 0.0);
			put("pet", 0.0);
			put("childish", 0.0);
			put("cooking", 0.0);
			put("exasperation", 0.0);
			put("religion", 0.0);
			put("hipster", 0.0);
			put("internet", 0.0);
			put("surprise", 0.0);
			put("reading", 0.0);
			put("worship", 0.0);
			put("leader", 0.0);
			put("independence", 0.0);
			put("movement", 0.0);
			put("body", 0.0);
			put("noise", 0.0);
			put("eating", 0.0);
			put("medieval", 0.0);
			put("zest", 0.0);
			put("confusion", 0.0);
			put("water", 0.0);
			put("sports", 0.0);
			put("death", 0.0);
			put("healing", 0.0);
			put("legend", 0.0);
			put("heroic", 0.0);
			put("celebration", 0.0);
			put("restaurant", 0.0);
			put("ridicule", 0.0);
			put("programming", 0.0);
			put("dominant_heirarchical", 0.0);
			put("military", 0.0);
			put("neglect", 0.0);
			put("swimming", 0.0);
			put("exotic", 0.0);
			put("love", 0.0);
			put("hiking", 0.0);
			put("communication", 0.0);
			put("hearing", 0.0);
			put("order", 0.0);
			put("sympathy", 0.0);
			put("hygiene", 0.0);
			put("weather", 0.0);
			put("anonymity", 0.0);
			put("trust", 0.0);
			put("ancient", 0.0);
			put("deception", 0.0);
			put("fabric", 0.0);
			put("air_travel", 0.0);
			put("fight", 0.0);
			put("dominant_personality", 0.0);
			put("music", 0.0);
			put("vehicle", 0.0);
			put("politeness", 0.0);
			put("toy", 0.0);
			put("farming", 0.0);
			put("meeting", 0.0);
			put("war", 0.0);
			put("speaking", 0.0);
			put("listen", 0.0);
			put("urban", 0.0);
			put("shopping", 0.0);
			put("disgust", 0.0);
			put("fire", 0.0);
			put("tool", 0.0);
			put("phone", 0.0);
			put("gain", 0.0);
			put("sound", 0.0);
			put("injury", 0.0);
			put("sailing", 0.0);
			put("rage", 0.0);
			put("science", 0.0);
			put("work", 0.0);
			put("appearance", 0.0);
			put("optimism", 0.0);
			put("warmth", 0.0);
			put("youth", 0.0);
			put("sadness", 0.0);
			put("fun", 0.0);
			put("emotional", 0.0);
			put("joy", 0.0);
			put("affection", 0.0);
			put("fashion", 0.0);
			put("lust", 0.0);
			put("shame", 0.0);
			put("torment", 0.0);
			put("economics", 0.0);
			put("anger", 0.0);
			put("politics", 0.0);
			put("ship", 0.0);
			put("clothing", 0.0);
			put("car", 0.0);
			put("strength", 0.0);
			put("technology", 0.0);
			put("breaking", 0.0);
			put("shape_and_size", 0.0);
			put("power", 0.0);
			put("vacation", 0.0);
			put("animal", 0.0);
			put("ugliness", 0.0);
			put("party", 0.0);
			put("terrorism", 0.0);
			put("smell", 0.0);
			put("blue_collar_job", 0.0);
			put("poor", 0.0);
			put("plant", 0.0);
			put("pain", 0.0);
			put("beauty", 0.0);
			put("timidity", 0.0);
			put("philosophy", 0.0);
			put("negotiate", 0.0);
			put("negative_emotion", 0.0);
			put("cleaning", 0.0);
			put("messaging", 0.0);
			put("competing", 0.0);
			put("law", 0.0);
			put("friends", 0.0);
			put("payment", 0.0);
			put("achievement", 0.0);
			put("alcohol", 0.0);
			put("disappointment", 0.0);
			put("liquid", 0.0);
			put("feminine", 0.0);
			put("weapon", 0.0);
			put("children", 0.0);
			put("ocean", 0.0);
			put("giving", 0.0);
			put("contentment", 0.0);
			put("writing", 0.0);
			put("rural", 0.0);
			put("positive_emotion", 0.0);
			put("musical", 0.0);
		}};
		
		setInspiringEmotion(ARRAY_OF_EMOTIONS[RAND.nextInt(ARRAY_OF_EMOTIONS.length)]);
		vector.put(getInspiringEmotion(), 1.0);
		return vector;
	}

	public File[] findInspiringWikifoniaFiles(int numberOfInspiringFiles) throws IOException, InterruptedException {
		File[] files = readBestMatches(numberOfInspiringFiles);
		
		return files;
	}

	private File[] readBestMatches(int numberOfInspiringFiles) throws IOException {
		File[] files = new File[numberOfInspiringFiles];
		
		BufferedReader bf = new BufferedReader(new FileReader(matchListFile));
		
		String line;
		int i = 0;
		while((line=bf.readLine())!=null && i < numberOfInspiringFiles) {
			files[i++] = new File(TabDriver.dataDir + "/Wikifonia_xmls/" + line.split("\\s+", 2)[1]);
		}
		
		return files;
	}

	public void retreiveWikifoniaFiles(int count) {
		if (!useExistingEmpaths) {
			System.out.println("Finding inspiring songs from the songs database");
			CommandlineExecutor.execute("python script/retrieveSongsWithClosestLyrics.py " + inspiringEmotionFile + " wikifonia_lyrics_empath.txt " + count + " " + all_tweet_empath_vecs_idx, matchListFile);
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Muse muse = new Muse();
		File[] files = muse.findInspiringWikifoniaFiles(10);
		System.out.println(Arrays.toString(files));
	}

	public Inspiration getInspiration() {
		Inspiration inspiration = new Inspiration(InspirationSource.RANDOM);
		inspiration.analyzeInputForEmotion(getInspiringEmotion());
		return inspiration;
	}

	Random rand = new Random();
	
	public String composeDescription(Pair<String, Map<String, Double>> empathVecForGenSong, String lyricString) throws InterruptedException, IOException {
		StringBuilder str = new StringBuilder();
		
		int i = rand.nextInt(4);
		if(i==0)
			str.append("For those that know me, I pay attention to anything related to ");
		else if (i==1)
			str.append("I find that I'm commonly drawn towards posts about ");
		else if (i==2)
			str.append("I spend a lot of time thinking and reading about ");
		else
			str.append("One common interest that I have is ");
		str.append(inspiringTweet.searchKeyword.replaceAll("\"", ""));
		
		if (rand.nextBoolean()) {
			str.append(", and I ");
			i = rand.nextInt(7);
			if (i==0)
				str.append("stumbled on");
			else if (i==1)
				str.append("found");
			else if (i==2)
				str.append("read");
			else if (i==3)
				str.append("discovered");
			else if (i==4)
				str.append("saw");
			else if (i==5)
				str.append("noticed");
			else
				str.append("came across");
				
			str.append(" this tweet");
		} else {
			str.append(", so naturally this tweet");
			i = rand.nextInt(4);
			if (i==0)
				str.append(" caught my eye");
			else if (i==1)
				str.append(" jumped out");
			else if (i==2)
				str.append(" stood out");
			else
				str.append(" got my attention");
		}
		
		str.append(" from my friend " + inspiringTweet.username + " posted " + inspiringTweet.date + ":\n\n\"" + inspiringTweet.tweettext + "\"\n\n");
		
		final String empathSummary = summarizeEmpathVec(inspiringEmpathVec).replaceAll("_", " ");
		
		i = rand.nextInt(3);
		if (i == 0)
			str.append("It got me thinking about ");
		else if (i==1)
			str.append("Thoughts started coming about ");
		else
			str.append("It made me reflect on ");

		
		str.append(empathSummary);
		if (rand.nextBoolean())
			str.append(" themes");
		else
			str.append(" ideas");
		
		i = rand.nextInt(5);
		if (i == 0)
			str.append(" and ultimately led me to compose this piece:");
		else if (i==1)
			str.append(", and piece by piece out came this song:");
		else if (i==2)
			str.append(". Then the words started coming:");
		else if (i==3)
			str.append(". That's how this piece sort of came together:");
		else
			str.append(" and I couldn't help but write this song:");
		
		str.append("\n\n\"" + StringUtils.capitalize(lyricString) + "\"\n\n");
		
		i = rand.nextInt(4);
		if (i == 0)
			str.append("I intended that the piece should be about ");
		else if (i==1)
			str.append("I'd been thinking ");
		else if (i==2)
			str.append("It started out as just thoughts on ");
		else if (i==3)
			str.append("At the beginning it was ");
		
		str.append(empathSummary + ", " + compareEmpathVectors(empathVecForGenSong.getSecond(), inspiringEmpathVec));
				
		i = rand.nextInt(4);
		if (i == 0)
			str.append(" In any case, I hope you find it meaningful.");
		else if (i==1)
			str.append(" Hopefully it resonates with you.");
		else if (i==2)
			str.append(" Hopefully you'll appreciate it.");
		else if (i==3)
			str.append(" I hope that you like it.");
		
		return str.toString();
	}

	private static final int NUM_TOP = 2;
	private String summarizeEmpathVec(Map<String, Double> inspiringEmpathVec2) {
		StringBuilder str = new StringBuilder();

		String[] topN = findTopN(inspiringEmpathVec2,NUM_TOP);
		
		for (int i = 0; i < topN.length; i++) {
			if (i > 0) {
				if (i == topN.length-1) {
					if (i == 1)
						str.append(" and ");
					else
						str.append(", and ");
				} else {
					str.append(", ");
				}
			}
			str.append(topN[i]);
		}
		
		return str.toString();
	}

	private String[] findTopN(Map<String, Double> inspiringEmpathVec2, int n) {
		List<String> topN = new LinkedList<String>();
		List<Double> topNScores = new LinkedList<Double>();
		
		for (Entry<String, Double> empathEntry : inspiringEmpathVec2.entrySet()) {
			for (int i = 0; i < n; i++) {
				if (topNScores.size() <= i || empathEntry.getValue() > topNScores.get(i) || (empathEntry.getValue() == topNScores.get(i) && prefRanks.get(empathEntry.getKey()) < prefRanks.get(topN.get(i)))) {
					topNScores.add(i, empathEntry.getValue());
					topN.add(i,empathEntry.getKey());
					break;
				}
			}
		}
		
		return topN.subList(0, n).toArray(new String[0]);
	}

	private String compareEmpathVectors(Map<String, Double> generatedEmpathVec, Map<String, Double> inspiringEmpathVec) {
		StringBuilder str = new StringBuilder(); 
		boolean same = (new HashSet<String>(Arrays.asList(findTopN(generatedEmpathVec,NUM_TOP)))).equals(new HashSet<String>(Arrays.asList(findTopN(inspiringEmpathVec,NUM_TOP)))); 
		
		if (same) {
			int i = rand.nextInt(4);
			if (i == 0)
				str.append("and I think it really stayed true to that.");
			else if (i==1)
				str.append("and that's what I feel is conveyed in the piece.");
			else if (i==2)
				str.append("and I think you get that sense from listening to the song.");
			else if (i==3)
				str.append("and I think that's what's conveyed through the music.");
		} else {
			int i = rand.nextInt(4);
			if (i == 0)
				str.append("but in the end I felt that it had more of a ");
			else if (i==1)
				str.append("but in the end it came closer to a ");
			else if (i==2)
				str.append("but it turned out to have more of a ");
			else if (i==3)
				str.append("however it really wound up with more of a ");
			str.append(summarizeEmpathVec(generatedEmpathVec).replaceAll("_", " "));
			str.append(" theme.");
		}
		return str.toString();
	}

	private final static String generatedSongLyricsFile = "generatedSongLyrics.txt";
	private final static String generatedSongLyricsEmpathFile = "generatedSongLyricsEmpath.txt";
	public Pair<String, Map<String, Double>> getEmpathVector(String generatedSongLyrics) throws InterruptedException, IOException {
		PrintWriter pw = new PrintWriter(generatedSongLyricsFile);
		pw.println("Generated Song Lyrics");
		pw.println(generatedSongLyrics.replace("\n", " "));
		pw.close();
		if (!useExistingEmpaths) CommandlineExecutor.execute("python script/computeSentimentVectorFromLyrics.py " + generatedSongLyricsFile, generatedSongLyricsEmpathFile);
		Thread.sleep(1000);
		return readInEmpathVecFromFile(generatedSongLyricsEmpathFile).get(0);
	}

	public String getEmpathSummary() {
		return summarizeEmpathVec(inspiringEmpathVec);
	}

	public String getInspiringEmotion() {
		if (inspiringTweet != null)
			return inspiringEmotion + " - " + inspiringTweet.toString();
		else
			return inspiringEmotion;
	}

	public void setInspiringEmotion(String inspiringEmotion) {
		this.inspiringEmotion = inspiringEmotion;
	}

	
	public String[][] findInspiringLyricDBMatches(int inspiringFileCountLyricsDb) throws IOException {
//		printInspiringEmpathVecToFile();
		String[][] matches = parseMatchingLyricsFromLyricsDB(inspiringFileCountLyricsDb);
		
		return matches;
	}

	private String[][] parseMatchingLyricsFromLyricsDB(int inspiringFileCountLyricsDb) throws IOException {
		String[][] matches = new String[inspiringFileCountLyricsDb][];
		
		BufferedReader bf = new BufferedReader(new FileReader(matchingLyricsEmpathListFile));
		
		String line;
		int i = 0;
		while((line=bf.readLine())!=null && i < inspiringFileCountLyricsDb) {
			String lyrics = parseLyricLine(line)[3];
			lyrics = lyrics.substring(1, lyrics.length()-1).trim();
			lyrics = lyrics.replaceAll(" *\\\\n *","\n");
			lyrics = lyrics.replaceAll(" *\\\\r *","\n");
			lyrics = lyrics.replaceAll("\\\\n\\\\n+","\n");
			lyrics = lyrics.replaceAll("\\\\","");
			matches[i++] = lyrics.split("\n");
		}
		
		assert i == inspiringFileCountLyricsDb: "Didn't find as many in the python-generated file as expected.";
		System.out.println("TRAINING LYRICS ON " + i + " (of requested " + inspiringFileCountLyricsDb + ") LYRIC FILES ");
		
		bf.close();
		
		return matches;
	}

	private String[] parseLyricLine(String line) {
		return line.split(" \\[\\[:DELIMITER:\\]\\] ");
	}
	
	private final static Map<String, Double> prefRanks = new HashMap<String, Double>(){{
		put("help", 0.0);
		put("office", 0.0);
		put("violence", 0.0);
		put("dance", 0.0);
		put("money", 0.0);
		put("wedding", 0.0);
		put("valuable", 0.0);
		put("domestic_work", 0.0);
		put("sleep", 0.0);
		put("medical_emergency", 0.0);
		put("cold", 0.5);
		put("hate", 0.5);
		put("cheerfulness", 0.7);
		put("aggression", 0.3);
		put("occupation", 0.0);
		put("envy", 0.5);
		put("anticipation", 0.7);
		put("family", 0.0);
		put("crime", 0.0);
		put("attractive", 0.0);
		put("masculine", 0.0);
		put("prison", 0.0);
		put("health", 0.0);
		put("pride", 0.3);
		put("dispute", 0.0);
		put("nervousness", 0.5);
		put("government", 0.0);
		put("weakness", 0.3);
		put("horror", 0.0);
		put("swearing_terms", 0.0);
		put("leisure", 0.0);
		put("suffering", 0.5);
		put("royalty", 0.0);
		put("wealthy", 0.0);
		put("white_collar_job", 0.0);
		put("tourism", 0.0);
		put("furniture", 0.0);
		put("school", 0.0);
		put("magic", 0.0);
		put("beach", 0.0);
		put("journalism", 0.0);
		put("morning", 0.0);
		put("banking", 0.0);
		put("social_media", 0.3);
		put("exercise", 0.0);
		put("night", 0.5);
		put("kill", 0.0);
		put("art", 0.0);
		put("play", 0.0);
		put("computer", 0.0);
		put("college", 0.0);
		put("traveling", 0.0);
		put("stealing", 0.0);
		put("real_estate", 0.0);
		put("home", 0.0);
		put("divine", 0.0);
		put("sexual", 0.0);
		put("fear", 0.5);
		put("monster", 0.0);
		put("irritability", 0.0);
		put("superhero", 0.0);
		put("business", 0.0);
		put("driving", 0.0);
		put("pet", 0.0);
		put("childish", 0.0);
		put("cooking", 0.0);
		put("exasperation", 0.0);
		put("religion", 0.0);
		put("hipster", 0.0);
		put("internet", 0.0);
		put("surprise", 0.6);
		put("reading", 0.0);
		put("worship", 0.0);
		put("leader", 0.0);
		put("independence", 0.0);
		put("movement", 0.0);
		put("body", 0.0);
		put("noise", 0.0);
		put("eating", 0.0);
		put("medieval", 0.0);
		put("zest", 0.0);
		put("confusion", 0.0);
		put("water", 0.0);
		put("sports", 0.0);
		put("death", 0.3);
		put("healing", 0.6);
		put("legend", 0.0);
		put("heroic", 0.0);
		put("celebration", 0.6);
		put("restaurant", 0.0);
		put("ridicule", 0.0);
		put("programming", 0.0);
		put("dominant_heirarchical", 0.0);
		put("military", 0.0);
		put("neglect", 0.3);
		put("swimming", 0.0);
		put("exotic", 0.3);
		put("love", 1.0);
		put("hiking", 0.0);
		put("communication", 0.0);
		put("hearing", 0.0);
		put("order", 0.0);
		put("sympathy", 0.3);
		put("hygiene", 0.0);
		put("weather", 0.0);
		put("anonymity", 0.0);
		put("trust", 0.3);
		put("ancient", 0.0);
		put("deception", 0.5);
		put("fabric", 0.0);
		put("air_travel", 0.0);
		put("fight", 0.0);
		put("dominant_personality", 0.0);
		put("music", 0.3);
		put("vehicle", 0.0);
		put("politeness", 0.0);
		put("toy", 0.0);
		put("farming", 0.0);
		put("meeting", 0.0);
		put("war", 0.5);
		put("speaking", 0.0);
		put("listen", 0.0);
		put("urban", 0.0);
		put("shopping", 0.0);
		put("disgust", 0.5);
		put("fire", 0.0);
		put("tool", 0.0);
		put("phone", 0.0);
		put("gain", 0.0);
		put("sound", 0.0);
		put("injury", 0.0);
		put("sailing", 0.0);
		put("rage", 0.3);
		put("science", 0.0);
		put("work", 0.0);
		put("appearance", 0.0);
		put("optimism", 0.5);
		put("warmth", 0.5);
		put("youth", 0.0);
		put("sadness", 0.5);
		put("fun", 0.3);
		put("emotional", 0.0);
		put("joy", 0.7);
		put("affection", 0.6);
		put("fashion", 0.0);
		put("lust", 0.5);
		put("shame", 0.5);
		put("torment", 0.0);
		put("economics", 0.0);
		put("anger", 0.5);
		put("politics", 0.0);
		put("ship", 0.0);
		put("clothing", 0.0);
		put("car", 0.0);
		put("strength", 0.0);
		put("technology", 0.0);
		put("breaking", 0.0);
		put("shape_and_size", 0.0);
		put("power", 0.3);
		put("vacation", 0.0);
		put("animal", 0.0);
		put("ugliness", 0.0);
		put("party", 0.0);
		put("terrorism", 0.0);
		put("smell", 0.0);
		put("blue_collar_job", 0.0);
		put("poor", 0.0);
		put("plant", 0.0);
		put("pain", 0.6);
		put("beauty", 0.6);
		put("timidity", 0.3);
		put("philosophy", 0.3);
		put("negotiate", 0.0);
		put("negative_emotion", 0.0);
		put("cleaning", 0.0);
		put("messaging", 0.0);
		put("competing", 0.0);
		put("law", 0.0);
		put("friends", 0.6);
		put("payment", 0.0);
		put("achievement", 0.0);
		put("alcohol", 0.0);
		put("disappointment", 0.5);
		put("liquid", 0.0);
		put("feminine", 0.0);
		put("weapon", 0.0);
		put("children", 0.0);
		put("ocean", 0.0);
		put("giving", 0.0);
		put("contentment", 0.0);
		put("writing", 0.0);
		put("rural", 0.0);
		put("positive_emotion", 0.0);
		put("musical", 0.0);
	}};

	public Tweet getTweet() {
		return inspiringTweet;
	}

	private static double empathVecWeightInRating = 0.5;
	private static double lyricDiversityWeightInRating = 0.5;
	private static double harmonyDiversityWeightInRating = 0.5;
	private static double pitchDiversityWeightInRating = 0.5;
	private static double rhythmDiversityWeightInRating = 0.5;
	private static double lengthWeightInRating = 0.1;
	
	public double getRating(Pair<String, Map<String, Double>> empathVecForGenSong, String lyricString, List<HarmonyToken> harmonyGenerate, List<PitchToken> pitchGenerate, List<RhythmToken> rhythmGenerate) throws InterruptedException, IOException {

		double empathVecDifferenceScore = 0.0;
		for (String key : inspiringEmpathVec.keySet()) {
			empathVecDifferenceScore += Math.abs(inspiringEmpathVec.get(key) - empathVecForGenSong.getSecond().get(key));
		}
		empathVecDifferenceScore /= inspiringEmpathVec.size(); // normalize to be between 0 and 1
		
		Set<String> uniqWords = new HashSet<String>();
		final String[] words = lyricString.split("\\s+");
		for (String word : words) {
			uniqWords.add(word.replaceAll("[^\\w]", ""));
		}
		
		double lyricDiversityScore = uniqWords.size() * 1.0 / words.length;
		
		double harmonyDiversity = 0.0;
		double pitchDiversity = 0.0;
		double rhythmDiversity = 0.0;
		
		return empathVecWeightInRating * empathVecDifferenceScore + lyricDiversityWeightInRating * lyricDiversityScore + lengthWeightInRating * words.length + harmonyDiversityWeightInRating * harmonyDiversity + pitchDiversityWeightInRating * pitchDiversity + rhythmDiversityWeightInRating * rhythmDiversity;
	}

	public void retreiveClosestLyrics(int inspiringFileCountLyricsDb) {
		if (!useExistingEmpaths) {
			System.out.println("Finding inspiring lyrics from the lyric database");
			CommandlineExecutor.execute("python script/retrieveLyricsWithClosestLyrics.py " + inspiringEmotionFile + " /Users/norkish/Archive/2017_BYU/ComputationalCreativity/data/data/lyrics_db_empaths_deeper_dedup.txt " + inspiringFileCountLyricsDb + " " + all_tweet_empath_vecs_idx, matchingLyricsEmpathListFile);
		}
	}
}
