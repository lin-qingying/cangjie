package org.cangnova.cangjie.cfir.tree.generator

import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.config.AbstractBuilderConfigurator

class BuilderConfigurator(model: Model) : AbstractBuilderConfigurator<Element, Implementation, Field>(model) {
    override val namePrefix: String
        get() = "Cfir"

    override val defaultBuilderPackage: String
        get() = "org.cangnova.cangjie.cfir.tree.builder"

    override fun configureBuilders() = with(CfirTree) {
        concreteElements().forEach { element ->
            builder(element) {
                withCopy()
            }
        }

        // constructor 有两个具名实现，需要分别指定
        builder(constructor, "CfirConstructorImpl") {
            withCopy()
        }
        builder(constructor, "CfirPrimaryConstructor") {
            withCopy()
        }

        // resolvedImportDirective 单独配置
        builder(resolvedImportDirective) {
            withCopy()
        }

        // lazyBlock / lazyExpression 不需要 builder（占位节点，不对外构造）


    }

    private fun CfirTree.concreteElements(): List<Element> = listOf(
        packageDirective,
        // importDirective 已改为抽象基类，由 resolvedImportDirective 替代
        // constructor 已单独配置为两个具名实现

        // -------- 声明节点 --------
        file, classDeclaration, interfaceDeclaration,structDeclaration,enumDeclaration,  enumConstructor, extend, typeAlias, namedFunction, anonymousFunction, mainFunction, macroDeclaration, finalizer,
        invalidDeclaration, property, fieldVariable, patternVariable, valueParameter, typeParameter,

        // -------- 语句 / 表达式节点 --------
        block,
        literalExpression, stringInterpolation, functionCall, propertyAccess, qualifiedAccess, assignment, binaryOp,
        comparisonExpression, typeOperator, ifExpression, matchExpression, matchBranch, catchClause, loopExpression, forInExpression, tryExpression,
        throwExpression, returnExpression, jumpExpression, anonymousFunctionExpression, rangeExpression, arrayLiteral, tupleLiteral,
        spawnExpression, synchronizedExpression, unsafeExpression, quoteExpression, macroExpression, subscriptExpression, errorExpression,

        // -------- 模式节点 --------
        constPattern, wildcardPattern, bindingPattern, tuplePattern, enumPattern, typePattern,

        // -------- 类型引用节点 --------
        resolvedTypeRef, userTypeRef, basicTypeRef, implicitTypeRef, functionTypeRef, tupleTypeRef, varrayTypeRef,

        // -------- 引用节点 --------
        namedReference, resolvedNamedReference, errorReference, thisReference,
    )
}
