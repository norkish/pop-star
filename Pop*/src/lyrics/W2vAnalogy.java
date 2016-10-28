package lyrics;


import java.io.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;

import static java.lang.Math.toIntExact;

public class W2vAnalogy {
    final long max_size = 2000;         // max length of strings
    final long number_of_suggestions_to_show = 1000;                  // number of closest words that will be shown
    final long  max_w = 50;              // max length of vocabulary entries

    public HashSet<SmartWord> main(String bin, SmartWord oldTheme, SmartWord newTheme, SmartWord oldLyric) {
        File file;

        //TODO: is this st1 correct?
        char st1[] = new char[toIntExact(max_size)];
        char inputWords[][] = new char[100][toIntExact(max_size)]; //strings max length of max_size
        double dist, len;
        String[] closest_words = new String[toIntExact(number_of_suggestions_to_show)];
        double[] closest_distances = new double[toIntExact(number_of_suggestions_to_show)];
        double[] vec = new double[toIntExact(max_size)];
        long numberOfWordsInVector, numberOfDimensionsInVector;
        long a, b, c, d;
        long[] inputWordVocabPositions = new long[100];

        long number_of_input_words;
        char ch;

        StringBuilder sb = new StringBuilder(bin);
        String file_name = sb.toString();
        file = new File(file_name); // rb?
        if (file == null) {
            System.out.println("# Input file not found\n");
            return null;
        }

        try {
            //TODO: make sure this is the best way to read
            //BufferedReader br = new BufferedReader(new FileReader(file));
            //Scanner sc = new Scanner(file);
            DataInputStream data_in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));



//            byte[] bytes = new byte[8];
            data_in.readLong();
//            long asLong = (bytes[0] & 0xFF)
//                    | ((bytes[1] & 0xFF) << 8)
//                    | ((bytes[2] & 0xFF) << 16)
//                    | ((bytes[3] & 0xFF) << 24);
////                    | ((bytes[4] & 0xFF) << 32)
////                    | ((bytes[5] & 0xFF) << 40)
////                    | ((bytes[6] & 0xFF) << 48)
////                    | ((bytes[7] & 0xFF) << 56);
//            numberOfWordsInVector = asLong;
//            System.out.println("Words in vector: " + numberOfWordsInVector);// should be 681320
//            System.out.println("Bit count in 681320: " + Long.bitCount(681320));
//
//
//            bytes = new byte[8];
            data_in.readLong();
//            asLong = (bytes[0] & 0xFF)
//                    | ((bytes[1] & 0xFF) << 8)
//                    | ((bytes[2] & 0xFF) << 16)
//                    | ((bytes[3] & 0xFF) << 24);
////                    | ((bytes[4] & 0xFF) << 32)
////                    | ((bytes[5] & 0xFF) << 40)
////                    | ((bytes[6] & 0xFF) << 48)
////                    | ((bytes[7] & 0xFF) << 56);
//            numberOfDimensionsInVector = asLong;
//            System.out.println("Dimensions in vector: " + numberOfDimensionsInVector);// should be 200


            //TODO: fix
            //fscanf(file, "%lld", &words);
            //words = data_in.readLong();
            numberOfWordsInVector = 681320;

            //TODO: fix
            //fscanf(file, "%lld", &size);
            //size = data_in.readLong();
            numberOfDimensionsInVector = 200;

            //TODO: fix
            float[] M = new float[(int) (numberOfDimensionsInVector + numberOfDimensionsInVector * numberOfWordsInVector + 1)];
//            ArrayList<Float> M = new ArrayList<Float>();

            char[] vocab = new char[toIntExact(max_w * numberOfWordsInVector + max_w + 1)];
//            ArrayList<Character> vocab = new ArrayList<Character>();

