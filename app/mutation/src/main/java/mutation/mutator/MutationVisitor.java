package mutation.mutator;

import static mutation.mutator.AstNodeFactory.nameExpr;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.AssignExpr.Operator;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.visitor.CloneVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mutation.parser.ClassAnalyzer;
import mutation.parser.ClassDescriptor;
import mutation.parser.VariableDescriptor;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.Randomness;

/**
 * Created by hoangtung on 4/3/15.
 */
public class MutationVisitor extends AbstractMutationVisitor {
	private static final int INT_CONST_ADJUST_HALF_VALUE = 5;
	private List<Integer> lineNumbers;
	private Map<Integer, List<MutationNode>> result;
	private MutationMap mutationMap;
	private CloneVisitor nodeCloner;
	private ClassAnalyzer clasAnalyzer;
	private ClassDescriptor classDescriptor;
	
	public void reset(ClassDescriptor classDescriptor, List<Integer> lineNos) {
		this.lineNumbers = lineNos;
		this.classDescriptor = classDescriptor;
		result.clear();
	}

	public MutationVisitor(MutationMap mutationMap, ClassAnalyzer classAnalyzer) {
		lineNumbers = new ArrayList<Integer>();
		result = new HashMap<Integer, List<MutationNode>>();
		nodeCloner = new CloneVisitor();
		setMutationMap(mutationMap);
		setClasAnalyzer(classAnalyzer);
	}
	
	public void run(CompilationUnit cu) {
		cu.accept(this, true);
		
	}
	
	@Override
	protected boolean beforeVisit(Node node) {
		for (Integer lineNo : lineNumbers) {
			if (lineNo >= node.getBeginLine() && lineNo <= node.getEndLine()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected boolean beforeMutate(Node node) {
		return lineNumbers.contains(node.getBeginLine());
	}
	
	@Override
	public void visit(ForStmt n, Boolean arg) {
		if (beforeVisit(n)) {
			n.getBody().accept(this, arg);
		}
	}
	
	@Override
	public void visit(WhileStmt n, Boolean arg) {
		if (beforeVisit(n)) {
			n.getBody().accept(this, arg);
		}
	}
	
	private MutationNode newNode(Node node) {
		MutationNode muNode = new MutationNode(node);
		CollectionUtils.getListInitIfEmpty(result, node.getBeginLine())
				.add(muNode);
		return muNode;
	}
	
	@Override
	public boolean mutate(AssignExpr n) {
		MutationNode muNode = newNode(n);
		// change the operator
		List<Operator> muOps = mutationMap.getMutationOp(n.getOperator());
		if (!muOps.isEmpty()) {
			for (Operator muOp : muOps) {
				AssignExpr newNode = (AssignExpr) nodeCloner.visit(n, null); 
				newNode.setOperator(muOp);
				muNode.getMutatedNodes().add(newNode);
			}
		}
		return true;
	}
	
	@Override
	public boolean mutate(BinaryExpr n) {
		MutationNode muNode = newNode(n);
		// change the operator
		List<BinaryExpr.Operator> muOps = mutationMap.getMutationOp(n.getOperator());
		if (!muOps.isEmpty()) {
			for (BinaryExpr.Operator muOp : muOps) {
				BinaryExpr newNode = (BinaryExpr) nodeCloner.visit(n, null); 
				newNode.setOperator(muOp);
				muNode.getMutatedNodes().add(newNode);
			}
		}
		return true;
	}
	
	@Override
	public boolean mutate(NameExpr n) {
		MutationNode muNode = newNode(n);
		VariableSubstitution varSubstituion = new VariableSubstitutionImpl(
				n.getName(), n.getEndLine(), n.getEndColumn(), classDescriptor);
		List<VariableDescriptor> candidates = varSubstituion.find();
		for (VariableDescriptor var : candidates) {
			if (!var.getName().equals(n.getName())) {
				muNode.getMutatedNodes().add(nameExpr(var.getName()));
			}
		}
		return false;
	}
	
	@Override
	public boolean mutate(BooleanLiteralExpr n) {
		newNode(n).getMutatedNodes().add(new BooleanLiteralExpr(!n.getValue()));
		return false;
	}
	
	@Override
	public boolean mutate(IntegerLiteralExpr n) {
		Integer val = Integer.valueOf(n.getValue());
		Integer newVal = val;
		if (val == Integer.MAX_VALUE) {
			newVal -= Randomness.nextInt(INT_CONST_ADJUST_HALF_VALUE);
		} else if (val == Integer.MIN_VALUE) {
			newVal += Randomness.nextInt(INT_CONST_ADJUST_HALF_VALUE);
		} else {
			newVal = Randomness.nextInt(val - INT_CONST_ADJUST_HALF_VALUE, 
					val + INT_CONST_ADJUST_HALF_VALUE);
		}
		newNode(n).getMutatedNodes().add(
				new IntegerLiteralExpr(newVal.toString()));
		return false;
	}
	
	public void setMutationMap(MutationMap mutationMap) {
		this.mutationMap = mutationMap;
	}
	
	public void setClasAnalyzer(ClassAnalyzer clasAnalyzer) {
		this.clasAnalyzer = clasAnalyzer;
	}
	
	public CloneVisitor getNodeCloner() {
		return nodeCloner;
	}
	
	public Map<Integer, List<MutationNode>> getResult() {
		return result;
	}
	
	public static class MutationNode {
		private Node orgNode;
		private List<Node> mutatedNodes;
		
		public MutationNode(Node orgNode) {
			mutatedNodes = new ArrayList<Node>();
			this.orgNode = orgNode;
		}
		
		public static MutationNode of(Node orgNode, Node newNode) {
			MutationNode node = new MutationNode(orgNode);
			node.mutatedNodes.add(newNode);
			return node;
		}

		public Node getOrgNode() {
			return orgNode;
		}

		public List<Node> getMutatedNodes() {
			return mutatedNodes;
		}
	}
}
