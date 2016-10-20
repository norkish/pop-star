package orchestrate;

import java.util.Iterator;

import composition.Composition;
import globalstructure.SegmentType;
import substructure.SegmentSubstructure;
import utils.Triple;

public class CompingOrchestrator extends Orchestrator {

	@Override
	public Orchestration orchestrate(Composition newSong) {
		Orchestration orchestration = new Orchestration();
		
		for (Iterator<Triple<SegmentType, Integer, SegmentSubstructure>> segmentIter = newSong.getStructure().new SegmentIterator<Triple<SegmentType, Integer, SegmentSubstructure>>(); segmentIter.hasNext();) {
			Triple<SegmentType, Integer, SegmentSubstructure> segment = (Triple<SegmentType, Integer, SegmentSubstructure>) segmentIter.next();
			SegmentSubstructure segmentSubstructure = segment.getThird();
			orchestration.addMeasures(segmentSubstructure.getMeasureCount(), segmentSubstructure.timeSignature);
		}
		
		return orchestration;
	}

}
