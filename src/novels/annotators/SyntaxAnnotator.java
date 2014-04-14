package novels.annotators;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import novels.Token;
import novels.Book;

import org.maltparser.MaltParserService;
import org.maltparser.core.exception.MaltChainedException;
import org.maltparser.core.symbol.SymbolTable;
import org.maltparser.core.syntaxgraph.DependencyStructure;
import org.maltparser.core.syntaxgraph.edge.Edge;
import org.maltparser.core.syntaxgraph.node.DependencyNode;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class SyntaxAnnotator {

	public static String MALT_DIR = "files/malt/";
	public static String MALT = "engmalt.linear-1.7.mco";
	public int id = 0;

	public static String getAgent(Book book, int index) {
		Token token = book.tokens.get(index);
		if (token.head != -1) {
			Token head = book.tokens.get(token.head);
			// collapse conj (e.g. "He and Darcy went ..." where syntax = darcy
			// ->/conj He, he ->/nsubj went)
			if (token.deprel.equals("conj") && head.head != -1) {
				token = head;
				head = book.tokens.get(head.head);
			}

			if (head.pos.startsWith("V")
					&& (token.deprel.equals("nsubj") || token.deprel
							.equals("agent"))) {
				TreeSet<Integer> deps = book.dependents.get(head.tokenId);

				/*
				 * for (int d : deps) { Token dep = book.tokens.get(d); if
				 * (dep.deprel.equals("dobj")) { String term=dep.lemma; if
				 * (book.tokenToCharacter.containsKey(dep.tokenId)) { term="[" +
				 * book
				 * .characters[book.tokenToCharacter.get(dep.tokenId).getCharacterId
				 * ()].name + "]"; } return head.lemma + "_" + term; } }
				 */
				return head.word;
			}
			if (head.pos.equals("MD")
					&& (token.deprel.equals("nsubj") || token.deprel
							.equals("agent"))) {
				boolean modal = false;
				TreeSet<Integer> deps = book.dependents.get(head.tokenId);

				String deplemma = null;
				for (int d : deps) {
					Token dep = book.tokens.get(d);
					if (dep.pos.startsWith("V")) {
						modal = true;
						deplemma = dep.word;
					}
				}
				if (modal) {
					return deplemma;
				}
			}
		}
		return null;
	}

	public static String getPatient(Book book, int index) {
		Token token = book.tokens.get(index);
		if (token.head != -1) {
			Token head = book.tokens.get(token.head);
			// collapse conj (e.g. "He and Darcy went ..." where syntax = darcy
			// ->/conj He, he ->/nsubj went)
			if (token.deprel.equals("conj") && head.head != -1) {
				token = head;
				head = book.tokens.get(head.head);
			}
			if (head.pos.startsWith("V")
					&& (token.deprel.equals("dobj") || token.deprel
							.equals("nsubjpass"))) {
				return head.word;
			}
		}
		return null;
	}

	public static String getMods(Book book, int index) {
		Token token = book.tokens.get(index);
		if (token.head != -1) {
			Token head = book.tokens.get(token.head);
			// collapse conj (e.g. "He and Darcy went ..." where syntax = darcy
			// ->/conj He, he ->/nsubj went)
			if (token.deprel.equals("conj") && head.head != -1) {
				token = head;
				head = book.tokens.get(head.head);
			}
			if ((head.pos.startsWith("NN") || head.pos.startsWith("JJ"))
					&& token.deprel.equals("nsubj")) {
				TreeSet<Integer> deps = book.dependents.get(head.tokenId);
				boolean predicate = false;
				for (int d : deps) {
					Token dep = book.tokens.get(d);
					if (dep.lemma.equals("be")) {
						predicate = true;
					}
				}
				if (predicate) {
					return head.word;
				}
			}
		}
		return null;
	}

	public static String getPoss(Book book, int index) {
		Token token = book.tokens.get(index);
		if (token.head != -1) {
			Token head = book.tokens.get(token.head);
			// collapse conj (e.g. "He and Darcy went ..." where syntax = darcy
			// ->/conj He, he ->/nsubj went)
			if (token.deprel.equals("conj") && head.head != -1) {
				token = head;
				head = book.tokens.get(head.head);
			}
			if (token.deprel.equals("poss")) {
				return head.word;
			}
		}
		return null;
	}

	public static ArrayList<Token> readDocs(List<String> infiles) {
		ArrayList<Token> tokens = new ArrayList<Token>();
		int parOffset=0;
		int sentenceOffset=0;
		int tokenOffset=0;
		try {

			for (String infile : infiles) {
				BufferedReader in1 = new BufferedReader(new InputStreamReader(
						new FileInputStream(infile), "UTF-8"));
				String str1;

				while ((str1 = in1.readLine()) != null) {
					Token token = new Token(str1.trim(), parOffset, sentenceOffset, tokenOffset);
					tokens.add(token);
					
				}
				Token lastToken=tokens.get(tokens.size()-1);
				parOffset=lastToken.p+1;
				sentenceOffset=lastToken.sentenceID+1;
				tokenOffset=lastToken.tokenId+1;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tokens;
	}

	public static ArrayList<Token> readDoc(String infile) {
		ArrayList<Token> tokens = new ArrayList<Token>();
		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(infile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				Token token = new Token(str1.trim());
				tokens.add(token);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tokens;
	}

	public static HashSet<Integer> getRecursiveDeps(int index, Book book) {
		HashSet<Integer> deps = new HashSet<Integer>();
		if (book.dependents.containsKey(index)) {
			TreeSet<Integer> nextDeps = book.dependents.get(index);
			for (int i : nextDeps) {
				deps.addAll(getRecursiveDeps(i, book));
			}
		}
		deps.add(index);
		return deps;
	}

	public static void setDependents(Book book) {
		book.dependents = new HashMap<Integer, TreeSet<Integer>>();
		for (Token tok : book.tokens) {
			int head = tok.head;
			if (head != -1) {
				TreeSet<Integer> deps = null;
				if (book.dependents.containsKey(head)) {
					deps = book.dependents.get(head);
				} else {
					deps = new TreeSet<Integer>();
				}
				deps.add(tok.tokenId);
				book.dependents.put(head, deps);

			}
		}
	}

	MaltParserService service;
	StanfordCoreNLP pipeline;
	int count;
	boolean stateLess;

	public void initialize() {
		try {

			if (!stateLess) {
				Properties props = new Properties();
				props.put("annotators", "tokenize, ssplit, pos, lemma, ner");

				pipeline = new StanfordCoreNLP(props);

				service = new MaltParserService(id);

				System.out.println("Creating maltparser with id " + id);
				service.initializeParserModel(String.format(
						"-c %s -m parse -w %s -lfi parser.log", MALT, MALT_DIR));
			}
		} catch (MaltChainedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<Token> process(String doc)
			throws Exception {

		if (service == null || count % 10 == 0) {
			initialize();
		}
		count++;

		ArrayList<Token> allWords = new ArrayList<Token>();

		Annotation document = new Annotation(doc);

		System.err.println("Tagging and parsing...");
		pipeline.annotate(document);

		int s = 0;
		int t = 0;
		int p = 0;

		boolean quotation = false;

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int totalSentences = sentences.size() - 1;

		for (CoreMap sentence : sentences) {
			if (s % 100 == 0 || s == totalSentences) {
				double ratio = ((double) s) / totalSentences;
				System.err.print(String.format(
						"\t%.3f (%s out of %s) processed\r", ratio, s,
						totalSentences));
			}

			int id = 1;
			String[] parseTokens = new String[sentence.get(
					TokensAnnotation.class).size()];

			ArrayList<Token> annos = new ArrayList<Token>();

			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {

				String word = token.get(TextAnnotation.class);
				String pos = token.get(PartOfSpeechAnnotation.class);
				String lemma = token.get(LemmaAnnotation.class);
				String ne = token.get(NamedEntityTagAnnotation.class);
				int beginOffset = token.beginPosition();
				int endOffset = token.endPosition();
				String whitespaceAfter = token.after();
				String original = token.originalText();

				if (word.equals("``")) {
					quotation = true;
				}

				Token anno = new Token();
				anno.original = original;
				anno.word = word;
				anno.pos = pos;
				anno.lemma = lemma;
				anno.ner = ne;
				anno.sentenceID = s;
				anno.tokenId = t;
				anno.beginOffset = beginOffset;
				anno.endOffset = endOffset;
				anno.quotation = quotation;
				anno.setWhitespaceAfter(whitespaceAfter);
				anno.p = p;
				annos.add(anno);
				String parseToken = String.format("%d\t%s\t%s\t%s\t%s\t%s", id,
						word, lemma, pos, pos, "_");
				parseTokens[id - 1] = parseToken;
				id++;
				allWords.add(anno);
				t++;
				whitespaceAfter = anno.whitespaceAfter;

				if (word.equals("''")) {
					quotation = false;
				}

				if (token.after().matches("\\n{2,}")) {
					p++;
				}
			}
			s++;
			DependencyStructure graph = service.parse(parseTokens);
			SymbolTable symboltable = graph.getSymbolTables().getSymbolTable(
					"DEPREL");
			SortedSet<Integer> depInts = graph.getDependencyIndices();
			for (int dint : depInts) {
				if (dint > 0) {
					// annos offset = 0, maltparser offset = 1
					Token anno = annos.get(dint - 1);
					DependencyNode node = graph.getDependencyNode(dint);
					DependencyNode head = node.getHead();

					int headIndex = 0;
					String label = "null";
					if (head != null) {
						Edge edge = node.getHeadEdge();
						headIndex = head.getIndex();
						label = edge.getLabelSymbol(symboltable);

					}

					int globalHead = -1;

					if (headIndex > 0) {
						int offset = headIndex - dint;
						globalHead = offset + anno.tokenId;
					}

					anno.head = globalHead;
					anno.deprel = label;
					annos.set(dint - 1, anno);
				}
			}

		}
		p++;

		System.err.println();

		service.terminateParserModel();

		return allWords;

	}

	public static String getPath(int s, int t, ArrayList<Token> allWords) {

		boolean collapseConj = true;

		Token source = allWords.get(s);
		Token target = allWords.get(t);
		int sourceHead = s;
		Token parent = null;

		boolean withinSentenceMatch = false;
		int withinSentenceCommon = -1;
		ArrayList<Integer> up = new ArrayList<Integer>();
		ArrayList<String> ups = new ArrayList<String>();
		while (sourceHead != -1) {
			parent = allWords.get(sourceHead);
			sourceHead = parent.head;
			up.add(parent.tokenId);
			ups.add(parent.deprel);
		}

		sourceHead = t;
		parent = null;

		ArrayList<Integer> down = new ArrayList<Integer>();
		ArrayList<String> downs = new ArrayList<String>();

		while (sourceHead != -1) {
			parent = allWords.get(sourceHead);
			sourceHead = parent.head;
			down.add(parent.tokenId);
			downs.add(parent.deprel);

			if (up.contains(sourceHead) && sourceHead != -1) {
				withinSentenceMatch = true;
				withinSentenceCommon = sourceHead;
				break;
			}

		}

		String chain = "";
		if (withinSentenceMatch) {
			for (int i = 0; i < up.size(); i++) {
				int index = up.get(i);
				String deprel = ups.get(i);
				chain += "^" + deprel;
				if (index == withinSentenceCommon) {
					break;
				}
			}

			for (int i = down.size() - 1; i >= 0; i--) {
				String deprel = downs.get(i);
				chain += ">" + deprel;
			}
		} else {
			for (int i = 0; i < up.size(); i++) {
				String deprel = ups.get(i);
				chain += "^" + deprel;
			}

			int jump = target.sentenceID - source.sentenceID;
			for (int i = 0; i < jump; i++) {
				chain += "=root";
			}

			for (int i = down.size() - 1; i >= 0; i--) {
				String deprel = downs.get(i);
				chain += ">" + deprel;
			}
		}

		if (collapseConj) {
			chain = chain.replaceAll("^conj", "");
			chain = chain.replaceAll(">conj", "");
		}
		return chain;
	}

	public static int getEndpoint(int i, Book book) {
		Token token = book.tokens.get(i);
		int corefHead=-1;
		int head = token.coref;

		if (token.coref != 0) {
			corefHead = token.coref;
			int hops=0;
			if (corefHead != 0) {
				head=corefHead;
			}
			while (corefHead != 0) {
				hops++;
				if (hops > 100) {
					break;
				}
				Token tokenHead = book.tokens.get(corefHead);

				if (tokenHead.coref == corefHead) {
					break;
				}
				corefHead = tokenHead.coref;
				if (corefHead != 0) {
					head=corefHead;
				}
			}
				
		}
		return head;
	}

}