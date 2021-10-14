package com.ddd.spec.check.rule.naming;

import com.ddd.spec.check.rule.I18nResources;
import com.ddd.spec.check.rule.utils.ViolationUtils;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

public class ConstantFieldShouldBeUpperCaseRule extends AbstractJavaRule {

    @Override
    public Object visit(ASTFieldDeclaration node, Object data) {
        String constantName = node.jjtGetChild(1).jjtGetChild(0).getImage();
        //Constant should be upper
        if (!(constantName.equals(constantName.toUpperCase()))) {
            ViolationUtils.addViolationWithPrecisePosition(this, node, data,
                    I18nResources.getMessage("java.naming.ConstantFieldShouldBeUpperCaseRule.violation.msg",
                            constantName));
        }
        return super.visit(node, data);
    }

}
