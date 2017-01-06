package orchestrate;

import composition.Composition;

public class CompingOrchestrator extends Orchestrator {

	@Override
	public Orchestration orchestrate(Composition newSong) {
		
		Orchestration orchestration = new Orchestration();
		orchestration.addTrack("Melody", 0);
		orchestration.addTrack("Piano", 0);
		orchestration.addTrack("Bass", 132);
		orchestration.addTrack("Drums", 132);
		
//		for (Iterator<Triple<SegmentType, Integer, SegmentStructure>> segmentIter = newSong.getStructure().new SegmentIterator<Triple<SegmentType, Integer, SegmentStructure>>(); segmentIter.hasNext();) {
//			Triple<SegmentType, Integer, SegmentStructure> segment = (Triple<SegmentType, Integer, SegmentStructure>) segmentIter.next();
//			SegmentStructure segmentSubstructure = segment.getThird();
//			orchestration.addMeasures(segmentSubstructure.getMeasureCount(), segmentSubstructure.timeSignature);
//			
//			
//			
//		}
		
		return orchestration;
	}

}
