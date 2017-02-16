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

public abstract class WikifoniaCorrection {

	public static class WikifoniaDirectionCorrection extends WikifoniaCorrection {

		private CorrectionType corrType;
		DirectionType oldDirection;
		DirectionType newDirection;
		int measureNumber = -1;
		
		public WikifoniaDirectionCorrection(CorrectionType replaceDirection, DirectionType oldDirection, DirectionType newDirection,
				int measureNumber) {
			this.corrType = replaceDirection;
			this.oldDirection = oldDirection;
			this.newDirection = newDirection;
			this.measureNumber = measureNumber;
		}

		@Override
		public String toString() {
			return "WikifoniaCorrection [" + (corrType != null ? "corrType=" + corrType + ", " : "")
					+ (oldDirection != null ? "oldDirection=" + oldDirection + ", " : "")
					+ (newDirection != null ? "newDirection=" + newDirection + ", " : "") + "measureNumber=" + measureNumber
					+ "]";
		}
		
		@Override
		protected void applyManualCorrection(MusicXMLParser musicXML) {
			List<Node> measures = MusicXMLSummaryGenerator.getMeasuresForPart(musicXML,0);
			Node measure = measures.get(measureNumber);
			switch (corrType) {
			case REPLACE:
				replaceDirection(measure,oldDirection,newDirection);
				break;
			case REMOVE:
				removeDirection(measure,oldDirection);
				break;
			case ADD:
				addDirection(measure,newDirection);
				break;
			}
		}
	}

	public static class WikifoniaKeyCorrection extends WikifoniaCorrection {

		private int measure;
		private int fifths;
		private String mode;

		public WikifoniaKeyCorrection(int measure, int newFifths, String mode) {
			this.measure = measure;
			this.fifths = newFifths;
			this.mode = mode;
		}

		@Override
		protected void applyManualCorrection(MusicXMLParser musicXML) {
			List<Node> measures = MusicXMLSummaryGenerator.getMeasuresForPart(musicXML,0);
			Node measureNode = measures.get(measure);
			MusicXMLSummaryGenerator.printNode(measureNode, System.out);
			replaceKey(measureNode,fifths,mode);
			MusicXMLSummaryGenerator.printNode(measureNode, System.out);
		}
	}

	public enum CorrectionType {
		REPLACE, REMOVE, ADD

	}

