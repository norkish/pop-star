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
import pitch.Pitches;
import rhythm.RhythmSegment;
import rhythm.Rhythms;
import structure.Structure;
import substructure.Substructure;
import utils.Pair;
import utils.Triple;

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

	public String print(boolean printHeader, boolean printSubstructure, boolean printNonMelodicContent, boolean printMelody, boolean printLyrics)
	{
		if (!(printHeader || printSubstructure || printNonMelodicContent || printMelody))
			return "";
		
		StringBuilder str = new StringBuilder();
		
		Map<SegmentType, LyricSegment[]> lyricsBySegment = lyrics.getLyricsBySegment();
		Map<SegmentType, ProgressionSegment[]> harmonyBySegment = harmony.getProgressions();
		Map<SegmentType, RhythmSegment[]> rhythmBySegment = melody.getRhythms().getRhythmBySegment();
		Map<SegmentType, PitchSegment[]> pitchesBySegment = melody.getPitches().getPitchesBySegment();
		
		if (printHeader)
		{
			str.append(title);
			str.append('\n');
			str.append(composer);
			str.append("\nInspiration: ");
			str.append(inspiration);
			str.append("\n\n");
		}
		
		if (printNonMelodicContent || printSubstructure || printMelody)
		{
			for (Iterator<Triple<SegmentType, Integer, Substructure>> segmentIter = structure.new SegmentIterator<Triple<SegmentType, Integer, Substructure>>(); segmentIter.hasNext();) {
				Triple<SegmentType, Integer, Substructure> segment = (Triple<SegmentType, Integer, Substructure>) segmentIter.next();
				
				SegmentType segmentType = segment.getFirst();
				Integer segTypeIdx = segment.getSecond();
				Substructure substructure = segment.getThird();
				
				str.append(segmentType);
				str.append(' ');
				str.append((segTypeIdx + 1));
				str.append(":\n\n");
				
				if (printSubstructure) {
					str.append(substructure);
					str.append("\n\n");
				}
				
				if (printLyrics || printMelody || printNonMelodicContent) {
					for (int i = 0; i < substructure.linesPerSegment; i++) {
						if (printNonMelodicContent){
							str.append(harmonyBySegment.get(segmentType)[segTypeIdx].getLine(i));
							str.append('\n');
						}
						if (printMelody) {
							str.append(pitchesBySegment.get(segmentType)[segTypeIdx].getLine(i));
							str.append('\n');
							str.append(rhythmBySegment.get(segmentType)[segTypeIdx].getLine(i));
							str.append('\n');
						}
						if (printLyrics) {
							str.append(lyricsBySegment.get(segmentType)[segTypeIdx].getLine(i));
							str.append('\n');
						}
						str.append('\n');
					}
				}
				
			}
		}
		
		return str.toString();
	}
}
