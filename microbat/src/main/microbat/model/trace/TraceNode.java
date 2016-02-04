package microbat.model.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.algorithm.graphdiff.GraphDiff;
import microbat.algorithm.graphdiff.HierarchyGraphDiffer;
import microbat.model.BreakPoint;
import microbat.model.BreakPointValue;
import microbat.model.value.VarValue;

public class TraceNode{
	
	public final static int STEP_CORRECT = 0;
	public final static int STEP_INCORRECT = 1;
	public final static int STEP_UNKNOWN = 2;
	
	public final static int VARS_CORRECT = 3;
	public final static int VARS_INCORRECT = 4;
	public final static int VARS_UNKNOWN = 5;
	
	private double suspicousScore;
	private int checkTime = -1;
	private int stepCorrectness = STEP_UNKNOWN;
	private int varsCorrectness = VARS_UNKNOWN;
	
	private BreakPoint breakPoint;
	private BreakPointValue programState;
	private BreakPointValue afterStepInState;
	private BreakPointValue afterStepOverState;
	
	private List<GraphDiff> consequences;
	
	private List<VarValue> readVariables = new ArrayList<>();
	private List<VarValue> writtenVariables = new ArrayList<>();
	
	private Map<TraceNode, List<String>> dominators = new HashMap<>();
	private Map<TraceNode, List<String>> dominatees = new HashMap<>();
	
	/**
	 * the order of this node in the whole trace, starting from 1.
	 */
	private int order;
	
	/**
	 * indicate whether this node has been marked correct/incorrect by user
	 */
	private Boolean markedCorrrect;
	
	
	private TraceNode stepInNext;
	private TraceNode stepInPrevious;
	
	private TraceNode stepOverNext;
	private TraceNode stepOverPrevious;
	
	private List<TraceNode> invocationChildren = new ArrayList<>();
	private TraceNode invocationParent;
	
	private boolean isException;
	
	public TraceNode(BreakPoint breakPoint, BreakPointValue programState, int order) {
		super();
		this.breakPoint = breakPoint;
		this.programState = programState;
		this.order = order;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + order;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TraceNode other = (TraceNode) obj;
		if (order != other.order)
			return false;
		return true;
	}

	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("order ");
		buffer.append(getOrder());
		buffer.append("~");
		
		buffer.append(getClassName());
		buffer.append(": line ");
		buffer.append(getLineNumber());
		
		String methodName = this.breakPoint.getMethodName();
		if(methodName != null){
			buffer.append(" in ");
			buffer.append(methodName);
			buffer.append("(...)");
		}
	
