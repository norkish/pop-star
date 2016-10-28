package lyrics;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;

import static java.lang.Math.toIntExact;
import static javafx.scene.input.KeyCode.M;

public class W2vCommander {
    private final long max_size = 2000;         // max length of strings
    private final long number_of_suggestions_to_show = 1000;                  // number of closest words that will be shown
    private final long  max_w = 50;              // max length of vocabulary entries

    private int n_inputWords;
    private long numberOfDimensionsInVector;
    private long numberOfWordsInVector;
    private double[] vec = new double[toIntExact(max_size)];
    private double dist, len;
    private File file;
    private long a, b, c, d;
    private long[] inputWordVocabPositions = new long[100];
    private char ch;

    private HashSet<W2vJob> jobs;


    public void setupAll(String bin, HashSet<W2vJob> jobs) {
        this.jobs = jobs;
        char inputWords[][] = this.setupInput(jobs);
        this.setupVec(bin, inputWords);
        this.setupClosest();
        this.runAllW2vOperations(jobs);
    }

    public HashSet<W2vJob> runAll() {
        this.runAllW2vOperations(jobs);
        return jobs;//TODO > make sure this pointer is still okay
    }

    //private char[][] setupInput(SmartWord oldTheme, SmartWord newTheme, SmartWord oldLyric) {
    private char[][] setupInput(HashSet<W2vJob> jobs) {
        char inputWords[][] = new char[100][toIntExact(max_size)]; //strings max length of max_size
        int k = 0;
        for (W2vJob job : jobs) {
            job.explain();
            for (int j = 0; j < job.size(); j++) {
                inputWords[k] = job.toStrArray()[j].toCharArray();
                k++;
            }
        }
        n_inputWords = k;
        return inputWords;
    }

