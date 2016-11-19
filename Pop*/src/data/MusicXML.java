package data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
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
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MusicXML {

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, TransformerException {
		File file = new File("/Users/norkish/Downloads/test.mxl");
		MusicXML muxXMLObj = parseCompressed(file);
	}

	public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

	    transformer.transform(new DOMSource(doc), 
	         new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	
	private static MusicXML parseCompressed(File file) throws IOException, SAXException, ParserConfigurationException, TransformerException {
		ZipFile zf = new ZipFile(file);
		for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
			ZipEntry ze = e.nextElement();
			String name = ze.getName();
			if (name.endsWith(".xml")) {
				InputStream in = zf.getInputStream(ze);
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//				dbf.setValidating(false);
//				dbf.setNamespaceAware(true);
//				dbf.setFeature("http://xml.org/sax/features/namespaces", false);
//				dbf.setFeature("http://xml.org/sax/features/validation", false);
//				dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
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
				Document document = builder.parse(in);
				System.out.println(document.getDocumentElement().getNodeName());
				printDocument(document, System.out);
			}
		}
		zf.close();
		return null;
	}

}
