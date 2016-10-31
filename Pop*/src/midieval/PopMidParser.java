package midieval;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import pitch.Pitch;
import utils.Pair;

public class PopMidParser {

	public static void parse_time(Map<String,Object> fields, String fname){
		String[] timeSplit = ((String) fields.get(fname)).split(";");
		List<Pair<Pair<Integer, Integer>, Integer>> new_time = new ArrayList<Pair<Pair<Integer,Integer>,Integer>>();
		//removes final semicolon
		for (String time_sig : timeSplit){
			time_sig = time_sig.trim();
			if (time_sig.length() == 0)
				continue;
			String[] time_sig_tick = time_sig.split(":");
			//if valid time signature
			if (time_sig_tick[0].trim().length() > 0) {
				//create tuple for numerator/denominator
				String[] timeSigSplit = time_sig_tick[0].trim().split("/");
				Pair<Integer,Integer> newTime_sig = null;
				if (timeSigSplit.length > 1) {
					newTime_sig = new Pair<Integer,Integer>(timeSigSplit[0].equals("?") ? null:Integer.parseInt(timeSigSplit[0]), timeSigSplit[1].equals("?") ? null:Integer.parseInt(timeSigSplit[1]));
				}
				//add time signature if it"s given, otherwise put null
				if (time_sig_tick.length == 1 || time_sig_tick[1].trim().equals("?") || time_sig_tick[1].trim().length() == 0) {
					new_time.add(new Pair<Pair<Integer,Integer>,Integer>(newTime_sig,null));
				} else {
					new_time.add(new Pair<Pair<Integer,Integer>, Integer>(newTime_sig,Integer.parseInt(time_sig_tick[1])));
				}
			}
		}
		
		new_time.get(0).setSecond(0); // set start tick of first time sig to 0
		List<Pair<Pair<Integer, Integer>, Integer>> finalTimeSigs = new ArrayList<Pair<Pair<Integer,Integer>,Integer>>();
		finalTimeSigs.add(new_time.get(0));
		fields.put(fname, finalTimeSigs);
		//check that there"s no looping through time signatures (e.g., time signature reiterated in each track)
		Pair<Pair<Integer, Integer>, Integer> t;
		for (int i = 1; i < new_time.size(); i++) {
			t = new_time.get(i);
			if (t.getSecond() == null || finalTimeSigs.get(finalTimeSigs.size()-1).getSecond() == null || t.getSecond() > finalTimeSigs.get(finalTimeSigs.size()-1).getSecond()) {
				finalTimeSigs.add(t);
			}
		}
	}
	
	public static void parse_key(Map<String,Object> fields, String fname){
		String[] keySplit = ((String) fields.get(fname)).split(";");
		List<Pair<Integer,Integer>> new_key = new ArrayList<Pair<Integer, Integer>>();
		//removes final semicolon
		for (String key_sig : keySplit) {
			key_sig = key_sig.trim();
			if (key_sig.length() == 0)
				continue;
			String[] key_sig_tick = key_sig.split(":");
			//if valid key signature
			if (key_sig_tick[0].trim().length() > 0) {
				//create tuple for numerator/denominator
				key_sig = key_sig_tick[0].trim();
				//0 is C, 12 is Am
				int key_sig_int = -1;
				if (!key_sig.equals("?")) {
					if (key_sig.endsWith("m")){
						key_sig_int = Pitch.getPitchValue(key_sig.substring(0,key_sig.length()-1)) + 12;
					} else {
						key_sig_int = Pitch.getPitchValue(key_sig);
					}
				}
				//add key signature if it"s given, otherwise put null
				if (key_sig_tick.length == 1 || key_sig_tick[1].trim().equals("?") || key_sig_tick[1].trim().length() == 0) {
					new_key.add(new Pair<Integer,Integer>(key_sig_int,null));
				} else {
					new_key.add(new Pair<Integer, Integer>(key_sig_int,(int) Double.parseDouble(key_sig_tick[1])));
				}
			}
		}
		
		new_key.get(0).setSecond(0);
		List<Pair<Integer,Integer>> finalKeySigs = new ArrayList<Pair<Integer,Integer>>();
		finalKeySigs.add(new_key.get(0));
		fields.put(fname, finalKeySigs);
		Pair<Integer,Integer> t;
		//check that there"s no looping through key signatures (e.g., key signature reiterated in each track)
		for (int i = 1; i < new_key.size(); i++) {
			t = new_key.get(i);
			if (t.getSecond() == null || finalKeySigs.get(finalKeySigs.size()-1).getSecond() == null || t.getSecond() > finalKeySigs.get(finalKeySigs.size()-1).getSecond()) {
				finalKeySigs.add(t);
			}
		}
	}

