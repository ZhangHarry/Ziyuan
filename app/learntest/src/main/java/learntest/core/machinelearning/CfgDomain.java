package learntest.core.machinelearning;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cfg.CfgNode;

import java.util.Queue;

import learntest.core.commons.data.decision.DecisionProbes;

/**
 * @author ZhangHr
 */
public class CfgDomain {
	private static Logger log = LoggerFactory.getLogger(PrecondDecisionLearner.class);
	HashMap<CfgNode, CfgNodeDomainInfo> dominationMap = new HashMap<>();

	/**
	 * start node should cover all nodes ,but there may be a bug? 
	 * todo : check org.apache.commons.math.linear.BigMatrixImpl.getSubMatrix.643
	 * @param startNode
	 * @param decisionNodes
	 * @return
	 */
	public HashMap<CfgNode, CfgNodeDomainInfo> constructDominationMap(CfgNode startNode, List<CfgNode> decisionNodes) {
		/** get post domain relationship */
		initDominationMap(dominationMap, startNode);
		while (travelAndChange(dominationMap)) {
			;
		}
		
		/** get control dependency */
		HashMap<CfgNode, Integer> visited = new HashMap<>();
		Queue<CfgNode> queue = new LinkedList<>();
		queue.add(startNode);
		while (queue.size() > 0){
			CfgNode node = queue.poll();
			visited.put(node, node.getLine());
			CfgNodeDomainInfo domainInfo = dominationMap.get(node);
			HashMap<CfgNode, Integer> postD = domainInfo.postDomain;
			List<CfgNode> children = DecisionProbes.getChildDecision(node);
			for (CfgNode child : children) {
				if (!postD.containsKey(child)) { // child is reachable from node, and child is not post-dominator of node 
					domainInfo.addDominatee(child);
					dominationMap.get(child).addDominator(node);
				}else{ // child is post-dominator of node , the dominators of node may also dominate child
					for (CfgNode dominator : domainInfo.dominators) {
						HashMap<CfgNode, Integer> doMinatorPostD = dominationMap.get(dominator).postDomain; 
						if (!doMinatorPostD.containsKey(child)) {
							dominationMap.get(dominator).addDominatee(child);
							dominationMap.get(child).addDominator(dominator);
						}
					}
				}
				if (!visited.containsKey(child)) {
					queue.add(child);
				}
			}
		}
		
		for (CfgNode cfgNode : decisionNodes) {
			if (dominationMap.get(cfgNode) == null) {
				dominationMap.put(cfgNode, new CfgNodeDomainInfo(cfgNode));
			}
		}
		return dominationMap;
	}

	private boolean travelAndChange(HashMap<CfgNode, CfgNodeDomainInfo> dominationMap) {
		boolean modified = false;
		Set<Entry<CfgNode, CfgNodeDomainInfo>> set = dominationMap.entrySet();
		for (Entry<CfgNode, CfgNodeDomainInfo> entry : set) {
			CfgNodeDomainInfo curNodeInfo = dominationMap.get(entry.getKey());
			HashMap<CfgNode, Integer> post = new HashMap<>();
			if (curNodeInfo.children.size() == 1) {
				CfgNode child = curNodeInfo.children.keySet().iterator().next();
				post.putAll(dominationMap.get(child).postDomain);
				post.put(child, child.getLine());
			}else if (curNodeInfo.children.size() > 1) {
				post.putAll(getCommonPostD(curNodeInfo.children)); // get intersection of post-domain set
			}
			if(addAndmodified(curNodeInfo, post))
				modified = true;;
		}
		
		/**
		 * check the situation of loop.
		 * The loop header may have two branches, first branch is inloop and does not exit process, second branch break loop.
		 * Thus, loop header should dominate second branch indeed.
		 */
		if (!modified) {
			for (Entry<CfgNode, CfgNodeDomainInfo> entry : set) {
				CfgNodeDomainInfo curNodeInfo = dominationMap.get(entry.getKey());
				if (curNodeInfo.children.size() > 1) {
					CfgNode dominatedChild;
					if ( (dominatedChild = doMinatedChild(curNodeInfo)) != null) {
						HashMap<CfgNode, Integer> post = new HashMap<>();
						post.putAll(dominationMap.get(dominatedChild).postDomain);
						post.put(dominatedChild, dominatedChild.getLine());
						if(addAndmodified(curNodeInfo, post))
							modified = true;;
					}
				}
			}
		}
		
		return modified;
	}

	private CfgNode doMinatedChild(CfgNodeDomainInfo curNodeInfo) {
		int count = 0;
		CfgNode domainee = null;
		for (CfgNode node : curNodeInfo.children.keySet()) {
			if (!dominationMap.get(node).postDomain.containsKey(curNodeInfo.node)) {
				count++;
				domainee = node;
				if (count > 1) {
					return null;
				}
			}
		}
		return domainee;
	}

	private boolean addAndmodified(CfgNodeDomainInfo curNodeInfo, HashMap<CfgNode, Integer> post) {
		int originalSize = curNodeInfo.postDomain.size();
		curNodeInfo.postDomain.putAll(post);
		int curSize = curNodeInfo.postDomain.size();
		if (curSize > originalSize) {
			return true;
		}
		return false;
	}

	private HashMap<CfgNode, Integer> getCommonPostD(HashMap<CfgNode, Integer> parent) {
		CfgNode[] nodes = parent.keySet().toArray(new CfgNode[]{});
		HashMap<CfgNode, Integer> commons = new HashMap<>();
		commons.putAll(dominationMap.get(nodes[0]).postDomain);
		int i =1;
		while (i<nodes.length && commons.size() > 0){
			HashMap<CfgNode, Integer> list = dominationMap.get(nodes[i]).postDomain;
			HashMap<CfgNode, Integer> temp = new HashMap<>(list.size());
			for (CfgNode cfgNode : list.keySet()) {
				if (commons.containsKey(cfgNode)) {
					temp.put(cfgNode, cfgNode.getLine());
				}
			}
			i++;
			commons.clear();
			commons = temp;
		}
		return commons;
	}

	private void initDominationMap(HashMap<CfgNode, CfgNodeDomainInfo> dominationMap, CfgNode startNode) {
		Stack<CfgNode> stack = new Stack<>();
		/** establish direct relationship */
		stack.push(startNode);
		if (!dominationMap.containsKey(startNode)) {
			dominationMap.put(startNode, new CfgNodeDomainInfo(startNode));
		}
		while (!stack.isEmpty()) {
			CfgNode curNode = stack.pop();
			dominationMap.get(curNode).postDomain.put(curNode, curNode.getLine());
			Collection<CfgNode> children = curNode.getBranches();
			if (children!=null && children.size() > 0) {
				for (CfgNode node : children) {
					if (!dominationMap.containsKey(node)) {
						dominationMap.put(node, new CfgNodeDomainInfo(node));
						stack.push(node);
					}
					dominationMap.get(curNode).children.put(node, node.getLine());
				}
			}
		}
	}

}
