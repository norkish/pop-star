package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import config.SongConfiguration;
import dbtb.data.CommandlineExecutor;
import tabcomplete.main.TabDriver;

public class Muse {

	Map<String,Double> inspiringEmpathVec;
	String inspiringEmotion = "Complex";
	
	public static final String[] ARRAY_OF_EMOTIONS = new String[]{"joy","surprise","anger","sadness","fear","disgust"};
	private static final Random RAND = new Random(SongConfiguration.randSeed);
	private static final String inspiringEmotionFile = "inspiring_empath.txt";
	private static final String matchListFile = "training_songs_sorted_by_relevance.txt";

	public Muse() {
		inspiringEmpathVec = defineRandomEmpathVector();
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
		
		inspiringEmotion = ARRAY_OF_EMOTIONS[RAND.nextInt(ARRAY_OF_EMOTIONS.length)];
		vector.put(inspiringEmotion, 1.0);
		return vector;
	}

	public File[] findInspiringFiles(int numberOfInspiringFiles) throws IOException {
		File[] files = new File[numberOfInspiringFiles];
		printInspiringEmpathVecToFile();
		retrieveBestMatchesForTraining(numberOfInspiringFiles);
		files = readBestMatches(numberOfInspiringFiles);
		//randomly choose a handful?
		
		return files;
	}

	private File[] readBestMatches(int numberOfInspiringFiles) throws IOException {
		File[] files = new File[numberOfInspiringFiles];
		
		BufferedReader bf = new BufferedReader(new FileReader(matchListFile));
		
		String line;
		int i = 0;
		while((line=bf.readLine())!=null) {
			files[i++] = new File(TabDriver.dataDir + "/Wikifonia_xmls/" + line.split("\\s+", 2)[1]);
		}
		return files;
	}

	private void retrieveBestMatchesForTraining(int count) {
		CommandlineExecutor.execute("python /Users/norkish/git/pop-star/Pop*/script/retrieveSongsWithClosestLyrics.py " + inspiringEmotionFile + " wikifonia_lyrics_empath.txt " + count, matchListFile);
	}

	private void printInspiringEmpathVecToFile() throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(inspiringEmotionFile);
		
		pw.write(inspiringEmotion);
		pw.write("\n{");
		boolean first = true;
		for (Entry<String, Double> entry : inspiringEmpathVec.entrySet()) {
			if (first)
				first = false;
			else 
				pw.write(", ");
			pw.write("\'" + entry.getKey() + "\': " + entry.getValue());
		}
		pw.write("}");
		pw.close();
	}

	public static void main(String[] args) throws IOException {
		Muse muse = new Muse();
		File[] files = muse.findInspiringFiles(10);
		System.out.println(Arrays.toString(files));
	}
}
