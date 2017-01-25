package datastructure;

import java.util.List;

import japa.parser.ASTHelper;
import japa.parser.ast.Node;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.AssignExpr.Operator;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.ModifierVisitorAdapter;
import sav.strategies.dto.BreakPoint.Variable;

public class AddItselfCodeVisitor<A> extends ModifierVisitorAdapter<A> {
	
	private String methodName;

	private int lineNo;

	// variables at learning location {x, y, ...}
	private Variable learnVar;
	
	public AddItselfCodeVisitor(String methodName, int lineNo, Variable learnVar) {
		this.methodName = methodName;
		this.lineNo = lineNo;
		this.learnVar = learnVar;
	}
	
	@Override
	public Node visit(BlockStmt n, A arg) {
		BlockStmt block = new BlockStmt();

		List<Statement> stmts = n.getStmts();

		if (stmts != null) {
			for (int i = 0; i < stmts.size(); i++) {
				Statement stmt = (Statement) stmts.get(i);
				
				if (stmt.getBeginLine() == lineNo) {
					if (!learnVar.getType().equals("int")) {
						Expression var = new NameExpr(learnVar.getFullName());
						
						NameExpr clazz = new NameExpr("Utility");
						MethodCallExpr call = new MethodCallExpr(clazz, "toItself");
						
						ASTHelper.addArgument(call, var);
						
						ASTHelper.addStmt(block, call);
					}
				}

				ASTHelper.addStmt(block, stmt);
				stmts.set(i, (Statement) stmt.accept(this, arg));
			}

			removeNulls(stmts);
		}

		return block;
	}
	
	private void removeNulls(final List<?> list) {
		for (int i = list.size() - 1; i >= 0; i--) {
			if (list.get(i) == null) {
				list.remove(i);
			}
		}
	}

}
