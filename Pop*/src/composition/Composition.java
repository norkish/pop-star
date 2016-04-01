package composition;

import java.util.Iterator;

import globalstructure.SegmentType;
import harmony.Harmony;
import inspiration.Inspiration;
import lyrics.Lyrics;
import melody.Melody;
import structure.Structure;
import utils.Pair;

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
		StringBuilder str = new StringBuilder();
		
		str.append(title);
		str.append('\n');
		str.append(composer);
		str.append("\n\n");
		
		for (Iterator<Pair<SegmentType,Integer>> segmentIter = structure.new SegmentIterator<Pair<SegmentType, Integer>>(); segmentIter.hasNext();) {
			Pair<SegmentType, Integer> segment = (Pair<SegmentType, Integer>) segmentIter.next();
			
		}
		
		return str.toString();
	}
}
