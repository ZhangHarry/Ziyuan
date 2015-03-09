package libsvm.core;

import java.util.Arrays;
import java.util.Random;

import libsvm.extension.MultiCutMachine;
import libsvm.extension.PositiveSeparationMachine;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MachineSimpleTests {

	private static final Logger LOGGER = Logger.getLogger(MachineSimpleTests.class);

	private static final int NUMBER_OF_FEATURES = 2;
	private static final int NUMBER_OF_DATA_POINTS = 1000;
	
	private static final Random RANDOM = new Random();

	private Machine normalMachine;
	private Machine improvedMachine;
	private Machine positiveSeparationMachine;

	@Before
	public void prepareMachinesForTwoFeatures() {
		normalMachine = setupMachine(new Machine(), NUMBER_OF_FEATURES);
		improvedMachine = setupMachine(new MultiCutMachine(), NUMBER_OF_FEATURES);
		positiveSeparationMachine = setupMachine(new PositiveSeparationMachine(), NUMBER_OF_FEATURES);
	}

	private Machine setupMachine(final Machine machine, int numberOfFeatures) {
		return machine.setNumberOfFeatures(numberOfFeatures).setParameter(
				new Parameter().setMachineType(MachineType.C_SVC).setKernelType(KernelType.LINEAR)
						.setEps(1.0).setUseShrinking(false).setPredictProbability(false));
	}


	
	@Test
	public void whenThereAreTwoFeatures() {
		// Separator: ax + by = c
		int a=  2;
		int b = 3;
		int c = 15;
		int x, y;
		for (int i = 0; i < NUMBER_OF_DATA_POINTS; i++) {
			Category category = Category.POSITIVE;
			x = i;
			y = (c - a * x) / b - 1;

			normalMachine.addDataPoint(category, x, y);
		}
		
		for (int i = 0; i < NUMBER_OF_DATA_POINTS; i++) {
			Category category = Category.NEGATIVE;
			x = i;
			y = (c - a * x) / b + 1;

			normalMachine.addDataPoint(category, x, y);
		}

		final double normalModelAccuracy = normalMachine.train().getModelAccuracy();
		LOGGER.log(Level.DEBUG, "Normal SVM:" + normalModelAccuracy);
		
		LOGGER.info("Learned logic:");
		LOGGER.info("Normal machine: " + normalMachine.getLearnedLogic());
		
		Divider divider = normalMachine.getModel().getExplicitDivider();
		double[] expectedCoefficients = new double[]{-2, -3, -15};
		
		double[] coefficients = new CoefficientProcessing().process(divider);
		LOGGER.info("After trying to make result as integer: " + Arrays.toString(coefficients));
		compareCoefficients(expectedCoefficients, coefficients);
	}

	@Test
	public void whenThereAreThreeFeatures() {
		normalMachine = setupMachine(new Machine(), 3);
		
		// Separator: ax + by + cz = d
		int a =  3;
		int b = 7;
		int c = 19;
		int d = 80;
		int x, y, z;
		for (int i = 0; i < NUMBER_OF_DATA_POINTS; i++) {
			Category category = Category.POSITIVE;
			x = (int)(NUMBER_OF_DATA_POINTS * Math.random());
			y = (int)(NUMBER_OF_DATA_POINTS * Math.random());
			z = (d - a * x - b * y) / c + 1;

			normalMachine.addDataPoint(category, x, y, z);
		}
		
		for (int i = 0; i < NUMBER_OF_DATA_POINTS; i++) {
			Category category = Category.NEGATIVE;
			x = (int)(NUMBER_OF_DATA_POINTS * Math.random());
			y = (int)(NUMBER_OF_DATA_POINTS * Math.random());
			z = (d - a * x - b * y) / c - 1;

			normalMachine.addDataPoint(category, x, y, z);
		}

		final double normalModelAccuracy = normalMachine.train().getModelAccuracy();
		LOGGER.log(Level.DEBUG, "Normal SVM:" + normalModelAccuracy);
		
		LOGGER.info("Learned logic:");
		LOGGER.info("Normal machine: " + normalMachine.getLearnedLogic());		
		
		Divider divider = normalMachine.getModel().getExplicitDivider();
		double[] expectedCoefficients = new double[]{3, 7, 19, 80};
		
		double[] coefficients = new CoefficientProcessing().process(divider);
		LOGGER.info("After trying to make result as integer: " + Arrays.toString(coefficients));
		compareCoefficients(expectedCoefficients, coefficients);
	}
	
//	@Test
//	public void testWithRandomData() {
//		LOGGER.info("===========[testWithRandomData]===========");
//		for (int i = 0; i < NUMBER_OF_DATA_POINTS; i++) {
//			final double[] values = { Math.random(), Math.random() };
//			final Category category = Category.random();
//			normalMachine.addDataPoint(category, values);
//			improvedMachine.addDataPoint(category, values);
//		}
//
//		final double normalModelAccuracy = normalMachine.train().getModelAccuracy();
//		LOGGER.log(Level.DEBUG, "Normal SVM:" + normalModelAccuracy);
//		final double improvedModelAccuracy = improvedMachine.train().getModelAccuracy();
//		LOGGER.log(Level.DEBUG, "Improved SVM:" + improvedModelAccuracy);
//
//		Assert.assertTrue("Improved algorithm produces lower accuracy model than normal one.",
//				Double.compare(normalModelAccuracy, improvedModelAccuracy) <= 0);
//		
//		LOGGER.info("Learned logic:");
//		LOGGER.info("Normal machine: " + normalMachine.getLearnedLogic());
//		LOGGER.info("Improved machine: " + improvedMachine.getLearnedLogic());
//	}
//	
//	@Test
//	public void testWithTwoLinearSeparableData() {
//		LOGGER.info("===========[testWithTwoLinearSeparableData]===========");
//		
//		// x => 3 ^ y <= 5 are considered POSITIVE
//		int countPositive = 0, countNegative = 0;
//		for (int i = 0; i < NUMBER_OF_DATA_POINTS; i++) {
//			double x = RANDOM.nextInt();
//			double y = RANDOM.nextInt();
//			final Category category = x >= 3 && y <= 5 ? Category.POSITIVE : Category.NEGATIVE;
//			if (Category.POSITIVE == category) {
//				countPositive++;
//			} else {
//				countNegative++;
//			}
//			normalMachine.addDataPoint(category, x, y);
//			positiveSeparationMachine.addDataPoint(category, x, y);
//		}
//
//		LOGGER.log(Level.DEBUG, "Positive cases =" + countPositive);
//		LOGGER.log(Level.DEBUG, "Negative cases =" + countNegative);
//
//		final double normalModelAccuracy = normalMachine.train().getModelAccuracy();
//		LOGGER.log(Level.DEBUG, "Normal SVM:" + normalModelAccuracy);
//		final double improvedModelAccuracy = positiveSeparationMachine.train().getModelAccuracy();
//		LOGGER.log(Level.DEBUG, "Improved SVM:" + improvedModelAccuracy);
//
//		// We expect the improved model must perform better in this case
//		Assert.assertTrue(
//				"Improved algorithm does not produce higer accuracy model than normal one."
//						+ " Normal:" + normalModelAccuracy + "; Improved:" + improvedModelAccuracy,
//				Double.compare(normalModelAccuracy, improvedModelAccuracy) < 0);
//
//		LOGGER.info("Learned logic:");
//		LOGGER.info("Normal machine: " + normalMachine.getLearnedLogic());
//		LOGGER.info("PS machine: " + positiveSeparationMachine.getLearnedLogic());
//	}
	
	/**
	 * @param expectedCoefficients
	 * @param coefficients
	 */
	private void compareCoefficients(double[] expectedCoefficients,
			double[] coefficients) {
		double EPSILON = 0.2;
		for(int i = 0; i < coefficients.length; i++){
			double errorRate = Math.abs((coefficients[i] - expectedCoefficients[i]) / expectedCoefficients[i]);
			Assert.assertTrue(errorRate < EPSILON);
		}
	}
}
