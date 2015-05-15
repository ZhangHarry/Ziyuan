package sav.commons.testdata.calculator;

import org.junit.Assert;
import org.junit.Test;

public class CalculatorTestPassed {

	@Test
	public void testPassed1() {
		int x = 0;
		int y = 1;
		int max = Calculator.getSum(x, y);
		Assert.assertTrue(Calculator.validateGetSum(x, y, max));
	}

	@Test
	public void testPassed2() {
		int x = 1;
		int y = 1;
		int max = Calculator.getSum(x, y);
		Assert.assertTrue(Calculator.validateGetSum(x, y, max));
	}

	@Test
	public void testPassed3() {
		int x = 2;
		int y = 3;
		int max = Calculator.getSum(x, y);
		Assert.assertTrue(Calculator.validateGetSum(x, y, max));
	}

	@Test
	public void testPassed4() {
		int x = -1;
		int y = 1;
		int max = Calculator.getSum(x, y);
		Assert.assertTrue(Calculator.validateGetSum(x, y, max));
	}
}
