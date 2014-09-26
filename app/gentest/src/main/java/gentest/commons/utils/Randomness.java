/**
 * Copyright TODO
 */
package gentest.commons.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sav.common.core.utils.CollectionUtils;

/**
 * @author LLT
 * for centralization.
 */
public final class Randomness {
	private Randomness() {}

	public static final long SEED = 0;
	public static int totalCallsToRandom = 0;
	static Random random = new Random(SEED);
	
	private static Random getRandom() {
		totalCallsToRandom++;
		return random;
	}

	public static void reset(long newSeed) {
		random = new Random(newSeed);
	}

	public static boolean nextRandomBool() {
		return getRandom().nextBoolean();
	}

	public static float nextFloat() {
		return getRandom().nextFloat();
	}
	
	public static int nextRandomInt(int i) {
		return getRandom().nextInt(i);
	}
	
	public static <T> T randomMember(T[] arr) {
		if (CollectionUtils.isEmpty(arr)) {
			return null;
		}
		return arr[nextRandomInt(arr.length)];
	}

	public static <T> T randomMember(List<T> list) {
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		return list.get(nextRandomInt(list.size()));
	}

	public static <T> List<T> randomSubList(List<T> allList, int subSize) {
		ArrayList<T> shuffleList = new ArrayList<T>(allList);
		Collections.shuffle(shuffleList, getRandom());
		return shuffleList.subList(0, subSize);
	}

}
