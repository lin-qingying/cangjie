package org.cangjie.cfir.tree.generator

import org.cangjie.cfir.tree.generator.context.AbstractCfirTreeImplementationConfigurator
import org.cangjie.cfir.tree.generator.model.Element

object ImplementationConfigurator : AbstractCfirTreeImplementationConfigurator() {
    override fun configure(model: Model) = with(CfirTree) {
        noImpl(declaration)
        noImpl(declarationStatus)
        noImpl(expression)
        noImpl(pattern)
        noImpl(typeRef)
        noImpl(reference)

        concreteElements().forEach { element ->
            impl(element) {
                publicImplementation()
                defaultNull("source", withGetter = true)
            }
        }
    }

    override fun configureAllImplementations(model: Model) {
        configureFieldInAllImplementations("source") {
            defaultNull(it, withGetter = true)
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