		return buffer.toString();
	}
	
	public String getClassName(){
		return this.breakPoint.getClassCanonicalName();
	}
	
	public String getDeclaringCompilationUnitName(){
		return this.breakPoint.getDeclaringCompilationUnitName();
	}
	
	public int getLineNumber(){
		return this.breakPoint.getLineNo();
	}

	public BreakPoint getBreakPoint() {
		return breakPoint;
	}

	public void setBreakPoint(BreakPoint breakPoint) {
		this.breakPoint = breakPoint;
	}

	public BreakPointValue getProgramState() {
		return programState;
	}

	public void setProgramState(BreakPointValue programState) {
		this.programState = programState;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public Boolean getMarkedCorrrect() {
		return markedCorrrect;
	}

	public void setMarkedCorrrect(Boolean markedCorrrect) {
		this.markedCorrrect = markedCorrrect;
	}

	public BreakPointValue getAfterState() {
		if(this.afterStepOverState != null){
			return this.afterStepOverState;
		}
		else{
			return afterStepInState;			
		}
		
	}
//
//	public void setAfterState(BreakPointValue afterState) {
//		this.afterStepInState = afterState;
//	}

	public TraceNode getStepInNext() {
		return stepInNext;
	}

	public void setStepInNext(TraceNode stepInNext) {
		this.stepInNext = stepInNext;
	}

	public TraceNode getStepInPrevious() {
		return stepInPrevious;
	}

	public void setStepInPrevious(TraceNode stepInPrevious) {
		this.stepInPrevious = stepInPrevious;
	}

	public TraceNode getStepOverNext() {
		return stepOverNext;
	}

	public void setStepOverNext(TraceNode stepOverNext) {
		this.stepOverNext = stepOverNext;
	}

	public TraceNode getStepOverPrevious() {
		return stepOverPrevious;
	}

	public void setStepOverPrevious(TraceNode stepOverPrevious) {
		this.stepOverPrevious = stepOverPrevious;
	}

	public List<TraceNode> getInvocationChildren() {
		return invocationChildren;
	}

	public void setInvocationChildren(List<TraceNode> invocationChildren) {
		this.invocationChildren = invocationChildren;
	}

	public void addInvocationChild(TraceNode node){
		this.invocationChildren.add(node);
	}

	public TraceNode getInvocationParent() {
		return invocationParent;
	}

	public void setInvocationParent(TraceNode invocationParent) {
		this.invocationParent = invocationParent;
	}

	public BreakPointValue getAfterStepInState() {
		return afterStepInState;
	}

	public void setAfterStepInState(BreakPointValue afterStepInState) {
		this.afterStepInState = afterStepInState;
	}

	public BreakPointValue getAfterStepOverState() {
		return afterStepOverState;
	}

	public void setAfterStepOverState(BreakPointValue afterStepOverState) {
		this.afterStepOverState = afterStepOverState;
	}

	public List<GraphDiff> getConsequences() {
		return consequences;
	}

	public void setConsequences(List<GraphDiff> consequences) {
		this.consequences = consequences;
	}

	public void conductStateDiff() {
		BreakPointValue nodeBefore = getProgramState();
		BreakPointValue nodeAfter = getAfterState();
		
//		if(getOrder() == 6){
//			System.currentTimeMillis();
//		}
		
		HierarchyGraphDiffer differ = new HierarchyGraphDiffer();
		differ.diff(nodeBefore, nodeAfter);
		List<GraphDiff> diffs = differ.getDiffs();
		this.consequences = diffs;
	}

	public Map<TraceNode, List<String>> getDominator() {
		return dominators;
	}

	public void setDominator(Map<TraceNode, List<String>> dominator) {
		this.dominators = dominator;
	}

	public Map<TraceNode, List<String>> getDominatee() {
		return dominatees;
	}

	public void setDominatee(Map<TraceNode, List<String>> dominatee) {
		this.dominatees = dominatee;
	}
	
	public void addDominator(TraceNode node, List<String> variables){
		List<String> varIDs = this.dominators.get(node);
		if(varIDs == null){
			this.dominators.put(node, variables);
		}
		else{
			varIDs.addAll(variables);
		}
	}
	
	public void addDominatee(TraceNode node, List<String> variables){
		List<String> varIDs = this.dominatees.get(node);
		if(varIDs == null){
			this.dominatees.put(node, variables);
		}
		else{
			varIDs.addAll(variables);
		}
	}

	public boolean isException() {
		return isException;
	}

	public void setException(boolean isException) {
		this.isException = isException;
	}

	public List<VarValue> getReadVariables() {
		return readVariables;
	}

	public void setReadVariables(List<VarValue> readVariables) {
		this.readVariables = readVariables;
	}
	
	public void addReadVariable(VarValue var){
		this.readVariables.add(var);
	}

	public List<VarValue> getWrittenVariables() {
		return writtenVariables;
	}

	public void setWrittenVariables(List<VarValue> writtenVariables) {
		this.writtenVariables = writtenVariables;
	}
	
	public void addWrittenVariable(VarValue var){
		this.writtenVariables.add(var);
	}

	public double getSuspicousScore() {
		return suspicousScore;
	}

	public void setSuspicousScore(double suspicousScore) {
		this.suspicousScore = suspicousScore;
	}

	public int getCheckTime() {
		return checkTime;
	}

	public void setCheckTime(int markTime) {
		this.checkTime = markTime;
	}

	public int getStepCorrectness() {
		return stepCorrectness;
	}

	public void setStepCorrectness(int stepCorrectness) {
		this.stepCorrectness = stepCorrectness;
	}

	public int getVarsCorrectness() {
		return varsCorrectness;
	}

	public void setVarsCorrectness(int varsCorrectness) {
		this.varsCorrectness = varsCorrectness;
	}

	public List<TraceNode> getNonCorrectDominators() {
		List<TraceNode> nonCorrectDominators = new ArrayList<>();
		for(TraceNode dominator: dominators.keySet()){
			if(dominator.getStepCorrectness() != TraceNode.STEP_CORRECT){
				nonCorrectDominators.add(dominator);
			}
		}
		return nonCorrectDominators;
	}

	public void addSuspicousScore(double score) {
		this.suspicousScore += score;
	}
	
}
