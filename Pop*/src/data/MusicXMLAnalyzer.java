package data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import pitch.Pitch;
import tabcomplete.rhyme.Phonetecizer;
import utils.Counter;
import utils.Pair;
import utils.Triple;

public class MusicXMLAnalyzer {

	public static int getBackupEventCount(MusicXML m) {
		return m.xml.getElementsByTagName("backup").getLength();
	}

	public static int getRevisionCount(MusicXML m) {
		NodeList sourceNodes = m.xml.getElementsByTagName("source");

		for (int i = 0; i < sourceNodes.getLength(); i++) {
			Element sourceNode = (Element) sourceNodes.item(i);
			String text = sourceNode.getTextContent();
			if (text.startsWith("http://wikifonia.org/node/")) {
				return Integer.parseInt(text.substring(text.indexOf("revisions/") + 10, text.indexOf("/view")));
			}
		}

		return -1;
	}

	public static String getTitle(MusicXML m) {
		Node titleItem = m.xml.getElementsByTagName("movement-title").item(0);
		return titleItem == null? "MISSING_TITLE" : titleItem.getTextContent();
	}

	public static String getComposer(MusicXML m) {
		NodeList creators = m.xml.getElementsByTagName("creator");
		for (int i = 0; i < creators.getLength(); i++) {
			Element creator = (Element) creators.item(i);
			if (creator.getAttribute("type").equals("composer")) {
				return creator.getTextContent();
			}
		}

		return "MISSING_COMPOSER";
	}

	public static boolean isSingleLyric(Element lyric) {
		Node syllabicItem = lyric.getElementsByTagName("syllabic").item(0);
		return syllabicItem != null && syllabicItem.getTextContent().equals("single");
	}

	public static boolean isBeginLyric(Element lyric) {
		Node syllabicItem = lyric.getElementsByTagName("syllabic").item(0);
		return syllabicItem != null && syllabicItem.getTextContent().equals("begin");
	}

	public static Triple<Integer, String, NodeList> parseNote(Element note) {
		Node typeItem = note.getElementsByTagName("type").item(0);
		String type = typeItem == null ? "full bar" : typeItem.getTextContent();
		Integer midiPitch = -1;
		Element pitch = (Element) note.getElementsByTagName("pitch").item(0);
		if (pitch != null) {
			String step = pitch.getElementsByTagName("step").item(0).getTextContent();
			int octave = Integer.parseInt(pitch.getElementsByTagName("octave").item(0).getTextContent());
			Node item = pitch.getElementsByTagName("alter").item(0);
			int alter = item == null ? 0 : Integer.parseInt(item.getTextContent());
			midiPitch = convertNoteToMIDIPitch(step, alter, octave);
		}
		return new Triple<Integer, String, NodeList>(midiPitch, type, note.getElementsByTagName("lyric"));
	}

	public static Integer convertNoteToMIDIPitch(String step, int alter, int octave) {
		Integer pitch = Pitch.getPitchValue(step);

		if (pitch == 13) {
			return -1;
		} else {
			return ((pitch - 3 + 12) % 12) + octave * 12 + alter;
		}
	}

	public static boolean hasOpenRepeat(Element measure) {
		NodeList elementsByTagName = measure.getElementsByTagName("repeat");
		for (int i = 0; i < elementsByTagName.getLength(); i++) {
			Element repeat = (Element) elementsByTagName.item(i);
			if (repeat.getAttribute("direction").equals("forward"))
				return true;
		}
		return false;
	}

	public static boolean hasCloseRepeat(Element measure) {
		NodeList elementsByTagName = measure.getElementsByTagName("repeat");
		for (int i = 0; i < elementsByTagName.getLength(); i++) {
			Element repeat = (Element) elementsByTagName.item(i);
			if (repeat.getAttribute("direction").equals("backward"))
				return true;
		}
		return false;
	}

