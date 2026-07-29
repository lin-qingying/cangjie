/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid

/**
 * static 成员完整性共享查询。
 *
 * 官方 `HasIncompleteStaticFuncOrProp` 先取得接口类型的完整 static 成员表，再按签名判断
 * 每个抽象函数/属性是否有 concrete 实现。该对象只计算事实，不报告诊断，供泛型实例化
 * 和继承实现义务等消费者复用。
 */
internal object CfirStaticMemberCompleteness {
    /**
     * 判断一组已实例化上界是否声明了 static 函数或属性。
     */
    context(context: CheckerContext)
    fun hasStaticMembers(upperBounds: List<ConeCangJieType>): Boolean =
        upperBounds.any { upperBound -> upperBound.staticMemberScopeOrNull()?.collectStaticCallables()?.isNotEmpty() == true }

    /**
     * 返回接口类型仍未实现的 static 成员集合。
     *
     * 非接口类型返回空集合；官方当前只对接口类型实参执行该完整性扫描，抽象 class 的
     * 实现义务由声明/extend 继承检查负责。
     */
    context(context: CheckerContext)
    fun unimplementedStaticRequirements(type: ConeCangJieType): Set<CfirCallableSymbol<*>> {
        val expandedType = type.fullyExpandedType(context.session)
        val symbol = expandedType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return emptySet()
        if (symbol.cfir !is CfirInterface) return emptySet()

        val members = symbol
            .staticScopeForQualifierType(context.session, context.scopeSession, expandedType)
            .collectStaticCallables()
        if (members.isEmpty()) return emptySet()

        return members
            .groupBy { member -> member.overrideSignatureKey() }
            .values
            .mapNotNullTo(linkedSetOf()) { sameSignatureMembers ->
                val abstractRequirement = sameSignatureMembers.firstOrNull { member -> member.isAbstractLike(context) }
                    ?: return@mapNotNullTo null
                val hasImplementation = sameSignatureMembers.any { member -> !member.isAbstractLike(context) }
                abstractRequirement.takeUnless { hasImplementation }
            }
    }

    /**
     * 查找 interface static 默认实现体内第一个未被最终 interface 类型实现的 static 依赖。
     *
     * 官方 `CheckInvokeTargetHasImpl` 在调用默认 static 成员时递归遍历其实现体：若实现体引用
     * 另一个 interface static 函数/属性，而该引用在最终 qualifier interface 类型上没有 concrete
     * 实现，则把外层调用诊断为 `INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL`。
     * 若最终 qualifier 已被泛型实例化成员冲突标记，其同签名 concrete 候选不能作为有效实现。
     */
    context(context: CheckerContext)
    fun firstUnimplementedStaticDependency(
        finalQualifierType: ConeCangJieType,
        invokedSymbol: CfirCallableSymbol<*>,
        finalQualifierHasMemberConflict: Boolean = false,
    ): CfirCallableSymbol<*>? {
        val finalInterfaceType = finalQualifierType.fullyExpandedType(context.session)
        val finalInterfaceSymbol = finalInterfaceType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return null
        if (finalInterfaceSymbol.cfir !is CfirInterface) return null

        val invokedStaticMember = invokedSymbol.staticInterfaceMemberOrNull() ?: return null
        if (invokedStaticMember.isAbstractLike(context)) return null

        val scanner = StaticInterfaceInvocationDependencyScanner(
            finalInterfaceType,
            finalQualifierHasMemberConflict,
        )
        return scanner.findUnimplementedDependency(
            implementationSymbol = invokedStaticMember,
            bodyOwnerType = invokedStaticMember.cfir.dispatchReceiverType ?: finalInterfaceType,
        )
    }

    /**
     * 为具体 class-like 类型创建带类型替换的 static scope。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.staticMemberScopeOrNull(): CfirContainingNamesAwareScope? {
        val expandedType = fullyExpandedType(context.session)
        val symbol = expandedType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return null
        return symbol.staticScopeForQualifierType(context.session, context.scopeSession, expandedType)
    }

    /**
     * 从 static scope 收集函数与属性符号；scope 已完成继承合并与泛型替换。
     */
    private fun CfirContainingNamesAwareScope.collectStaticCallables(): List<CfirCallableSymbol<*>> = buildList {
        for (name in getCallableNames()) {
            processFunctionsByName(name) { symbol: CfirFunctionSymbol<*> ->
                if (symbol.isBound && symbol.cfir.status.isStatic) add(symbol)
            }
            processPropertiesByName(name) { symbol: CfirPropertySymbol ->
                if (symbol.isBound && symbol.cfir.status.isStatic) add(symbol)
            }
        }
    }

