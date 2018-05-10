package novels.training;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import novels.optimization.MulticlassSparseRegression;
import novels.supersense.SupersenseFeatures;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Train an MEMM
 * 
 * @author dbamman
 * 
 */
public class SupersenseTrain {

	String trainFile;
	String devFile;

	String baseModelFile;
	String modelFile;

	int NUM_LABELS;

	HashMap<String, Integer> featureVocab;

	HashMap<String, Integer> labelVocab;
	public static String[] reverseLabelIds;

	String[] reverseFeatureIds;

	ArrayList<ArrayList<HashSet<Integer>>> x;
	ArrayList<ArrayList<Integer>> y;
	int numFeats;

	double L1Lambda = 1;
	double L2Lambda = 0;

	double L2start = 1;
	double L2end = 1;
	double L2steps = 1;

	public SupersenseTrain() {
		featureVocab = Maps.newHashMap();
		x = Lists.newArrayList();
		y = Lists.newArrayList();

		featureVocab.put(SupersenseFeatures.BIAS, 0);
		numFeats++;

	}

	public HashSet<Integer> convertStringFeats(HashSet<String> stringFeats) {
		HashSet<Integer> feats = Sets.newHashSet();
		for (String feat : stringFeats) {
			int featureId = -1;
			if (featureVocab.containsKey(feat)) {
				featureId = featureVocab.get(feat);
			} else {
				featureId = numFeats++;
				featureVocab.put(feat, featureId);
			}
			feats.add(featureId);
		}
		return feats;
	}

	public void reorderLabels(ArrayList<Integer> labelList) {

		String last = "O";
		int index = labelVocab.get(last);
		int lastIndex = reverseLabelIds.length - 1;
		String swapLabel = reverseLabelIds[lastIndex];
		labelVocab.put(swapLabel, index);
		labelVocab.put(last, lastIndex);

		for (int i = 0; i < labelList.size(); i++) {
			int labelId = labelList.get(i);
			if (labelId == index) {
				labelList.set(i, lastIndex);
			} else if (labelId == lastIndex) {
				labelList.set(i, index);
			}
		}

		reverseLabelIds[lastIndex] = last;
		reverseLabelIds[index] = swapLabel;

	}

