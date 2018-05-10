package novels.training;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import novels.supersense.SupersenseFeatures;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Decode using a trained MEMM
 * 
 * @author dbamman
 * 
 */
public class SupersenseTest {

	String modelFile;
	String predFile;

	boolean greedy = true;

	HashMap<String, Integer> featureVocab;
	double[][] weights;

	static int NUM_LABELS;
	HashMap<String, Integer> labelVocab;
	String[] reverseLabelIds;

	public void readModel() {
		featureVocab = Maps.newHashMap();

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(modelFile), "UTF-8"));
			String str1;

			int currentId = 0;
			String[] base = in1.readLine().trim().split("\t");
			int numFeatures = Integer.valueOf(base[0]);
			NUM_LABELS = Integer.valueOf(base[1]);

			weights = new double[NUM_LABELS][numFeatures + 1];
			labelVocab = Maps.newHashMap();

			while ((str1 = in1.readLine()) != null) {
				try {
					String[] parts = str1.trim().split("\t");
					String type = parts[0];

					if (type.equals("FEAT")) {

						double weight = Double.valueOf(parts[1]);
						String label = parts[2];
						String feature = parts[3];

						int featureId = -1;
						if (featureVocab.containsKey(feature)) {
							featureId = featureVocab.get(feature);
						} else {
							featureId = currentId;
							featureVocab.put(feature, currentId);
							currentId++;
						}

						weights[labelVocab.get(label)][featureId] = weight;

					} else if (type.equals("LABEL")) {
						String label = parts[1];
						Integer labelId = Integer.valueOf(parts[2]);
						labelVocab.put(label, labelId);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			in1.close();

			reverseLabelIds = new String[labelVocab.size()];
			for (String label : labelVocab.keySet()) {
				reverseLabelIds[labelVocab.get(label)] = label;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SupersenseEvaluationContainer test() {
		ArrayList<ArrayList<HashMap<String,String>>> wordList = Lists.newArrayList();
		ArrayList<ArrayList<Integer>> labelList = Lists.newArrayList();

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(predFile), "UTF-8"));
			String str1;

			ArrayList<HashMap<String, String>> sequence = Lists.newArrayList();
			ArrayList<Integer> labels = Lists.newArrayList();

			while ((str1 = in1.readLine()) != null) {
				try {
					String[] parts = str1.trim().split("\t");
					if (parts.length > 2) {
						
						HashMap<String, String> feats=Maps.newHashMap();
						feats.put("word", parts[0]);
						feats.put("pos", parts[2]);
						feats.put("lemma", parts[3]);
						String label = parts[5];
						
						sequence.add(feats);
						
						int labelId = -1;
						if (labelVocab.containsKey(label)) {
							labelId = labelVocab.get(label);
						}

						labels.add(labelId);
					} else {

						if (sequence.size() > 0 && labels.size() > 0) {
							wordList.add(sequence);
							labelList.add(labels);
						}

						sequence = Lists.newArrayList();
						labels = Lists.newArrayList();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (sequence.size() > 0 && labels.size() > 0) {
				wordList.add(sequence);
				labelList.add(labels);
			}
			
			in1.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		SupersenseEvaluationContainer eval=new SupersenseEvaluationContainer(NUM_LABELS, reverseLabelIds);

		for (int seqId = 0; seqId < wordList.size(); seqId++) {
			ArrayList<HashMap<String,String>> words = wordList.get(seqId);
			ArrayList<Integer> seqlabels = labelList.get(seqId);

			Integer[] trueLabels = new Integer[seqlabels.size()];
			seqlabels.toArray(trueLabels);

			Integer[] currentLabels = new Integer[seqlabels.size()];

			if (!greedy) {
				viterbiDecode(words, currentLabels);
			} else {
				for (int i = 0; i < words.size(); i++) {
					greedyDecode(words, currentLabels, i);
				}
			}

			for (int i = 0; i < currentLabels.length; i++) {
				try {
					eval.confusion[currentLabels[i]][trueLabels[i]]++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
				
		return eval;
		

	}

	public int[] viterbiDecode(ArrayList<HashMap<String,String>> words, Integer[] currentLabels) {

		double[][] viterbiLattice = new double[words.size()][NUM_LABELS];
		for (int i = 0; i < words.size(); i++) {
			for (int j = 0; j < NUM_LABELS; j++) {
				viterbiLattice[i][j] = Double.NEGATIVE_INFINITY;
			}
		}
		int[][] backpointers = new int[words.size()][NUM_LABELS];

		int index = 0;
		double Z = 0;
		HashSet<String> stringFeats = SupersenseFeatures.extractFeatures(words,
				currentLabels, index);

		for (int i = 0; i < NUM_LABELS; i++) {

			double score = 0;
			for (String feat : stringFeats) {
				if (featureVocab.containsKey(feat)) {
					int featureId = featureVocab.get(feat);
					score += weights[i][featureId];
				}
			}
			score = Math.exp(score);

			viterbiLattice[0][i] = score;
			Z += score;

		}
		for (int i = 0; i < NUM_LABELS; i++) {
			viterbiLattice[0][i] /= Z;
			viterbiLattice[0][i] = Math.log(viterbiLattice[0][i]);
		}

		double[] tmpScores = new double[NUM_LABELS];

		for (index = 1; index < words.size(); index++) {

			// time step t-1
			for (int previousLabel = 0; previousLabel < NUM_LABELS; previousLabel++) {

				Z = 0;
				currentLabels[index - 1] = previousLabel;

				stringFeats = SupersenseFeatures.extractFeatures(
						words, currentLabels, index);
				
				// time step t
				for (int currentLabel = 0; currentLabel < NUM_LABELS; currentLabel++) {

					double score = 0;
					for (String feat : stringFeats) {
						if (featureVocab.containsKey(feat)) {
							int featureId = featureVocab.get(feat);
							score += weights[currentLabel][featureId];
						}
					}
					score = Math.exp(score);

					Z += score;
					tmpScores[currentLabel] = score;

				}

				for (int i = 0; i < NUM_LABELS; i++) {
					tmpScores[i] /= Z;

					double viterbiScore = Math.log(tmpScores[i])
							+ viterbiLattice[index - 1][previousLabel];

					if (viterbiScore > viterbiLattice[index][i]) {
						viterbiLattice[index][i] = viterbiScore;
						backpointers[index][i] = previousLabel;
					}

				}

			}
		}

		int[] bestPath = new int[words.size()];

		double highScore = Double.NEGATIVE_INFINITY;
		int high = -1;
		int T = words.size() - 1;
		for (int i = 0; i < NUM_LABELS; i++) {
			// System.out.println(viterbiLattice[T][i]);
			if (viterbiLattice[T][i] > highScore) {
				highScore = viterbiLattice[T][i];
				high = i;
			}
		}
		for (int t = T; t >= 0; t--) {
			bestPath[t] = high;
			high = backpointers[t][high];

		}

		for (int i = 0; i < bestPath.length; i++) {
			currentLabels[i] = bestPath[i];
		}

		return bestPath;

	}

	public void greedyDecode(ArrayList<HashMap<String,String>> words, Integer[] currentLabels, int index) {

		int high = -1;
		double highScore = -1;
		
		HashSet<String> stringFeats = SupersenseFeatures.extractFeatures(words,
				currentLabels, index);
		
		for (int i = 0; i < NUM_LABELS; i++) {
		
			double score = 0;
			for (String feat : stringFeats) {
				if (featureVocab.containsKey(feat)) {
					int featureId = featureVocab.get(feat);
		
					score += weights[i][featureId];
				}
			}
			if (score > highScore) {
				highScore = score;
				high = i;
			}
		}
		currentLabels[index] = high;

	}

	public static void main(String[] args) throws ParseException {

		SupersenseTest test = new SupersenseTest();

		Options options = new Options();
		options.addOption("w", true, "input model file");
		options.addOption("test", true, "test file to create predictions for");
		options.addOption("g", false, "use greedy decoding");

		CommandLine cmd = null;
		CommandLineParser parser = new BasicParser();
		cmd = parser.parse(options, args);

		if (cmd.hasOption("w")) {
			test.modelFile = cmd.getOptionValue("w");
		} else {
			System.out
					.println("You must specify an input model file with -w <file>");
			System.exit(1);
		}

		if (cmd.hasOption("test")) {
			test.predFile = cmd.getOptionValue("test");
		} else {
			System.out
					.println("You must specify a test file with -test <file>");
			System.exit(1);
		}

		if (cmd.hasOption("g")) {
			test.greedy = true;
		}

		test.readModel();
		SupersenseEvaluationContainer eval=test.test();
		eval.print();

	}

}
