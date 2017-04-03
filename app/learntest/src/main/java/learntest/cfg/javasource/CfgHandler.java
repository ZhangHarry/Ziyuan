package learntest.cfg.javasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import common.cfg.CFG;
import common.cfg.CfgDecisionNode;
import common.cfg.CfgEdge;
import common.cfg.CfgFalseEdge;
import common.cfg.CfgNode;
import common.cfg.CfgTrueEdge;
import learntest.breakpoint.data.DecisionBkpsData;
import learntest.breakpoint.data.DecisionLocation;
import learntest.calculator.MultiNotDividerBasedCategoryCalculator;
import learntest.calculator.OrCategoryCalculator;
import learntest.cfg.ICfgHandler;
import learntest.main.model.Branch;
import learntest.main.model.DecisionBranch;
import learntest.main.model.LoopBranch;
import learntest.testcase.data.BreakpointData;
import learntest.testcase.data.LoopTimesData;
import libsvm.core.CategoryCalculator;
import libsvm.core.Divider;
import libsvm.extension.MultiDividerBasedCategoryCalculator;
import sav.common.core.Pair;
import sav.common.core.formula.AndFormula;
import sav.common.core.formula.Formula;
import sav.common.core.formula.NotFormula;
import sav.strategies.dto.execute.value.ExecVar;

/**
 * contain all the cfg structure information. The original author does not maintain
 * structure information inside cfg.
 * @author linyun
 *
 */
public class CfgHandler implements ICfgHandler {
	private CFG cfg;
	private BreakpointCreator bkpsCreator;
	private DecisionBkpsData decisionBkpsData;
	
	private CfgDecisionNode begin;
	
	private Map<Integer, CfgDecisionNode> nodeMap;	
	private Map<CfgDecisionNode, CfgDecisionNode> trueNext;
	private Map<CfgDecisionNode, CfgDecisionNode> falseNext;
	private Map<CfgDecisionNode, CfgDecisionNode> next;
	
	private Map<CfgDecisionNode, List<CfgDecisionNode>> parents;
	
	private Set<CfgDecisionNode> ends;
	
	private List<ExecVar> vars;
	private List<ExecVar> originalVars;
	
	private double totalBranch = 1;
	
	public CfgHandler(CFG cfg, BreakpointCreator bkpsCreator) {
		this.cfg = cfg;
		this.bkpsCreator = bkpsCreator;
		List<CfgEdge> entryOutEdges = cfg.getEntryOutEdges();
		if (!entryOutEdges.isEmpty()) {
			CfgNode dest = entryOutEdges.get(0).getDest();
			if (dest instanceof CfgDecisionNode) {
				begin = (CfgDecisionNode) dest;
			}
		}
		nodeMap = new HashMap<Integer, CfgDecisionNode>();
		trueNext = new HashMap<CfgDecisionNode, CfgDecisionNode>();
		falseNext = new HashMap<CfgDecisionNode, CfgDecisionNode>();
		next = new HashMap<CfgDecisionNode, CfgDecisionNode>();
		parents = new HashMap<CfgDecisionNode, List<CfgDecisionNode>>();
		ends = new HashSet<CfgDecisionNode>();
		List<CfgNode> vertices = cfg.getVertices();
		for (CfgNode node : vertices) {
			if (node instanceof CfgDecisionNode) {
				
				//calculate total branches
				if (((CfgDecisionNode) node).isLoop()) {
					totalBranch += 3;
				} else {
					totalBranch += 2;
				}
				
				nodeMap.put(node.getBeginLine(), (CfgDecisionNode) node);
				List<CfgEdge> outEdges = cfg.getOutEdges(node);
				CfgDecisionNode trueNode = null;
				CfgDecisionNode falseNode = null;
				for (CfgEdge edge : outEdges) {
					CfgNode dest = edge.getDest();
					if (dest instanceof CfgDecisionNode) {
						if (edge instanceof CfgTrueEdge) {
							trueNode = (CfgDecisionNode) dest;
						} else if (edge instanceof CfgFalseEdge) {
							falseNode  = (CfgDecisionNode) dest;
						}
					}
				}
				if (trueNode != falseNode) {
					if (trueNode != null && trueNode.getBeginLine() > node.getBeginLine()) {
						trueNext.put((CfgDecisionNode) node, trueNode);
						List<CfgDecisionNode> parentList = parents.get(trueNode);
						if (parentList == null) {
							parentList = new ArrayList<CfgDecisionNode>();
							parents.put(trueNode, parentList);
						}
						parentList.add((CfgDecisionNode) node);
					}
					if (falseNode != null && falseNode.getBeginLine() > node.getBeginLine()) {
						falseNext.put((CfgDecisionNode) node, falseNode);
						List<CfgDecisionNode> parentList = parents.get(falseNode);
						if (parentList == null) {
							parentList = new ArrayList<CfgDecisionNode>();
							parents.put(falseNode, parentList);
						}
						parentList.add((CfgDecisionNode) node);
					}
				} else if (trueNode != null && trueNode.getBeginLine() > node.getBeginLine()) {
					next.put((CfgDecisionNode) node, trueNode);
					List<CfgDecisionNode> parentList = parents.get(trueNode);
					if (parentList == null) {
						parentList = new ArrayList<CfgDecisionNode>();
						parents.put(trueNode, parentList);
					}
					parentList.add((CfgDecisionNode) node);
				} else {
					ends.add((CfgDecisionNode) node);
				}
			}
		}
	}
	