            //vocab = (char *)malloc((long long)words * max_w * sizeof(char));
            //M = (double *)malloc((long long)words * (long long)size * sizeof(double));
            //if (M == null) {
                //System.out.println("# Cannot allocate memory: " + words * size * sizeof(double) / 1048576 + " MB " + words + size);
                //return;
            //}
            for (b = 0; b < numberOfWordsInVector; b++) {
                a = 0;
                boolean eof = false;
//                while (true) {
                while (!eof) {
                    try {

                    //System.out.println("Entering while 1.\nb: " + b + "\na: " + a);
                    //TODO: fix this
                    //if (data_in.available() > 0)
                        //System.out.print("vocab element changed from " + vocab[toIntExact(b * max_w + a)]);
                        vocab[toIntExact(b * max_w + a)] = (char)data_in.readByte();
                        //System.out.println(" to " + vocab[toIntExact(b * max_w + a)] + "\n");
//                    else
//                        break;
//                    vocab.set(toIntExact(b * max_w + a), data_in.readChar());
//                    sb = new StringBuilder(vocab);
//                    sb.setCharAt(toIntExact(b * max_w + a), data_in.readChar());
//                    vocab = sb.toString();

                    //if (feof(file) || (vocab[b * max_w + a] == ' '))
                    //    break;
                        if (eof)
                            break;
                    if (vocab[toIntExact(b * max_w + a)] == (int)' ')
                        break;

//                  if ((a < max_w) && (vocab[b * max_w + a] != '\n'))
//                      a++;
                    if ((a < max_w) && (vocab[toIntExact(b * max_w + a)] != (int)'\n'))
                        a++;
                    } catch (EOFException e) {
                        eof = true;
                    }
                }
                vocab[toIntExact(b * max_w + a)] = (char)'0';
//                vocab.set(toIntExact(b * max_w + a), '0');
//                sb = new StringBuilder(vocab);
//                sb.setCharAt(toIntExact(b * max_w + a), '0');
//                vocab = sb.toString();

                for (a = 0; a < numberOfDimensionsInVector; a++) {
                    //fread(&M[a + b * size], sizeof(double), 1, file);
                    //TODO: understand how to do this
                    //data_in.read(M[a + b * size], 8, 1, file);
                    System.out.print("M element changed from " + M[toIntExact(a + b * numberOfDimensionsInVector)]);
                    byte[] floatBytes = new byte[4];
                    //data_in.readFloat();
                    data_in.read(floatBytes);
                    //M[toIntExact(a + b * numberOfDimensionsInVector)] = (float) (data_in.read(bytes));
                    int asInt = (floatBytes[0] & 0xFF)
                            | ((floatBytes[1] & 0xFF) << 8)
                            | ((floatBytes[2] & 0xFF) << 16)
                            | ((floatBytes[3] & 0xFF) << 24);
                    float asFloat = Float.intBitsToFloat(asInt);
                    M[toIntExact(a + b * numberOfDimensionsInVector)] = asFloat;
                    System.out.println(" to " + M[toIntExact(a + b * numberOfDimensionsInVector)] + "\n");
                }

                len = 0;
                for (a = 0; a < numberOfDimensionsInVector; a++)
                    len += M[toIntExact(a + b * numberOfDimensionsInVector)] * M[toIntExact(a + b * numberOfDimensionsInVector)];
                len = (double)java.lang.Math.sqrt(len);
                for (a = 0; a < numberOfDimensionsInVector; a++) {
                    M[toIntExact((a + b * numberOfDimensionsInVector))] /= (len);
                    //double newThing = M.get(toIntExact(a + b * size)) / len;
                    //M.set(toIntExact(a + b * size), newThing);
                }
            }
            data_in.close();


        System.out.println("# " + oldTheme.getText() + " is to " + newTheme.getText() + " as " + oldLyric.getText() + " is to...\n");


