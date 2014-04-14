package novels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import novels.entities.Antecedent;

import com.google.common.collect.Maps;

public class Book {
	
	public String id;
	
	public ArrayList<Token> tokens;
	// CharacterAnnotator
	public BookCharacter[] characters;
	// SyntaxAnnotator
	public HashMap<Integer, TreeSet<Integer>> dependents;
	// PhraseAnnotator.  Contains all animate NPs, Characters and Pronouns
	public TreeMap<Integer, Antecedent> animateEntities;
	// Maps tokens to the assigned character
	public TreeMap<Integer, Antecedent> tokenToCharacter;
	// List of quotations
	public ArrayList<Quotation> quotations;

	public Book(ArrayList<Token> tokens) {
		this.tokens=tokens;
		animateEntities=Maps.newTreeMap();
	}
	
	public JSONObject toJson() {
		JSONObject bookJson=new JSONObject();
		bookJson.put("id", id);
		JSONArray characterJson=new JSONArray();
		for (BookCharacter c : characters) {
			characterJson.add(c.toJson());
		}
		bookJson.put("characters", characterJson);
		return bookJson;
	}
}