	public List<Branch> getTotalBranches(){
		List<Branch> totalBranches = new ArrayList<>();
		for(Integer lineNo: nodeMap.keySet()){
			CfgDecisionNode node = nodeMap.get(lineNo);
			
			totalBranches.add(new DecisionBranch(lineNo, true));
			totalBranches.add(new DecisionBranch(lineNo, false));
			
			if(node.isLoop()){
				totalBranches.add(new LoopBranch(lineNo, true));
				totalBranches.add(new LoopBranch(lineNo, false));
			}
		}
		
		return totalBranches;
	}
	
	public void setVars(List<ExecVar> vars, List<ExecVar> originalVars) {
		this.vars = vars;
		this.originalVars = originalVars;
	}
	
	public void setPrecondition(int lineNo, Pair<Formula, Formula> formulas, List<Divider> dividers) {
		CfgDecisionNode node = nodeMap.get(lineNo);
		node.setTrueFalse(formulas.first());
		if (node.isLoop()) {
			node.setOneMore(formulas.second());
		}
		node.setDividers(dividers);
		
		if (dividers == null || dividers.isEmpty()) {
			CfgDecisionNode trueNode = trueNext.get(node);
			CfgDecisionNode falseNode = falseNext.get(node);
			if (trueNode != null || falseNode != null) {
				List<List<CategoryCalculator>> preconditions = node.getPreconditions();
				if (trueNode != null) {
					for (List<CategoryCalculator> list : preconditions) {
						List<CategoryCalculator> cur = new ArrayList<CategoryCalculator>(list);
						trueNode.addPrecondition(cur);
					}
				}
				if (falseNode != null) {
					for (List<CategoryCalculator> list : preconditions) {
						List<CategoryCalculator> cur = new ArrayList<CategoryCalculator>(list);
						falseNode.addPrecondition(cur);
					}
				}
			}
			return;
		}
		
		CfgDecisionNode trueNode = trueNext.get(node);
		CfgDecisionNode falseNode = falseNext.get(node);
		if (trueNode != null || falseNode != null) {
			List<List<CategoryCalculator>> preconditions = node.getPreconditions();
			if (trueNode != null) {
				CategoryCalculator condition = new MultiDividerBasedCategoryCalculator(dividers);
				if (preconditions.isEmpty()) {
					List<CategoryCalculator> cur = new ArrayList<CategoryCalculator>();
					cur.add(condition);
					trueNode.addPrecondition(cur);
				} else {
					for (List<CategoryCalculator> list : preconditions) {
						List<CategoryCalculator> cur = new ArrayList<CategoryCalculator>(list);
						cur.add(condition);
						trueNode.addPrecondition(cur);
					}
				}				
			}
			if (falseNode != null && !node.isLoop()) {
				CategoryCalculator condition = new MultiNotDividerBasedCategoryCalculator(dividers);
				if (preconditions.isEmpty()) {
					List<CategoryCalculator> cur = new ArrayList<CategoryCalculator>();
					cur.add(condition);
					falseNode.addPrecondition(cur);
				} else {
					for (List<CategoryCalculator> list : preconditions) {
						List<CategoryCalculator> cur = new ArrayList<CategoryCalculator>(list);
						cur.add(condition);
						falseNode.addPrecondition(cur);
					}
				}				
			}
		}
	}
	
	public OrCategoryCalculator getPreConditions(int lineNo) {
		CfgDecisionNode node = nodeMap.get(lineNo);
		return new OrCategoryCalculator(node.getPreconditions(), vars, originalVars);
	}
	
	/*public boolean isEnd(int lineNo) {
		return ends.contains(nodeMap.get(lineNo));
	}*/
	
	public boolean isRelevant(int lineNo) {
		return nodeMap.get(lineNo).isRelevant();
	}

	public List<List<Formula>> buildPaths() {
		List<List<Formula>> res = new ArrayList<List<Formula>>();
		if (begin == null) {
			return res;
		}
		buildPaths(begin, new ArrayList<Formula>(), res);
		return res;
	}
	
