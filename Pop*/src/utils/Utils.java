package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import harmony.Chord;

public class Utils {

	/*
	 * Credit to http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
	 */
	public static Object deepCopy(Object orig) {
        Object obj = null;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }

	public static Object join(List line, String delimiter) {
		StringBuilder str = new StringBuilder();
		boolean first = true;
		for (Object object : line) {
			if(!first)
				str.append(delimiter);
			else
				first = false;
			str.append(object);
		}
		
		return str.toString();
	}

	public static String getPositionString(int i)
	{
		String posStr = "the ";
		if (i == -1)
		{
			posStr += "LAST";
		}
		else if (i == 0)
		{
			posStr += "FIRST";
		}
		else if (i == 1)
		{
			posStr += "SECOND";
		}
		else if (i == 2)
		{
			posStr += "THIRD";
		}
		else
		{
			posStr += (i+1) + "TH";
		}
		
		return posStr + " position";
	}
}
