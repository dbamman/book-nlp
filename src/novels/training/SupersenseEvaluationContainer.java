package novels.training;

import java.util.HashMap;

import com.google.common.collect.Maps;

public class SupersenseEvaluationContainer {

	HashMap<String, String> readable;

	public int[][] confusion;
	String[] reverseLabelIds;
	HashMap<String, Integer> labelVocab;
	int NUM_LABELS;

	public SupersenseEvaluationContainer(int NUM_LABELS, String[] reverseLabelIds) {
		this.NUM_LABELS = NUM_LABELS;
		confusion = new int[NUM_LABELS][NUM_LABELS];

		this.reverseLabelIds = reverseLabelIds;
		this.labelVocab = Maps.newHashMap();
		for (int i = 0; i < reverseLabelIds.length; i++) {
			this.labelVocab.put(reverseLabelIds[i], i);
		}

		readable = Maps.newHashMap();

		readable.put("03", "noun.Tops");
		readable.put("04", "noun.act");
		readable.put("05", "noun.animal");
		readable.put("06", "noun.artifact");
		readable.put("07", "noun.attribute");
		readable.put("08", "noun.body");
		readable.put("09", "noun.cognition");
		readable.put("10", "noun.communication");
		readable.put("11", "noun.event");
		readable.put("12", "noun.feeling");
		readable.put("13", "noun.food");
		readable.put("14", "noun.group");
		readable.put("15", "noun.location");
		readable.put("16", "noun.motive");
		readable.put("17", "noun.object");
		readable.put("18", "noun.person");
		readable.put("19", "noun.phenomenon");
		readable.put("20", "noun.plant");
		readable.put("21", "noun.possession");
		readable.put("22", "noun.process");
		readable.put("23", "noun.quantity");
		readable.put("24", "noun.relation");
		readable.put("25", "noun.shape");
		readable.put("26", "noun.state");
		readable.put("27", "noun.substance");
		readable.put("28", "noun.time");
		readable.put("29", "verb.body");
		readable.put("30", "verb.change");
		readable.put("31", "verb.cognition");
		readable.put("32", "verb.communication");
		readable.put("33", "verb.competition");
		readable.put("34", "verb.consumption");
		readable.put("35", "verb.contact");
		readable.put("36", "verb.creation");
		readable.put("37", "verb.emotion");
		readable.put("38", "verb.motion");
		readable.put("39", "verb.perception");
		readable.put("40", "verb.possession");
		readable.put("41", "verb.social");
		readable.put("42", "verb.stative");
		readable.put("43", "verb.weather");

	}

	public SupersenseEvaluationContainer() {

	}

	public void add(SupersenseEvaluationContainer eval) {
		if (confusion == null) {
			this.NUM_LABELS = eval.NUM_LABELS;
			this.confusion = new int[NUM_LABELS][NUM_LABELS];
			this.reverseLabelIds = new String[NUM_LABELS];
			for (int i = 0; i < NUM_LABELS; i++) {
				this.reverseLabelIds[i] = eval.reverseLabelIds[i];
			}
			this.labelVocab = Maps.newHashMap();
			for (int i = 0; i < reverseLabelIds.length; i++) {
				this.labelVocab.put(reverseLabelIds[i], i);
			}
		}

		int[] nameMap = new int[NUM_LABELS];
		for (int i = 0; i < NUM_LABELS; i++) {
			String name = reverseLabelIds[i];
			int index = eval.labelVocab.get(name);
			nameMap[i] = index;
		}
		for (int i = 0; i < NUM_LABELS; i++) {
			for (int j = 0; j < NUM_LABELS; j++) {
				confusion[i][j] += eval.confusion[nameMap[i]][nameMap[j]];
			}
		}
	}

	public double accuracy() {

		float trialTotal = 0;
		float trueCorrectTotal = 0;

		for (int i = 0; i < NUM_LABELS; i++) {

			trueCorrectTotal += confusion[i][i];

			for (int j = 0; j < NUM_LABELS; j++) {
				trialTotal += confusion[i][j];
			}

		}

		return trueCorrectTotal / trialTotal;
	}

