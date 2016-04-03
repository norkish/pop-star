package utils;

public class MathUtils {
	public static double logSum(double logP, double logQ) {
		if(logQ != Double.NEGATIVE_INFINITY)
			return logQ + Math.log1p(Math.exp(logP - logQ));
		else if (logP != Double.NEGATIVE_INFINITY)
			return logP + Math.log1p(Math.exp(logQ - logP));
		else
			return Double.NEGATIVE_INFINITY;
	}
}
