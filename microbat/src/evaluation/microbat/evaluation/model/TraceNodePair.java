package microbat.evaluation.model;

import java.util.ArrayList;
import java.util.List;

import microbat.algorithm.graphdiff.GraphDiff;
import microbat.algorithm.graphdiff.HierarchyGraphDiffer;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.PrimitiveValue;
import microbat.model.value.ReferenceValue;
import microbat.model.value.VarValue;
import microbat.model.value.VirtualValue;
import microbat.model.variable.Variable;
import microbat.model.variable.VirtualVar;
import microbat.util.PrimitiveUtils;

public class TraceNodePair {

	private TraceNode originalNode;
	private TraceNode mutatedNode;
	
	private boolean isExactlySame;
	
	public TraceNodePair(TraceNode mutatedNode, TraceNode originalNode) {
		this.originalNode = originalNode;
		this.mutatedNode = mutatedNode;
	}

	public TraceNode getOriginalNode() {
		return originalNode;
	}

	public void setOriginalNode(TraceNode originalNode) {
		this.originalNode = originalNode;
	}

	public TraceNode getMutatedNode() {
		return mutatedNode;
	}

	public void setMutatedNode(TraceNode mutatedNode) {
		this.mutatedNode = mutatedNode;
	}

	public void setExactSame(boolean b) {
		this.isExactlySame = b;
	}

	public boolean isExactSame(){
		return this.isExactlySame;
	}

	@Override
	public String toString() {
		return "TraceNodePair [originalNode=" + originalNode + ", mutatedNode="
				+ mutatedNode + ", isExactlySame=" + isExactlySame + "]";
	}

	
//	public List<String> findWrongVarIDs() {
//		List<String> wrongVarIDs = new ArrayList<>();
//		
//		for(VarValue mutatedReadVar: mutatedNode.getReadVariables()){
//			VarValue originalReadVar = findCorrespondingVarWithDifferentValue(mutatedReadVar, 
//					originalNode.getReadVariables(), mutatedNode.getReadVariables());
//			if(originalReadVar != null){
//				wrongVarIDs.add(mutatedReadVar.getVarID());				
//			}
//		}
//		
//		for(VarValue mutatedWrittenVar: mutatedNode.getWrittenVariables()){
//			VarValue originalWrittenVar = findCorrespondingVarWithDifferentValue(mutatedWrittenVar, 
//					originalNode.getWrittenVariables(), mutatedNode.getWrittenVariables());
//			if(originalWrittenVar != null){
//				wrongVarIDs.add(mutatedWrittenVar.getVarID());				
//			}
//		}
//		
//		System.currentTimeMillis();
//		
//		return wrongVarIDs;
//	}
	