    private void setupVec(String bin, char inputWords[][]) {
        StringBuilder sb = new StringBuilder(bin);
        String file_name = sb.toString();
        file = new File(file_name);
        if (file == null) {
            System.out.println("# Input file not found\n");
            return;
        }

        try {
            DataInputStream data_in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

            //TODO: fix
            //fscanf(file, "%lld", &words);
            //words = data_in.readLong();
            numberOfWordsInVector = 681320;
            System.out.println("Words: " + numberOfWordsInVector);

            //TODO: fix
            //fscanf(file, "%lld", &size);
            //size = data_in.readLong();
            numberOfDimensionsInVector = 200;
            System.out.println("Size: " + numberOfDimensionsInVector);

            float[] M = new float[(int) (numberOfDimensionsInVector + numberOfDimensionsInVector * numberOfWordsInVector + 1)];

            char[] vocab = new char[toIntExact(max_w * numberOfWordsInVector + max_w + 1)];
            for (b = 0; b < numberOfWordsInVector; b++) {
                a = 0;
                boolean eof = false;
                while (!eof) {
                    try {
                        vocab[toIntExact(b * max_w + a)] = (char) data_in.readByte();
                        if (eof)
                            break;
                        if (vocab[toIntExact(b * max_w + a)] == (int) ' ')
                            break;

                        if ((a < max_w) && (vocab[toIntExact(b * max_w + a)] != (int) '\n'))
                            a++;
                    } catch (EOFException e) {
                        eof = true;
                    }
                }
                vocab[toIntExact(b * max_w + a)] = (char) '0';

                for (a = 0; a < numberOfDimensionsInVector; a++) {
                    byte[] bytes = new byte[4];
                    data_in.read(bytes);
                    int asInt = (bytes[0] & 0xFF)
                            | ((bytes[1] & 0xFF) << 8)
                            | ((bytes[2] & 0xFF) << 16)
                            | ((bytes[3] & 0xFF) << 24);
                    float asFloat = Float.intBitsToFloat(asInt);
                    M[toIntExact(a + b * numberOfDimensionsInVector)] = asFloat;
                }

                len = 0;
                for (a = 0; a < numberOfDimensionsInVector; a++)
                    len += M[toIntExact(a + b * numberOfDimensionsInVector)] * M[toIntExact(a + b * numberOfDimensionsInVector)];
                len = (double) java.lang.Math.sqrt(len);
                for (a = 0; a < numberOfDimensionsInVector; a++) {
                    M[toIntExact((a + b * numberOfDimensionsInVector))] /= (len);
                }
            }
            data_in.close();

            for (a = 0; a < 100; a++) {
                for (int z = 0; z < max_size; z++) {
                    inputWords[toIntExact(a)][toIntExact(z)] = ' ';
                }
            }
            a = 0;
            b = 0;
            c = 0;
            for (a = 0; a < n_inputWords; a++) {
                outerloop:
                for (b = 0; b < numberOfWordsInVector; b++) {
                    String temp = "";
                    int x = 0;
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
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupClosest() {
        String[] closest_words = new String[toIntExact(number_of_suggestions_to_show)];
        double[] closest_distances = new double[toIntExact(number_of_suggestions_to_show)];
        for (int a = 0; a < number_of_suggestions_to_show; a++)
            closest_distances[toIntExact(a)] = 0;
        for (int a = 0; a < number_of_suggestions_to_show; a++)
            closest_words[toIntExact(a)] = "";
    }

    private void runAllW2vOperations(HashSet<W2vJob> jobs) {
        for (W2vJob job : jobs) {
            //TODO: Does this instanceof thing work?
            if (job instanceof AnalogyJob)
                this.analogy((AnalogyJob)job);
            else if (job instanceof ThemeJob)
                this.theme((ThemeJob)job);
            else if (job instanceof SimilarJob)
                this.similar((SimilarJob)job);
        }
    }

    private AnalogyJob analogy(AnalogyJob job) {
        SmartWord oldTheme = job.getOldTheme();
        SmartWord newTheme = job.getNewTheme();
        SmartWord oldLyric = job.getOldWord();
        StringBuilder sb = new StringBuilder(oldTheme.getText());
        char inputChars[] = new char[toIntExact(max_size)];
        int i1;
        int i2;
        int i3;
        for (i1 = 0; i1 < oldTheme.getText().length(); i1++)
            inputChars[i1] = oldTheme.getText().charAt(i1);
        inputChars[i1] = ' ';
        for (i2 = i1 + 1; i2 - i1 < newTheme.getText().length() + 1; i2++)
            inputChars[i2] = newTheme.getText().charAt(i2 - i1 - 1);
        inputChars[i2] = ' ';
        for (i3 = i2 + 1; i3 - i2 < oldLyric.getText().length() + 1; i3++)
            inputChars[i3] = oldLyric.getText().charAt(i3 - i2 - 1);

        char st1[] = new char[toIntExact(max_size)];
        int a = 0;
        while (true) {
            st1[toIntExact(a)] = inputChars[toIntExact(a)];
            if ((st1[toIntExact(a)] == '\n') || (a >= max_size - 1)) {
                st1[toIntExact(a)] = '0';
                break;
            }
            a++;
        }

        String[] closest_words = new String[toIntExact(number_of_suggestions_to_show)];
        double[] closest_distances = new double[toIntExact(number_of_suggestions_to_show)];

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
        for (c = 0; c < numberOfWordsInVector; c++) {
            if (c == inputWordVocabPositions[0])
                continue;
            if (c == inputWordVocabPositions[1])
                continue;
            if (c == inputWordVocabPositions[2])
                continue;
            a = 0;
            for (b = 0; b < n_inputWords; b++)
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
        job.setW2vSuggestions(tagged_w2v_results);
        return job;
    }

    private ThemeJob theme(ThemeJob job) {
        HashSet<SmartWord> result = new HashSet<SmartWord>();
        job.setW2vSuggestions(result);
        return job;
    }

    private SimilarJob similar(SimilarJob job) {
        HashSet<SmartWord> result = new HashSet<SmartWord>();
        job.setW2vSuggestions(result);
        return job;
    }

}






















































