package microbat.codeanalysis.ast;

import java.util.ArrayList;
import java.util.List;

import microbat.util.JavaUtil;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

public class VariableScopeParser {

	private List<LocalVariableScope> variableScopeList = new ArrayList<>();
	
	public void parseLocalVariableScopes(List<String> interestingClasses){
		System.currentTimeMillis();
		for(String qualifiedName: interestingClasses){
			ICompilationUnit iunit = JavaUtil.findICompilationUnitInProject(qualifiedName);
			if(iunit == null){
				System.err.println("The class " + qualifiedName + 
						" does not have its ICompilationUnit in workspace");
			}
			else{
				CompilationUnit cu = JavaUtil.convertICompilationUnitToASTNode(iunit);
				parseLocalVariables(cu);
			}
		}
	}

	private void parseLocalVariables(CompilationUnit cu) {
		cu.accept(new ASTVisitor() {
			public boolean visit(VariableDeclarationFragment fragment){
				SimpleName name = fragment.getName();
				ASTNode scope = findLeastContainingBlock(name);
				if(scope != null){
					LocalVariableScope lvs = new LocalVariableScope(name.getIdentifier(), scope);
					if(!variableScopeList.contains(lvs)){
						variableScopeList.add(lvs);						
					}
				}
				return true;
			}
			
			public boolean visit(SingleVariableDeclaration svd){
				SimpleName name = svd.getName();
				ASTNode scope = findLeastContainingBlock(name);
				if(scope != null){
					LocalVariableScope lvs = new LocalVariableScope(name.getIdentifier(), scope);
					if(!variableScopeList.contains(lvs)){
						variableScopeList.add(lvs);						
					}			
				}
				
				return false;
			}
			
			
		});
		
	}

	private ASTNode findLeastContainingBlock(ASTNode node){
		ASTNode parentNode = node.getParent();
		while(!(parentNode instanceof Block) && 
				!(parentNode instanceof MethodDeclaration) &&
				!(parentNode instanceof ForStatement) &&
				!(parentNode instanceof DoStatement) &&
				!(parentNode instanceof EnhancedForStatement) &&
				!(parentNode instanceof IfStatement) &&
				!(parentNode instanceof SwitchCase) &&
				!(parentNode instanceof TryStatement) &&
				!(parentNode instanceof WhileStatement)){
			parentNode = parentNode.getParent();
			if(parentNode == null){
				break;
			}
		}
		
		return parentNode;
	}
	
	public List<LocalVariableScope> getVariableScopeList() {
		return variableScopeList;
	}
	
//	public LocalVariableScope parseScope(BreakPoint breakPoint, LocalVar localVar){
//		final CompilationUnit cu = JavaUtil.findCompilationUnitInProject(breakPoint.getClassCanonicalName());
//		final String varName = localVar.getName();
//		final int lineNumber = breakPoint.getLineNo();
//		
//		cu.accept(new ASTVisitor(){
//			public boolean visit(SimpleName name){
//				int lin = cu.getLineNumber(name.getStartPosition());
//				if(lin == lineNumber && varName.equals(name.getIdentifier())){
//					if(name.resolveBinding() instanceof IVariableBinding){
//						ASTNode scope = findLeastContainingBlock(name);
//						if(scope != null){
//							LocalVariableScope lvs = new LocalVariableScope(name.getIdentifier(), scope);
//							variableScopeList.add(lvs);
//						}
//					}
//				}
//				
//				return false;
//			}
//		});
//		
//		/**
//		 * Analyzing the byte code could let us know that a "this" local variable is implicitly read or written, however,
//		 * we may not explicitly find a "this" in AST.
//		 */
//		if(this.variableScopeList.isEmpty() && varName.equals("this")){
//			return null;
//		}
//		else{
//			LocalVariableScope lvScope = this.variableScopeList.get(0);
//			return lvScope;			
//		}
//	}

	public LocalVariableScope parseMethodScope(String typeName, final int lineNumber, final String variableName) {
		final CompilationUnit cu = JavaUtil.findCompilationUnitInProject(typeName);
		
		if(cu == null){
			System.currentTimeMillis();
		}
		
		cu.accept(new ASTVisitor(){
			public boolean visit(MethodDeclaration md){
				
				int startLine = cu.getLineNumber(md.getStartPosition());
				int endLine = cu.getLineNumber(md.getStartPosition()+md.getLength());
				if(startLine <= lineNumber && endLine >= lineNumber){
					LocalVariableScope lvs = new LocalVariableScope(variableName, md);
					variableScopeList.add(lvs);
				}
				
				return false;
			}
		});
		
		return this.variableScopeList.get(0);
	}
}