	public void readData() {
		ArrayList<ArrayList<HashMap<String,String>>> wordList = Lists.newArrayList();
		ArrayList<ArrayList<Integer>> labelList = Lists.newArrayList();

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(trainFile), "UTF-8"));
			String str1;
			labelVocab = Maps.newHashMap();
			int currentLabels = 0;

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
						
						if (!labelVocab.containsKey(label)) {
							labelId = currentLabels++;
							labelVocab.put(label, labelId);
						} else {
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

		NUM_LABELS = labelVocab.size();

		reverseLabelIds = new String[NUM_LABELS];
		for (String feat : labelVocab.keySet()) {
			int id = labelVocab.get(feat);
			reverseLabelIds[id] = feat;
		}

	
			
		for (int i=0; i<wordList.size(); i++) {
			
			ArrayList<HashMap<String,String>> sequence=wordList.get(i);
			ArrayList<Integer> seqlabels=labelList.get(i);
			
			Integer[] labels = new Integer[seqlabels.size()];
			seqlabels.toArray(labels);
			
			ArrayList<HashSet<Integer>> xSeq=Lists.newArrayList();
			ArrayList<Integer> ySeq=Lists.newArrayList();

			for (int j = 0; j < labels.length; j++) {
				HashSet<String> stringFeats = SupersenseFeatures.extractFeatures(sequence,
						labels, j);
				HashSet<Integer> feats = convertStringFeats(stringFeats);

				xSeq.add(feats);
				ySeq.add(labels[j]);
			}
			
			x.add(xSeq);
			y.add(ySeq);
			
		}

	}

	public void sweep() {
		double[] weights = new double[NUM_LABELS * numFeats];

		double increment = (L2end - L2start) / L2steps;
		if (L2end == L2start) {
			increment = 1;
		}
		for (double L2 = L2start; L2 <= L2end; L2 += increment) {
			System.out.println(String.format("L2 value: %.3f", L2));
			modelFile = String.format("%s.L2.%.3f.model", baseModelFile, L2);
			weights = train(L2, weights);
			SupersenseTest test = new SupersenseTest();
			test.predFile = devFile;
			test.modelFile = modelFile;
			test.readModel();
			SupersenseEvaluationContainer eval=test.test();
			eval.print();
		}
	}

	public double[] train(double L1, double[] initialWeights) {

		MulticlassSparseRegression reg = new MulticlassSparseRegression(NUM_LABELS, x, y, L1,
				L2Lambda, numFeats);
		HashSet<Integer> biases = Sets.newHashSet();
		biases.add(featureVocab.get(SupersenseFeatures.BIAS));
		reg.setBiasParameters(biases);

		reg.initial = initialWeights;
		double[] weights = reg.regress();
		printWeightsToFile(modelFile, reg);
		return weights;

	}

	public double[] train(double L1) {

		MulticlassSparseRegression reg = new MulticlassSparseRegression(NUM_LABELS, x, y, L1,
				L2Lambda, numFeats);
		HashSet<Integer> biases = Sets.newHashSet();
		biases.add(featureVocab.get(SupersenseFeatures.BIAS));
		reg.setBiasParameters(biases);
		double[] weights = reg.regress();
		printWeightsToFile(modelFile, reg);
		return weights;

	}

	public void finalizeFeatures() {
		reverseFeatureIds = new String[numFeats];
		for (String feat : featureVocab.keySet()) {
			int id = featureVocab.get(feat);
			reverseFeatureIds[id] = feat;
		}

	}

	public void printWeightsToFile(String weightFile, MulticlassSparseRegression reg) {

		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(weightFile), "UTF-8");

			out.write(String.format("%s\t%s\n", numFeats, NUM_LABELS));

			for (String label : labelVocab.keySet()) {
				out.write(String.format("%s\t%s\t%s\n", "LABEL", label,
						labelVocab.get(label)));
			}

			for (int i = 0; i < NUM_LABELS; i++) {
				for (int j = 0; j < numFeats; j++) {
					String value = reverseFeatureIds[j];

					double weight = reg.model.classweights[i][j];
					if (weight == 0) {
						continue;
					}
					out.write(String.format("%s\t%s\t%s\t%s\n", "FEAT", weight,
							reverseLabelIds[i], value));
				}
			}

			out.close();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws ParseException {

		SupersenseTrain memm = new SupersenseTrain();

		Options options = new Options();
		options.addOption("w", true, "output model file");
		options.addOption("train", true, "training file");
		options.addOption("dev", true,
				"development file (for tuning regularizers");

		options.addOption("L2start", true, "L2 starting value for sweep");
		options.addOption("L2end", true, "L2 ending value for sweep");
		options.addOption("L2steps", true, "L2 number of steps to sweep");

		CommandLine cmd = null;
		CommandLineParser parser = new BasicParser();
		cmd = parser.parse(options, args);

		if (cmd.hasOption("w")) {
			memm.baseModelFile = cmd.getOptionValue("w");
		} else {
			System.out
					.println("You must specify an output file to write to with -w <file>");
			System.exit(1);
		}

		if (cmd.hasOption("train")) {
			memm.trainFile = cmd.getOptionValue("train");
		} else {
			System.out
					.println("You must specify an training file to write to with -train <file>");
			System.exit(1);
		}

		if (cmd.hasOption("dev")) {
			memm.devFile = cmd.getOptionValue("dev");
		} else {
			System.out
					.println("You must specify an development file to write to with -dev <file>");
			System.exit(1);
		}

		if (cmd.hasOption("L2start")) {
			memm.L2start = Double.valueOf(cmd.getOptionValue("L2start"));
			memm.L2end = memm.L2start;
		}
		if (cmd.hasOption("L2end")) {
			memm.L2end = Double.valueOf(cmd.getOptionValue("L2end"));
		}
		if (cmd.hasOption("L2steps")) {
			memm.L2steps = Double.valueOf(cmd.getOptionValue("L2steps"));
		}

		memm.readData();
		memm.finalizeFeatures();
		memm.sweep();
	}

}