	public static Pair<String, String> parseChordSymbol(Element chord) {

		String root = chord.getElementsByTagName("root-step").item(0).getTextContent();
		Node alterItem = chord.getElementsByTagName("root-alter").item(0);
		if (alterItem != null) {
			int alter = Integer.parseInt(alterItem.getTextContent());
			switch (alter) {
			case -2:
				root += "b";
			case -1:
				root += "b";
				break;
			case 2:
				root += "#";
			case 1:
				root += "#";
			default:
			}
		}

		Element kindElement = (Element) chord.getElementsByTagName("kind").item(0);
		String kind = kindElement == null ? "major" : kindElement.getAttribute("text");
		// Node item = chord.getElementsByTagName("bass-step").item(0);
		// if (item != null) {
		// str.append(" ");
		// str.append(item.getTextContent());
		// }

		return new Pair<String, String>(root, kind);
	}

	public static String getScoreVersion(MusicXML m) {
		return ((Element) m.xml.getElementsByTagName("score-partwise").item(0)).getAttribute("version").toString();
	}

	public static Pair<Integer, Integer> parseTimeSig(Element timeSignature) {
		Integer beats = Integer.parseInt(timeSignature.getElementsByTagName("beats").item(0).getTextContent());
		Integer beat_type = Integer.parseInt(timeSignature.getElementsByTagName("beat-type").item(0).getTextContent());
		return new Pair<Integer, Integer>(beats, beat_type);
	}

	public static NodeList getTimeSignatures(MusicXML m) {
		return m.xml.getElementsByTagName("time");
	}

	public static Pair<Integer, String> parseKeySig(Element keySignature) {
		Integer fifths = Integer.parseInt(keySignature.getElementsByTagName("fifths").item(0).getTextContent());
		Node modeItem = keySignature.getElementsByTagName("mode").item(0);
		String mode = modeItem == null ? "major" : modeItem.getTextContent();
		return new Pair<Integer, String>(fifths, mode);
	}

	public static NodeList getChords(MusicXML m) {
		return m.xml.getElementsByTagName("harmony");
	}

	public static List<Node> getMeasures(MusicXML m) {
		List<Node> returnList = new ArrayList<Node>();
		
		NodeList nodeList = m.xml.getElementsByTagName("measure");
		for (int i = 0; i < nodeList.getLength(); i++) {
			returnList.add(nodeList.item(i));
		}
		
		return returnList;
	}
	
	public static List<Node> getMeasuresForPart(MusicXML m, int i) {
		NodeList scorePartwiseEls = m.xml.getElementsByTagName("score-partwise");
		assert scorePartwiseEls.getLength() == 1 : "Score has " + scorePartwiseEls.getLength() + " parts";
		
		NodeList children = scorePartwiseEls.item(0).getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node child = children.item(j);
			if (child.getNodeName().equals("part")) {
				List<Node> returnList = new ArrayList<Node>();
				
				NodeList nodeList = ((Element) child).getElementsByTagName("measure");
				for (int k = 0; k < nodeList.getLength(); k++) {
					returnList.add(nodeList.item(k));
				}
				
				return returnList;
			}
		}
		