	public static Map<String, Map<String, Object>> load_popmid(File annots_file) throws FileNotFoundException{
		String[] annot_fields = null;
		Map<String,Map<String,Object>> annots = new HashMap<String,Map<String,Object>>();
		Scanner scan = new Scanner(annots_file);
		String line;
		Map<String,Object> fields;
		
		while (scan.hasNextLine()){
			line = scan.nextLine();
			String[] lineSplit = line.trim().split("\t");
			if (annot_fields == null) {// set column headers
				annot_fields = lineSplit;
			} else {
				fields = new HashMap<String, Object>();
				for (int i = 0; i < annot_fields.length; i++) {
					if (i < lineSplit.length) {
						fields.put(annot_fields[i], lineSplit[i].trim());
					} else {
						fields.put(annot_fields[i], "False");
					}
				}

				//title to lower case
				for (String fname : new String[]{"title","artist","blues_rock"}){
					fields.put(fname,((String)fields.get(fname)).trim().toLowerCase());
				}

				//artist to lower case and remove "the"
				if (((String)fields.get("artist")).startsWith("the ")) {
					fields.put("artist", ((String)fields.get("artist")).substring(4));
				}

				for (String fname : new String[]{"lyr_track","mel_track","comb_idx","naive_mel_track","muse_mel_track","muse_comb_idx"}){
					fields.put(fname, Integer.parseInt((String)fields.get(fname)));
					if (((int) fields.get(fname)) < 0){
						fields.put(fname, null);
					}
				}

				for (String fname : new String[]{"comb_idx","muse_comb_idx"}){
					if (fields.get(fname) != null) {
						fields.put(fname,((int)fields.get(fname)) - 1);
					}
				}

				String[] lyr = ((String) fields.get("first_lyr")).split(":");
				fields.put("first_lyr", new Pair<String,Integer>(lyr[0],Integer.parseInt(lyr[1])));

				lyr = ((String) fields.get("last_lyr")).split(":");
				fields.put("last_lyr", new Pair<String,Integer>(lyr[0],Integer.parseInt(lyr[1])));

				for (String fname : new String[]{"time","muse_time"}){
					parse_time(fields,fname);
				}

				for (String fname : new String[]{"time_reliable","extra_notes_intro","extra_notes_middle_outro","clean_rhythm_mel","clean_rhythm_acc","clean_pitch_mel","clean_pitch_acc","swing","mel_chords","fade_end","definitive_end"}){
					fields.put(fname, Boolean.parseBoolean((String) fields.get(fname)));
				}	

				for (String fname : new String[]{"key","muse_key"}) {
					parse_key(fields, fname);
				}

				fields.put("pickup", Double.parseDouble((String) fields.get("pickup")));

				fields.put("notes",((String) fields.get("notes")).trim());

//				fields.put("mel_split",((String) fields.get("mel_split")).split(";"));
//				fields["mel_split"] = [int(x) for x in fields["mel_split"] if len(x.strip()) > 0]
//				fields["mel_split"] = [x if x >= 0 else null for x in fields["mel_split"]]

				annots.put((String) fields.get("midi"), fields);
			}
		}
		scan.close();
		return annots;
	}

	
	public static Map<String, Map<String, Object>> select(Map<String, Map<String, Object>> annots, String fname, String regex, boolean inverse_selection) {
		Map<String, Map<String, Object>> new_annots = new HashMap<String, Map<String, Object>>();
		for (Entry<String, Map<String, Object>> entry : annots.entrySet()) {
			String filename = entry.getKey();
			Map<String, Object> item = entry.getValue();
			String obj1 = item.get(fname) == null ? "null" : item.get(fname).toString();
			if ((obj1.matches(regex)) != inverse_selection) {
				new_annots.put(filename, item);
			}
		}
		return new_annots;
	}

/*	public static Map<String, Map<String, Object>> select_len(Map<String, Map<String, Object>> annots, String fname, Object fvalue, boolean inverse_selection) {
		Map<String, Map<String, Object>> new_annots = new HashMap<String, Map<String, Object>>();
		for filename,item in annots.iteritems():
			if (len(item[fname]) == flen) != inverse_selection:
				new_annots[filename] = item
		return new_annots
	}

	public static Map<String, Map<String, Object>> select_single_key(Map<String, Map<String, Object>> annots, String fname, Object fvalue, boolean inverse_selection) {
		Map<String, Map<String, Object>> new_annots = new HashMap<String, Map<String, Object>>();		
		for filename,item in annots.iteritems():
			song_key = null
			add = True
			for key in item["key"]:
				if song_key == null:
					song_key = (key[0]%12 if key[0] != "?" else null)
				elif song_key != (key[0]%12 if key[0] != "?" else null):
					add = False
					break
			if add:
				new_annots[filename] = item

		return new_annots;
	}
 */

