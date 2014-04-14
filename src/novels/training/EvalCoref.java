package novels.training;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import novels.Book;
import novels.BookNLP;
import novels.Token;
import novels.annotators.CoreferenceAnnotator;
import novels.annotators.SyntaxAnnotator;
import novels.entities.Antecedent;
import novels.util.Util;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class EvalCoref {

	CoreferenceAnnotator coref;
	double L1 = 2;
	double L2 = .1;

	public EvalCoref() {
		coref = new CoreferenceAnnotator();
	}

	public class Acc {
		int correct;
		int total;
		double accuracy;
	}
	public Acc evaluate(HashMap<String, Integer> testValues, HashMap<String, Book> books) {
		int correct = 0;
		int total = 0;
		for (String key : testValues.keySet()) {
		//	System.out.println(key);
			String[] cols = key.split("\\.");
			String bookId = cols[0];
			int tokenId = Integer.valueOf(cols[1]);
			int truth = testValues.get(key);
			Book book = books.get(bookId);
			int predicted = process(book, book.tokens.get(tokenId));
			//System.out.println(key + "\t" + truth + "\t" + predicted);
			if (truth == predicted) {
				correct++;
			}
			total++;
		}
		double accuracy = ((double) correct) / total;
		System.out.println(String.format("Accuracy: %.3f (%d/%d)", accuracy,
				correct, total));
		Acc acc=new Acc();
		acc.correct=correct;
		acc.total=total;
		acc.accuracy=accuracy;
		return acc;

	}

	public int process(Book book, Token token) {
		HashMap<Antecedent, Double> scores = Maps.newHashMap();
		ArrayList<Antecedent> candidates = coref.getCandidates(token.tokenId,
				book);
		for (Antecedent candidate : candidates) {
			double score = coref.score(token, candidate, book);
			scores.put(candidate, score);
		}

		ArrayList<Object> sorted = Util.sortHashMapByValue(scores);
		if (sorted.size() > 0) {
			Antecedent winner = (Antecedent) sorted.get(0);
			return winner.getHead(book).tokenId;
		}
		return -1;
	}

	public static void main(String[] args) {
		EvalCoref eval = new EvalCoref();

		Options options = new Options();
		options.addOption("training", true, "training data");
		options.addOption("o", true,
				"output file to save the trained weights to");
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
			eval.L2 = Double.valueOf(cmd.getOptionValue("l2"));
		}
		if (cmd.hasOption("l1")) {
			eval.L1 = Double.valueOf(cmd.getOptionValue("l1"));
		}

		String paths = "coref/docPaths.txt";

		TrainCoref trainCoref=new TrainCoref();
		HashMap<String, String> docPaths = Util.readDocPaths(paths);
		HashSet<String> activeBooks = trainCoref.readTrainingData(cmd
				.getOptionValue("training"));
		File outputWeights = new File(cmd.getOptionValue("o"));

		ArrayList<Book> books = Lists.newArrayList();
		HashMap<String, Book> bookIDToBook = Maps.newHashMap();

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
			bookIDToBook.put(bookId, book);
		}

		eval.tenFoldTest(10, bookIDToBook, trainCoref);

	}

	public void tenFoldTest(int folds, HashMap<String, Book> bookMap, TrainCoref initialTrainCoref) {
		ArrayList<String> allData = new ArrayList<String>(
				initialTrainCoref.training.keySet());
		
		ArrayList<Book> books=Lists.newArrayList();
		books.addAll(bookMap.values());
		HashMap<String, Integer> completeTraining=Maps.newHashMap();
		completeTraining.putAll(initialTrainCoref.training);
			
		Collections.shuffle(allData);
		int size=allData.size()/folds;
		ArrayList<String>[] buckets=new ArrayList[folds];
		int last=0;
		int next=last+size;
		for (int i=0; i<folds; i++) {
			buckets[i]=Lists.newArrayList();
			for (int j=last; j<next && j<allData.size(); j++) {
				buckets[i].add(allData.get(j));
			}
			last=next;
			next=last+size;
		}
		
		int total=0;
		int correct=0;
		for (int i=0; i<folds; i++) {
			
			TrainCoref trainCoref=new TrainCoref();
			trainCoref.L1=L1;
			trainCoref.L2=L2;

			
			System.out.println("FOLD " + i);
			ArrayList<String> train=Lists.newArrayList();
			ArrayList<String> test=Lists.newArrayList();
			for (int j=0; j<buckets.length; j++) {
				if (i == j) {
					test.addAll(buckets[j]);
				} else {
					train.addAll(buckets[j]);
				}
			}
			trainCoref.training=Maps.newHashMap();
			for (int j=0; j<train.size(); j++) {
				String key=train.get(j);
				trainCoref.training.put(key, completeTraining.get(key));
			}
			HashMap<String, Integer> testValues=Maps.newHashMap();
			for (int j=0; j<test.size(); j++) {
				String key=test.get(j);
				testValues.put(key, completeTraining.get(key));
			}
			File outputWeights=new File("/tmp/weights");
			trainCoref.train(books, coref, outputWeights);
			coref.readWeights(outputWeights.getAbsolutePath());
			Acc acc=evaluate(testValues, bookMap);
			correct+=acc.correct;
			total+=acc.total;
			
		}
		double totalAccuracy=((double) correct)/total;
		System.out.println(String.format("Total accuracy: %.5f (%d/%d)", totalAccuracy, correct, total));
	}

}
