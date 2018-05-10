package novels.optimization;

import java.util.ArrayList;
import java.util.HashSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.stanford.nlp.optimization.DiffFunction;

/**
 * Multiclass logistic regression with sparse features
 * @author dbamman
 *
 */
public class MulticlassSparseRegression {

	int numFeatures;
	double lambda;
	double threshold = 1e-4;
	int memory = 15;
	ArrayList<HashSet<Integer>> x;
	ArrayList<Integer> y;

	int numInstances;

	// num possible outcomes
	int K;
	// num training x K
	double[][] current;
	double[] normalizers;
	double L2Lambda;
	double L1Lambda;

	public double[] initial;

	public TrainedModel model;
	HashSet<Integer> biases;

	public void setBiasParameters(HashSet<Integer> biasesToSet) {
		this.biases = Sets.newHashSet();
		for (int bias : biasesToSet) {
			for (int i = 0; i < K; i++) {
				this.biases.add(i * numFeatures + bias);
			}
		}
		OWLQN.biasParameters = biasesToSet;

	}

	public double[] regress() {
		LogisticDiffFunction diff = new LogisticDiffFunction();
		if (initial == null) {
			initial = new double[K * numFeatures];
		}
		OWLQN qn = new OWLQN();
//		qn.setQuiet(true);
		System.out.println("regressing with " + x.size()
				+ " training samples of length " + numFeatures);

		double[] gradient = qn.minimize(diff, initial, L1Lambda, threshold,
				memory);

		model = new TrainedModel(gradient, K, numFeatures);
		return gradient;
	}

	/**
	 * Input
	 * 
	 * @param x
	 *            . List of features indices for each training example
	 *            (integers)
	 * @param y
	 *            . List of true label for each training example (String)
	 * @param L1Lambda
	 * @param L2Lambda
	 * @param length
	 */
	public MulticlassSparseRegression(int K, ArrayList<ArrayList<HashSet<Integer>>> x,
			ArrayList<ArrayList<Integer>> y, double L1Lambda, double L2Lambda, int length) {
		
		this.x=Lists.newArrayList();
		this.y=Lists.newArrayList();
		
		for (ArrayList<HashSet<Integer>> seq : x) {
			this.x.addAll(seq);
		}
		for (ArrayList<Integer> seqLab : y) {
			this.y.addAll(seqLab);
		}
		
		this.numFeatures = length;
		this.K = K;
		this.numInstances = this.x.size();
		current = new double[numInstances][K];
		normalizers = new double[numInstances];
		this.L2Lambda = L2Lambda;
		this.L1Lambda = L1Lambda;
		
		System.out.println(String.format("L1: %.10f, L2: %.10f", L1Lambda, L2Lambda));
	}

	/*
	 * Set inner product between weights and feature vector for each class given
	 * an input and save the normalizer over all of them.
	 */
	public void setCurrent(double[] weights) {

		for (int i = 0; i < numInstances; i++) {

			HashSet<Integer> feats = x.get(i);

			double normalizer = 0;
			for (int j = 0; j < K; j++) {
				double dot = 0;

				for (int key : feats) {

					int index = j * numFeatures + key;
					dot += weights[index];

				}

				current[i][j] = Math.exp(dot);
				normalizer += current[i][j];
			}
			normalizers[i] = normalizer;
		}
	}

	public class LogisticDiffFunction implements DiffFunction {

		public double[] derivativeAt(double[] arg0) {

			setCurrent(arg0);

			int arglength = arg0.length;
			double[] gradient = new double[arglength];

			for (int i = 0; i < numInstances; i++) {

				HashSet<Integer> feats = x.get(i);

				for (int j = 0; j < K; j++) {
					double dot = current[i][j];

					double cval = dot / normalizers[i];

					double count = 0;
					double negCount = 0;

					if (y.get(i) == j) {
						count = 1;
					} else {
						negCount = 1;
					}
					double diff = 1 - cval;

					double negdiff = 0 - cval;

					for (int key : feats) {

						int index = j * numFeatures + key;
						if (count > 0) {
							gradient[index] -= diff;
						}
						if (negCount > 0) {
							gradient[index] -= negdiff;
						}

					}
				}
			}

			if (L2Lambda > 0) {
				for (int i = 0; i < gradient.length; i++) {
					if (!biases.contains(i)) {
						gradient[i] += L2Lambda * (arg0[i]);
					}
				}
			}

			return gradient;
		}

		public int domainDimension() {
			return numFeatures;
		}

		public double valueAt(double[] arg0) {
			double loss = 0;
			setCurrent(arg0);

			for (int i = 0; i < numInstances; i++) {

				int j = y.get(i);

				double dot = current[i][j];
				double cval = dot / normalizers[i];
				loss -= (Math.log(cval));

			}

			if (L2Lambda > 0) {
				for (int i = 0; i < arg0.length; i++) {
					if (!biases.contains(i)) {
						loss += (1 / 2.0 * L2Lambda * (arg0[i]) * (arg0[i]));
					}
				}
			}

			return loss;
		}

	}

	public class TrainedModel {
		public double[][] classweights; // K x numFeatures
		int K;

		public TrainedModel(double[] weights, int K, int numFeatures) {
			classweights = new double[K][numFeatures];
			for (int j = 0; j < K; j++) {
				for (int n = 0; n < numFeatures; n++) {
					int index = j * numFeatures + n;
					classweights[j][n] = weights[index];
				}
			}
			this.K = K;
		}
	}
}
