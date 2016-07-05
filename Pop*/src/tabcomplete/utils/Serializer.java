package tabcomplete.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import tabcomplete.rawsheet.LyricSheet;

public class Serializer {

	public static Object load(String fileToDeserialize) {
		System.out.print("Deserializing " + fileToDeserialize + "... ");
		Object e = null;
		try {
			FileInputStream fileIn = new FileInputStream(fileToDeserialize);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			e = in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException i) {
			i.printStackTrace();
			return e;
		} catch (ClassNotFoundException c) {
			System.out.println("Employee class not found");
			c.printStackTrace();
			return e;
		}
		System.out.println("Success!");
		return e;
	}

	public static void serialize(Object e, String destinationFile) {
		try {
			FileOutputStream fileOut = new FileOutputStream(destinationFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(e);
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in " + destinationFile + "\n");
		} catch (IOException i) {
			i.printStackTrace();
		}
	}

}
