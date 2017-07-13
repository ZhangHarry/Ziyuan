/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.commons.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import sav.common.core.iface.HasProbabilityType;
import sav.common.core.utils.RandomUtils;
import sav.common.core.utils.Randomness;

/**
 * @author LLT
 *
 */
public class RandomnessTest {
	
	@Test
	public void testRandomDouble() {
		for (int i = 0; i < 100; i++) {
			System.out.println(Randomness.nextDouble());
		}
	}
	
	@Test
	public void testRandomLong() {
		for (int i = 0; i < 1000; i++) {
			System.out.println(RandomUtils.nextLong(-1000, 1000, new Random()));
		}
	}

	@Test
	public void testRandomSubList() {
		List<Integer> allList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		for (int i = 0; i < 100; i++) {
			System.out.println(Randomness.randomSubList(allList));
		}
	}
	
	@Test
	public void testRandomSubListFix() {
		List<Integer> allList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		for (int i = 0; i < 100; i++) {
			List<Integer> subList = Randomness.randomSubList(allList, 5);
			System.out.println(subList);
			Assert.assertFalse(duplicate(subList));
		}
	}
	
	private boolean duplicate(List<Integer> subList) {
		List<Integer> checkedList = new ArrayList<Integer>();
		for (Integer val : subList) {
			if (checkedList.contains(val)) {
				return true;
			}
			checkedList.add(val);
		}
		return false;
	}

	@Test
	public void testRandomInt() {
		for (int i = 0; i < 100; i++) {
			System.out.println(Randomness.nextInt(15));
		}
	}
	
	@Test
	public void testRandomWithDistribution() {
		HasProbabilityType[] eles = TypeWithProbability.values();
		for (int i = 0; i < 100; i++) {
			System.out.println(Randomness.randomWithDistribution(eles));
		}
	}
	
	private static enum TypeWithProbability implements HasProbabilityType {
		TYPE1(10),
		TYPE2(4),
		TYPE3(2),
		TYPE4(1);
		
		private int prob;
		private TypeWithProbability(int prob) {
			this.prob = prob;
		}
		
		@Override
		public int getProb() {
			return prob;
		}
		
	}
}
