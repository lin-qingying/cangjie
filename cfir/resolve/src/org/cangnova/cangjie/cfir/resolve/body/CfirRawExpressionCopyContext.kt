package org.cangnova.cangjie.cfir.resolve.body

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.CfirDefaultRawCopyContext
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirRawCopyState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.LoopJumpScope
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 从真实词法入口建立 RAW 表达式的复制环境。
 * 内部声明由 copier 分配独立符号，外部声明、循环目标及输入推断变量仅显式借用。
 * 资源依赖计算与独立调用试算共用相同的 metadata 所有权规则。
 */
internal fun createRawExpressionCopyContext(
    expression: CfirExpression,
    context: BodyResolveContext,
    incomingVariables: Set<TypeConstructorMarker>,
    additionalDeclarations: Collection<CfirDeclaration> = emptyList(),
): CfirDefaultRawCopyContext {
    fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())

    val borrowedElements = identitySet<CfirElement>()
    val borrowedSymbols = identitySet<CfirBasedSymbol<*>>()
    val handles = identitySet<Any>()
    val ownedSymbols = identitySet<CfirBasedSymbol<*>>()
    fun borrow(declaration: CfirDeclaration) {
        borrowedElements += declaration
        borrowedSymbols += declaration.symbol
        handles += declaration.moduleData
        if (declaration is CfirTypeParameterRefsOwner) declaration.typeParameters.forEach { borrowedSymbols += it.symbol }
        if (declaration is CfirFunction) declaration.valueParameters.forEach { borrowedSymbols += it.symbol }
        if (declaration is CfirClassLikeDeclaration) handles += declaration.scopeProvider
        if (declaration is CfirPatternVariable) declaration.pattern.bindingVariables().forEach { borrowedSymbols += it.symbol }
    }
    context.containers.forEach(::borrow)
    context.file.declarations.forEach(::borrow)
    additionalDeclarations.forEach(::borrow)
    context.towerDataContext.localScopes.forEach { scope ->
        borrowedSymbols += scope.variables.values
        borrowedSymbols += scope.properties.values
        borrowedSymbols += scope.classLikeSymbols.values
    }
    context.implicitValueStorage.implicitValues.forEach { borrowedSymbols += it.boundSymbol }
    context.loopJumpScopes.forEach { scope ->
        when (scope) {
            is LoopJumpScope.Body -> borrowedElements += scope.loop
            is LoopJumpScope.ConditionPart -> borrowedElements += scope.loop
            LoopJumpScope.FunctionBoundary -> Unit
        }
    }
    expression.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirDeclaration) {
                ownedSymbols += element.symbol
                handles += element.moduleData
                if (element is CfirClassLikeDeclaration) handles += element.scopeProvider
            }
            if (element is CfirResolvedNamedReference) borrowedSymbols += element.resolvedSymbol
            if (element is CfirThisReference) element.boundSymbol?.let(borrowedSymbols::add)
            if (element is CfirResolvedTypeRef) element.coneType.forEachTypeIncludingMetadata { type ->
                when (type) {
                    is ConeTypeParameterType -> borrowedSymbols += type.lookupTag.typeParameterSymbol
                    is ConeLookupTagBasedType -> (type.lookupTag as? ConeClassLikeLookupTagImpl)?.boundSymbol?.second
                        ?.let(borrowedSymbols::add)
                    else -> Unit
                }
            }
            element.acceptChildren(this)
        }
    })
    borrowedSymbols.removeAll(ownedSymbols)
    handles.addAll(incomingVariables)
    return CfirDefaultRawCopyContext(CfirRawCopyState(borrowedElements, borrowedSymbols), handles)
}
