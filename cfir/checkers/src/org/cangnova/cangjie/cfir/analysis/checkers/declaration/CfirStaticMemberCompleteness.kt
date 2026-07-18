/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType

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
}
