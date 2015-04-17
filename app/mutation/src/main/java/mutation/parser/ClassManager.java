package mutation.parser;

import japa.parser.ast.expr.NameExpr;

import java.util.HashMap;
import java.util.Map;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by hoangtung on 4/1/15.
 */
public class ClassManager
{
    public Map<String, ClassDescriptor> classMap;

    public ClassManager()
    {
        classMap = new HashMap<>();
    }

    public void acceptClass(ClassDescriptor cl)
    {
        classMap.put(cl.getQuantifiedName(), cl);
    }

    public void merge(ClassManager cm)
    {
        classMap.putAll(cm.classMap);
    }

    /**
     * get class descriptor from quantified / non-quantified class name
     * @param className
     * @return
     */
    public ClassDescriptor getClassFromName(String className)
    {
        throw new NotImplementedException();
    }

    /**
     * get variable descriptor from its name
     * @param className
     * @param quantifiedVarName
     * @param beginLine
     * @param endLine
     * @return
     */
    public VariableDescriptor getVarFromName(String className, String quantifiedVarName, int beginLine, int endLine)
    {
        throw new NotImplementedException();
    }

    public VariableDescriptor getVarFromName(String className, NameExpr varName)
    {
        throw new NotImplementedException();
    }
}
