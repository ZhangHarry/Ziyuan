/**
 * Copyright TODO
 */
package junit;

import java.util.ArrayList;
import java.util.List;

import gentest.VariableNamer;
import gentest.commons.utils.TypeUtils;
import gentest.data.statement.RAssignment;
import gentest.data.statement.RConstructor;
import gentest.data.statement.Rmethod;
import japa.parser.ASTHelper;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LiteralExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.PrimitiveType.Primitive;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import sav.common.core.utils.Assert;
import sav.common.core.utils.CollectionUtils;

/**
 * @author LLT
 *
 */
public class AstNodeConverter {
	private VariableNamer varNamer;
	
	public AstNodeConverter(VariableNamer varNamer) {
		this.varNamer = varNamer;
	}
	
	/**
	 * assignment for primitive types, String, enum (constantTypes)
	 */
	public Statement fromRAssignment(RAssignment assignment) {
		/* type */
		Type paramType = null;
		Class<?> vartype = assignment.getType();
		if (TypeUtils.isPrimitive(vartype)) {
			/* primitive */
			paramType = new PrimitiveType(TypeUtils.getAssociatePrimitiveType(vartype));
		} else {
			/* primitive wrapper, enum or String*/
			paramType = toReferenceType(vartype.getSimpleName());
		}
		
		/* value */
		Object varValue = assignment.getValue();
		/* primitive or primitive wrapper */
		Expression initExpr = toNullLiteralExpr(varValue);
		if (initExpr == null) {
			initExpr = toPrimitiveLiteralExpr(vartype, varValue);
		}
		if (initExpr == null) {
			/* string */
			initExpr = toStringLiteralExpr(vartype, varValue);
		} 
		if (initExpr == null) {
			/* enum */
			initExpr = toEnumFieldExpr(vartype, varValue);
		}
		Assert.assertTrue(initExpr != null);

		VariableDeclarator varDecl = new VariableDeclarator();
		varDecl.setId(new VariableDeclaratorId(varNamer.getName(assignment
				.getOutVarId())));
		varDecl.setInit(initExpr);
		Expression expr = new VariableDeclarationExpr(paramType, CollectionUtils.listOf(varDecl));
		ExpressionStmt stmt = new ExpressionStmt(expr);
		return stmt;
	}

	private Expression toNullLiteralExpr(Object varValue) {
		if (varValue == null) {
			return new NullLiteralExpr();
		}
		return null;
	}

	private FieldAccessExpr toEnumFieldExpr(Class<?> vartype, Object varValue) {
		if (TypeUtils.isEnum(varValue)) {
			Enum<?> enumValue = (Enum<?>) varValue;
			NameExpr scope = ASTHelper.createNameExpr(enumValue
					.getDeclaringClass().getSimpleName());
			return new FieldAccessExpr(scope, enumValue.name());
		}
		return null;
	}

	private LiteralExpr toStringLiteralExpr(Class<?> vartype, Object varValue) {
		if (TypeUtils.isString(vartype)) {
			return new StringLiteralExpr(varValue.toString());
		}
		return null;
	}

	private LiteralExpr toPrimitiveLiteralExpr(Class<?> vartype, Object value) {
		/* primitive or primitiveWrapper */
		Primitive primitiveType = TypeUtils.getAssociatePrimitiveType(vartype);
		if (primitiveType != null) {
			switch (primitiveType) {
			case Boolean:
				return new BooleanLiteralExpr((Boolean) value);
			case Char:
				return new CharLiteralExpr(value.toString());
			case Byte:
			case Int:
			case Short:
				return new IntegerLiteralExpr(value.toString());
			case Double:
				return new DoubleLiteralExpr(value.toString());
			case Float:
			case Long:
				return new LongLiteralExpr(value.toString());
			default:
				return new StringLiteralExpr(value.toString());
			}
		}
		return null;
	}

	public Statement fromRConstructor(RConstructor constructor) {
		/* Type */
		Type type = toReferenceType(constructor.getName());
		List<VariableDeclarator> vars = new ArrayList<VariableDeclarator>();
		/* variable name*/
		VariableDeclarator var = new VariableDeclarator(
				new VariableDeclaratorId(varNamer.getName(constructor
						.getOutVarId())));
		/* constructor input */
		List<Expression> constructorArgs = new ArrayList<Expression>();
		for (int inVar : constructor.getInVarIds()) {
			constructorArgs.add(new NameExpr(varNamer.getName(inVar)));
		}
		/* statement */
		Expression initExpr = new ObjectCreationExpr(null,
				new ClassOrInterfaceType(constructor.getName()), constructorArgs);
		var.setInit(initExpr);
		vars.add(var);
		Expression expr = new VariableDeclarationExpr(type , vars );
		ExpressionStmt stmt = new ExpressionStmt(expr);
		return stmt;
	}

	public Statement fromRMethod(Rmethod rmethod) {
		NameExpr scope = new NameExpr(varNamer.getName(rmethod.getReceiverVarId()));
		List<Expression> inputs = new ArrayList<Expression>();
		for (int inId : rmethod.getInVarIds()) {
			inputs.add(new NameExpr(varNamer.getName(inId)));
		}
		MethodCallExpr callExpr = new MethodCallExpr(scope, rmethod.getName(), inputs);
		
		Expression stmtExpr = null;
		if (rmethod.hasOutputVar()) {
			List<VariableDeclarator> vars = new ArrayList<VariableDeclarator>();
			VariableDeclarator varDecl = new VariableDeclarator(
					new VariableDeclaratorId(varNamer.getName(rmethod.getOutVarId())), 
					callExpr);
			vars.add(varDecl);
			stmtExpr = new VariableDeclarationExpr(toReferenceType(rmethod
					.getReturnType().getSimpleName()), vars);
		} else {
			stmtExpr = callExpr;
		}
		return new ExpressionStmt(stmtExpr);
	}
	
	private ReferenceType toReferenceType(String typeName) {
		return new ReferenceType(new ClassOrInterfaceType(typeName));
	}
}
