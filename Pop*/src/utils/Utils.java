package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import constraint.ConstraintBlock;
import data.MusicXMLParser.Harmony;
import lyrics.Lyric;

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

	public static <T> String join(List<T> line, String delimiter) {
		StringBuilder str = new StringBuilder();
		boolean first = true;
		for (T object : line) {
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

	public static <T extends Comparable<T>> Map<T, List<Integer>> sortByListSize(Map<T, List<Integer>> map, final boolean order)
    {
        List<Entry<T, List<Integer>>> list = new LinkedList<Entry<T, List<Integer>>>(map.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<T, List<Integer>>>()
        {
            public int compare(Entry<T, List<Integer>> o1,
                    Entry<T, List<Integer>> o2)
            {
                if (order)
                {
                    return o1.getValue().size() - o2.getValue().size();
                }
                else
                {
                    return o2.getValue().size() - o1.getValue().size();

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<T, List<Integer>> sortedMap = new LinkedHashMap<T, List<Integer>>();
        for (Entry<T, List<Integer>> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

	public static <T> void incrementValueForKey(Map<T, Integer> map, T key) {
		Integer count = map.get(key);
		map.put(key, count == null ? 1 : count + 1);		
	}

	public static <S, T> void incrementValueForKeys(Map<S, Map<T, Integer>> map2d, S key1,
			T key2) {
		Map<T,Integer> map1d = map2d.get(key1);
		if (map1d == null) { // never even seen prevState
			map1d = new HashMap<T,Integer>();
			map1d.put(key2, 1);
			map2d.put(key1, map1d);
		} else { // seen prev state
			Integer count = map1d.get(key2);
			map1d.put(key2, count == null ? 1 : count + 1);
		}		
	}
}
