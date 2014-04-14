package novels.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import novels.Book;
import novels.BookNLP;
import novels.Token;
import novels.annotators.CoreferenceAnnotator;
import novels.annotators.SyntaxAnnotator;
import novels.entities.Antecedent;
import novels.optimization.SparseRegression;
import novels.util.PrintUtil;
import novels.util.Util;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.stanford.math.primitivelib.autogen.matrix.DoubleSparseVector;

public class TrainCoref {

	BookNLP booknlp;
	HashMap<String, Integer> training;

	HashMap<String, Integer> featureRegistry;
	String[] reverseFeatures;
	
	double L1 = 2;
	double L2 = .1;

	public TrainCoref() {
		featureRegistry = Maps.newHashMap();
	}

	public HashSet<String> readTrainingData(String infile) {
		training = Maps.newHashMap();
		HashSet<String> activeDocs = Sets.newHashSet();
		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(infile), "UTF-8"));
			String str1;
			while ((str1 = in1.readLine()) != null) {
				String[] parts = str1.trim().split("\t");
				String docId = parts[0];
				activeDocs.add(docId);
				String pronoun = parts[1];
				int ant = Integer.valueOf(parts[2]);
				String key = docId + "." + pronoun;
				training.put(key, ant);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return activeDocs;
	}

	public void train(ArrayList<Book> books, CoreferenceAnnotator coref,
			File outputWeights) {

		int curId = 0;
		
		System.err.println(String.format("L1 = %s, L2 = %s", L1, L2));

		ArrayList<DoubleSparseVector> train = new ArrayList<DoubleSparseVector>();
		ArrayList<Integer> response = new ArrayList<Integer>();

		// set features
		for (Book book : books) {
			for (Token token : book.tokens) {
				String key = book.id + "." + token.tokenId;
				if (training.containsKey(key)) {

					int truth = training.get(key);
					ArrayList<Antecedent> candidates = coref.getCandidates(
							token.tokenId, book);
					boolean containsTruth = false;
					for (Antecedent candidate : candidates) {
						Token head = candidate.getHead(book);
						if (head.tokenId == truth) {
							containsTruth = true;
						}
					}
					if (containsTruth) {
						for (Antecedent candidate : candidates) {
							HashMap<String, Object> features = coref
									.getFeatures(token, candidate, book);
							for (String feat : features.keySet()) {
								if (!featureRegistry.containsKey(feat)) {
									featureRegistry.put(feat, curId++);
								}
							}
						}
					}
				}
			}
		}
		reverseFeatures = new String[featureRegistry.size()];
		for (String feat : featureRegistry.keySet()) {
			reverseFeatures[featureRegistry.get(feat)] = feat;
		}

		for (Book book : books) {
			for (Token token : book.tokens) {
				String key = book.id + "." + token.tokenId;

				if (training.containsKey(key)) {

					int truth = training.get(key);
					ArrayList<Antecedent> candidates = coref.getCandidates(
							token.tokenId, book);
					String path = PrintUtil.getSpan(token.tokenId - 100,
							token.tokenId + 1, book);
					boolean containsTruth = false;
					for (Antecedent candidate : candidates) {
						// double score = coref.score(token, candidate, book);
						Token head = candidate.getHead(book);
						if (head.tokenId == truth) {
							containsTruth = true;
						}
						// System.out.println(String.format("%s, %s %s",
						// head.word,
						// score, containsTruth));
					}

					if (containsTruth) {
						for (Antecedent candidate : candidates) {
							HashMap<String, Object> features = coref
									.getFeatures(token, candidate, book);
							Token head = candidate.getHead(book);
							DoubleSparseVector feats = new DoubleSparseVector(
									featureRegistry.size());
							for (String feat : features.keySet()) {
								int featId = featureRegistry.get(feat);
								// System.out.println(feat + "\t" + featId);
								double val = (double) ((Integer) features
										.get(feat));
								feats.set(featId, val);
							}
							train.add(feats);
							if (head.tokenId == truth) {
								response.add(1);
							} else {
								response.add(0);
							}
						}
					}
				}
			}
		}

		SparseRegression lr = new SparseRegression(train, response, L1,
				L2, featureRegistry.size());
		double[] gradient = lr.regress();
		// System.out.println(Arrays.toString(gradient));
		HashMap<String, Double> weights = Maps.newHashMap();
		for (int i = 0; i < gradient.length; i++) {
			weights.put(reverseFeatures[i], gradient[i]);
		}

		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outputWeights), "UTF-8");
			ArrayList<Object> sorted = Util.sortHashMapByValue(weights);
			for (Object o : sorted) {
				String s = (String) o;
				double w = weights.get(s);
				if (w != 0) {
					out.write(s + "\t" + w + "\n");
				}
			}

			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		TrainCoref trainCoref = new TrainCoref();

		Options options = new Options();
		options.addOption("training", true, "training data");
		options.addOption("o", true, "output file to save the trained weights to");
		options.addOption("l1", true, "L1 regularization parameter");
		options.addOption("l2", true, "L2 regularization parameter");

		CommandLine cmd = null;
		try {
			CommandLineParser parser = new BasicParser();
			cmd = parser.parse(options, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (cmd.hasOption("l2")) {
			trainCoref.L2=Double.valueOf(cmd.getOptionValue("l2"));
		}
		if (cmd.hasOption("l1")) {
			trainCoref.L1=Double.valueOf(cmd.getOptionValue("l1"));
		}
		
		
		String paths = "coref/docPaths.txt";

		HashMap<String, String> docPaths = Util.readDocPaths(paths);
		HashSet<String> activeBooks = trainCoref.readTrainingData(cmd.getOptionValue("training"));
		File outputWeights = new File(cmd.getOptionValue("o"));

		ArrayList<Book> books = Lists.newArrayList();

		for (String bookId : activeBooks) {
			System.out.println(String.format("Reading #%s: %s", bookId,
					docPaths.get(bookId)));
			ArrayList<Token> tokens = SyntaxAnnotator.readDoc(docPaths
					.get(bookId));
			Book book = new Book(tokens);
			book.id = bookId;

			BookNLP bookNLP = new BookNLP();
			bookNLP.process(book);
			books.add(book);
		}

		CoreferenceAnnotator coreference = new CoreferenceAnnotator();
		trainCoref.train(books, coreference, outputWeights);

	}
}
