package composition;

import java.util.Iterator;
import java.util.Map;

import globalstructure.SegmentType;
import harmony.Harmony;
import harmony.ProgressionSegment;
import inspiration.Inspiration;
import lyrics.LyricSegment;
import lyrics.Lyrics;
import melody.Melody;
import pitch.PitchSegment;
import rhythm.RhythmSegment;
import structure.Structure;
import substructure.SegmentSubstructure;
import utils.Triple;
import utils.Utils;

public class Composition {

	private String title = "BSSF";
	private String composer = "Pop*";
	private Structure structure = null;
	private Inspiration inspiration = null;
	private Lyrics lyrics = null;
	private Harmony harmony = null;
	private Melody melody = null;

	public void setStructure(Structure structure) {
		this.structure  = structure;
	}

	public void setInspiration(Inspiration inspiration) {
		this.inspiration = inspiration;
	}

	public void setLyrics(Lyrics lyrics) {
		this.lyrics  = lyrics;
	}

	public void setHarmony(Harmony harmony) {
		this.harmony  = harmony;
	}

	public void setMelody(Melody melody) {
		this.melody  = melody;
	}

	public String toString()
	{
		return print(true, true, true, false, true);
	}

	public String print(boolean printHeader, boolean printSubstructure, boolean printHarmony, boolean printMelody, boolean printLyrics)
	{
		if (!(printHeader || printSubstructure || printHarmony || printMelody || printLyrics))
			return "";
		
		StringBuilder str = new StringBuilder();
		
		if (printHeader)
		{
			str.append("Title: ");
			str.append(title);
			str.append('\n');
			str.append("Composer: ");
			str.append(composer);
			str.append("\nInspiration: ");
			str.append(inspiration.getExplaination());
			str.append("\n\n");
		}
		
		if (printHarmony || printSubstructure || printMelody || printLyrics)
		{
			Map<SegmentType, LyricSegment[]> lyricsBySegment = null;
			Map<SegmentType, ProgressionSegment[]> harmonyBySegment = null;
			Map<SegmentType, RhythmSegment[]> rhythmBySegment = null;
			Map<SegmentType, PitchSegment[]> pitchesBySegment = null;
			
			if (printLyrics) {
				lyricsBySegment = lyrics.getLyricsBySegment();
			}
			if (printHarmony) {
				harmonyBySegment = harmony.getProgressions();
			}
			if (printMelody) {
				rhythmBySegment = melody.getRhythms().getRhythmBySegment();
				pitchesBySegment = melody.getPitches().getPitchesBySegment();
			}
			
			for (Iterator<Triple<SegmentType, Integer, SegmentSubstructure>> segmentIter = structure.new SegmentIterator<Triple<SegmentType, Integer, SegmentSubstructure>>(); segmentIter.hasNext();) {
				Triple<SegmentType, Integer, SegmentSubstructure> segment = (Triple<SegmentType, Integer, SegmentSubstructure>) segmentIter.next();
				
				SegmentType segmentType = segment.getFirst();
				Integer segTypeIdx = segment.getSecond();
				SegmentSubstructure substructure = segment.getThird();
				
				str.append(segmentType);
				str.append(' ');
				str.append((segTypeIdx + 1));
				str.append(":\n");
				
				if (printSubstructure) {
					str.append("Structure:\n");
					str.append(substructure);
					str.append("\n");
				}
				
				if (printLyrics || printMelody || printHarmony) {
					for (int i = 0; i < substructure.linesPerSegment; i++) {
						if (printHarmony){
							str.append(Utils.join(harmonyBySegment.get(segmentType)[segTypeIdx].getLine(i),"\t"));
							str.append('\n');
						}
						if (printMelody) {
							str.append(Utils.join(pitchesBySegment.get(segmentType)[segTypeIdx].getLine(i),"\t"));
							str.append('\n');
							str.append(Utils.join(rhythmBySegment.get(segmentType)[segTypeIdx].getLine(i),"\t"));
							str.append('\n');
						}
						if (printLyrics) {
							str.append(Utils.join(lyricsBySegment.get(segmentType)[segTypeIdx].getLine(i)," "));
							str.append('\n');
						}
					}
					str.append('\n');
				}
			}
		}
		
		return str.toString();
	}

	public Structure getStructure() {
		return this.structure;
	}
}