	public List<String> findSingleWrongWrittenVarID(Trace trace){
		List<String> wrongVarIDs = new ArrayList<>();
		
		for(VarValue mutatedWrittenVar: mutatedNode.getWrittenVariables()){
			List<VarValue> mutatedVarList = findCorrespondingVarWithDifferentValue(mutatedWrittenVar, 
					originalNode.getWrittenVariables(), mutatedNode.getWrittenVariables(), trace);
			if(!mutatedVarList.isEmpty()){
				for(VarValue value: mutatedVarList){
					wrongVarIDs.add(value.getVarID());
				}
				
				return wrongVarIDs;
			}
		}
		
		return wrongVarIDs;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<String> findSingleWrongReadVarID(Trace mutatedTrace){
		
		List<String> wrongVarIDs = new ArrayList<>();
		
		for(VarValue mutatedReadVar: mutatedNode.getReadVariables()){
			List<VarValue> mutatedVarList = findCorrespondingVarWithDifferentValue(mutatedReadVar, 
					originalNode.getReadVariables(), mutatedNode.getReadVariables(), mutatedTrace);
			if(!mutatedVarList.isEmpty()){
				for(VarValue value: mutatedVarList){
					wrongVarIDs.add(value.getVarID());
				}
				
				return wrongVarIDs;
			}
		}
		
		return wrongVarIDs;
	}

	/**
	 * Normally, it should return a list of a single variable value. 
	 * 
	 * However, it is possible that it returns a list
	 * of two variable values. Such a case happen when the mutated variable and original variable are synonymous reference 
	 * variable, e.g., obj. They are different because some of their attributes or elements are different, e.g., obj.x. In 
	 * this case, the simulated user does not know whether the reference variable itself (e.g., obj) is wrong, or its 
	 * attribute or element (e.g., obj.x) is wrong. Therefore, we return both case for the simulated debugging. In above case,
	 * the first element is the reference variable value, and the second one is the wrong attribute variable value.
	 * 
	 * @param mutatedVar
	 * @param originalList
	 * @param mutatedList
	 * @return
	 */
	private List<VarValue> findCorrespondingVarWithDifferentValue(VarValue mutatedVar, List<VarValue> originalList, 
			List<VarValue> mutatedList, Trace mutatedTrace) {
		List<VarValue> differentVarValueList = new ArrayList<>();
		
		if(mutatedVar instanceof VirtualValue && PrimitiveUtils.isPrimitiveTypeOrString(mutatedVar.getType())){
			for(VarValue originalVar: originalList){
				if(originalVar instanceof VirtualValue && mutatedVar.getType().equals(originalVar.getType())){
					/**
					 * currently, the mutation will not change the order of virtual variable.
					 */
					int mutatedIndex = mutatedList.indexOf(mutatedVar);
					int originalIndex = originalList.indexOf(originalVar);
					
					if(mutatedIndex==originalIndex && !mutatedVar.getStringValue().equals(originalVar.getStringValue())){
						differentVarValueList.add(mutatedVar);
						return differentVarValueList;
					}
				}
				
			}
		}
		else if(mutatedVar instanceof PrimitiveValue){
			for(VarValue originalVar:originalList){
				if(originalVar instanceof PrimitiveValue){
					if(mutatedVar.getVarName().equals(originalVar.getVarName()) 
							&& !mutatedVar.getStringValue().equals(originalVar.getStringValue())){
						differentVarValueList.add(mutatedVar);
						return differentVarValueList;
					}
				}
			}
		}
		else if(mutatedVar instanceof ReferenceValue){
			for(VarValue originalVar: originalList){
				if(originalVar instanceof ReferenceValue){
					if(mutatedVar.getVarName().equals(originalVar.getVarName())){
						ReferenceValue mutatedRefVar = (ReferenceValue)mutatedVar;
						setChildren(mutatedRefVar, mutatedNode);
						ReferenceValue originalRefVar = (ReferenceValue)originalVar;
						setChildren(originalRefVar, originalNode);
						
						if(mutatedRefVar.getChildren() != null && originalRefVar.getChildren() != null){
							HierarchyGraphDiffer differ = new HierarchyGraphDiffer();
							differ.diff(mutatedRefVar, originalRefVar);
							
							if(!differ.getDiffs().isEmpty()){
								differentVarValueList.add(mutatedVar);
							}
							
							for(GraphDiff diff: differ.getDiffs()){
								if(diff.getDiffType().equals(GraphDiff.UPDATE)){
									VarValue mutatedSubVarValue = (VarValue) diff.getNodeBefore();
									
									String varID = mutatedSubVarValue.getVarID();
									if(!varID.contains(":") && !varID.contains(VirtualVar.VIRTUAL_PREFIX)){
										String order = mutatedTrace.findDefiningNodeOrder(Variable.READ, mutatedNode, varID);
										varID = varID + ":" + order;
									}
									mutatedSubVarValue.setVarID(varID);
									
									differentVarValueList.add(mutatedSubVarValue);
									
									/** one wrong attribute is enough for debugging. */
									break;
								}
							}
							
							return differentVarValueList;	
						}
						else if((mutatedRefVar.getChildren() == null && originalRefVar.getChildren() != null) ||
								(mutatedRefVar.getChildren() != null && originalRefVar.getChildren() == null)){
							differentVarValueList.add(mutatedVar);
							return differentVarValueList;
						}
					}
				}
			}
		}
		
		return differentVarValueList;
	}
	
	private void setChildren(ReferenceValue refVar, TraceNode node){
		if(refVar.getChildren()==null){
			if(node.getProgramState() != null){
				
				String varID = refVar.getVarID();
				varID = varID.substring(0, varID.indexOf(":"));
				
				VarValue vv = node.getProgramState().findVarValue(varID);
				if(vv != null){
					refVar.setChildren(vv.getChildren());
				}				
			}
		}
	}
}
