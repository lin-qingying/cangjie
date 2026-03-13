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

        configureFieldInAllLeafBuilders("source") {
            defaultNull(it)
        }
    }

    private fun CfirTree.concreteElements(): List<Element> = listOf(
        packageDirective, importDirective,
        file, classDeclaration, enumConstructor, extend, typeAlias, function, mainFunction, macroDeclaration, finalizer,
        constructor, invalidDeclaration, property, variable, patternVariable, valueParameter, typeParameter,
        block, literalExpression, stringInterpolation, functionCall, propertyAccess, qualifiedAccess, assignment, binaryOp,
        comparisonExpression, typeOperator, ifExpression, matchExpression, matchBranch, catchClause, loopExpression, forInExpression, tryExpression,
        throwExpression, returnExpression, jumpExpression, lambdaExpression, rangeExpression, arrayLiteral, tupleLiteral,
        spawnExpression, subscriptExpression, errorExpression,
        constPattern, wildcardPattern, bindingPattern, tuplePattern, enumPattern, typePattern,
        resolvedTypeRef, userTypeRef, basicTypeRef, implicitTypeRef, functionTypeRef, tupleTypeRef, varrayTypeRef, errorTypeRef,
        namedReference, resolvedNamedReference, errorReference
    )
}
