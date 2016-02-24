package microbat.evaluation.util;

import java.util.ArrayList;
import java.util.List;

import microbat.evaluation.model.PairList;
import microbat.evaluation.model.TraceNodePair;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;

public class DiffUtil {
	/**
	 * {@code multisetList} represents the multiset list corresponds to {@code commonTokenList}. For example, the common
	 * token list for two strings [a b c a d] and [a c a d] is [a c a d], its corresponding multiset list is {a, a}, {c, c}, 
	 * {a, a} and {d, d}.<p>
	 * 
	 * This method is for recursively invoked. Therefore, I can achieve the common token list and its corresponding multiset
	 * in the process of computing longest common subsequence. <p>
	 * 
	 * Initially, the {@code multisetList} and {@code commonTokenList} have the same content, e.g., [a b c a d] and {a}, {b},
	 * {c}, {a}, {d}.
	 * 
	 * @param multisetList
	 * @param commonTokenList
	 * @param tokenList2
	 */
	public static PairList generateMatchedTraceNodeList(Trace mutatedTrace, Trace correctTrace) {
		
		List<TraceNodePair> pairList = new ArrayList<>();
		
		TraceNode[] mutatedTraceArray = mutatedTrace.getExectionList().toArray(new TraceNode[0]);
		TraceNode[] correctTraceArray = correctTrace.getExectionList().toArray(new TraceNode[0]);
		
		TraceNodeSimilarityComparator sc = new TraceNodeSimilarityComparator(); 
		
		double[][] scoreTable = buildScoreTable(mutatedTraceArray, correctTraceArray, sc);

		for (int i = mutatedTraceArray.length, j = correctTraceArray.length; (i > 0 && j > 0);) {
			if (mutatedTraceArray[i - 1].hasSameLocation(correctTraceArray[j - 1])) {
				
				if(mutatedTraceArray[i - 1].getOrder() == 15 && correctTraceArray[j-1].getOrder()==15){
					System.currentTimeMillis();
				}
				
				double sim = sc.compute(mutatedTraceArray[i - 1], correctTraceArray[j - 1]);
				double increase = scoreTable[i][j]-scoreTable[i-1][j-1];
				
				if(Math.abs(sim - increase) < 0.01 && sim != 0){
					TraceNodePair pair = new TraceNodePair(mutatedTraceArray[i - 1], correctTraceArray[j - 1]);
					pairList.add(pair);
					
					pair.setExactSame(sim > 0.99);
					
					i--;
					j--;
				}
				else{
					if (scoreTable[i - 1][j] >= scoreTable[i][j - 1]){
						i--;					
					}
					else{
						j--;					
					}
				}
				
			} else {
				if (scoreTable[i - 1][j] >= scoreTable[i][j - 1]){
					i--;					
				}
				else{
					j--;					
				}
			}
		}

		reverseOrder(pairList);
		PairList list = new PairList(pairList);
		
		return list;
	}
	
	private static void reverseOrder(List<TraceNodePair> pairList){
		
		int midIndex = pairList.size()/2;
		for(int i=0; i<midIndex; i++){
			TraceNodePair tmp = pairList.get(i);
			pairList.set(i, pairList.get(pairList.size()-1-i));
			pairList.set(pairList.size()-1-i, tmp);
		}
		
	}
	
	private static double[][] buildScoreTable(TraceNode[] nodeList1, TraceNode[] nodeList2, TraceNodeSimilarityComparator comparator){
		double[][] similarityTable = new double[nodeList1.length + 1][nodeList2.length + 1];
		for (int i = 0; i < nodeList1.length + 1; i++)
			similarityTable[i][0] = 0;
		for (int j = 0; j < nodeList2.length + 1; j++)
			similarityTable[0][j] = 0;

		for (int i = 1; i < nodeList1.length + 1; i++){
			for (int j = 1; j < nodeList2.length + 1; j++) {
				if (nodeList1[i - 1].hasSameLocation(nodeList2[j - 1])){
					double value = similarityTable[i - 1][j - 1] + comparator.compute(nodeList1[i - 1], nodeList2[j - 1]);
					similarityTable[i][j] = getLargestValue(value, similarityTable[i-1][j], similarityTable[i][j-1]);
				}
				else {
					similarityTable[i][j] = (similarityTable[i - 1][j] >= similarityTable[i][j - 1]) ? 
							similarityTable[i - 1][j] : similarityTable[i][j - 1];
				}
			}
		}
		
		return similarityTable;
	}
	
	
	public static double getLargestValue(double entry1, double entry2, double entry3){
		double value = (entry1 > entry2)? entry1 : entry2;
		return (value > entry3)? value : entry3;
	}
}