    /**
     * 在一个默认实现体内递归寻找未实现的 interface static 依赖。
     */
    private class StaticInterfaceInvocationDependencyScanner(
        private val finalInterfaceType: ConeCangJieType,
        private val finalQualifierHasMemberConflict: Boolean,
    ) {
        private val visitedImplementations = linkedSetOf<VisitedImplementationKey>()

        context(context: CheckerContext)
        fun findUnimplementedDependency(
            implementationSymbol: CfirCallableSymbol<*>,
            bodyOwnerType: ConeCangJieType,
        ): CfirCallableSymbol<*>? {
            val staticMember = implementationSymbol.staticInterfaceMemberOrNull() ?: return null
            val declaration = staticMember.cfir as? CfirCallableDeclaration ?: return null
            if (!visitedImplementations.add(staticMember.visitedKey(bodyOwnerType))) return null
            staticMember.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

            for (body in declaration.staticImplementationBodies()) {
                val visitor = object : CfirDefaultVisitorVoid() {
                    var result: CfirCallableSymbol<*>? = null

                    override fun visitElement(element: CfirElement) {
                        if (result != null) return
                        element.acceptChildren(this, null)
                    }

                    override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                        visitStaticReference(functionCall)
                    }

                    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
                        visitStaticReference(qualifiedAccessExpression)
                    }

                    private fun visitStaticReference(expression: CfirQualifiedAccessExpression) {
                        if (result != null) return
                        val referencedSymbol = expression.resolvedCallableSymbolOrNull()
                            ?.staticInterfaceMemberOrNull()
                        if (referencedSymbol != null) {
                            val referenceOwnerType = expression.explicitReceiver?.coneTypeOrNull ?: bodyOwnerType
                            val implementation = implementedStaticMemberOrNull(
                                finalInterfaceType = finalInterfaceType,
                                referencedSymbol = referencedSymbol,
                                referenceOwnerType = referenceOwnerType,
                                finalQualifierHasMemberConflict = finalQualifierHasMemberConflict,
                            )
                            result = if (implementation == null) {
                                referencedSymbol
                            } else {
                                findUnimplementedDependency(
                                    implementationSymbol = implementation,
                                    bodyOwnerType = implementation.cfir.dispatchReceiverType ?: finalInterfaceType,
                                )
                            }
                            if (result != null) return
                        }
                        expression.acceptChildren(this, null)
                    }
                }
                body.accept(visitor, null)
                visitor.result?.let { return it }
            }

            return null
        }

        private fun CfirCallableSymbol<*>.visitedKey(bodyOwnerType: ConeCangJieType): VisitedImplementationKey =
            VisitedImplementationKey(
                declaration = unwrapSubstitutionOverrides().cfir,
                signature = overrideSignatureKey(),
                bodyOwnerType = bodyOwnerType.renderForDebugging(),
            )
    }

    private data class VisitedImplementationKey(
        val declaration: CfirCallableDeclaration,
        val signature: String,
        val bodyOwnerType: String,
    )

    /**
     * 判断 [referencedSymbol] 在最终 interface qualifier 类型上是否有 concrete static 实现。
     */
    context(context: CheckerContext)
    private fun implementedStaticMemberOrNull(
        finalInterfaceType: ConeCangJieType,
        referencedSymbol: CfirCallableSymbol<*>,
        referenceOwnerType: ConeCangJieType,
        finalQualifierHasMemberConflict: Boolean,
    ): CfirCallableSymbol<*>? {
        if (finalQualifierHasMemberConflict) return null
        val expandedFinalType = finalInterfaceType.fullyExpandedType(context.session)
        val finalInterfaceSymbol = expandedFinalType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return null
        val target = referencedSymbol.staticInterfaceMemberOrNull() ?: return null
        val ownerSubstitutor = createCallableOwnerUseSiteSubstitutor(context.session, target, referenceOwnerType)
        val requiredSignature = target.unwrapSubstitutionOverrides().overrideSignatureKey(ownerSubstitutor)
        val scope = finalInterfaceSymbol.staticScopeForQualifierType(
            context.session,
            context.scopeSession,
            expandedFinalType,
        )

        var implementation: CfirCallableSymbol<*>? = null
        when (target.cfir) {
            is CfirFunction -> scope.processFunctionsByName(target.name) { candidate ->
                if (candidate.isConcreteStaticImplementationOf(requiredSignature, context)) {
                    implementation = candidate
                }
            }
            is CfirProperty -> scope.processPropertiesByName(target.name) { candidate ->
                if (candidate.isConcreteStaticImplementationOf(requiredSignature, context)) {
                    implementation = candidate
                }
            }
            else -> Unit
        }
        return implementation
    }

    /**
     * 当前候选是否是指定 static requirement 签名的 concrete 实现。
     */
    private fun CfirCallableSymbol<*>.isConcreteStaticImplementationOf(
        requiredSignature: String,
        context: CheckerContext,
    ): Boolean {
        if (!isBound) return false
        lazyResolveToPhase(CfirResolvePhase.STATUS)
        return cfir.status.isStatic &&
            overrideSignatureKey() == requiredSignature &&
            !isAbstractLike(context)
    }

    /**
     * 归一化到可参与 interface static 实现义务的函数/属性符号。
     */
    context(context: CheckerContext)
    private fun CfirCallableSymbol<*>.staticInterfaceMemberOrNull(): CfirCallableSymbol<*>? {
        val symbol = when (this) {
            is CfirPropertyAccessorSymbol -> propertySymbol
            else -> this
        }
        if (!symbol.isBound) return null
        symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
        val declaration = symbol.cfir
        if (!declaration.status.isStatic) return null
        if (declaration !is CfirNamedFunction && declaration !is CfirProperty) return null

        val owner = context.ownerClassSymbol(symbol.unwrapSubstitutionOverrides())?.cfir
        return symbol.takeIf { owner is CfirInterface }
    }

    /**
     * 返回 static 默认实现需要扫描的实现体。
     */
    private fun CfirCallableDeclaration.staticImplementationBodies() = when (this) {
        is CfirFunction -> listOfNotNull(body)
        is CfirProperty -> listOfNotNull(getter?.body, setter?.body)
        else -> emptyList()
    }

    /**
     * 解析 qualified access 背后的 callable symbol，包含错误引用中保留的单候选。
     */
    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            is CfirErrorNamedReference ->
                (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }
}
