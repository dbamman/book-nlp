package novels.util;

import java.io.BufferedReader;
import java.io.File;
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

import novels.Book;
import novels.BookCharacter;
import novels.Quotation;
import novels.Token;
import novels.annotators.SyntaxAnnotator;
import novels.entities.Antecedent;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;

public class PrintUtil {

	public static String BLACK = "\033[0m";
	public static String RED = "\033[91m";

	public static void printCharacterTokenInfo(Book book) {
		for (int i = 0; i < book.tokens.size(); i++) {
			if (book.tokenToCharacter.containsKey(i)) {
				Antecedent ant = book.tokenToCharacter.get(i);
				Token head = ant.getHead(book);
				System.out.println(head.word + " " + head.tokenId + " "
						+ ant.getCharacterId());
				i = ant.getEnd() + 1;
			}
		}
	}

	public static void printTokens(Book book, String outFile) {
		OutputStreamWriter out = null;
		
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			out.write(Token.ORDER + "\n");
			for (Token anno : book.tokens) {
		//		if (book.tokenToCharacter.containsKey(anno.tokenId)) {
		//			anno.characterId=book.tokenToCharacter.get(anno.tokenId).getCharacterId();
		//		}
				out.write(anno + "\n");
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printBookJson(Book book, File outFile) {
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			out.write(book.toJson().toString());
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void printCharacterInfo(Book book, File outFile) {
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			for (BookCharacter character : book.characters) {
				out.write(character.toJson().toString() + "\n");
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void printWithLinksAndCorefAndQuotes(File outFile, Book book) {
		HashMap<Integer, Quotation> starts = Maps.newHashMap();
		HashMap<Integer, Quotation> ends = Maps.newHashMap();
		for (Quotation quote : book.quotations) {
			starts.put(quote.start, quote);
			ends.put(quote.end, quote);
		}

		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			out.write("<html><head><style> .bookquote {background-color:#FFDDFF; margin-top:1px; margin-bottom:1px; overflow:auto; display:inline; border-bottom: dotted; border-width: thin; }</style></head><body>");
			
			out.write("<h1>Characters</h1>");
			HashMap<BookCharacter, Integer> charCounts=Maps.newHashMap();
			for (BookCharacter character : book.characters) {
				charCounts.put(character, character.count);
			}
			ArrayList<Object> sorted=Util.sortHashMapByValue(charCounts);
			for (Object o : sorted) {
				BookCharacter character=(BookCharacter)o;
				out.write(String.format("%s<br />", character.findName()));
			}
			
			out.write("<h1>Text</h1>");
			
			int lastPar = -1;
			for (Token token : book.tokens) {
				if (token.p != lastPar) {
					out.write("<p/>");
				}
				lastPar = token.p;

				if (starts.containsKey(token.tokenId)) {
					Quotation quote = starts.get(token.tokenId);
					out.write("<div class=\"bookquote\">");
				}

				String head = null;
				if (token.coref != 0) {
					head = book.tokens.get(SyntaxAnnotator.getEndpoint(
							token.tokenId, book)).word;
					out.write(String.format("%s (<font color='blue'><b>%s</b></font>)%s",
							token.word, head,
							token.getWhiteSpaceAfter()));
				} else {
					out.write(String.format("%s%s",
							token.word,
							token.getWhiteSpaceAfter()));
				}
				if (ends.containsKey(token.tokenId)) {
					Quotation quote = ends.get(token.tokenId);
					String name = "unknown";
					if (quote.attributionId != 0) {
						//Antecedent ant = book.animateEntities
						//		.get(quote.attributionId);
						//name = ant.getString(book);
						Token tok = book.tokens.get(quote.attributionId);
						if (tok.characterId != -1)
							name = book.characters[tok.characterId].name;
						else
							name = tok.word;
						
					}
					Quotation endQuote = ends.get(token.tokenId);

					out.write(String.format(
							" (<font color='red'><b>%s</b></font>)</div>",
							name.trim()));
				}

			}
			
			
			
			out.write("</body></html>");
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		

	}

	
	public void printWithLinks(String infile, String outFile, Book book) {
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");

			HashSet<Integer> seen = Sets.newHashSet();
			try {
				BufferedReader in1 = new BufferedReader(new InputStreamReader(
						new FileInputStream(infile), "UTF-8"));
				String str1;

				while ((str1 = in1.readLine()) != null) {
					String[] parts = str1.trim().split("\t");
					int s = Integer.valueOf(parts[0]);
					seen.add(s);

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			int lastPar = -1;
			for (Token token : book.tokens) {
				if (token.p != lastPar) {
					out.write("<p/>");
				}
				lastPar = token.p;
				out.write(String.format("<a href='%s'>%s</a>%s", token.tokenId,
						token.word, token.getWhiteSpaceAfter()));
			}
			out.close();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void printWithLinksAndCoref(String outFile, Book book) {
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			int lastPar = -1;
			for (Token token : book.tokens) {
				if (token.p != lastPar) {
					out.write("<p/>");
				}
				lastPar = token.p;
				String head = null;
				if (token.coref != 0) {
					head = book.tokens.get(SyntaxAnnotator.getEndpoint(
							token.tokenId, book)).word;
					out.write(String.format("<a href='%s'>%s</a>(%s)%s",
							token.tokenId, token.word, head,
							token.getWhiteSpaceAfter()));
				} else {
					out.write(String.format("<a href='%s'>%s</a>%s",
							token.tokenId, token.word,
							token.getWhiteSpaceAfter()));
				}

			}
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void printQuotes(File outFile, Book book) {
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			///*
			for (Quotation quote : book.quotations) {
				String guessString = "";
				int characterId = -1;
				
					
				if (quote.attributionId != 0) {
					Token token = book.tokens.get(quote.attributionId);						
					guessString = token.word;										
					characterId = token.characterId;
				}

				
				
				out.write(String.format("%s\t%s\t%s\t%d\t%s\t%s\t%s\t%d\n", book.id,
						quote.start, quote.end, quote.sentenceId, 0, quote.attributionId,
						guessString, characterId));

			}
			//*/
			/*
			for (Quotation quote : book.quotations) {
				String guessString = "";
				if (quote.attributionId != 0) {
					Token token = book.tokens.get(quote.attributionId);
					guessString = token.word;
				}
				out.write(String.format("%s\t%s\t%s\t%s\t%s\t%s\n", book.id,
						quote.start, quote.end, 0, quote.attributionId,
						guessString));
			}
			*/
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void printPronounCandidates(File outFile, Book book,
			HashMap<Integer, HashSet<Integer>> cands) {
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			for (Integer i : cands.keySet()) {
				Token token = book.tokens.get(i);
				if (token.coref != 0) {
					out.write(i + "\t");
					Token head = book.tokens.get(token.coref);
					out.write(String.format("%s\t%d\t%s", token.coref, head.characterId, head.word));
					out.write("\n");
				} 

//				out.write("\t");
//				for (Integer c : cands.get(i)) {
//					out.write(c + " ");
//				}
//				out.write("\n");
			}
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static String getSpan(int start, int end, Book book) {
		if (start < 0) {
			start = 0;
		}
		if (end > book.tokens.size()) {
			end=book.tokens.size();
		}
		StringBuffer buffer = new StringBuffer();
		
		for (int i = start; i < end; i++) {
			buffer.append(book.tokens.get(i).word + " ");
		}
		return buffer.toString();
	}

	public static String getColoredSpan(int main, int start, int end,
			ArrayList<Antecedent> cands, Book book) {
		StringBuffer buffer = new StringBuffer();
		HashSet<Integer> inds = Sets.newHashSet();
		for (Antecedent cand : cands) {
			inds.add(cand.getHead(book).tokenId);
		}
		inds.add(main);
		for (int i = start; i < end; i++) {
			if (inds.contains(i)) {
				buffer.append(BLACK);
			}
			buffer.append(book.tokens.get(i).word);

			if (inds.contains(i)) {
				buffer.append("/" + i + " ");
				buffer.append(RED);
			} else {
				buffer.append(" ");
			}
		}
		return buffer.toString();
	}
}
