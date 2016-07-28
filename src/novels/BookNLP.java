package novels;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import novels.annotators.CharacterAnnotator;
import novels.annotators.CharacterFeatureAnnotator;
import novels.annotators.CoreferenceAnnotator;
import novels.annotators.PhraseAnnotator;
import novels.annotators.QuotationAnnotator;
import novels.annotators.SyntaxAnnotator;
import novels.util.PrintUtil;
import novels.util.Util;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

public class BookNLP {

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


		CharacterFeatureAnnotator featureAnno = new CharacterFeatureAnnotator();
		featureAnno.annotatePaths(book);
		PrintUtil.printBookJson(book, charFile);

	}

	public void process(Book book) {
		System.out.println("Setting Dependents");
		SyntaxAnnotator.setDependents(book);
		
		System.out.println("Adding Dictionary");
		Dictionaries dicts = new Dictionaries();
		dicts.readAnimate(animacyFile, genderFile, maleFile, femaleFile);
		dicts.processHonorifics(book.tokens);

		System.out.println("Annotating Chatacters");
		CharacterAnnotator charFinder = new CharacterAnnotator();

		charFinder.findCharacters(book, dicts);
		charFinder.resolveCharacters(book, dicts);
		
		System.out.println("Getting Phrases");
		PhraseAnnotator phraseFinder = new PhraseAnnotator();
		phraseFinder.getPhrases(book, dicts);
		
		System.out.println("Resolving Pronouns");
		CoreferenceAnnotator coref = new CoreferenceAnnotator();
		coref.readWeights(weights);
		coref.resolvePronouns(book);

		System.out.println("Setting Character IDs");
		SyntaxAnnotator.setCharacterIds(book);	


		QuotationAnnotator quoteFinder = new QuotationAnnotator();
		quoteFinder.findQuotations(book, dicts);
	}

	public void dumpForAnnotation(Book book, File outputDirectory, String prefix) {
		File pronounCands = new File(outputDirectory, prefix + ".pronoun.cands");
		File quotes = new File(outputDirectory, prefix + ".quote.cands");

		CoreferenceAnnotator coref = new CoreferenceAnnotator();
		HashMap<Integer, HashSet<Integer>> cands = coref.getResolvable(book);
		PrintUtil.printPronounCandidates(pronounCands, book, cands);
		PrintUtil.printQuotes(quotes, book);

	}

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.addOption("f", false, "force processing of text file");
		options.addOption("printHTML", false, "print HTML file for inspection");
		options.addOption("w", true, "coreference weight file");
		options.addOption("doc", true, "text document to process");
		options.addOption("tok", true, "processed text document");
		options.addOption("docId", true, "text document ID to process");
		options.addOption("p", true, "output directory");
		options.addOption("id", true, "book ID");
		options.addOption("d", false, "dump pronoun and quotes for annotation");

		CommandLine cmd = null;
		try {

			CommandLineParser parser = new BasicParser();
			cmd = parser.parse(options, args);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String outputDirectory = null;
		String prefix = "book.id";
		
		if (!cmd.hasOption("p")) {
			System.err.println("Specify output directory with -p <directory>");
			System.exit(1);
		} else {
			outputDirectory = cmd.getOptionValue("p");
		}

		if (cmd.hasOption("id")) {
			prefix = cmd.getOptionValue("id");
		}

		File directory = new File(outputDirectory);
		directory.mkdirs();

		String tokenFileString = null;
		if (cmd.hasOption("tok")) {
			tokenFileString = cmd.getOptionValue("tok");
			File tokenDirectory = new File(tokenFileString).getParentFile();
			tokenDirectory.mkdirs();
		} else {
			System.err.println("Specify token file with -tok <filename>");
			System.exit(1);
		}

		options.addOption("printHtml", false,
				"write HTML file with coreference links and speaker ID for inspection");

		BookNLP bookNLP = new BookNLP();
		// int docId = Integer.valueOf(cmd.getOptionValue("docId"));

		// generate or read tokens
		ArrayList<Token> tokens = null;
		File tokenFile = new File(tokenFileString);
		if (!tokenFile.exists() || cmd.hasOption("f")) {
			String doc = cmd.getOptionValue("doc");
			String text = Util.readText(doc);
			text = Util.filterGutenberg(text);
			SyntaxAnnotator syntaxAnnotator = new SyntaxAnnotator();
			tokens = syntaxAnnotator.process(text);
			System.out.println("Processing text");
		} else {
			if (tokenFile.exists()) {
				System.out.println(String.format("%s exists...",
						tokenFileString));
			}
			tokens = SyntaxAnnotator.readDoc(tokenFileString);
			System.out.println("Using preprocessed tokens");
		}

		Book book = new Book(tokens);
		
		
		if (cmd.hasOption("w")) {
			bookNLP.weights = cmd.getOptionValue("w");
			System.out.println(String.format("Using coref weights: ",
					bookNLP.weights));
		} else {
			bookNLP.weights = BookNLP.corefWeights;
			System.out.println("Using default coref weights");
		}

		book.id = prefix;
		bookNLP.process(book, directory, prefix);


		if (cmd.hasOption("d")) {
			System.out.println("Dumping for annotation");
			bookNLP.dumpForAnnotation(book, directory, prefix);
		}

		if (cmd.hasOption("printHTML")) {
			File htmlOutfile = new File(directory, prefix + ".html");
			PrintUtil.printWithLinksAndCorefAndQuotes(htmlOutfile, book);
		}
		// Print out tokens
		PrintUtil.printTokens(book, tokenFileString);

	}
}