	public static void main(String[] args) throws FileNotFoundException {
		Map<String, Map<String, Object>> annots = load_popmid(new File("/Users/norkish/Archive/2016_BYU/dlrgroup/software/metadata_for_midis.csv"));
		
		Map<String,Pair<String,Boolean>> selectCriteria = new LinkedHashMap<String,Pair<String,Boolean>>();
		selectCriteria.put("mel_track",new Pair<String,Boolean>("^null$",true));
		selectCriteria.put("time",new Pair<String,Boolean>("\\[\\[\\[[34], 4\\], 0\\]\\]",false));
		selectCriteria.put("time_reliable",new Pair<String,Boolean>("^true$",false));
		selectCriteria.put("notes",new Pair<String,Boolean>("^$",false));
		selectCriteria.put("swing",new Pair<String,Boolean>("^false$",false));
		selectCriteria.put("clean_rhythm_mel",new Pair<String,Boolean>("^true$",false));
		selectCriteria.put("clean_pitch_mel",new Pair<String,Boolean>("^true$",false));
		selectCriteria.put("key",new Pair<String,Boolean>("^\\[\\[[0-9, ]+\\]\\]$",false));
//		selectCriteria.put("comb_idx",new Pair<String,Boolean>("^null$",false));
//		selectCriteria.put("extra_notes_middle_outro",new Pair<String,Boolean>("^false$",false));
//		selectCriteria.put("mel_chords",new Pair<String,Boolean>("^false$",false));
		
		System.out.println(annots.size() + " entries loaded from file");
		for (Entry<String, Pair<String, Boolean>> entry : selectCriteria.entrySet()) {
			String fname = entry.getKey();
			String fvalue = entry.getValue().getFirst();
			boolean inverse = entry.getValue().getSecond();
			annots = select(annots, fname, fvalue, inverse);
			System.out.println(annots.size() + " entries remain after effecting filter " + entry);
		}
		
		for (String string : annots.keySet()) {
			System.out.println(string + ":");
			for (String string2: annots.get(string).keySet()) {
				System.out.println("\t" + string2 + ":\t" + (annots.get(string).get(string2)== null?null:annots.get(string).get(string2).toString()));
			}
			break;
		}
	}
}
