package microbat.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.evaluation.accuracy.Accuracy;
import microbat.evaluation.model.PairList;
import microbat.evaluation.model.StepOperationTuple;
import microbat.evaluation.model.TraceNodePair;
import microbat.evaluation.model.Trial;
import microbat.evaluation.util.DiffUtil;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.trace.TraceNodeReverseOrderComparator;
import microbat.model.value.VarValue;
import microbat.recommendation.StepRecommender;
import microbat.recommendation.UserFeedback;
import microbat.util.JTestUtil;
import microbat.util.Settings;
import sav.strategies.dto.ClassLocation;

public class SimulatedMicroBat {
	
	
	List<TraceNode> falsePositive = new ArrayList<>();
	List<TraceNode> falseNegative = new ArrayList<>();
	
	private SimulatedUser user = new SimulatedUser();
	private StepRecommender recommender;
	
	public Trial detectMutatedBug(Trace mutatedTrace, Trace correctTrace, ClassLocation mutatedLocation, 
			String testCaseName, String mutatedFile) throws SimulationFailException {
		
		boolean enableClear = false;
		
		PairList pairList = DiffUtil.generateMatchedTraceNodeList(mutatedTrace, correctTrace);
		
		TraceNode rootCause = findRootCause(mutatedLocation.getClassCanonicalName(), 
				mutatedLocation.getLineNo(), mutatedTrace, pairList);
		
		System.currentTimeMillis();
//		Object dom = rootCause.findAllDominatees();
//		dominatees.add(rootCause);
		
//		List<TraceNode> dominatees = findAllDominatees(mutatedTrace, mutatedLocation);
		Map<Integer, TraceNode> allWrongNodeMap = findAllWrongNodes(pairList, mutatedTrace);
		
		if(!allWrongNodeMap.isEmpty()){
			List<TraceNode> wrongNodeList = new ArrayList<>(allWrongNodeMap.values());
			Collections.sort(wrongNodeList, new TraceNodeReverseOrderComparator());
//			TraceNode observedFaultNode = wrongNodeList.get(0);
			
			TraceNode observedFaultNode = findObservedFault(wrongNodeList);
			
			Trial trial = startSimulation(observedFaultNode, rootCause, mutatedTrace, allWrongNodeMap, pairList, 
					testCaseName, mutatedFile, enableClear);
			return trial;
			
		}
		else{
			return null;
		}
		
		
//		Accuracy accuracy = computeAccuracy(dominatees, allWrongNodes);
//		
//		if(accuracy.getRecall() < 0.95){
//			System.out.println(mutatedLocation.getClassCanonicalName() + ":" + mutatedLocation.getLineNo() + " has problem");
//			TraceNodeSimilarityComparator sc = new TraceNodeSimilarityComparator();
//			TraceNode node = falseNegative.get(0);
//			TraceNodePair pair = pairList.findByMutatedNode(node);
//			
//			double d = sc.compute(pair.getMutatedNode(), pair.getOriginalNode());
//			
//			System.currentTimeMillis();
//		}
//		
//		System.out.println(accuracy);
	}
	
	private TraceNode findObservedFault(List<TraceNode> wrongNodeList){
		TraceNode observedFaultNode = null;
		
		for(TraceNode node: wrongNodeList){
			if(!JTestUtil.isInTestCase(node.getDeclaringCompilationUnitName())){
				observedFaultNode = node;
				break;
			}
		}
		
		return observedFaultNode;
	}
	
