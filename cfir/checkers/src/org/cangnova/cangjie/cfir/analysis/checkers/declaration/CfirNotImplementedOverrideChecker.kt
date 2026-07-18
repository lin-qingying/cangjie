/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name

/**
 * 未实现抽象成员检查器。
 *
 * 对齐 Kotlin FIR `FirNotImplementedOverrideChecker` 的核心行为：非 abstract class/struct
 * 如果继承的抽象成员没有具体实现，则报告 `ABSTRACT_MEMBER_NOT_IMPLEMENTED`。
 *
 * 注意：extend 引入的接口不影响本体的抽象成员实现义务，因此这里使用仅基于本体继承关系的 scope。
 */
object CfirNotImplementedOverrideChecker : CfirClassLikeChecker() {
    /**
     * ObjC CJMapping 注解名。
     */
    private val OBJC_CJ_MAPPING = Name.identifier("ObjCCJMapping")

    /**
     * 检查 class/struct 是否仍有未实现的 inherited abstract member。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass && declaration !is CfirStruct) return
        if (declaration.status.isAbstract || declaration.status.isSealed) return
        if (declaration.hasAnnotation(OBJC_CJ_MAPPING) && declaration.superTypeRefs.isNotEmpty()) return

        val classScope = createOwnMemberScope(declaration)
        if (!classScope.hasUnimplementedAbstractMember(declaration, context)) return

        reporter.reportOn(
            source = declaration.classLikeDeclarationHeaderDiagnosticSource(),
            factory = CfirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED,
            a = declaration.name,
        )
    }

    /**
     * 创建仅包含本体声明的成员 scope（不含 extend 引入的接口/成员）。
     * extend 是外部扩展，不应影响类/struct 本体的抽象成员实现检查。
     *
     * 注意：directSupertypeProvider 也不传，因为 CfirSuperTypeGraphStore 会合并
     * extend 引入的超类型。传 null 让 scope 退回到 declaration.superTypeRefs，
     * 这只包含本体直接声明的继承关系。
     */
    context(context: CheckerContext)
    private fun createOwnMemberScope(declaration: CfirClassLikeDeclaration): CfirTypeScope {
        return when (declaration) {
            is CfirClass -> context.session.cangjieScopeProvider.getDeclarationSiteMemberScope(
                declaration,
                context.session,
                context.scopeSession,
            )

            else -> {
                val classLikeSymbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
                CfirClassUseSiteMemberScope(
                    session = context.session,
                    classLikeSymbol,
                    context.session.symbolProvider,
                    extendProvider = context.session.extendProvider,
                    directSupertypeProvider = context.session.directSupertypeProviderOrNull,
                    scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
                )
            }
        }
    }
}

/**
 * 判断 scope 中是否存在 owner 必须实现但尚未实现的抽象成员。
 */
private fun CfirTypeScope.hasUnimplementedAbstractMember(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    for (name in getCallableNames()) {
        val functionSymbols = mutableListOf<CfirFunctionSymbol<*>>()
        processFunctionsByName(name) { functionSymbols += it }
        if (functionSymbols.hasUnimplementedAbstractBySignature(ownerDeclaration, context)) {
            return true
        }

        val propertySymbols = mutableListOf<CfirPropertySymbol>()
        processPropertiesByName(name) { propertySymbols += it }
        if (propertySymbols.hasUnimplementedAbstractBySignature(ownerDeclaration, context)) {
            return true
        }
    }
    return false
}

/**
 * 按 override signature 分组检查 callable 符号集合中是否存在未实现抽象成员。
 */
private fun <S : CfirCallableSymbol<*>> List<S>.hasUnimplementedAbstractBySignature(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (isEmpty()) return false

    val visibleGroups = this
        .asSequence()
        .filter { it.isBound }
        .filter { it.isVisibleIn(ownerDeclaration, context) }
        .groupBy { it.overrideSignatureKey() }

    for ((_, symbols) in visibleGroups) {
        if (symbols.hasConcreteInterfaceImplementationConflict(ownerDeclaration, context)) {
            return true
        }

        val abstractSymbols = symbols.filter { it.isAbstractLike(context) }
        if (abstractSymbols.isEmpty()) continue

        for (abstractSymbol in abstractSymbols) {
            val hasConcreteImplementation = symbols.any { candidate ->
                candidate !== abstractSymbol &&
                    !candidate.isAbstractLike(context) &&
                    candidate.canImplementAbstractMember(abstractSymbol)
            }
            if (!hasConcreteImplementation) {
                return true
            }
        }
    }

    return false
}

/**
 * 检查多个接口继承的 concrete 成员是否形成实现冲突。
 *
 * 当前类没有自己的 concrete 实现、但从多个不同接口继承同一签名 concrete 成员时，需要视为
 * 抽象实现义务未满足。
 */
private fun <S : CfirCallableSymbol<*>> List<S>.hasConcreteInterfaceImplementationConflict(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    val ownerClassId = (ownerDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId
    val hasConcreteClassImplementation = any { symbol ->
        if (symbol.isAbstractLike(context)) return@any false
        val owner = context.ownerClassSymbol(symbol)?.cfir ?: return@any false
        owner !is CfirInterface
    }
    if (hasConcreteClassImplementation) return false

    val hasOwnConcreteImplementation = any { symbol ->
        symbol.ownerClassId(context) == ownerClassId && !symbol.isAbstractLike(context)
    }
    if (hasOwnConcreteImplementation) return false

    val inheritedConcreteInterfaceOwners = mapNotNull { symbol ->
        if (symbol.ownerClassId(context) == ownerClassId) return@mapNotNull null
        val owner = context.ownerClassSymbol(symbol)?.cfir
        if (owner !is CfirInterface) return@mapNotNull null
        if (symbol.isAbstractLike(context)) return@mapNotNull null
        (owner.symbol as? CfirClassLikeSymbol<*>)?.classId
    }.toSet()

    return inheritedConcreteInterfaceOwners.size > 1
}

/**
 * 判断候选 concrete 成员的可见性是否足以实现指定抽象成员。
 */
private fun CfirCallableSymbol<*>.canImplementAbstractMember(abstractSymbol: CfirCallableSymbol<*>): Boolean {
    val compareResult = Visibilities.compare(cfir.status.visibility, abstractSymbol.cfir.status.visibility)
    return compareResult != null && compareResult >= 0
}
