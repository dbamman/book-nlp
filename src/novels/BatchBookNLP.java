package novels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import novels.annotators.CharacterAnnotator;
import novels.annotators.CharacterFeatureAnnotator;
import novels.annotators.CoreferenceAnnotator;
import novels.annotators.PhraseAnnotator;
import novels.annotators.QuotationAnnotator;
import novels.annotators.SupersenseAnnotator;
import novels.annotators.SyntaxAnnotator;
import novels.util.PrintUtil;
import novels.util.Util;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import com.google.common.collect.Lists;

public class BatchBookNLP {

	private static final String animacyFile = "files/stanford/animate.unigrams.txt";
	private static final String genderFile = "files/stanford/namegender.combine.txt";
	private static final String femaleFile = "files/stanford/female.unigrams.txt";
	private static final String maleFile = "files/stanford/male.unigrams.txt";
	private static final String corefWeights = "files/coref.weights";

	public String weights = corefWeights;

	/**
	 * Annotate a book with characters, coreference and quotations
	 * 
	 * @param book
	 */
	public void process(Book book, File outputDirectory, String outputPrefix) {
		File charFile = new File(outputDirectory, outputPrefix + ".book");

		process(book);

		QuotationAnnotator quoteFinder = new QuotationAnnotator();
		quoteFinder.findQuotations(book);

		CharacterFeatureAnnotator featureAnno = new CharacterFeatureAnnotator();
		featureAnno.annotatePaths(book);
		PrintUtil.printBookJson(book, charFile);

	}


	public void process(Book book) {
		SyntaxAnnotator.setDependents(book);

		Dictionaries dicts = new Dictionaries();
		dicts.readAnimate(animacyFile, genderFile, maleFile, femaleFile);
		dicts.processHonorifics(book.tokens);

		CharacterAnnotator charFinder = new CharacterAnnotator();

		charFinder.findCharacters(book, dicts);
		charFinder.resolveCharacters(book, dicts);

		PhraseAnnotator phraseFinder = new PhraseAnnotator();
		phraseFinder.getPhrases(book, dicts);

		CoreferenceAnnotator coref = new CoreferenceAnnotator();
		coref.readWeights(weights);
		coref.resolvePronouns(book);
		charFinder.resolveRemainingGender(book);
	}

	public void dumpForAnnotation(Book book, File outputDirectory, String prefix) {
		File pronounCands = new File(outputDirectory, prefix + ".pronoun.cands");
		File quotes = new File(outputDirectory, prefix + ".quote.cands");

		CoreferenceAnnotator coref = new CoreferenceAnnotator();
		HashMap<Integer, HashSet<Integer>> cands = coref.getResolvable(book);
		PrintUtil.printPronounCandidates(pronounCands, book, cands);
		PrintUtil.printQuotes(quotes, book);

	}

	/*
	 * Batch process a file containing a list of filenames and the output
	 * directory they should be written to. 
	 * 
	 * ./runjava BatchBookNLP input_filename.txt output_directory/ 
	 * 
	 * input_filename.txt contains one book
	 * per line, and each line contains a unique identifier for a book along
	 * with its absolute path. The unique identifier is important since all
	 * files written for that book can be found under that identifier.
	 * 
	 * oliver_twist /path/to/oliver_twist.txt p_and_p
	 * /path/to/pride_and_prejudice.txt
	 * 
	 * 
	 */
	public static void main(String[] args) throws Exception {

		String filelist = args[0];
		String mainOutputDirectory = args[1];

		int c = 0;
		SyntaxAnnotator syntaxAnnotator = null;
		BatchBookNLP bookNLP = null;

		BufferedReader in1 = new BufferedReader(new InputStreamReader(new FileInputStream(filelist), "UTF-8"));
		String str1;

		while ((str1 = in1.readLine()) != null) {
			try {
				String[] parts = str1.split("\t");
				String identifier = parts[0];
				String filename = parts[1];
				String outputDirectory = mainOutputDirectory + "/" + identifier;
				String tokenFileString = mainOutputDirectory + "/" + identifier + "/" + identifier + ".tokens";

				String doc = filename;

				File tokenDirectory = new File(tokenFileString).getParentFile();
				tokenDirectory.mkdirs();

				File directory = new File(outputDirectory);
				directory.mkdirs();

				// generate or read tokens
				ArrayList<Token> tokens = null;
				String text = Util.readText(doc);
				text = Util.filterGutenberg(text);

				// Reload models every 5 books
				if (c % 5 == 0) {

					bookNLP = new BatchBookNLP();
					syntaxAnnotator = new SyntaxAnnotator();
				}

				tokens = syntaxAnnotator.process(text);
				
				SupersenseAnnotator supersenseAnnotator=new SupersenseAnnotator();
				supersenseAnnotator.process(tokens);
				
				Book book = new Book(tokens);

				bookNLP.weights = BatchBookNLP.corefWeights;

				book.id = identifier;
				bookNLP.process(book, directory, identifier);

				PrintUtil.printTokens(book, tokenFileString);
			} catch (Exception e) {
				System.out.println("problem with:\t" + str1.trim());
				e.printStackTrace();
			}
		}
		in1.close();
	}
}
