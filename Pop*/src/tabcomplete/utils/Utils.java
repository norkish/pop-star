package tabcomplete.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class Utils {
	private static Scanner scanner = new Scanner(System.in);

	public static void promptEnterKey(String string) {
		System.out.print(string);
		scanner.nextLine();
	}

	public static String removeParen(String data) {
		StringBuilder buffer = new StringBuilder();

		int parenthesisCounter = 0;

		for (char c : data.toCharArray()) {
			if (c == '(' || c == '{' || c == '[')
				parenthesisCounter++;
			if (c == ')' || c == '}' || c == ']')
				parenthesisCounter--;
			if (!(c == '(' || c == '{' || c == '[' || c == ']' || c == ')' || c == '}') && parenthesisCounter == 0)
				buffer.append(c);
		}
		return buffer.toString();
	}

	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	public static int factorial(int n) {
        int fact = 1; // this  will be the result
        for (int i = 2; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

	public static void print2DMatrixDouble(double[][] matrix) {
		System.out.println("___________");
		for (double[] row : matrix) {
			System.out.print("|");
			boolean first = true;
			for (double col : row) {
				if (first)
					first = false;
				else
					System.out.print("\t");
				System.out.print(col);
			}
			System.out.println("|");
		}
		System.out.println("___________");
	}

	public static void print2DMatrixInt(int[][] matrix) {
		System.out.println("___________");
		for (int[] row : matrix) {
			System.out.print("|");
			boolean first = true;
			for (int col : row) {
				if (first)
					first = false;
				else
					System.out.print("\t");
				System.out.print(col);
			}
			System.out.println("|");
		}
		System.out.println("___________");		
	}
}
