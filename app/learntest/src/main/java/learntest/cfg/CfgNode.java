package learntest.cfg;

import japa.parser.ast.Node;

public interface CfgNode {
	
	public Type getType();
	
	public Node getAstNode();
	
	public int getBeginLine();
	
	public int getTrueBeginLine();
	
	public String getStmtType();

	public static enum Type {
		ENTRY,
		EXIT,
		DECISIONS
	}

}
