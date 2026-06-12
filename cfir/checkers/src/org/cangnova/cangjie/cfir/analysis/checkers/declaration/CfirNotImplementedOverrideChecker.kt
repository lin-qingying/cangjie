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
 * Alignment target: Kotlin FIR `FirNotImplementedOverrideChecker` core behavior.
 *
 * For non-abstract class/struct declarations, report when inherited abstract members
 * remain without concrete implementation.
 *
 * 注意：extend 引入的接口不影响本体的抽象成员实现义务，因此这里使用
 * 不含 extendProvider 的 scope，只检查类/struct 自身声明的继承关系。
 */
object CfirNotImplementedOverrideChecker : CfirClassLikeChecker() {
    private val OBJC_CJ_MAPPING = Name.identifier("ObjCCJMapping")

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
     */
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

private fun <S : CfirCallableSymbol<*>> List<S>.hasConcreteInterfaceImplementationConflict(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    val ownerClassId = (ownerDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId
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

private fun CfirCallableSymbol<*>.canImplementAbstractMember(abstractSymbol: CfirCallableSymbol<*>): Boolean {
    val compareResult = Visibilities.compare(cfir.status.visibility, abstractSymbol.cfir.status.visibility)
    return compareResult != null && compareResult >= 0
}
