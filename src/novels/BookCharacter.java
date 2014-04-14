package novels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import novels.util.Util;

import com.google.common.collect.Maps;

public class BookCharacter {

	public static int AGENT = 1;
	public static int PATIENT = 2;
	public static int MOD = 3;
	public static int POSS = 4;
	public static int SPEAKING = 5;

	public String name;
	public int id;
	public int count;
	public int gender;
	public HashMap<String, Integer> nameCounts;

	TreeMap<Integer, String> agents;
	TreeMap<Integer, String> patients;
	TreeMap<Integer, String> mods;
	TreeMap<Integer, String> poss;
	TreeMap<Integer, String> speaking;

	public BookCharacter(String name, int id) {
		this.name = name;
		this.id = id;
		nameCounts = Maps.newHashMap();
		agents = Maps.newTreeMap();
		patients = Maps.newTreeMap();
		mods = Maps.newTreeMap();
		poss = Maps.newTreeMap();
		speaking = Maps.newTreeMap();
	}

	public JSONObject toJson() {

		JSONObject jsonCharacter = new JSONObject();
		jsonCharacter.put("id", id);
		jsonCharacter.put("g", gender);
		JSONArray names = new JSONArray();

		ArrayList<Object> sorted = Util.sortHashMapByValue(nameCounts);
		int nc = 0;
		for (Object o : sorted) {
			String name = (String) o;
			int c = nameCounts.get(name);
			nc += c;
			JSONObject jsonName = new JSONObject();
			jsonName.put("n", name);
			jsonName.put("c", c);
			names.add(jsonName);
		}
		jsonCharacter.put("NNPcount", nc);

		JSONArray jsonAgents = new JSONArray();
		for (int i : agents.keySet()) {
			JSONObject jsonAgent = new JSONObject();
			jsonAgent.put("i", i);
			jsonAgent.put("w", agents.get(i));
			jsonAgents.add(jsonAgent);
		}
		JSONArray jsonPatients = new JSONArray();
		for (int i : patients.keySet()) {
			JSONObject jsonPatient = new JSONObject();
			jsonPatient.put("i", i);
			jsonPatient.put("w", patients.get(i));
			jsonPatients.add(jsonPatient);
		}
		JSONArray jsonMods = new JSONArray();
		for (int i : mods.keySet()) {
			JSONObject jsonMod = new JSONObject();
			jsonMod.put("i", i);
			jsonMod.put("w", mods.get(i));
			jsonMods.add(jsonMod);
		}
		JSONArray jsonPosses = new JSONArray();
		for (int i : poss.keySet()) {
			JSONObject jsonPoss = new JSONObject();
			jsonPoss.put("i", i);
			jsonPoss.put("w", poss.get(i));
			jsonPosses.add(jsonPoss);
		}

		JSONArray jsonSpeaking = new JSONArray();
		for (int i : speaking.keySet()) {
			JSONObject jsonSpeech = new JSONObject();
			jsonSpeech.put("i", i);
			jsonSpeech.put("w", speaking.get(i));
			jsonSpeaking.add(jsonSpeech);
		}

		jsonCharacter.put("names", names);
		jsonCharacter.put("agent", jsonAgents);
		jsonCharacter.put("patient", jsonPatients);
		jsonCharacter.put("poss", jsonPosses);
		jsonCharacter.put("mod", jsonMods);
		jsonCharacter.put("speaking", jsonSpeaking);

		return jsonCharacter;
	}

	public void addFeature(int type, String feat, int position) {
		if (type == AGENT) {
			agents.put(position, feat);
		} else if (type == PATIENT) {
			patients.put(position, feat);
		} else if (type == MOD) {
			mods.put(position, feat);
		} else if (type == POSS) {
			poss.put(position, feat);
		} else if (type == SPEAKING) {
			speaking.put(position, feat);
		}
	}

	/*
	 * Add a character mention to the counts for observed names.
	 */
	public void add(String name) {
		int count = 0;
		if (nameCounts.containsKey(name)) {
			count = nameCounts.get(name);
		}
		count++;
		this.count++;
		nameCounts.put(name, count);
	}

	public void setDominantName() {
		if (nameCounts.size() > 0) {
			ArrayList<Object> sorted = Util.sortHashMapByValue(nameCounts);
			name = (String) sorted.get(0);
			if (sorted.size() > 1) {
				name += "/" + (String) sorted.get(1);
			}
			if (sorted.size() > 2) {
				name += "/" + (String) sorted.get(2);
			}
		}

	}

	public String findName() {
		ArrayList<Object> sorted = Util.sortHashMapByValue(nameCounts);
		StringBuffer buffer = new StringBuffer();
		buffer.append(count + "\t");
		for (int i = 0; i < sorted.size(); i++) {
			String n = (String) sorted.get(i);
			buffer.append(String.format("%s (%s) ", n, nameCounts.get(n)));
		}
		return buffer.toString();
	}

}