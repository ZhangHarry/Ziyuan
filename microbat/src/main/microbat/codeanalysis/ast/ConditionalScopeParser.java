package microbat.codeanalysis.ast;

import microbat.model.Scope;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class ConditionalScopeParser {
	
	class ScopeVisitor extends ASTVisitor{
		CompilationUnit cu;
		Scope scope;
		int conditionLineNumber;
		
		public ScopeVisitor(CompilationUnit cu, int lineNumber){
			this.cu = cu;
			this.conditionLineNumber = lineNumber;
		}
		
		public boolean visit(DoStatement statement){
			int line = cu.getLineNumber(statement.getExpression().getStartPosition());
			if(line == conditionLineNumber){
				setScope(statement);
				return false;
			}
			
			return true;
		}
		
		public boolean visit(EnhancedForStatement statement){
			int line = cu.getLineNumber(statement.getStartPosition());
			if(line == conditionLineNumber){
				setScope(statement);
				return false;
			}
			return true;
		}
		
		public boolean visit(ForStatement statement){
			int line = cu.getLineNumber(statement.getExpression().getStartPosition());
			if(line == conditionLineNumber){
				setScope(statement);
				return false;
			}
			return true;
		}
		
		public boolean visit(IfStatement statement){
			int line = cu.getLineNumber(statement.getExpression().getStartPosition());
			if(line == conditionLineNumber){
				setScope(statement);
				return false;
			}
			return true;
		}
		
		public boolean visit(SwitchStatement statement){
			int line = cu.getLineNumber(statement.getExpression().getStartPosition());
			if(line == conditionLineNumber){
				setScope(statement);
				return false;
			}
			return true;
		}
		
		public boolean visit(WhileStatement statement){
			int line = cu.getLineNumber(statement.getExpression().getStartPosition());
			if(line == conditionLineNumber){
				setScope(statement);
				return false;
			}
			return true;
		}
		
		private void setScope(Statement statement){
			scope = new Scope();
			scope.setCompilationUnit(cu);
			scope.setStartLine(cu.getLineNumber(statement.getStartPosition())); 
			scope.setEndLine(cu.getLineNumber(statement.getStartPosition()+statement.getLength())); 
		}
	}
	
	public Scope parseScope(CompilationUnit cu, int conditionLineNumber){
		
		ScopeVisitor scopeVisitor = new ScopeVisitor(cu, conditionLineNumber);
		cu.accept(scopeVisitor);
		
		Scope scope = scopeVisitor.scope;
		return scope;
	}
}