	private Trial startSimulation(TraceNode observedFaultNode, TraceNode rootCause, Trace mutatedTrace, 
			Map<Integer, TraceNode> allWrongNodeMap, PairList pairList, String testCaseName, String mutatedFile, boolean enableClear) 
					throws SimulationFailException {
		Settings.interestedVariables.clear();
		Settings.localVariableScopes.clear();
		Settings.potentialCorrectPatterns.clear();
		recommender = new StepRecommender();
		
		List<StepOperationTuple> jumpingSteps = new ArrayList<>();
		
		try{
			TraceNode lastNode = observedFaultNode;
			TraceNode suspiciousNode = observedFaultNode;
			String feedbackType = user.feedback(suspiciousNode, pairList, mutatedTrace.getCheckTime(), true, enableClear);
			
			jumpingSteps.add(new StepOperationTuple(suspiciousNode, feedbackType));
			
			if(!feedbackType.equals(UserFeedback.UNCLEAR)){
				setCurrentNodeChecked(mutatedTrace, suspiciousNode);		
				updateVariableCheckTime(mutatedTrace, suspiciousNode);
			}
			
			int feedbackTimes = 1;
			
			boolean isBugFound = rootCause.getLineNumber()==suspiciousNode.getLineNumber();
			while(!isBugFound){
				suspiciousNode = findSuspicioiusNode(suspiciousNode, mutatedTrace, feedbackType);
				isBugFound = rootCause.getLineNumber()==suspiciousNode.getLineNumber();
				
				if(!isBugFound){
					feedbackType = user.feedback(suspiciousNode, pairList, mutatedTrace.getCheckTime(), false, enableClear);
					jumpingSteps.add(new StepOperationTuple(suspiciousNode, feedbackType));
					
					if(!feedbackType.equals(UserFeedback.UNCLEAR)){
						setCurrentNodeChecked(mutatedTrace, suspiciousNode);		
						updateVariableCheckTime(mutatedTrace, suspiciousNode);
					}
					
					feedbackTimes++;
					
					if(feedbackTimes > mutatedTrace.size()){
						break;
					}
				}
				else{
					jumpingSteps.add(new StepOperationTuple(suspiciousNode, "Bug Found"));
				}
				
				if(suspiciousNode.getOrder() == lastNode.getOrder()){
					break;
				}
				
				lastNode = suspiciousNode;
			}
			
			Trial trial = constructTrial(rootCause, mutatedTrace, testCaseName,
					mutatedFile, isBugFound, jumpingSteps);
			
			return trial;
		}
		catch(Exception e){
			e.printStackTrace();
			String msg = "The program stuck in " + testCaseName +", the mutated line is " + rootCause.getLineNumber();
			SimulationFailException ex = new SimulationFailException(msg);
			throw ex;
		}
	}

	private Trial constructTrial(TraceNode rootCause, Trace mutatedTrace,
			String testCaseName, String mutatedFile, boolean isBugFound, List<StepOperationTuple> jumpingSteps) {
		
		List<String> jumpStringSteps = new ArrayList<>();
		System.out.println("bug found: " + isBugFound);
		for(StepOperationTuple tuple: jumpingSteps){
			String str = tuple.getNode().toString() + ": " + tuple.getUserFeedback() + "\n";
			System.out.print(str);		
			jumpStringSteps.add(str);
		}
		System.out.println("Root Cause:" + rootCause);
		
		Trial trial = new Trial();
		trial.setTestCaseName(testCaseName);
		trial.setBugFound(isBugFound);
		trial.setMutatedLineNumber(rootCause.getLineNumber());
		trial.setJumpSteps(jumpStringSteps);
		trial.setTotalSteps(mutatedTrace.size());
		trial.setMutatedFile(mutatedFile);
		trial.setResult(isBugFound? Trial.SUCESS : Trial.FAIL);
		return trial;
	}
	
	private void setCurrentNodeChecked(Trace trace, TraceNode currentNode) {
		int checkTime = trace.getCheckTime()+1;
		currentNode.setCheckTime(checkTime);
		trace.setCheckTime(checkTime);
	}
	
	private void updateVariableCheckTime(Trace trace, TraceNode currentNode) {
		for(VarValue var: currentNode.getReadVariables()){
			String varID = var.getVarID();
			if(Settings.interestedVariables.contains(varID)){
				Settings.interestedVariables.add(varID, trace.getCheckTime());
			}
		}
		
		for(VarValue var: currentNode.getWrittenVariables()){
			String varID = var.getVarID();
			if(Settings.interestedVariables.contains(varID)){
				Settings.interestedVariables.add(varID, trace.getCheckTime());
			}
		}
	}

	
	protected List<TraceNode> findAllDominatees(Trace mutationTrace, ClassLocation mutatedLocation){
		Map<Integer, TraceNode> allDominatees = new HashMap<>();
		
		for(TraceNode mutatedNode: mutationTrace.getExectionList()){
			if(mutatedNode.getClassCanonicalName().equals(mutatedLocation.getClassCanonicalName()) 
					&& mutatedNode.getLineNumber() == mutatedLocation.getLineNo()){
				
				if(allDominatees.get(mutatedNode.getOrder()) == null){
					Map<Integer, TraceNode> dominatees = mutatedNode.findAllDominatees();
					allDominatees.putAll(dominatees);
					allDominatees.put(mutatedNode.getOrder(), mutatedNode);
				}
				
			}
		}
		
		return new ArrayList<>(allDominatees.values());
	}
	