	private static Map<String, List<WikifoniaCorrection>> corrections;
	static {
		Map<String, List<WikifoniaCorrection>> acorrections = new HashMap<String, List<WikifoniaCorrection>>();
		
		addKeyChangeCorrection(acorrections, " Elton John, Bernie Taupin - Goodbye, Yellow Brick Road.mxl", 0, -1, "major"); // coda -> al coda

		addDirectionCorrection(acorrections, "A.B. Quintanilla III, Pete Astudillo - Baila Esta Cumbia.mxl", 61, CorrectionType.ADD, null, DirectionType.CODA1); // add coda
		
		addDirectionCorrection(acorrections, "A.B. Quintanilla III, Pete Astudillo - Baila Esta Cumbia.mxl", 47, CorrectionType.ADD, null, DirectionType.AL_CODA1); // add coda
		
		addDirectionCorrection(acorrections, "A.Preud'homme - Op de Purperen Hei.mxl", 32, CorrectionType.ADD, null, DirectionType.CODA1); // add coda

		addDirectionCorrection(acorrections, "A.S.Sullivan, W.S.Gilbert - With Cat-like Tread.mxl", 7, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addDirectionCorrection(acorrections, "A.W. LeRoy, A. Hazes - De Vlieger.mxl", 33, CorrectionType.REMOVE, DirectionType.CODA1, null); // remove coda
		addDirectionCorrection(acorrections, "A.W. LeRoy, A. Hazes - De Vlieger.mxl", 33, CorrectionType.REMOVE, DirectionType.CODA1, null); // remove coda
		addDirectionCorrection(acorrections, "A.W. LeRoy, A. Hazes - De Vlieger.mxl", 31, CorrectionType.REMOVE, DirectionType.CODA1, null); 

		addDirectionCorrection(acorrections, "Adam Fine - Laplace.mxl", 22, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);
		addDirectionCorrection(acorrections, "Adam Fine - Laplace.mxl", 23, CorrectionType.ADD, null, DirectionType.DC_AL_CODA1); // add coda
		
		addDirectionCorrection(acorrections, "Adam Levine, James Valentine - She Will Be Loved.mxl", 34, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1); 
		addDirectionCorrection(acorrections, "Adam Levine, James Valentine - She Will Be Loved.mxl", 48, CorrectionType.ADD, null, DirectionType.CODA1); 

		addDirectionCorrection(acorrections, "Adrienne Anderson, Barry Manilow - Could It Be Magic.mxl", 38, CorrectionType.ADD, null, DirectionType.DS_AL_CODA1);
		addDirectionCorrection(acorrections, "Adrienne Anderson, Barry Manilow - Could It Be Magic.mxl", 36, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);

		addDirectionCorrection(acorrections, "Agustin Lara - Solamente una Vez.mxl", 32, CorrectionType.ADD, null, DirectionType.DC_AL_CODA1);
		addDirectionCorrection(acorrections, "Agustin Lara - Solamente una Vez.mxl", 28, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);
		
		addDirectionCorrection(acorrections, "Alanis Morissette, Glen Ballard - Ironic.mxl", 31, CorrectionType.REMOVE, DirectionType.CODA1, null);
		addDirectionCorrection(acorrections, "Alanis Morissette, Glen Ballard - Ironic.mxl", 10, CorrectionType.REMOVE, DirectionType.CODA1, null);
		addDirectionCorrection(acorrections, "Alanis Morissette, Glen Ballard - Ironic.mxl", 32, CorrectionType.ADD, null, DirectionType.CODA1);
		
		addDirectionCorrection(acorrections, "Albert Hammond, Mike Hazlewood - Little Arrows.mxl", 48, CorrectionType.ADD, null, DirectionType.CODA1);
		
		addDirectionCorrection(acorrections, "Albert Parlow - Aambeeld-Polka.mxl", 92, CorrectionType.REPLACE, DirectionType.SEGNO, DirectionType.DS_AL_CODA1);
		addDirectionCorrection(acorrections, "Albert Parlow - Aambeeld-Polka.mxl", 32, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);
		
		addDirectionCorrection(acorrections, "Alicia Keys, George Harry, Kerry Brothers Jr. - No One.mxl", 39, CorrectionType.REPLACE, DirectionType.SEGNO, DirectionType.DS_AL_CODA1);
		addDirectionCorrection(acorrections, "Alicia Keys, George Harry, Kerry Brothers Jr. - No One.mxl", 23, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);

		addDirectionCorrection(acorrections, "Alicia Keys, George Harry, Kerry Brothers Jr. - No One.mxl.1", 39, CorrectionType.REPLACE, DirectionType.SEGNO, DirectionType.DS_AL_CODA1);
		addDirectionCorrection(acorrections, "Alicia Keys, George Harry, Kerry Brothers Jr. - No One.mxl.1", 23, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);
		
		addDirectionCorrection(acorrections, "Allee Willis, Jon Lind - Boogie Wonderland.mxl", 11, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addDirectionCorrection(acorrections, "Amador  Perez Dimas - Nereidas.mxl", 81, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addDirectionCorrection(acorrections, "Andre Moss - My Spanish Rose.mxl", 37, CorrectionType.REMOVE, DirectionType.SEGNO, null); // take off segno
		
		addDirectionCorrection(acorrections, "Annibale E I Cantori Moderni - Titoli (Triniti).mxl", 9, CorrectionType.REMOVE, DirectionType.CODA1, null);
		addDirectionCorrection(acorrections, "Annibale E I Cantori Moderni - Titoli (Triniti).mxl", 20, CorrectionType.REMOVE, DirectionType.CODA1, null);
		
		addDirectionCorrection(acorrections, "Anthony Newley, Leslie Bricusse - The Candy Man.mxl", 23, CorrectionType.ADD, null, DirectionType.DS_AL_CODA1);
		
		addDirectionCorrection(acorrections, "Antonio Carlos Jobim, Vinicius de Moraes - A Felicidade.mxl", 48, CorrectionType.ADD, null, DirectionType.DC_AL_CODA1);
		
		addDirectionCorrection(acorrections, "Arr. H. Laukens - IK HEB UNNE SPIJKER IN MUNNE KOP.mxl", 56, CorrectionType.REPLACE, DirectionType.SEGNO, DirectionType.DS_AL_CODA1);
		addDirectionCorrection(acorrections, "Arr. H. Laukens - IK HEB UNNE SPIJKER IN MUNNE KOP.mxl", 36, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);
		
		addDirectionCorrection(acorrections, "Arr. H. Laukens - IK HOU DR ZO VAN.mxl", 35, CorrectionType.REMOVE, DirectionType.CODA1, null);
		addDirectionCorrection(acorrections, "Arr. H. Laukens - IK HOU DR ZO VAN.mxl", 69, CorrectionType.REMOVE, DirectionType.CODA1, null);
		
		addDirectionCorrection(acorrections, "Art Noel - If You Ever Go To Ireland.mxl", 4, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);
		addDirectionCorrection(acorrections, "Art Noel - If You Ever Go To Ireland.mxl", 20, CorrectionType.REMOVE, DirectionType.CODA1, null);
		addDirectionCorrection(acorrections, "Art Noel - If You Ever Go To Ireland.mxl", 20, CorrectionType.REPLACE, DirectionType.SEGNO, DirectionType.DS_AL_CODA1);
		
		addDirectionCorrection(acorrections, "Arthur Terker, Harry Pyle, J. Russel Robinson - Meet Me In No Special Place.mxl", 33, CorrectionType.ADD, null, DirectionType.DC_AL_CODA1);
		addDirectionCorrection(acorrections, "Arthur Terker, Harry Pyle, J. Russel Robinson - Meet Me In No Special Place.mxl", 33, CorrectionType.ADD, null, DirectionType.AL_CODA1);
		
		addDirectionCorrection(acorrections, "Astor Piazzolla - Oblivion.mxl", 20, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1);
		addDirectionCorrection(acorrections, "Astor Piazzolla - Oblivion.mxl", 49, CorrectionType.REPLACE, DirectionType.SEGNO, DirectionType.DS_AL_CODA1);
		
		addDirectionCorrection(acorrections, "B Goodman, C Christian - Air Mail Special.mxl", 32, CorrectionType.REMOVE, DirectionType.CODA1, null);
		addDirectionCorrection(acorrections, "B Goodman, C Christian - Air Mail Special.mxl", 33, CorrectionType.REMOVE, DirectionType.CODA1, null);
		
		addDirectionCorrection(acorrections, "Carole King - You've Got a Friend.mxl", 47, CorrectionType.ADD, null, DirectionType.DS_AL_FINE);
		addDirectionCorrection(acorrections, "Carole King - You've Got a Friend.mxl", 20, CorrectionType.ADD, null, DirectionType.SEGNO);
		addDirectionCorrection(acorrections, "Carole King - You've Got a Friend.mxl", 33, CorrectionType.ADD, null, DirectionType.FINE);		
		
		
		
		
		
		
		addDirectionCorrection(acorrections, "Billie Joe Armstrong - Wake Me Up When September Ends.mxl", 58, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addDirectionCorrection(acorrections, "Billy Joel - Honesty.mxl", 2, CorrectionType.ADD, null, DirectionType.SEGNO); 
		addDirectionCorrection(acorrections, "Billy Joel - Honesty.mxl", 29, CorrectionType.ADD, null, DirectionType.CODA1);
		addDirectionCorrection(acorrections, "Billy Joel - Honesty.mxl", 16, CorrectionType.ADD, null, DirectionType.AL_CODA1);
		
		addDirectionCorrection(acorrections, "Billy Joel - Just The Way You Are.mxl", 99, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		addDirectionCorrection(acorrections, "Bob Davie, Marvin Moore - The Green Door.mxl", 10, CorrectionType.REPLACE, DirectionType.CODA1, DirectionType.AL_CODA1); // coda -> al coda
		
		
		corrections = Collections.unmodifiableMap(acorrections);
	}
	
	private static void addKeyChangeCorrection(Map<String, List<WikifoniaCorrection>> acorrections, String file,
			int measure, int newFifths, String mode) {
		List<WikifoniaCorrection> corrsForFile = getCorrectionsForFile(acorrections, file);
		
		corrsForFile.add(new WikifoniaKeyCorrection(measure, newFifths, mode));
	}

	private static void replaceKey(Node measure, int fifths, String mode) {
		Node keyNode = getKeyNode(measure);
		removeAllChildren(keyNode); // remove old direction type
		addKey(keyNode, fifths, mode);// add new direction type
	}

	private static void addKey(Node keyNode, int fifths, String mode) {
		Document doc = keyNode.getOwnerDocument();
		Node newNode = doc.createElement("fifths");
		newNode.appendChild(doc.createTextNode(""+fifths));
		keyNode.appendChild(newNode);
		newNode = doc.createElement("mode");
		newNode.appendChild(doc.createTextNode(mode));
		keyNode.appendChild(newNode);
	}

	private static Node getKeyNode(Node measure) {
		NodeList mChildren = measure.getChildNodes();
		MusicXMLSummaryGenerator.printNode(measure, System.err);
		for (int j = 0; j < mChildren.getLength(); j++) {
			Node mChild = mChildren.item(j);
			if (mChild instanceof Text) continue;
			String nodeName = mChild.getNodeName();
			if (nodeName.equals("attributes")) {
				NodeList mGrandchildren = mChild.getChildNodes();
				// do nothing, tempo would be found here, theoretically
				for (int k = 0; k < mGrandchildren.getLength(); k++) {
					Node mGrandchild = mGrandchildren.item(k);
					if (mGrandchild instanceof Text) continue;
					String gNodeName = mGrandchild.getNodeName();
					if (gNodeName.equals("key")) {
						return mGrandchild;
					} 
				}
			} 
		}
		
		System.err.println("Couldn't find key node in measure:");
		MusicXMLSummaryGenerator.printNode(measure, System.err);
		
		return null;
	}

	private static void addDirectionCorrection(Map<String, List<WikifoniaCorrection>> acorrections, String file, int i,
			CorrectionType replaceDirection, DirectionType coda1, DirectionType alCoda1) {
		List<WikifoniaCorrection> corrsForFile = getCorrectionsForFile(acorrections, file);
		
		corrsForFile.add(new WikifoniaDirectionCorrection(replaceDirection, coda1, alCoda1, i));
	}

	private static List<WikifoniaCorrection> getCorrectionsForFile(Map<String, List<WikifoniaCorrection>> acorrections,
			String file) {
		List<WikifoniaCorrection> corrsForFile = acorrections.get(file);
		if (corrsForFile == null) {
			corrsForFile = new ArrayList<WikifoniaCorrection>();
			acorrections.put(file, corrsForFile);
		}
		return corrsForFile;
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

	protected abstract void applyManualCorrection(MusicXMLParser musicXML);

	private static void removeDirection(Node measure, DirectionType oldDirection) {
		Node directionTypeNode = getMatchingDirectionTypeNode(measure, oldDirection);
		Node directionNode = directionTypeNode.getParentNode();
		directionNode.getParentNode().removeChild(directionNode);
	}

	private static void addDirection(Node measure, DirectionType newDirection) {
		Document doc = measure.getOwnerDocument();
		Node directionNode = doc.createElement("direction");
		Node directionTypeNode = directionNode.appendChild(doc.createElement("direction-type"));
		
		Node firstChild = null;
		NodeList mChildren = measure.getChildNodes();
		for (int j = 0; j < mChildren.getLength(); j++) {
			Node mChild = mChildren.item(j);
			if (!(mChild instanceof Text)) {
				firstChild = mChild;
				break;
			}
		}
		measure.insertBefore(directionNode, firstChild);

		addDirectionType(directionTypeNode, newDirection);// add new direction type
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
		
		System.err.println("Couldn't find direction-type ("+oldDirection+")node in measure:");
		MusicXMLSummaryGenerator.printNode(measure, System.err);
		
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
