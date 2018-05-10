package novels.annotators;

import java.util.ArrayList;

import novels.Token;
import novels.supersense.SupersenseTagger;

public class SupersenseAnnotator {

	// add supersense annotations to already tagged/parsed tokens
	public void process(ArrayList<Token> tokens) {
		SupersenseTagger supersenseTagger = new SupersenseTagger();

		supersenseTagger.modelFile = "files/supersense/supersense.model";
		supersenseTagger.readModel();
		supersenseTagger.predictFromTokens(tokens);
		
	}

}