	private void buildPaths(CfgDecisionNode node, List<Formula> prefix, List<List<Formula>> res) {
		CfgDecisionNode trueNode = trueNext.get(node);
		CfgDecisionNode falseNode = falseNext.get(node);
		if (trueNode == null && falseNode == null) {
			trueNode = next.get(node);
			falseNode = next.get(node);
		}
		Formula trueBranch = node.getTrueFalse();
		Formula falseBranch = null;
		Formula moreBranch = null;
		if (trueBranch != null) {
			falseBranch = new NotFormula(trueBranch);
		}
		if (node.isLoop()) {
			moreBranch = node.getOneMore();
			Formula tmp = moreBranch;
			if (trueBranch != null) {
				if (moreBranch != null) {
					tmp = new AndFormula(trueBranch, moreBranch);
				}
			}
			if (moreBranch != null) {
				if (trueBranch == null) {
					trueBranch = new NotFormula(moreBranch);
				} else {
					trueBranch = new AndFormula(trueBranch, new NotFormula(moreBranch));
				}
			}
			moreBranch = tmp;
			
			List<List<Formula>> truePaths = new ArrayList<List<Formula>>();
			if (trueBranch != null) {
				if (trueNode != null) {
					prefix.add(trueBranch);
					buildPaths(trueNode, prefix, truePaths);
					prefix.remove(prefix.size() - 1);
				} else {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					cur.add(trueBranch);
					truePaths.add(cur);
				}
			} else {
				if (trueNode != null) {
					buildPaths(trueNode, prefix, truePaths);
				} else {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					truePaths.add(cur);
				}
			}
			if (moreBranch != null) {
				if (trueNode != null) {
					prefix.add(moreBranch);
					buildPaths(trueNode, prefix, truePaths);
					prefix.remove(prefix.size() - 1);
				} else {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					cur.add(moreBranch);
					truePaths.add(cur);
				}
			}
			if (falseNode != null) {
				if (falseBranch != null) {
					prefix.add(falseBranch);
					buildPaths(falseNode, prefix, res);
					prefix.remove(prefix.size() - 1);
				} else {
					buildPaths(falseNode, prefix, res);
				}
				for (List<Formula> truePath : truePaths) {
					buildPaths(falseNode, truePath, res);
				}
			} else {
				if (falseBranch != null) {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					cur.add(falseBranch);
					res.add(cur);
				}
				res.addAll(truePaths);
			}
			/*if (falseBranch != null) {
				res.addAll(truePaths);
				if (falseNode != null) {
					prefix.add(falseBranch);
					buildPaths(falseNode, prefix, res);
					prefix.remove(prefix.size() - 1);
				} else {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					cur.add(falseBranch);
					res.add(cur);
				}
			} else {
				List<Formula> pre = null;
				if (!truePaths.isEmpty()) {
					pre = truePaths.remove(0);
				}
				if (pre == null) {
					pre = new ArrayList<Formula>(prefix);
				}
				res.addAll(truePaths);
				if (falseNode != null) {
					buildPaths(falseNode, pre, res);
				} else {
					res.add(pre);
				}
			}*/
		} else { // TODO: decrease test cases where trueNode = falseNode if necessary
			if (trueBranch != null) {
				if (trueNode != null) {
					prefix.add(trueBranch);
					buildPaths(trueNode, prefix, res);
					prefix.remove(prefix.size() - 1);
				} else {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					cur.add(trueBranch);
					res.add(cur);
				}
				if (falseNode != null) {
					prefix.add(falseBranch);
					buildPaths(falseNode, prefix, res);
					prefix.remove(prefix.size() - 1);
				} else {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					cur.add(falseBranch);
					res.add(cur);
				}
			} else {
				if (trueNode != null) {
					buildPaths(trueNode, prefix, res);
				} else {
					List<Formula> cur = new ArrayList<Formula>(prefix);
					res.add(cur);
				}
				if (falseNode != null) {
					buildPaths(falseNode, prefix, res);
				}
			}
		}
	}
	
	/*public void updateRelevance(Map<DecisionLocation, BreakpointData> bkpDataMap) {
		Map<Integer, DecisionLocation> map = new HashMap<Integer, DecisionLocation>();
		Set<Entry<DecisionLocation, BreakpointData>> entrySet = bkpDataMap.entrySet();
		List<Integer> changeList = new ArrayList<Integer>();
		for (Entry<DecisionLocation, BreakpointData> entry : entrySet) {
			map.put(entry.getKey().getLineNo(), entry.getKey());
			if(updateRelevance(nodeMap.get(entry.getKey().getLineNo()), entry.getValue())) {
				changeList.add(entry.getKey().getLineNo());
			}
		}
		while (!changeList.isEmpty()) {
			List<Integer> tmp = new ArrayList<Integer>();
			for (Integer lineNo : changeList) {
				List<CfgDecisionNode> parentList = parents.get(nodeMap.get(lineNo));
				if (parentList == null) {
					continue;
				}
				for (CfgDecisionNode node : parentList) {
					if (updateRelevance(node, bkpDataMap.get(map.get(node.getBeginLine())))) {
						tmp.add(node.getBeginLine());
					}
				}
			}
			changeList = tmp;
		}
	}*/
	