		return null;
	}

	public static NodeList getKeySignatures(MusicXML m) {
		Element el = (Element) m.xml.getElementsByTagName("part-list").item(0);
		return m.xml.getElementsByTagName("key");
	}

	public static NodeList getPartsList(MusicXML m) {
		Element el = (Element) m.xml.getElementsByTagName("part-list").item(0);
		return el.getElementsByTagName("score-part");
	}

	public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	public static void printNode(Node node, PrintStream out) {
		Document document = node.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		String str = serializer.writeToString(node);
		out.println(str);
	}

	/**
	 * @param file
	 * @return
	 * @throws ZipException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws TransformerException
	 */
	public static Document mxlToXML(File file)
			throws ZipException, IOException, ParserConfigurationException, SAXException, TransformerException {
		ZipFile zf = new ZipFile(file);
		Document document = null;
		for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
			ZipEntry ze = e.nextElement();
			String name = ze.getName();
			if (name.endsWith(".xml")) {
				InputStream in = zf.getInputStream(ze);
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				// dbf.setValidating(false);
				// dbf.setNamespaceAware(true);
				// dbf.setFeature("http://xml.org/sax/features/namespaces", false);
				// dbf.setFeature("http://xml.org/sax/features/validation", false);
				// dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
				dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
				DocumentBuilder builder = dbf.newDocumentBuilder();
				builder.setEntityResolver(new EntityResolver() {
					@Override
					public InputSource resolveEntity(String publicId, String systemId)
							throws SAXException, IOException {
						if (systemId.contains("foo.dtd")) {
							return new InputSource(new StringReader(""));
						} else {
							return null;
						}
					}
				});
				document = builder.parse(in);
				// System.out.println(document.getDocumentElement().getNodeName());
				// printDocument(document, System.out);
				// NodeList elementsByTagName = document.getElementsByTagName("part-list");
				// assert elementsByTagName.getLength() != 0;
				// printNode(elementsByTagName.item(0), System.out);
				return document;
			}
		}
		zf.close();
		return null;
	}

	public static void main(String[] args)
			throws IOException, SAXException, ParserConfigurationException, TransformerException {
		File[] files = new File("/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/Wikifonia").listFiles();
		// - distribution of parts per file
		Counter<Integer> partsPerFile = new Counter<Integer>();

		// - distribution on notes playing at the same time (both per song and overall)
		Counter<Integer> backupCounter = new Counter<Integer>();

		// - distribution on key change counts
		Counter<Integer> keySignaturesChangesPerFile = new Counter<Integer>();

		// - distribution on key signature
		Counter<String> keySignatureDistribution = new Counter<String>();

		// - distribution on time signature changes
		Counter<Integer> timeSignaturesChangesPerFile = new Counter<Integer>();

		// - distribution on time signature
		Counter<String> timeSignatureDistribution = new Counter<String>();

		// - how many are not in the same xml format
		Counter<String> xmlFormatDistribution = new Counter<String>();

		// - what’s the distribution on lyric count
		Counter<Integer> lyricCountDistribution = new Counter<Integer>();
		Counter<Integer> wordCountDistribution = new Counter<Integer>();

		// - what’s the distribution of recognizable lyrics
		Counter<Integer> percentSinglesInCMUDictDistribution = new Counter<Integer>();

		// - what’s the distribution on lyric count by language
		// Counter<String> languageDistribution = new Counter<String>();

		// - what’s the distribution on note count
		Counter<Integer> notesPerFileDistribution = new Counter<Integer>();
		Counter<String> noteTypeDistribution = new Counter<String>();
		Counter<Integer> noteDurationDistribution = new Counter<Integer>();
		Counter<Integer> notePitchDistribution = new Counter<Integer>();
		Counter<Integer> noteRangeDistribution = new Counter<Integer>();
		Counter<Integer> percentNotesWithLyricsDistribution = new Counter<Integer>();
		Counter<Integer> avgNotesPerMeasureDistribution = new Counter<Integer>();

		Counter<Integer> measureCountDistribution = new Counter<Integer>();

		// - what’s the distribution on repeats
		Counter<Integer> measureCountWithRepeatsDistribution = new Counter<Integer>();
		Counter<Integer> repeatCountDistribution = new Counter<Integer>();
		Counter<Integer> versesPerRepeatDistribution = new Counter<Integer>();

		// - what’s the distribution on chord count
		Counter<Integer> chordCountDistribution = new Counter<Integer>();
		Counter<String> chordRootDistribution = new Counter<String>();
		Counter<String> chordDistribution = new Counter<String>();
		Counter<Integer> avgChordsPerMeasureDistribution = new Counter<Integer>();

		// - what’s the distribution on tempo
		// Counter<Integer> tempoDistribution = new Counter<Integer>();

		// - what’s the distribution on tempo changes
		// Counter<Integer> tempoChangeCountDistribution = new Counter<Integer>();

		// - what’s the distribution on versions of a song
		Counter<Integer> arrangementCountDistribution = new Counter<Integer>();

		// - how many have syllabic beginning and end
		Counter<Integer> syllabicBreaksPerSongDistribution = new Counter<Integer>();

		// - how many revisions per song
		Counter<Integer> revisionsPerSong = new Counter<Integer>();

		// - what’s the distribution on songs per artist
		// - how many different artists are represented
		Map<String, Counter<String>> songsByArtistDistribution = new HashMap<String, Counter<String>>();

		Map<String, Map<Boolean, Map<Boolean,Counter<Boolean>>>> tallies = new HashMap<String, Map<Boolean, Map<Boolean,Counter<Boolean>>>>();
		int totalTallies = 0;
		
		for (File file : files) {
//			 if (!file.getName().equals("Ahmad Jamal - Poinciana.mxl"))
//			 continue;
			System.out.println(file.getAbsolutePath());
			MusicXML musicXML = new MusicXML(mxlToXML(file));
			try {

				String scoreVersion = getScoreVersion(musicXML);
				System.out.println("Score version:" + scoreVersion);
				xmlFormatDistribution.incrementCountFor(scoreVersion);

				String title = getTitle(musicXML);
				System.out.println("Title:" + title);
				String composer = getComposer(musicXML);
				System.out.println("Composer:" + composer);
				Counter<String> titleCounter = songsByArtistDistribution.get(composer);
				if (titleCounter == null) {
					titleCounter = new Counter<String>();
					songsByArtistDistribution.put(composer, titleCounter);
				}
				titleCounter.incrementCountFor(title);

				int revisionCount = getRevisionCount(musicXML);
				System.out.println("Wikifonia revision count:" + revisionCount);
				revisionsPerSong.incrementCountFor(revisionCount / 100);

				int numberOfTracks = getPartsList(musicXML).getLength();
				partsPerFile.incrementCountFor(numberOfTracks);
				System.out.println("Part count:" + numberOfTracks);

				// has lyrics in english,
				boolean english = false;

				int backupCount = getBackupEventCount(musicXML);
				if (backupCount != 0) {
					System.out.println("Backup count:" + backupCount);
				}
				backupCounter.incrementCountFor(backupCount);

				List<Node> measures = getMeasures(musicXML);
				int measureCount = measures.size();
				System.out.println("Measure count:" + measureCount);
				measureCountDistribution.incrementCountFor(measureCount);

				Stack<Integer> openRepeatMeasures = new Stack<Integer>();
				openRepeatMeasures.push(0);
				int totalRepeatedMeasures = measureCount;
				int numberOfRepetitions = 1;
				int repeatCount = 0;
				int highestPitch = 0;
				int lowestPitch = 1000;
				int lyricCount = 0;
				int wordCount = 0;
				int beginLyricCount = 0;
				int notesWithLyrics = 0;
				int noteCount = 0;
				int cmuDictWordCount = 0;
				for (int i = 0; i < measureCount; i++) {
					Element measure = (Element) measures.get(i);
					if (hasOpenRepeat(measure)) {
						openRepeatMeasures.push(i);
						repeatCount++;
					}

					NodeList notes = measure.getElementsByTagName("note");
					int noteCountForMeasure = notes.getLength();
					for (int j = 0; j < notes.getLength(); j++) {
						Element note = (Element) notes.item(j);
						Node durationItem = note.getElementsByTagName("duration").item(0);
						if (durationItem == null || durationItem.getTextContent().trim() == "0") {
							// grace note
							noteCountForMeasure--;
							continue;
						}

						noteDurationDistribution.incrementCountFor(Integer.parseInt(durationItem.getTextContent()));

						Triple<Integer, String, NodeList> parsedNote = parseNote(note);

						Integer pitch = parsedNote.getFirst();

						if (pitch > highestPitch)
							highestPitch = pitch;
						if (pitch != -1 && pitch < lowestPitch)
							lowestPitch = pitch;
						notePitchDistribution.incrementCountFor(pitch);

						String type = parsedNote.getSecond();
						noteTypeDistribution.incrementCountFor(type);

						NodeList lyrics = parsedNote.getThird();
						int lyricCountForNote = lyrics.getLength();
						lyricCount += lyricCountForNote;
						if (lyricCount != 0) {
							notesWithLyrics++;
						}
						if (lyricCountForNote - 1 > numberOfRepetitions) {
							numberOfRepetitions = lyricCountForNote - 1;
						}
						for (int k = 0; k < lyricCountForNote; k++) {
							Element lyric = (Element) lyrics.item(k);
							if (isBeginLyric(lyric)) {
								beginLyricCount++;
								wordCount++;
							} else if (isSingleLyric(lyric)) {
								if (Phonetecizer
										.cmuDictContains(lyric.getElementsByTagName("text").item(0).getTextContent()))
									cmuDictWordCount++;
								wordCount++;
							}
						}
					}
					avgNotesPerMeasureDistribution.incrementCountFor(noteCountForMeasure);
					noteCount += noteCountForMeasure;

					if (hasCloseRepeat(measure) && openRepeatMeasures.size() > 0) {
						totalRepeatedMeasures += (i + 1 - openRepeatMeasures.pop()) * (numberOfRepetitions);
						versesPerRepeatDistribution.incrementCountFor(numberOfRepetitions + 1);
						numberOfRepetitions = 1;
					}
				}
				double percentSinglesInCMUDict = 1.0 * cmuDictWordCount / (wordCount - beginLyricCount);
				System.out.println("Percent single in CMU dict:" + percentSinglesInCMUDict);
				if ((wordCount - beginLyricCount) > 20 && percentSinglesInCMUDict > .7) {
					english = true;
					System.out.println("Is in english");
				}
				System.out.println("Word count:" + wordCount);
				System.out.println("Begin Lyric Count:" + beginLyricCount);
				percentSinglesInCMUDictDistribution.incrementCountFor((int) (100 * percentSinglesInCMUDict));
				percentNotesWithLyricsDistribution.incrementCountFor((int) (100.0 * notesWithLyrics / noteCount));
				notesPerFileDistribution.incrementCountFor(noteCount);
				lyricCountDistribution.incrementCountFor(lyricCount);
				wordCountDistribution.incrementCountFor(wordCount);
				syllabicBreaksPerSongDistribution.incrementCountFor(beginLyricCount);
				noteRangeDistribution.incrementCountFor(highestPitch - lowestPitch);
				measureCountWithRepeatsDistribution.incrementCountFor(totalRepeatedMeasures);
				repeatCountDistribution.incrementCountFor(repeatCount);

				// with at least an average of 1 chord every two measures and no more than 3 chords per measure,
				NodeList chords = getChords(musicXML);
				for (int i = 0; i < chords.getLength(); i++) {
					Node chord = chords.item(i);
					Pair<String, String> parsedChordSymbol = parseChordSymbol((Element) chord);
					chordRootDistribution.incrementCountFor(parsedChordSymbol.getFirst());
					chordDistribution.incrementCountFor(parsedChordSymbol.getSecond());
				}
				int chordCount = chords.getLength();
				System.out.println("Chord count:" + chordCount);
				chordCountDistribution.incrementCountFor(chordCount);

				double avgChordsPerMeasure = (1.0 * chordCount / measureCount);
				avgChordsPerMeasureDistribution.incrementCountFor((int) (avgChordsPerMeasure * 10));

				// single valid key signature,
				NodeList keySignatures = getKeySignatures(musicXML);
				int keySignatureCount = keySignatures.getLength();
				System.out.println("Key signature count:" + keySignatureCount);
				for (int i = 0; i < keySignatures.getLength(); i++) {
					Node keySignature = keySignatures.item(i);
					Pair<Integer, String> keySig = parseKeySig((Element) keySignature);
					keySignatureDistribution.incrementCountFor("" + keySig.getFirst() + " " + keySig.getSecond());
					System.out.println("\tkey signature:" + keySig);
				}
				keySignaturesChangesPerFile.incrementCountFor(keySignatureCount);

				// single valid time signature,
				NodeList timeSignatures = getTimeSignatures(musicXML);
				int timeSignatureCount = timeSignatures.getLength();
				System.out.println("Time signature count:" + timeSignatureCount);
				String timeSignatureString = null;
				for (int i = 0; i < timeSignatureCount; i++) {
					Node timeSignature = timeSignatures.item(i);
					Pair<Integer, Integer> timeSig = parseTimeSig((Element) timeSignature);
					timeSignatureString = "" + timeSig.getFirst() + "/" + timeSig.getSecond();
					timeSignatureDistribution.incrementCountFor(timeSignatureString);
					System.out.println("\ttime signature:" + timeSig);
				}
				timeSignaturesChangesPerFile.incrementCountFor(timeSignatureCount);

				// only one note ever playing at a time
				boolean hasOverlappingNotes = backupCount > 0;
				boolean hasSyllables = beginLyricCount > 0;
				boolean hasChords = avgChordsPerMeasure >= .5;

				if (timeSignatureCount == 1 && keySignatureCount == 1 && english) {
					Map<Boolean, Map<Boolean, Counter<Boolean>>> talliesByTS = tallies.get(timeSignatureString);
					if (talliesByTS == null) {
						talliesByTS = new HashMap<Boolean, Map<Boolean, Counter<Boolean>>>();
						tallies.put(timeSignatureString, talliesByTS);
					}
					Map<Boolean, Counter<Boolean>> talliesByOverlaps = talliesByTS.get(hasOverlappingNotes);
					if (talliesByOverlaps == null) {
						talliesByOverlaps = new HashMap<Boolean,Counter<Boolean>>();
						talliesByTS.put(hasOverlappingNotes, talliesByOverlaps);
					}
					Counter<Boolean> talliesBySyllable = talliesByOverlaps.get(hasSyllables);
					if (talliesBySyllable == null) {
						talliesBySyllable = new Counter<Boolean>();
						talliesByOverlaps.put(hasSyllables, talliesBySyllable);
					}
					talliesBySyllable.incrementCountFor(hasChords);

					totalTallies++;
				}
			} catch (Exception e) {
				printDocument(musicXML.xml, System.out);

				e.printStackTrace();
				System.out.println(file.getAbsolutePath());

				Scanner scan = new Scanner(System.in);
				scan.nextLine();
				scan.close();
			}
		}

		for (Counter<String> versionCounter : songsByArtistDistribution.values()) {
			for (Integer count : versionCounter.getUnderlylingMap().values()) {
				arrangementCountDistribution.incrementCountFor(count);
			}
		}

		System.out.println("\nFor songs with English lyrics in a single key and time signature:");
		for (String ts : tallies.keySet()) {
			Map<Boolean, Map<Boolean, Counter<Boolean>>> talliesByTS = tallies.get(ts);
			for (Boolean overlapNotes : talliesByTS.keySet()) {
				Map<Boolean, Counter<Boolean>> talliesByOverlap = talliesByTS.get(overlapNotes);
				for (Boolean hasSyls : talliesByOverlap.keySet()) {
					Map<Boolean, Integer> talliesByHasSyls = talliesByOverlap.get(hasSyls).getUnderlylingMap();
					for (Boolean hasChords : talliesByHasSyls.keySet()) {
						System.out.println(talliesByHasSyls.get(hasChords) + " songs in " + ts + " with" + (overlapNotes?"":"out") + " overlaps, with" + 
								(hasSyls?"":"out") + " syllables, and with" + (hasChords?"":"out") + " chords/msr >= .5");
					}
				}
			}
		};
		System.out.println("All tallied:" + totalTallies);
		
		// print distributions
		String chartDir = "/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/charts/";
		partsPerFile.createHistogram("Parts Per File", "Parts Per File", "Frequency", chartDir + "partsPerFile.jpeg");
		backupCounter.createHistogram("Backups per file", "Backups per file (for overlapping notes)", "Frequency",
				chartDir + "backupsPerFile.jpeg");
		keySignaturesChangesPerFile.createHistogram("Key Sigs per file", "Key Sigs Per File", "Frequency",
				chartDir + "keySigsPerFile.jpeg");
		keySignatureDistribution.createHistogram("Key Sig Distribution", "Key Signature Distribution", "Frequency",
				chartDir + "keySigsDistribution.jpeg");
		timeSignaturesChangesPerFile.createHistogram("Time Sigs per file", "Time Sigs Per File", "Frequency",
				chartDir + "timeSigsPerFile.jpeg");
		timeSignatureDistribution.createHistogram("Time Sigs Distribution", "Time Sigs Distribution", "Frequency",
				chartDir + "timeSigsDistribution.jpeg");
		xmlFormatDistribution.createHistogram("XML Format Distribution", "XML Format Distribution", "Frequency",
				chartDir + "xmlFormatDistribution.jpeg");
		lyricCountDistribution.createHistogram("Lyric Count per file", "Lyric Count Per File", "Frequency",
				chartDir + "lyricCountPerFile.jpeg");
		wordCountDistribution.createHistogram("Word Count Distribution", "Word Count Distribution", "Frequency",
				chartDir + "wordCountDistribution.jpeg");
		percentSinglesInCMUDictDistribution.createHistogram("Percent Singles in CMU", "Percent Singles in CMU",
				"Frequency", chartDir + "percentSinglesInCMU.jpeg");
		notesPerFileDistribution.createHistogram("Note Count per file", "Note Count Per File", "Frequency",
				chartDir + "noteCountPerFile.jpeg");
		noteTypeDistribution.createHistogram("Note Type Distribution", "Note Type Distribution", "Frequency",
				chartDir + "noteTypeDistribution.jpeg");
		noteDurationDistribution.createHistogram("Note Duration Distribution", "Note Duration Distribution",
				"Frequency", chartDir + "noteDurationDistribution.jpeg");
		notePitchDistribution.createHistogram("Note Pitch Distribution", "Note Pitch Distribution", "Frequency",
				chartDir + "notePitchDistribution.jpeg");
		noteRangeDistribution.createHistogram("Pitch Range Distribution", "Pitch Range Distribution", "Frequency",
				chartDir + "pitchRangeDistribution.jpeg");
		percentNotesWithLyricsDistribution.createHistogram("Percent Notes with Lyrics Distribution",
				"Percent Notes with Lyrics Distribution", "Frequency",
				chartDir + "percentNotesWithLyricsDistribution.jpeg");
		avgNotesPerMeasureDistribution.createHistogram("Avg Notes per measure Distribution",
				"Avg Notes per measure Distribution", "Frequency", chartDir + "avgNotesPerMeasureDistribution.jpeg");
		measureCountDistribution.createHistogram("Measure Count Distribution", "Measure Count Distribution",
				"Frequency", chartDir + "measureCountDistribution.jpeg");
		measureCountWithRepeatsDistribution.createHistogram("Measure Count With Repeats Distribution",
				"Measure Count With Repeats Distribution", "Frequency",
				chartDir + "measureCountWithRepeatsDistribution.jpeg");
		repeatCountDistribution.createHistogram("Repeat Count Distribution", "Repeat Count Distribution", "Frequency",
				chartDir + "RepeatCountDistribution.jpeg");
		versesPerRepeatDistribution.createHistogram("Verses Per Repeat Distribution", "Verses Per Repeat Distribution",
				"Frequency", chartDir + "versesPerRepeatDistribution.jpeg");
		chordCountDistribution.createHistogram("Chord Count per File", "Chord Count per File", "Frequency",
				chartDir + "chordCountPerFile.jpeg");
		chordRootDistribution.createHistogram("Chord Root Distribution", "Chord Root Distribution", "Frequency",
				chartDir + "chordRootDistribution.jpeg");
		chordDistribution.createHistogram("Chord Distribution", "Chord Distribution", "Frequency",
				chartDir + "chordDistribution.jpeg");
		avgChordsPerMeasureDistribution.createHistogram("Avg Chords Per Measure Distribution",
				"Avg Chords Per Measure Distribution (*10^-1)", "Frequency",
				chartDir + "avgChordsPerMeasureDistribution.jpeg");
		arrangementCountDistribution.createHistogram("Arrangement Count Distribution", "Arrangement Count Distribution",
				"Frequency", chartDir + "arrangementCountDistribution.jpeg");
		syllabicBreaksPerSongDistribution.createHistogram("Syllabic Breaks Per Song Distribution",
				"Syllabic Breaks Per Song Distribution", "Frequency", chartDir + "syllabicBreaksPerSong.jpeg");
		revisionsPerSong.createHistogram("Revisions Per Song", "Revisions Per Song (*10^2)", "Frequency",
				chartDir + "revisionsPerSongs.jpeg");
		int missingSongCount = 0;
		Counter<String> songsWithMissingArtist = songsByArtistDistribution.get("MISSING_COMPOSER");
		if (songsWithMissingArtist != null) {
			for(int count : songsWithMissingArtist.getUnderlylingMap().values())
				missingSongCount += count;
		}
		System.out.println("Total songs with missing artists represented in dataset:" + missingSongCount);
		System.out.println("Total artists represented in dataset:" + (songsByArtistDistribution.size() - (songsWithMissingArtist == null ? 0 : 1)));
	}
}
