package novels.supersense;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import novels.Token;
import novels.training.SupersenseEvaluationContainer;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Add WordNet supersense tags
 * https://wordnet.princeton.edu/man/lexnames.5WN.html
 * 
 * @author dbamman
 * 
 */
public class SupersenseTagger {

	public String modelFile;
	String predFile;

	boolean greedy = true;

	HashMap<String, Integer> featureVocab;
	double[][] weights;

	static int NUM_LABELS;
	HashMap<String, Integer> labelVocab;
	String[] reverseLabelIds;

	HashMap<String, String> readable;

	public SupersenseTagger() {
		readable = Maps.newHashMap();

		readable.put("03", "noun.Tops");
		readable.put("04", "noun.act");
		readable.put("05", "noun.animal");
		readable.put("06", "noun.artifact");
		readable.put("07", "noun.attribute");
		readable.put("08", "noun.body");
		readable.put("09", "noun.cognition");
		readable.put("10", "noun.communication");
		readable.put("11", "noun.event");
		readable.put("12", "noun.feeling");
		readable.put("13", "noun.food");
		readable.put("14", "noun.group");
		readable.put("15", "noun.location");
		readable.put("16", "noun.motive");
		readable.put("17", "noun.object");
		readable.put("18", "noun.person");
		readable.put("19", "noun.phenomenon");
		readable.put("20", "noun.plant");
		readable.put("21", "noun.possession");
		readable.put("22", "noun.process");
		readable.put("23", "noun.quantity");
		readable.put("24", "noun.relation");
		readable.put("25", "noun.shape");
		readable.put("26", "noun.state");
		readable.put("27", "noun.substance");
		readable.put("28", "noun.time");
		readable.put("29", "verb.body");
		readable.put("30", "verb.change");
		readable.put("31", "verb.cognition");
		readable.put("32", "verb.communication");
		readable.put("33", "verb.competition");
		readable.put("34", "verb.consumption");
		readable.put("35", "verb.contact");
		readable.put("36", "verb.creation");
		readable.put("37", "verb.emotion");
		readable.put("38", "verb.motion");
		readable.put("39", "verb.perception");
		readable.put("40", "verb.possession");
		readable.put("41", "verb.social");
		readable.put("42", "verb.stative");
		readable.put("43", "verb.weather");
	}

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
		ArrayList<ArrayList<HashMap<String, String>>> wordList = Lists
				.newArrayList();
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

					HashMap<String, String> feats = Maps.newHashMap();
					feats.put("word", parts[0]);
					feats.put("pos", parts[2]);

					sequence.add(feats);
					labels.add(-1);
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

		SupersenseEvaluationContainer eval = new SupersenseEvaluationContainer(NUM_LABELS,
				reverseLabelIds);

		for (int seqId = 0; seqId < wordList.size(); seqId++) {

			ArrayList<HashMap<String, String>> words = wordList.get(seqId);
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

			for (int i = 0; i < words.size(); i++) {
				System.out.println(words.get(i).get("word") + "\t"
						+ reverseLabelIds[currentLabels[i]]);
			}
		}

		return eval;

	}

	public void predictFromTokens(ArrayList<Token> tokens) {
		ArrayList<ArrayList<HashMap<String, String>>> wordList = Lists
				.newArrayList();
		ArrayList<ArrayList<Integer>> labelList = Lists.newArrayList();

		try {

			ArrayList<HashMap<String, String>> sequence = Lists.newArrayList();
			ArrayList<Integer> labels = Lists.newArrayList();

			for (Token token : tokens) {
				try {

					HashMap<String, String> feats = Maps.newHashMap();
					feats.put("word", token.word);
					feats.put("pos", token.pos);
					feats.put("lemma", token.lemma);

					sequence.add(feats);
					labels.add(-1);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			wordList.add(sequence);
			labelList.add(labels);

		} catch (Exception e) {
			e.printStackTrace();
		}

		int tokenid = 0;
		for (int seqId = 0; seqId < wordList.size(); seqId++) {
				
			ArrayList<HashMap<String, String>> words = wordList.get(seqId);
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

			for (int i = 0; i < words.size(); i++) {
				String supersense = reverseLabelIds[currentLabels[i]];
				String[] parts=supersense.split("-");
				if (parts.length == 2) {
					parts[1]=readable.get(parts[1]);
					supersense=parts[0] + "-" + parts[1];
				}
				Token token = tokens.get(tokenid);
				token.supersense = supersense;
				tokenid++;
			}
		}

	}

	public int[] viterbiDecode(ArrayList<HashMap<String, String>> words,
			Integer[] currentLabels) {

		double[][] viterbiLattice = new double[words.size()][NUM_LABELS];
		for (int i = 0; i < words.size(); i++) {
			for (int j = 0; j < NUM_LABELS; j++) {
				viterbiLattice[i][j] = Double.NEGATIVE_INFINITY;
			}
		}
		int[][] backpointers = new int[words.size()][NUM_LABELS];

		int index = 0;
		double Z = 0;
		for (int i = 0; i < NUM_LABELS; i++) {
			currentLabels[index] = i;
			HashSet<String> stringFeats = SupersenseFeatures.extractFeatures(words,
					currentLabels, index);

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

			for (int previousLabel = 0; previousLabel < NUM_LABELS; previousLabel++) {

				Z = 0;
				currentLabels[index - 1] = previousLabel;

				for (int currentLabel = 0; currentLabel < NUM_LABELS; currentLabel++) {

					currentLabels[index] = currentLabel;

					HashSet<String> stringFeats = SupersenseFeatures.extractFeatures(
							words, currentLabels, index);

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

	public void greedyDecode(ArrayList<HashMap<String, String>> words,
			Integer[] currentLabels, int index) {

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
			score = Math.exp(score);
			if (score > highScore) {
				highScore = score;
				high = i;
			}
		}
		currentLabels[index] = high;

	}

	public static void main(String[] args) throws ParseException {

		SupersenseTagger test = new SupersenseTagger();

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
		SupersenseEvaluationContainer eval = test.test();

	}

}
