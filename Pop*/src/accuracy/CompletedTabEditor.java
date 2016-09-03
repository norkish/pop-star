package accuracy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import tabcomplete.main.TabDriver;

public class CompletedTabEditor {

	private static final String myDirectoryPath = TabDriver.dataDir + "/complete_tabs";

	private static final String labelTarget = "key";
	private static final Scanner inScan = new Scanner(System.in);

	public static void main(String[] args) throws IOException {
		File dir = new File(myDirectoryPath);
		File[] directoryListing = dir.listFiles();

		if (directoryListing != null) {
			for (File child : directoryListing) {
				String fileName = child.getName();
				if (fileName.startsWith(".")) continue;
				System.out.println("Loading " + fileName + "...");
				BufferedReader reader = new BufferedReader(new FileReader(child));
				List<String> lines = new ArrayList<String>();

				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
					lines.add(line);
				}

				String input = "blah";
				while (true) { // press enter if everything looks okay
					System.out.println("s - save");
					System.out.println("# - modify " + labelTarget + " at line #");
					System.out.println("k - skip");
					input = inScan.nextLine();
					if (input.equals("k")) {
						break;
					} else if (input.equals("s")) {
						PrintWriter writer = new PrintWriter(new File(myDirectoryPath + "/"
								+ fileName.substring(0, fileName.length() - 3) + labelTarget + ".annot.txt"));
						for (String string : lines) {
							writer.println(string);
						}
						writer.close();
						break;
					} else {
						try {
							int num = Integer.parseInt(input);
							int indexInLines = num * 2 + (labelTarget.equals("key")?0:1) + 3;
							String lineToEdit = lines.get(indexInLines);
							System.out.println("EDITING:\n" + lineToEdit);
							String[] split = lineToEdit.split("\t");
							System.out.print("New " + labelTarget + " for line " + num + ":");
							input = inScan.nextLine();
							if (labelTarget.equals("key")) {
								split[1] = input;
							}
							lines.set(indexInLines, StringUtils.join(split,"\t"));
						} catch (NumberFormatException e) {
						}
					}
					
					System.out.println("UPDATED CONTENT:");
					for (String string : lines) {
						System.out.println(string);
					}
				}
				reader.close();
			}
		}
	}
}
