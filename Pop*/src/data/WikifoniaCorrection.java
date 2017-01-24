package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import data.MusicXMLParser.DirectionType;

public class WikifoniaCorrection {

	public enum CorrectionType {
		REPLACE_DIRECTION, REMOVE_DIRECTION

	}

	private static Map<String, List<WikifoniaCorrection>> corrections;
	static {
		Map<String, List<WikifoniaCorrection>> acorrections = new HashMap<String, List<WikifoniaCorrection>>();
		
		addCorrection(acorrections, "Allee Willis, Jon Lind - Boogie Wonderland.mxl", 11,
				CorrectionType.REPLACE_DIRECTION, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda

		addCorrection(acorrections, "Amador  Perez Dimas - Nereidas.mxl", 81,
				CorrectionType.REPLACE_DIRECTION, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda

		addCorrection(acorrections, "Andre Moss - My Spanish Rose.mxl", 37,
				CorrectionType.REMOVE_DIRECTION, DirectionType.SEGNO, null); // take off segno

		addCorrection(acorrections, "A.S.Sullivan, W.S.Gilbert - With Cat-like Tread.mxl", 7,
				CorrectionType.REPLACE_DIRECTION, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addCorrection(acorrections, "Billie Joe Armstrong - Wake Me Up When September Ends.mxl", 58, 
				CorrectionType.REPLACE_DIRECTION, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addCorrection(acorrections, "Billy Joel - Just The Way You Are.mxl", 99, 
				CorrectionType.REPLACE_DIRECTION, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addCorrection(acorrections, "Bob Davie, Marvin Moore - The Green Door.mxl", 10,
				CorrectionType.REPLACE_DIRECTION, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		
		corrections = Collections.unmodifiableMap(acorrections);
	}
	
	private CorrectionType corrType;
	DirectionType oldDirection;
	DirectionType newDirection;
	int measureNumber = -1;

	public WikifoniaCorrection(CorrectionType corrType, DirectionType oldDirection, DirectionType newDirection, int measureNumber) {
		this.corrType = corrType;
		this.oldDirection = oldDirection;
		this.newDirection = newDirection;
		this.measureNumber = measureNumber;
	}

	private static void addCorrection(Map<String, List<WikifoniaCorrection>> acorrections, String file, int i,
			CorrectionType replaceDirection, DirectionType coda1, DirectionType alCoda1) {
		List<WikifoniaCorrection> corrsForFile = acorrections.get(file);
		if (corrsForFile == null) {
			corrsForFile = new ArrayList<WikifoniaCorrection>();
			acorrections.put(file, corrsForFile);
		}
		
		corrsForFile.add(new WikifoniaCorrection(replaceDirection, coda1, alCoda1, i));
	}

	public static void applyManualCorrections(MusicXMLParser musicXML, String name) {
		List<WikifoniaCorrection> corrsForFile = corrections.get(name);
		
		if (corrsForFile == null) return;
		
		System.err.println("Applying corrections to " + name);
		
		for (WikifoniaCorrection wikifoniaCorrection : corrsForFile) {
			System.err.println(wikifoniaCorrection);
			wikifoniaCorrection.applyManualCorrection(musicXML);
		}
	}


	@Override
	public String toString() {
		return "WikifoniaCorrection [" + (corrType != null ? "corrType=" + corrType + ", " : "")
				+ (oldDirection != null ? "oldDirection=" + oldDirection + ", " : "")
				+ (newDirection != null ? "newDirection=" + newDirection + ", " : "") + "measureNumber=" + measureNumber
				+ "]";
	}

	private void applyManualCorrection(MusicXMLParser musicXML) {
		List<Node> measures = MusicXMLSummaryGenerator.getMeasuresForPart(musicXML,0);
		Node measure = measures.get(measureNumber);
		switch (corrType) {
		case REPLACE_DIRECTION:
			replaceDirection(measure,oldDirection,newDirection);
			break;
		case REMOVE_DIRECTION:
			removeDirection(measure,oldDirection);
			break;
		}
	}

	private static void removeDirection(Node measure, DirectionType oldDirection) {
		Node directionTypeNode = getMatchingDirectionTypeNode(measure, oldDirection);
		Node directionNode = directionTypeNode.getParentNode();
		directionNode.getParentNode().removeChild(directionNode);
	}

	private static void replaceDirection(Node measure, DirectionType oldDirection, DirectionType newDirection) {
		Node directionTypeNode = getMatchingDirectionTypeNode(measure, oldDirection);
		removeAllChildren(directionTypeNode); // remove old direction type
		addDirectionType(directionTypeNode, newDirection);// add new direction type
	}

	private static Node getMatchingDirectionTypeNode(Node measure, DirectionType oldDirection) {
		NodeList mChildren = measure.getChildNodes();
		MusicXMLSummaryGenerator.printNode(measure, System.err);
		for (int j = 0; j < mChildren.getLength(); j++) {
			Node mChild = mChildren.item(j);
			if (mChild instanceof Text) continue;
			String nodeName = mChild.getNodeName();
			if (nodeName.equals("direction")) {
				NodeList mGrandchildren = mChild.getChildNodes();
				// do nothing, tempo would be found here, theoretically
				for (int k = 0; k < mGrandchildren.getLength(); k++) {
					Node mGrandchild = mGrandchildren.item(k);
					if (mGrandchild instanceof Text) continue;
					String gNodeName = mGrandchild.getNodeName();
					if (gNodeName.equals("direction-type")) {
						DirectionType type = DirectionType.parseDirectionType(mGrandchild);
						if (type == oldDirection) { 
							return mGrandchild;
						} 
					} 
				}
			} 
		}
		return null;
	}

	private static void addDirectionType(Node directionTypeNode, DirectionType directionType) {
		
		Node newNode = null;
		String textContent = null;
		
		Document doc = directionTypeNode.getOwnerDocument();
		switch(directionType) {
		case AL_CODA1:
			textContent = "To Coda 1";
			break;
		case AL_CODA2:
			textContent = "To Coda 2";
			break;
		case DS_AL_CODA1:
			textContent = "D.S. al Coda 1";
			break;
		case DS_AL_CODA2:
			textContent = "D.S. al Coda 2";
			break;
		case FINE:
			textContent = "Fine";
			break;
		case DC_AL_FINE:
			textContent = "D.C. al Fine";
			break;
		case DS_AL_FINE:
			textContent = "D.S. al Fine";
			break;
		case DC_AL_CODA1:
			textContent = "D.C. al Coda";
			break;
		case CODA1:
			newNode = doc.createElement("coda");
			break;
		case SEGNO:
			newNode = doc.createElement("segno");
			break;
		case CODA2:
		case IGNORE:
		default:
			throw new RuntimeException("Unhandled correction case");
		}

		if (newNode == null) {
			newNode = doc.createElement("words");
			newNode.appendChild(doc.createTextNode(textContent));
		}
		directionTypeNode.appendChild(newNode);
	}

	private static void removeAllChildren(Node directionNode) {
		NodeList children = directionNode.getChildNodes();
		for (int i = children.getLength()-1; i >= 0; i--) {
			Node item = children.item(i);
			directionNode.removeChild(item);
		}
	}

}