	private Map<Integer, TraceNode> findAllWrongNodes(PairList pairList, Trace mutatedTrace){
		Map<Integer, TraceNode> actualWrongNodes = new HashMap<>();
		for(TraceNode mutatedTraceNode: mutatedTrace.getExectionList()){
			TraceNodePair foundPair = pairList.findByMutatedNode(mutatedTraceNode);
			if(foundPair != null){
				if(!foundPair.isExactSame()){
					TraceNode mutatedNode = foundPair.getMutatedNode();
					actualWrongNodes.put(mutatedNode.getOrder(), mutatedNode);
				}
			}
			else{
				actualWrongNodes.put(mutatedTraceNode.getOrder(), mutatedTraceNode);
			}
		}
		return actualWrongNodes;
	}
	
	public Accuracy computeAccuracy(List<TraceNode> dominatees, List<TraceNode> actualWrongNodes) {
		double modelInfluencedSize = dominatees.size();
		
		List<TraceNode> commonNodes = findCommonNodes(dominatees, actualWrongNodes);
		
		double precision = (double)commonNodes.size()/modelInfluencedSize;
		double recall = (double)commonNodes.size()/actualWrongNodes.size();
		
		Accuracy accuracy = new Accuracy(precision, recall);
		
		return accuracy;
	}

	private List<TraceNode> findCommonNodes(List<TraceNode> dominatees,
			List<TraceNode> actualWrongNodes) {
		List<TraceNode> commonNodes = new ArrayList<>();
		
		falsePositive = new ArrayList<>();
		falseNegative = new ArrayList<>();
		
		for(TraceNode domiantee: dominatees){
			if(actualWrongNodes.contains(domiantee)){
				commonNodes.add(domiantee);
			}
			else{
				falsePositive.add(domiantee);
			}
		}
		
		for(TraceNode acturalWrongNode: actualWrongNodes){
			if(!commonNodes.contains(acturalWrongNode)){
				falseNegative.add(acturalWrongNode);
			}
		}
		
		return commonNodes;
	}

	private TraceNode findRootCause(String className, int lineNo, Trace mutatedTrace, PairList pairList) {
		for(TraceNode node: mutatedTrace.getExectionList()){
			if(node.getDeclaringCompilationUnitName().equals(className) && node.getLineNumber()==lineNo){
				TraceNodePair pair = pairList.findByMutatedNode(node);
				
				if(pair == null){
					System.currentTimeMillis();
				}
				
				return pair.getMutatedNode();
			}
		}
		
		System.currentTimeMillis();
		
		return null;
	}

	private TraceNode findSuspicioiusNode(TraceNode currentNode, Trace trace, String feedbackType) {
		setCurrentNodeCheck(trace, currentNode);
		
		
		if(!feedbackType.equals(UserFeedback.UNCLEAR)){
			setCurrentNodeCheck(trace, currentNode);					
		}
		
		TraceNode suspiciousNode = recommender.recommendNode(trace, currentNode, feedbackType);
		return suspiciousNode;
		
//		TraceNode suspiciousNode = null;
//		
//		ConflictRuleChecker conflictRuleChecker = new ConflictRuleChecker();
//		TraceNode conflictNode = conflictRuleChecker.checkConflicts(trace, currentNode.getOrder());
//		
//		if(conflictNode == null){
//			suspiciousNode = recommender.recommendNode(trace, currentNode, feedbackType);
//		}
//		else{
//			suspiciousNode = conflictNode;
//		}
//		
//		return suspiciousNode;
	}
	
	private void setCurrentNodeCheck(Trace trace, TraceNode currentNode) {
		int checkTime = trace.getCheckTime()+1;
		currentNode.setCheckTime(checkTime);
		trace.setCheckTime(checkTime);
	}


	
}