        sb = new StringBuilder(oldTheme.getText());
        char inputChars[] = new char[toIntExact(max_size)];
        int i1;
        int i2;
        int i3;
        for (i1 = 0; i1 < oldTheme.getText().length(); i1++)
            inputChars[i1] = oldTheme.getText().charAt(i1);
        inputChars[i1] = ' ';
        for (i2 = i1 + 1; i2 - i1< newTheme.getText().length() + 1; i2++)
            inputChars[i2] = newTheme.getText().charAt(i2 - i1 - 1);
        inputChars[i2] = ' ';
        for (i3 = i2 + 1; i3 - i2< oldLyric.getText().length() + 1; i3++)
            inputChars[i3] = oldLyric.getText().charAt(i3 - i2 - 1);

        a = 0;
        while (true) {
            //System.out.println("Entering while 2");
            st1[toIntExact(a)] = inputChars[toIntExact(a)];
            //st1[a) = str.charAt(a);
            if ((st1[toIntExact(a)] == '\n') || (a >= max_size - 1)) {
                st1[toIntExact(a)] = '0';
                break;
            }
            a++;
        }

        for (a = 0; a < 100; a++) {
            for (int z = 0; z < max_size; z++) {
                inputWords[toIntExact(a)][toIntExact(z)] = ' ';
            }
        }
        for (a = 0; a < number_of_suggestions_to_show; a++)
            closest_distances[toIntExact(a)] = 0;
        for (a = 0; a < number_of_suggestions_to_show; a++) {
            closest_words[toIntExact(a)] = "";
//            sb = new StringBuilder(closest_words[toIntExact(a)]);
//            sb.setCharAt(0, '0');
//            closest_words[toIntExact(a)] = sb.toString();
        }
        a = 0;
        number_of_input_words = 0;
        b = 0;
        c = 0;
//        while (true) {
            //System.out.println("Entering while 3");
            //st[number_of_input_words][b] = st1[c)
            String input1 = oldTheme.getText();
            String input2 = newTheme.getText();
            String input3 = oldLyric.getText();
            inputWords[0] = input1.toCharArray();
            inputWords[1] = input2.toCharArray();
            inputWords[2] = input3.toCharArray();


//            inputWords[toIntExact(number_of_input_words)][toIntExact(b)] = st1[toIntExact(c)];
//
////            sb = new StringBuilder(st[toIntExact(number_of_input_words)]);
////            sb.setCharAt(toIntExact(b), st1[toIntExact(c)]);
////            st[toIntExact(number_of_input_words)] = sb.toString();
//            b++;
//            c++;
//            inputWords[toIntExact(number_of_input_words)][toIntExact(b)] = 0;
////            sb = new StringBuilder(st[toIntExact(number_of_input_words)]);
////            sb.setCharAt(toIntExact(b), '0');
////            st[toIntExact(number_of_input_words)] = sb.toString();
//            if (st1[toIntExact(c)] == 0)
//                break;
//            if (st1[toIntExact(c)] == ' ') {
//                number_of_input_words++;
//                b = 0;
//                c++;
//            }
//        }
        number_of_input_words = 3;
//        if (number_of_input_words < 3) {
//            System.out.println("%c Only %lld words were entered.. three words are needed at the input to perform the calculation\n", '#', number_of_input_words);
//        }
        for (a = 0; a < 3; a++) { //used to be (a = 0; a < number_of_input_words; a++)
            outerloop:
            for (b = 0; b < numberOfWordsInVector; b++) {
                //if (!strcmp(&vocab[b * max_w], st[a]))
                //TODO: ensure this works
                String temp = "";
                int x = 0;
                //System.out.println("char at [b * max_w] in vocab: " + vocab[toIntExact(b * max_w + x)]);
                //System.out.println("char at [a][x] in inputWords: " + inputWords[toIntExact(a)][x]);
                while (vocab[toIntExact(b * max_w + x)] == inputWords[toIntExact(a)][x]) {
                    temp += vocab[toIntExact(b * max_w + x)];
                    char[] tempArray = inputWords[toIntExact(a)];
                    String tempString = new String(tempArray);
                    if (temp.equals(tempString))
                        break outerloop;
                    x++;
                }
            }
            if (b == numberOfWordsInVector)
                b = 0;
            inputWordVocabPositions[toIntExact(a)] = b;
            if (b == 0) {
                System.out.println("# Out of dictionary word! Whole program is broken!\n");
                break;
            }
        }
        for (a = 0; a < numberOfDimensionsInVector; a++)
            vec[toIntExact(a)] = M[toIntExact(a + inputWordVocabPositions[1] * numberOfDimensionsInVector)] - M[toIntExact(a + inputWordVocabPositions[0] * numberOfDimensionsInVector)] + M[toIntExact(a + inputWordVocabPositions[2] * numberOfDimensionsInVector)];
        len = 0;
        for (a = 0; a < numberOfDimensionsInVector; a++)
            len += vec[toIntExact(a)] * vec[toIntExact(a)];
        len = (double)java.lang.Math.sqrt(len);
        for (a = 0; a < numberOfDimensionsInVector; a++)
            vec[toIntExact(a)] /= len;
        for (a = 0; a < number_of_suggestions_to_show; a++)
            closest_distances[toIntExact(a)] = 0;
//        for (a = 0; a < number_of_suggestions_to_show; a++) {
//            //closest_words[a][0] = 0;
//            sb = new StringBuilder(closest_words[toIntExact(a)]);
//            if (sb.length() == 0)
//                sb.append('0');
//            else
//                sb.setCharAt(0, '0');
//            closest_words[toIntExact(a)] = sb.toString();
//
//        }
        for (c = 0; c < numberOfWordsInVector; c++) {
            if (c == inputWordVocabPositions[0])
                continue;
            if (c == inputWordVocabPositions[1])
                continue;
            if (c == inputWordVocabPositions[2])
                continue;
            a = 0;
            for (b = 0; b < number_of_input_words; b++)
                if (inputWordVocabPositions[toIntExact(b)] == c) a = 1;
            if (a == 1)
                continue;
            dist = 0;
            for (a = 0; a < numberOfDimensionsInVector; a++)
                dist += vec[toIntExact(a)] * M[toIntExact(a + c * numberOfDimensionsInVector)];
            for (a = 0; a < number_of_suggestions_to_show; a++) {
                if (dist > closest_distances[toIntExact(a)]) {
                    for (d = number_of_suggestions_to_show - 1; d > a; d--) {
                        closest_distances[toIntExact(d)] = closest_distances[toIntExact(d - 1)];

                        //strcpy(closest_words[d], closest_words[d - 1]);
                        sb = new StringBuilder(closest_words[toIntExact(d - 1)]);
                        closest_words[toIntExact(d)] = sb.toString();

                    }
                    closest_distances[toIntExact(a)] = dist;
                    //strcpy(closest_words[a], &vocab[c * max_w]);
                    char not0 = vocab[toIntExact(c * max_w)];
                    String temp = "";
                    int x = 1;
                    while (not0 != '0') {
                        temp += not0;
                        not0 = vocab[toIntExact(c * max_w + x)];
                        x++;
                    }
                    //sb = new StringBuilder(vocab[toIntExact(c * max_w)]);
                    closest_words[toIntExact(a)] = temp;

                    break;
                }
            }
        }
        ArrayList<String> w2v_results = new ArrayList<String>();
        for (a = 0; a < number_of_suggestions_to_show; a++) {
            //System.out.println(closest_words[toIntExact(a)] + "\t" + closest_distances[toIntExact(a)]);
            w2v_results.add(closest_words[toIntExact(a)]);
        }
        long startTime = System.nanoTime();
        HashSet<SmartWord> tagged_w2v_results = LyricPack.stringsToSmartLyrics_Stanford(w2v_results);
        long endTime = System.nanoTime();
        System.out.println("stringsToSmartLyrics_Stanford time for " + oldLyric.getText() + ": " + ((endTime - startTime) / 1000000) + " milliseconds (" + ((endTime - startTime) / 1000000000) + " seconds).");
        return tagged_w2v_results;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}






















































