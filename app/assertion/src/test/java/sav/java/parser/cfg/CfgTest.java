/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.java.parser.cfg;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

/**
 * @author LLT
 *
 */
public class CfgTest {

	@Test
	public void forToCfg() throws ParseException {
		String code =
				"for (int i = 0; i < arr.length; i++) {" +
				"	int a = i + 5;\n" +
				"	System.out.println(a);" +
				"}";
		cfgFromStmt(code);
	}
	
	@Test
	public void whileToCfg() throws ParseException {
		String code =
				"while (i < 10) { " +
				"	int a = i + 5; " +
				"	System.out.println(a);" +
				"}";
		cfgFromStmt(code);
	}
	
	@Test
	public void ifToCfg() throws ParseException {
		String str = 
				"if (m + 3 > this.a) {" +
				"	a = i + 5;" +
				"	System.out.println(a);" +
				"} else {" +
				"	a = i + 10;" +
				"	System.out.println(m);" +
				"}";
		cfgFromStmt(str);		
	}
	
	@Test
	public void labledToCfg() throws ParseException {
		String str = 
				"a: " +
				"do {" +
				"	executeFuncA();" +
				"	b: " +
				"		for (int i = 0; i < 10; i++) {" +
				"			a.add(i);" +
				"			if (a.size() == 1) {" +
				"				continue;" +
				"			}" +
				"			if (a.size() == 2) {" +
				"				continue a;" +
				"			}" +
				"			if (a.size() == 3) {" +
				"				executeWithASize3();" +
				"			}" +
				"			if (a.size() == 4) {" +
				"				break b;" +
				"			}" +
				"			if (a.size() == 5) {" +
				"				break a;" +
				"			}" +
				"		}" +
				"	executeFuncB();" +
				"} while (x > 0);";
		cfgFromStmt(str);	
	}
	
	@Test
	public void switchToCfg() throws ParseException {
		String str = 
				"switch(x) {" +
				"	case 1:" +
				"		executeFunc1();" +
				"		break;" +
				"	case 2:" +
				"		executeFunc2();" +
				"	default:" +
				"		executeDefault();" +
				"		break;" +
				"}";
		cfgFromStmt(str);	
	}
	
	@Test
	public void switchToCfg_defaultInTheMiddle() throws ParseException {
		String str = 
				"switch(x) {" +
				"	case 1:" +
				"		executeFunc1();" +
				"		break;" +
				"	default:" +
				"		executeDefault();" +
				"	case 2:" +
				"		executeFunc2();" +
				"		break;" +
				"}";
		cfgFromStmt(str);	
	}
	
	@Test
	public void runSwitch() {
		for (int i = 1; i < 5; i++) {
			switch (i) {
			case 1:
				System.out.println(1);
				break;
			case 3:
				System.out.println(3);
				break;
			default:
				System.out.println("default");
			case 4:
				System.out.println(4);
			}
		}
	}
	
	@Test
	public void tryCatchToCfg() throws ParseException {
		String str = 
				"try (Resource r1 = new Resource();" +
				"		Resource r2 = createR2()) {" +
				"	boolean fail = executeFunc1();" +
				"	if (fail) {" +
				"		throw new ExecutionError(a);" +
				"	}" +
				"} catch (ResourceLoaddingError e2) {" +
				"	log.logError(e2);" +
				"} catch (ExecutionError e1) {" +
				"	log.logError(e1);" +
				"} finally {" +
				"	releaseResource();" +
				"}";
		cfgFromStmt(str);	
	}
	
	@Test
	public void foreachToCfg() throws ParseException {
		String str = 
				"try {" +
					"for (Var a : arr) {" +
					"	Type type = a.getType();" +
					"	if (type == null) {" +
					"		throw new IllegalArgumentException();" +
					"	}" +
					"	System.out.println(type);" +
				"	}" +
				"} catch (IllegalArgumentException e) {" +
				"		/* ignore */" +
				"		log.log(e);" +
				"} finally {" +
				"	System.out.println(\"finish\");" +
				"}";
		cfgFromStmt(str);	
	}
	
	@Test
	public void methodToCfg() throws ParseException {
		CompilationUnit cu = JavaParser.parse(getClass()
				.getResourceAsStream("/MiniTest.txt"));
		for (TypeDeclaration type : cu.getTypes()) {
			for (BodyDeclaration body : type.getMembers()) {
				if (body instanceof MethodDeclaration) {
					MethodDeclaration method = (MethodDeclaration) body;
					System.out.println("---------------------------------------------");
					System.out.println(method.getName() + method.getParameters());
					System.out.println("---------------------------------------------");
					CFG cfg = CfgFactory.createCFG(method);
					
					CfgEntryNode entry = cfg.getEntry();
					
					List<CfgNode> decisionNodes = new ArrayList<CfgNode>();
					cfg.getDecisionNode(entry, decisionNodes, new HashSet<CfgNode>());
					System.out.println("decision nodes = " + decisionNodes);
					
					for (CfgNode node : decisionNodes) {
						for (CfgEdge edge : cfg.getOutEdges(node)) {
							System.out.println("line = " + edge.getDest().getAstNode().getBeginLine());
						}
					}
					
					System.out.println(cfg.toString());
				}
			}
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testTemplate() throws ParseException {
		String str = "";
//		cfgFromStmt(str);	
	}

	private void cfgFromStmt(String str) throws ParseException {
		CfgFactory factory = new CfgFactory();
		Node node = JavaParser.parseStatement(str);
		CFG cfg = factory.toCFG(node);
		System.out.println(cfg.toString());
	}
	
	
}