	public void updateRelevance(Map<DecisionLocation, BreakpointData> dataMap) {
		Map<Integer, BreakpointData> map = new HashMap<Integer, BreakpointData>();
		Set<DecisionLocation> locations = dataMap.keySet();
		for (DecisionLocation location : locations) {
			map.put(location.getLineNo(), dataMap.get(location));
		}
		updateRelevance(begin, map);
	}

	private boolean updateRelevance(CfgDecisionNode node, Map<Integer, BreakpointData> map) {
		if (!isRelevant(node.getBeginLine())) {
			return false;
		}
		boolean relevant = false;
		CfgDecisionNode trueNode = trueNext.get(node);
		if (trueNode != null) {
			relevant |= updateRelevance(trueNode, map);
		}
		CfgDecisionNode falseNode = falseNext.get(node);
		if (falseNode != null) {
			relevant |= updateRelevance(falseNode, map);
		}
		if (trueNode == null && falseNode == null) {
			CfgDecisionNode nextNode = next.get(node);
			if (nextNode != null) {
				relevant |= updateRelevance(nextNode, map);
			}
		}
		if (relevant) {
			//System.out.println("Node: " + node.getBeginLine() + " is relevant because of children");
			return true;
		}
		BreakpointData breakpointData = map.get(node.getBeginLine());
		relevant = breakpointData.getFalseValues().isEmpty() 
					|| breakpointData.getTrueValues().isEmpty();
		if (relevant) {
			//System.out.println("Node: " + node.getBeginLine() + " is relevant because of true/false");
			return true;
		}
		if (breakpointData instanceof LoopTimesData) {
			LoopTimesData loopTimesData = (LoopTimesData) breakpointData;
			relevant = loopTimesData.getOneTimeValues().isEmpty() 
						|| loopTimesData.getMoreTimesValues().isEmpty();
			if (relevant) {
				//System.out.println("Node: " + node.getBeginLine() + " is relevant because of once/more");
				return true;
			}
		}
		node.setRelevant(false);
		return false;
	}

	/*public void updateRelevance(BreakpointData breakpointData) {
		int line = breakpointData.getLocation().getLineNo();
		if (!updateRelevance(nodeMap.get(line), breakpointData)) {
			return;
		}
		List<Integer> changeList = new ArrayList<Integer>();
		changeList.add(line);
		while (!changeList.isEmpty()) {
			List<Integer> tmp = new ArrayList<Integer>();
			for (Integer lineNo : changeList) {
				List<CfgDecisionNode> parentList = parents.get(nodeMap.get(lineNo));
				if (parentList == null) {
					continue;
				}
				for (CfgDecisionNode node : parentList) {
					if (updateRelevance(node, null)) {
						tmp.add(node.getBeginLine());
					}
				}
			}
			changeList = tmp;
		}
	}
	
	private boolean updateRelevance(CfgDecisionNode node, BreakpointData breakpointData) {
		CfgDecisionNode nextNode = trueNext.get(node);
		if (nextNode != null) {
			if (nextNode.isRelevant()) {
				node.setRelevant(true);
				return false;
			}
		}
		nextNode = falseNext.get(node);
		if (nextNode != null) {
			if (nextNode.isRelevant()) {
				node.setRelevant(true);
				return false;
			}
		}
		nextNode = next.get(node);
		if (nextNode != null) {
			if (nextNode.isRelevant()) {
				node.setRelevant(true);
				return false;
			}
		}
		if (breakpointData == null) {
			if (!node.isRelevant()) {
				return false;
			}
			node.setRelevant(false);
			return true;
		}
		if (breakpointData.getFalseValues().isEmpty() || breakpointData.getTrueValues().isEmpty()) {
			node.setRelevant(true);
			return false;
		}
		if (breakpointData instanceof LoopTimesData) {
			if (((LoopTimesData) breakpointData).getOneTimeValues().isEmpty() 
					|| ((LoopTimesData) breakpointData).getMoreTimesValues().isEmpty()) {
				node.setRelevant(true);
				return false;
			}
		}
		if (!node.isRelevant()) {
			return false;
		}
		node.setRelevant(false);
		return true;
	}*/
	
	public boolean isEnd(int lineNo) {
		return ends.contains(nodeMap.get(lineNo));
	}

	public double getTotalBranch() {
		return totalBranch;
	}

	@Override
	public DecisionBkpsData getDecisionBkpsData() {
		if (decisionBkpsData == null) {
			decisionBkpsData = bkpsCreator.createBkpsfromCfg(cfg);
		}
		return decisionBkpsData;
	}
}
