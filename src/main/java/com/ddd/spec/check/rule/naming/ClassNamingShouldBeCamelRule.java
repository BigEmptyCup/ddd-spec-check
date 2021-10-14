package com.ddd.spec.check.rule.naming;

import com.ddd.spec.check.rule.I18nResources;
import com.ddd.spec.check.rule.utils.ViolationUtils;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;

import java.util.regex.Pattern;

public class ClassNamingShouldBeCamelRule extends AbstractJavaRule {

    private static final Pattern PATTERN
            = Pattern.compile("^I?([A-Z][a-z0-9]+)+(([A-Z])|(DO|DTO|VO|DAO|BO|DAOImpl|YunOS|AO|PO))?$");

    @Override
    public Object visit(ASTClassOrInterfaceDeclaration node, Object data) {
        if (PATTERN.matcher(node.getImage()).matches()) {
            return super.visit(node, data);
        }
        
        ViolationUtils.addViolationWithPrecisePosition(this, node, data,
                I18nResources.getMessage("java.naming.ClassNamingShouldBeCamelRule.violation.msg",
                        node.getImage()));

        return super.visit(node, data);
    }
}
