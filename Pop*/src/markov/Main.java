package markov;
import java.util.ArrayList;
import java.util.List;

import condition.ExactUnaryMatch;
import constraint.Constraint;

public class Main {
	public static void main(String[] args) {
		runMelodyExample();
		runLyricExample();
	}

	public static void runLyricExample() {
		String[] states = new String[] { "Clay", "loves", "Mary", "Paul","today" };
		double[] priors = new double[]{.25,.25,.25,.25,.25};
		double[][] transitions = new double[][] { { 0.,1.0,0.0,0.0,0.0 }, {.25,0.0,.5,.25,0.0}, 
			{ 0.0,2/3.,0.0,0.0,1/3. },{0.,0.,0.,0.,1.0},{0.,0.,0.,0.,0.} };
		
		SingleOrderMarkovModel<String> model = new SingleOrderMarkovModel<String>(states, priors, transitions);
		
		System.out.println("Single-Order Markov Model:\n" + model + "\n\n");
		
		int length = 4;
		List<Constraint<String>> constraints = new ArrayList<Constraint<String>>();
		
		Constraint<String> matchesClayConstraint = new Constraint<String>(0, new ExactUnaryMatch<String>(new String[]{"Clay"}), true);
		constraints.add(matchesClayConstraint);
		
		Constraint<String> matchesTodayConstraint = new Constraint<String>(3, new ExactUnaryMatch<String>(new String[]{"today"}), true);
		constraints.add(matchesTodayConstraint);
	
		AbstractMarkovModel<String> nhmmModel = new NHMM<String>(model, length, constraints);
		
		System.out.println("Constrained Single-Order Markov Model\n" + nhmmModel);
		
		for (Constraint<String> constraint : constraints) {
			System.out.println("Constraint: " + constraint);
		}
		
		System.out.println("\nCalculate probability of...");

		for (String seq : new String[]{"Clay loves Mary today","Mary loves Mary today",
				"Mary loves Paul today", "Mary loves Clay loves", "Mary today loves Clay",
				"Clay loves Paul today"}) {
			String[] strarr = seq.split("\\s");

			System.out.println("\t...\"" + seq + "\"");
			double mmProbabilityOfSequence = model.probabilityOfSequence(strarr);
			System.out.println("\t\tSingle-Order Markov Model Probability:" + mmProbabilityOfSequence);
			double nhmmProbabilityOfSequence = nhmmModel.probabilityOfSequence(strarr);
			System.out.println("\t\tConstrained Single-Order Markov Model Probability:" + nhmmProbabilityOfSequence);
			System.out.println("\t\tRatio: " + mmProbabilityOfSequence / nhmmProbabilityOfSequence);
			System.out.println();
		}
		
		System.out.println("\nGenerate 10 examples with Single-Order Markov Model:");
		
		for (int i = 0; i < 10; i++) {
			System.out.println("Gen " + i + ": " + model.generate(4));
		}

		System.out.println("\nGenerate 10 examples with Constrained Single-Order Markov Model:");
		
		for (int i = 0; i < 10; i++) {
			System.out.println("Gen " + i + ": " + nhmmModel.generate(4));
		}
		
		System.out.println();
	}
	
	public static void runMelodyExample() {
		Character[] states = new Character[] { 'C', 'D', 'E' };
		double[] priors = new double[]{.5, 1./6, 1./3};
		double[][] transitions = new double[][] { { .5, .25, .25 }, { .5, 0, .5 }, { .5, .25, .25 } };
		
		SingleOrderMarkovModel<Character> model = new SingleOrderMarkovModel<Character>(states, priors, transitions);
		
		System.out.println("Single-Order Markov Model:\n" + model + "\n\n");
		
		int length = 4;
		List<Constraint<Character>> constraints = new ArrayList<Constraint<Character>>();
		
		Constraint<Character> isDConstraint = new Constraint<Character>(3, new ExactUnaryMatch<Character>(new Character[]{'D'}), true);
		constraints.add(isDConstraint);
		
//		double p = 0.5;
//		double q = 0.;
//		double logP = Math.log(p);
//		double logQ = Math.log(q);
//		
//		System.out.println("p * q = " + (p * q));
//		System.out.println("p * q = " + (Math.exp(logP + logQ)));
//		System.out.println("p + q = " + (p+q));
//		System.out.println("p + q = " + Math.exp(Utils.logSum(logP, logQ)));
		
		AbstractMarkovModel<Character> nhmmModel = new NHMM<Character>(model, length, constraints);
		
		System.out.println("Constrained Single-Order Markov Model\n" + nhmmModel);
		
		for (Constraint<Character> constraint : constraints) {
			System.out.println("Constraint: " + constraint);
		}
		
		System.out.println("\nCalculate probability of...");

		for (String seq : new String[]{"CCCD","CCED","CECD","CEED","CDCD","CDED","ECCD","ECED"}) {
			Character[] chararr = new Character[seq.length()];
			for (int i = 0; i < chararr.length; i++) {
				chararr[i] = seq.charAt(i);
			}
			System.out.println("\t...\"" + seq + "\"");
			double mmProbabilityOfSequence = model.probabilityOfSequence(chararr);
			System.out.println("\t\tSingle-Order Markov Model Probability:" + mmProbabilityOfSequence);
			double nhmmProbabilityOfSequence = nhmmModel.probabilityOfSequence(chararr);
			System.out.println("\t\tConstrained Single-Order Markov Model Probability:" + nhmmProbabilityOfSequence);
			System.out.println("\t\tRatio: " + mmProbabilityOfSequence / nhmmProbabilityOfSequence);
			System.out.println();
		}


		System.out.println("\nGenerate 10 examples with Single-Order Markov Model:");
		
		for (int i = 0; i < 10; i++) {
			System.out.println("Gen " + i + ": " + model.generate(4));
		}

		System.out.println("\nGenerate 10 examples with Constrained Single-Order Markov Model:");
		
		for (int i = 0; i < 10; i++) {
			System.out.println("Gen " + i + ": " + nhmmModel.generate(4));
		}
		
		System.out.println();
	}
}