	public double macroF1() {

		double precisionTotal = 0;
		double recallTotal = 0;

		for (int i = 0; i < NUM_LABELS; i++) {

			int trialTotal = 0;
			int trueCorrectTotal = 0;
			for (int j = 0; j < NUM_LABELS; j++) {
				trialTotal += confusion[i][j];
				trueCorrectTotal += confusion[j][i];
			}
			double precision = 0;
			double recall = 0;

			if (confusion[i][i] > 0) {
				precision = (float) confusion[i][i] / trialTotal;
				recall = (float) confusion[i][i] / trueCorrectTotal;

			}

			precisionTotal += precision;
			recallTotal += recall;

		}

		/*
		 * Macro
		 */
		precisionTotal /= NUM_LABELS;
		recallTotal /= NUM_LABELS;
		double F1Total = 0;
		if (precisionTotal + recallTotal > 0) {

			F1Total = (2 * precisionTotal * recallTotal)
					/ (precisionTotal + recallTotal);
		}
		

		return F1Total;
	}

	public void print() {

		
		HashMap<String, String> map=Maps.newHashMap();
		HashMap<String, Integer> newvocab=Maps.newHashMap();
		HashMap<Integer, Integer> idmap=Maps.newHashMap();
		String[] reverseNewLabels;
		
		int maxid=0;
		for (int i=0; i<reverseLabelIds.length; i++) {
			String[] labelparts=reverseLabelIds[i].split("-");
			String label=labelparts[labelparts.length-1];
			map.put(reverseLabelIds[i], label);
			if (!newvocab.containsKey(label)) {
				newvocab.put(label, maxid);
				maxid++;
			}
			idmap.put(i, newvocab.get(label));
			
		}
		int[][] collapsedConfusion=new int[maxid][maxid];
		for (int i=0; i<confusion.length; i++) {
			for (int j=0; j<confusion.length; j++) {
				int s=idmap.get(i);
				int t=idmap.get(j);
				collapsedConfusion[s][t]+=confusion[i][j];
			}
		}

		int RED_LABELS=maxid;
		reverseNewLabels=new String[maxid];
		for (String id : newvocab.keySet()) {
			reverseNewLabels[newvocab.get(id)]=id;
		}
		int trialTotalAll = 0;
		int trueCorrectTotalAll = 0;
		int correctAll = 0;

		double precisionTotal = 0;
		double recallTotal = 0;

		for (int i = 0; i < RED_LABELS; i++) {

			int trialTotal = 0;
			int trueCorrectTotal = 0;
			for (int j = 0; j < RED_LABELS; j++) {
				trialTotal += collapsedConfusion[i][j];
				trueCorrectTotal += collapsedConfusion[j][i];
			}
			double precision = 0;
			double recall = 0;

			if (collapsedConfusion[i][i] > 0) {
				precision = (float) collapsedConfusion[i][i] / trialTotal;
				recall = (float) collapsedConfusion[i][i] / trueCorrectTotal;

			}

			double F1 = 0;
			if (precision != 0 || recall != 0) {
				F1 = (2 * precision * recall) / (precision + recall);
			}

			correctAll += collapsedConfusion[i][i];
			trueCorrectTotalAll += trueCorrectTotal;
			trialTotalAll += trialTotal;

			precisionTotal += precision;
			recallTotal += recall;

			System.out.println(String.format(
					"%s:\tPrecision: %.3f, Recall: %.3f, F: %.3f",
					readable.get(reverseNewLabels[i]), precision, recall, F1));
		}

		/*
		 * Macro
		 */
		precisionTotal /= RED_LABELS;
		recallTotal /= RED_LABELS;
		double F1Total = (2 * precisionTotal * recallTotal)
				/ (precisionTotal + recallTotal);
		System.out.println(String.format(
				"\nMacro Precision: %.3f, Macro Recall: %.3f, Macro F: %.3f",
				precisionTotal, recallTotal, F1Total));

		/*
		 * Micro
		 */
		double microPrecision = (float) correctAll / trialTotalAll;
		double microRecall = (float) correctAll / trueCorrectTotalAll;
		double microF1 = (2 * microPrecision * microRecall)
				/ (microPrecision + microRecall);

		System.out.println(String.format(
				"Micro Precision: %.3f, Micro Recall: %.3f, Micro F: %.3f",
				microPrecision, microRecall, microF1));

		System.out.println(String.format("Accuracy: %.3f", accuracy()));
	}
}
