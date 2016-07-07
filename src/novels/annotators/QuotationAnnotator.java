package novels.annotators;

import java.util.Map;
import java.util.TreeMap;

import novels.Book;
import novels.Quotation;
import novels.Token;
import novels.entities.PronounAntecedent;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;
/**
 * Attribute quotes to speakers
 * 
 * @author dbamman
 * 
 */
public class QuotationAnnotator {

	public void setQuotes(Book book) {
		boolean inQuote=false;
		int lastPar=-1;
		int lastStart=-1;
		for (int i=0; i<book.tokens.size(); i++) {
			Token token=book.tokens.get(i);
			if (token.p != lastPar) {
				inQuote=false;
			}
			token.quotation=inQuote;
			if (token.word.equals("''") || token.word.equals("``")) {
				if (inQuote) {
					Token start=book.tokens.get(lastStart);
					start.lemma="``";
				} else {
					lastStart=i;
				}
				inQuote = !inQuote;
			}
			lastPar=token.p;
			
		}
	}
	public void findQuotations(Book book) {

		setQuotes(book);
		// Add "I" as possible speaker
		for (Token token : book.tokens) {
			if (token.pos.startsWith("PRP") && (token.lemma.equals("I"))) {
				PronounAntecedent pronoun = new PronounAntecedent(
						token.tokenId, 0);
				book.animateEntities.put(token.tokenId, pronoun);
			}
		}

		TreeMap<Integer, Quotation> quotations;

		int start = 0;
		int end = 0;

		quotations = Maps.newTreeMap();

		// find all quotations (delimited by `` '')
		for (Token token : book.tokens) {
			if (token.lemma.equals("``")) {
				start = token.tokenId;
			} else if (token.lemma.equals("''")) {
				end = token.tokenId;

				if (start > -1) {
					Quotation quote = new Quotation(start, end,
							token.sentenceID);
					quote.p = token.p;
					quotations.put(start, quote);
				}
				start = -1;

			}
		}


		// // combine quotations than span multiple sentences
		// HashSet<Integer> rem = Sets.newHashSet();
		// for (Quotation quote : quotations.values()) {
		// Map.Entry<Integer, Quotation> map = quotations
		// .ceilingEntry(quote.sentenceId + 1);
		// if (map != null) {
		// Quotation next = map.getValue();
		// Token endTok = book.tokens.get(quote.end);
		// Token startTok = book.tokens.get(next.start);
		// if (endTok.sentenceID == startTok.sentenceID) {
		// next.start = quote.start;
		// rem.add(quote.sentenceId);
		// }
		//
		// }
		// }
		// for (Integer i : rem) {
		// quotations.remove(i);
		// }

		// find speakers within the span of the sentence itself, e.g.: `` ...
		// ,'' said Darcy , `` ... ''
		
		//for (Quotation quote : quotations.values()) {
		
		//	for (int i = quote.start; i <= quote.end; i++) {
		//		Token token = book.tokens.get(i);
		//		if (book.animateEntities.containsKey(i) && token.quotation == false && !token.pos.equals("PRP$")) {
		//			quote.attributionId = i;
		//			break;
		//		}
		//		
		//	}
		//}

		// span left until the previous sentence

		for (Quotation quote : quotations.values()) {
		
		
			if (quote.attributionId != 0) {
			
				continue;
			}
			
						

			int quoteSentence = book.tokens.get(quote.start).sentenceID;
			int i = quote.start;
			int currentSentence = quoteSentence;
			while (quoteSentence == currentSentence && i >= 0) {

				Token token = book.tokens.get(i);
				currentSentence = token.sentenceID;
				if (book.animateEntities.containsKey(i)
						&& token.quotation == false
						&& !token.pos.equals("PRP$")) {
					quote.attributionId = i;
					break;
				}
				i--;
			}
		}

		// span right until the next sentence
		for (Quotation quote : quotations.values()) {
		

			if (quote.attributionId != 0) {		

				continue;
			}


			int quoteSentence = book.tokens.get(quote.end).sentenceID;
			int i = quote.end;
			int currentSentence = quoteSentence;
			while (quoteSentence == currentSentence && i < book.tokens.size()) {

				Token token = book.tokens.get(i);
				currentSentence = token.sentenceID;
				if (book.animateEntities.containsKey(i)
						&& token.quotation == false
						&& !token.pos.equals("PRP$")) {
					quote.attributionId = i;
					break;
				}
				i++;
			}
		}

		// span left until the previous quote or a hard punctuation
		for (Quotation quote : quotations.values()) {

			if (quote.attributionId != 0) {
			

				continue;
			}


			Map.Entry<Integer, Quotation> map = quotations
					.floorEntry(quote.start - 1);
			if (map != null) {
				Quotation previous = map.getValue();
				for (int i = quote.start; i > previous.end && i >= 0; i--) {
					Token token = book.tokens.get(i);
					if (token.word.matches("[\\.!;\\?]")) {
						break;
					}
					if (book.animateEntities.containsKey(i)
							&& token.quotation == false
							&& !token.pos.equals("PRP$")) {
						quote.attributionId = i;
						break;
					}					
			/*
					if (token.pos.startsWith("NN") && token.deprel == "nsubj" && token.quotation == false && token.ner == "PERSON"){
						quote.attributionId = i;
						break;
					}
					if (token.pos.startsWith("NN") && token.deprel == "nsubj" && token.quotation == false){
						quote.attributionId = i;
						break;
					}
					if (token.deprel == "nsubj" && token.quotation == false){
						quote.attributionId = i;
						break;
					}
			*/		

				}
			}
		}

		// span right until the next quote or a hard punctuation
		for (Quotation quote : quotations.values()) {


			if (quote.attributionId != 0) {			

				continue;
			}


			Map.Entry<Integer, Quotation> map = quotations
					.ceilingEntry(quote.start + 1);
			if (map != null) {
				Quotation next = map.getValue();
				for (int i = quote.end; i < next.start && i < book.tokens.size(); i++) {
					Token token = book.tokens.get(i);
					if (token.word.matches("[\\.!;:\\?]")) {
						break;
					}
					if (book.animateEntities.containsKey(i)
							&& token.quotation == false
							&& !token.pos.equals("PRP$")) {
						quote.attributionId = i;
						break;
					}

				}
			}
		}
		//scanning into previous sentence until punctuation
	/*	
		for (Quotation quote : quotations.values()) {

			if (quote.attributionId != 0) {
			

				continue;
			}

			//System.out.println(quote.start + " \n");
			Map.Entry<Integer, Quotation> map = quotations
					.floorEntry(quote.start - 1);
			if (map != null) {
				int punctCount = 0;
				Quotation previous = map.getValue();
				for (int i = quote.start; i > previous.end && i >= 0; i--) {
					Token token = book.tokens.get(i);
					if (token.word.matches("[\\.!;\\?]")) {
						punctCount++;
						//System.out.print("\t" + token.word);
						if (punctCount == 2){
						//	System.out.print("\n");
							break;
						}
					}
					//System.out.println(token.pos + " " + token.deprel + " " + token.quotation + " " + token.ner + " " + i);
					if (token.pos.startsWith("NN") && token.deprel.equals("nsubj") && token.quotation == false && token.ner.equals("PERSON")){
						System.out.println(token.pos + " " + token.deprel + " " + token.quotation + " " + token.ner + " " + i);
						quote.attributionId = i;
						break;
					}
					else if (token.pos.startsWith("NN") && token.deprel.equals("nsubj") && token.quotation == false){
						System.out.println(token.pos + " " + token.deprel + " " + token.quotation + " " + token.ner + " " + i);
						quote.attributionId = i;
						break;
					}
					else if (token.deprel.equals("nsubj") && token.quotation == false){
						System.out.println(token.pos + " " + token.deprel + " " + token.quotation + " " + token.ner + " " + i);
						quote.attributionId = i;
						break;
					}
					else
						System.out.println("no");

				}
			}
		}
	*/
		Map<Integer, Integer> attribId = new HashMap<Integer, Integer>();


		for (Quotation quote : quotations.values()) {
			
			if (attribId.get(quote.p) == null){
				if (quote.attributionId != 0)
					attribId.put(quote.p, quote.attributionId);
			}else	
				quote.attributionId = attribId.get(quote.p);

		}
	
		book.quotations = Lists.newArrayList();
		for (Quotation quote : quotations.values()) {
			book.quotations.add(quote);
		}
		
	}

}
