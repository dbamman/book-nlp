package novels.training;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class SupersenseEval {

	String L1vals;
	String devFile;

	/*
	 * Directory contains 1.train, 1.dev, 1.test
	 */
	public void test(String directory, String baseModelFile) {

		int i = 1;
		SupersenseTrain memm = new SupersenseTrain();
		memm.trainFile = directory + "/" + i + ".train";
		memm.readData();
		memm.finalizeFeatures();

		String[] parts=L1vals.split(":");
		double[] L1s=new double[parts.length];
		for (int l_idx=0; l_idx<parts.length; l_idx++) {

			Double d=Double.valueOf(parts[l_idx]);
			L1s[l_idx]=d;
			
		}
		
		String bestModelFile = "";
		double highScore = -1;
		double bestL1 = 0;
		double[] weights = null;

		for (double L1 : L1s) {
			String modelFile = String.format("%s.iter.%d.L2.%.3f.model",
					baseModelFile, i, L1);
			memm.modelFile = modelFile;

			if (L1 == L1s[0]) {
				weights = memm.train(L1);
			} else {
				weights = memm.train(L1, weights);
			}

			if (L1s.length != 1) {

				SupersenseTest test = new SupersenseTest();
				test.predFile = directory + "/" + i + ".dev";
				test.modelFile = modelFile;
				test.readModel();
				SupersenseEvaluationContainer eval = test.test();
				eval.print();
				double score = eval.macroF1();
				System.out.println(String.format("L1: %.3f, Macro F1: %.3f",
						L1, score));
				if (score > highScore) {
					highScore = score;
					bestModelFile = modelFile;
					bestL1 = L1;
				}
			} else {
				bestModelFile = modelFile;
			}
		}

		if (L1s.length != 1) {
			System.out.println(String.format("Best L1: %.3f, %.3f", bestL1, highScore));
		}

		SupersenseTest test = new SupersenseTest();
		test.predFile = directory + "/" + i + ".test";
		test.modelFile = bestModelFile;

		test.readModel();
		SupersenseEvaluationContainer eval = test.test();

		eval.print();

	}

	public static void main(String[] args) throws ParseException {

		SupersenseEval test = new SupersenseEval();

		String directory = "";
		String outputPrefix = "";
		Options options = new Options();
		options.addOption("d", true, "directory containing splits");
		options.addOption("w", true, "model output file prefix");

		options.addOption("L1vals", true, "colon-separated L1 values");

		CommandLine cmd = null;
		CommandLineParser parser = new BasicParser();
		cmd = parser.parse(options, args);

		if (cmd.hasOption("w")) {
			outputPrefix = cmd.getOptionValue("w");
		} else {
			System.out
					.println("You must specify an output file to write to with -w <file>");
			System.exit(1);
		}
		if (cmd.hasOption("d")) {
			directory = cmd.getOptionValue("d");
		} else {
			System.out
					.println("You must specify an input directory containing splits with -d <directory>");
			System.exit(1);
		}

		if (cmd.hasOption("L1vals")) {
			test.L1vals=cmd.getOptionValue("L1vals");
		}


		test.test(directory, outputPrefix);
	}
}
