package testdata.benchmark;

public class ToOctal {

	public int toOctalTest(int value) {
		if (value < 0) {
			return -1;
		}

		StringBuilder sb = 
				new StringBuilder();
		while (value > 0) {
			sb.append(value % 8);
			value /= 8;
		}
		
		return Integer.parseInt(
			sb.reverse().toString());
	}
}
