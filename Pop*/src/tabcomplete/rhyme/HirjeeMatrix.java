package tabcomplete.rhyme;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import tabcomplete.main.TabDriver;
import utils.Pair;
import utils.Triple;

public class HirjeeMatrix {

	private static double[][] matrix = load();
	private static final String hirjeeFilePath = TabDriver.dataDir + "/hirjeeMatrix_withY.txt";

	public static double[][] load() {
		if (matrix == null) {

			BufferedReader bf;
			try {
				bf = new BufferedReader(new FileReader(hirjeeFilePath));

				String line = bf.readLine();
				String[] lineSplit = line.split("\t");
				int matrixWidth = lineSplit.length - 1;
				double value;
				matrix = new double[matrixWidth][matrixWidth];
				// System.out.println(Arrays.toString(lineSplit));
				// System.out.println(matrix.length);
				// System.out.println(matrix[1].length);

				int lineNum = 0;
				while ((line = bf.readLine()) != null) {
					lineSplit = line.split("\t");
					for (int i = lineNum; i < matrixWidth; i++) {
						if (i + 1 >= lineSplit.length || lineSplit[i + 1].length() == 0) {
							value = -50.0;
						} else {
							value = Double.parseDouble(lineSplit[i + 1]) * 10.;
						}

						matrix[lineNum][i] = value;
						matrix[i][lineNum] = value;
					}
					lineNum++;
				}

				bf.close();
			} catch (NumberFormatException | IOException e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
		}

		return matrix;
	}

	public static void main(String[] args) throws IOException {
		double[][] matrix = HirjeeMatrix.load();
		Map<String, Pair<Integer, PhoneCategory>> phonesDict = Phonetecizer.loadPhonesDict();

		for (double[] array : matrix) {
			for (double num : array)
				System.out.print(num + "\t");
			System.out.println();
		}

		for (String phone : phonesDict.keySet()) {
			System.out.println("AA with " + phone + ": "
					+ matrix[phonesDict.get(phone).getFirst()][phonesDict.get("AA").getFirst()]);
		}
	}

	public static double score(int phone, int phone2) {
		// TODO Auto-generated method stub
		return matrix[phone][phone2];
	}

	private static final int UNMATCHED_CODA_CONSONANT_AT_BEGINNING = 39;
	private static final int UNMATCHED_CODA_CONSONANT_AT_END = 40;
	
	public static double scoreSyllables(Triple<String, StressedPhone[], StressedPhone> mXML1LyricAndPronun,
			Triple<String, StressedPhone[], StressedPhone> mXML2LyricAndPronun) {
		// vowel
		double vowelScore = 0.0;
		StressedPhone vowel1 = mXML1LyricAndPronun.getThird();
		StressedPhone vowel2 = mXML2LyricAndPronun.getThird();
		vowelScore += matrix[vowel1.phone][vowel2.phone];

		// coda
		double codaScore = 0.0;
		StressedPhone[] coda1 = getCoda(mXML1LyricAndPronun);
		StressedPhone[] coda2 = getCoda(mXML2LyricAndPronun);

		if (coda1.length == 0 || coda2.length == 0) {
			if (coda1.length == 0 && coda2.length == 0) {
				codaScore = 3.0;
			} else {
				for (StressedPhone consonantPhoneme : coda1) {
					final int consIdx = consonantPhoneme.phone;
					codaScore = Math.max(matrix[consIdx][UNMATCHED_CODA_CONSONANT_AT_BEGINNING], matrix[consIdx][UNMATCHED_CODA_CONSONANT_AT_END]);
				}
				for (StressedPhone consonantPhoneme : coda2) {
					final int consIdx = consonantPhoneme.phone;
					codaScore = Math.max(matrix[consIdx][UNMATCHED_CODA_CONSONANT_AT_BEGINNING], matrix[consIdx][UNMATCHED_CODA_CONSONANT_AT_END]);
				}
				codaScore /= (coda1.length + coda2.length);
			}
		} else {
			double[][] alignmentMatrix = new double[coda1.length+1][coda2.length+1];
			alignmentMatrix[0][0] = 0;
			char[][] backtrack = new char[coda1.length+1][coda2.length+1];

			int cons1Idx, cons2Idx;
			for (int row = 1; row <= coda1.length; row++) {
				cons1Idx = coda1[row-1].phone;
				alignmentMatrix[row][0] = alignmentMatrix[row-1][0] + matrix[cons1Idx][UNMATCHED_CODA_CONSONANT_AT_BEGINNING];
				backtrack[row][0] = 'U';
			}
			for (int col = 1; col <= coda2.length; col++) {
				cons2Idx = coda2[col-1].phone;
				alignmentMatrix[0][col] = matrix[UNMATCHED_CODA_CONSONANT_AT_BEGINNING][cons2Idx];
				backtrack[0][col] = 'L';
			}
			
			double diag, up, left;
			for (int row = 1; row <= coda1.length; row++) {
				double[] prevMatrixRow = alignmentMatrix[row-1];
				double[] currMatrixRow = alignmentMatrix[row];
				char[] currBackTrackRow = backtrack[row];
				cons1Idx = coda1[row-1].phone;
				for (int col = 1; col <= coda2.length; col++) {
					cons2Idx = coda2[col-1].phone;
//						System.out.println("CONSONANT:" + coda1.get(row-1).phonemeEnum + " " + coda1.get(row-1).phonemeEnum.ordinal() + ", " + coda2.get(col-1).phonemeEnum + " " + coda2.get(col-1).phonemeEnum.ordinal());

					diag = prevMatrixRow[col-1] + matrix[cons1Idx][cons2Idx];
					left = currMatrixRow[col-1] + (row == coda1.length? matrix[UNMATCHED_CODA_CONSONANT_AT_END][cons2Idx] : Double.NEGATIVE_INFINITY);
					up = prevMatrixRow[col] + (col == coda2.length? matrix[cons1Idx][UNMATCHED_CODA_CONSONANT_AT_END] : Double.NEGATIVE_INFINITY);
	
					if (diag >= up) {
						if (diag >= left) {
							currMatrixRow[col] = diag;
							currBackTrackRow[col] = 'D';
						} else {
							currMatrixRow[col] = left;
							currBackTrackRow[col] = 'L';
						}
					} else {
						if (up >= left) {
							currMatrixRow[col] = up;
							currBackTrackRow[col] = 'U';
						} else {
							currMatrixRow[col] = left;
							currBackTrackRow[col] = 'L';
						}
					}
				}
			}
			
			int pathLen = 0;
			int row = coda1.length;
			int col = coda2.length;
			while(row != 0 || col != 0) {
				pathLen++;
				switch(backtrack[row][col]) {
				case 'D':
					row--;
					col--;
					break;
				case 'L':
					col--;
					break;
				case 'U':
					row--;
					break;
				}
			}
			
			codaScore = alignmentMatrix[coda1.length][coda2.length]/pathLen;
//				for (double[] ds : alignmentMatrix) {
//					System.out.println(Arrays.toString(ds));
//				}
		}
		
		// stress
		double stressScore = 0.0;
		
		final double score = vowelScore + codaScore + stressScore;
//			if (score > HIRJEE_RHYME_THRESHOLD)
//				System.out.print(s1 + " + " + s2 + " + " + vowelScore + " + " + codaScore + " + " + stressScore + " = " + score);
		return score;
	}

	private static StressedPhone[] getCoda(Triple<String, StressedPhone[], StressedPhone> mXML1LyricAndPronun) {
		StressedPhone vowel = mXML1LyricAndPronun.getThird();
		StressedPhone[] pronun = mXML1LyricAndPronun.getSecond();
		StressedPhone [] returnVal = null;
		int i;
		
		//[h,a,l,f]
		
		for (i = 0; i < pronun.length; i++) {
			if (returnVal != null) {
				returnVal[returnVal.length - (pronun.length-i)] = pronun[i];
			} else if (pronun[i] == vowel) {
				returnVal = new StressedPhone[pronun.length-(i+1)];
			}
		}
		
		return returnVal;
	}

}
