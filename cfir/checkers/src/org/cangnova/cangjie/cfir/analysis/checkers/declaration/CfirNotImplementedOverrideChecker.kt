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
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFunctionInheritanceScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPropertyInheritanceScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.declarations.resolvedInteropInfoOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.toLookupTag

/**
 * 未实现抽象成员检查器。
 *
 * 对齐 Kotlin FIR `FirNotImplementedOverrideChecker` 的核心行为：非 abstract class/struct
 * 如果继承的抽象成员没有具体实现，则报告 `ABSTRACT_MEMBER_NOT_IMPLEMENTED`。
 *
 * 注意：extend 引入的接口不影响本体的抽象成员实现义务，因此这里使用仅基于本体继承关系的 scope。
 */
object CfirNotImplementedOverrideChecker : CfirClassLikeChecker() {
    override val requiresImplementation: Boolean get() = true

    /**
     * 检查 class/struct 是否仍有未实现的 inherited abstract member。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass && declaration !is CfirStruct) return
        if (declaration.status.isAbstract || declaration.status.isSealed) return
        if (declaration.resolvedInteropInfoOrNull()?.cjmp?.isObjCMapping == true &&
            declaration.superTypeRefs.isNotEmpty()
        ) return

        val classLikeSymbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return
        val receiverType = classLikeSymbol.constructType(
            declaration.typeParameters.map { typeParameter ->
                ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
            }
        )
        val accessContext = context.accessContext(CfirAccessKind.CALLABLE).copy(
            receiverType = receiverType,
            qualifierSymbol = classLikeSymbol,
            lookupOrigin = CfirLookupOrigin.MEMBER,
        )
        val classScope = createOwnMemberScope(declaration)
        val obligation = classScope.findUnimplementedObligation(declaration, context, accessContext) ?: return

        when (obligation) {
            is UnimplementedObligation.AbstractDeclaration -> reporter.reportOn(
                source = declaration.classLikeNameOffsetsDiagnosticSource(),
                factory = CfirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED,
                a = declaration.name,
            )
            is UnimplementedObligation.InterfaceRequirement -> reporter.reportOn(
                source = declaration.classLikeNameOffsetsDiagnosticSource(),
                factory = CfirErrors.INTERFACE_MEMBER_MUST_BE_IMPLEMENTED,
                a = obligation.memberKind,
                b = obligation.symbol.name,
                c = declaration.name.asString(),
            )
        }
    }

    /**
     * 创建抽象成员实现义务检查所用的成员 scope。
     *
     * 采用 [CfirClassMemberScopeKind.BODY_LOOKUP]，其语义对齐官方
     * `LookUpImpl::ProcessStructDeclBody`，恰好是本检查需要的三条规则：
     * - 本体自身的 extend 成员**不可见**：类自身的 extend 不能解除它自己的抽象成员实现义务；
     * - 沿父类型递归时按 use-site 处理，因此**父类型经 extend 获得的成员可见**：这些成员随
     *   继承进入本体成员集合，可以满足接口实现义务；
     * - 直接父类型仍取自源码声明的 `superTypeRefs`，因此 extend 引入的父接口不构成本体义务。
     */
    context(context: CheckerContext)
    private fun createOwnMemberScope(declaration: CfirClassLikeDeclaration): CfirTypeScope {
        val classLikeSymbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
        return CfirClassUseSiteMemberScope(
            session = context.session,
            classLikeSymbol,
            context.session.symbolProvider,
            extendProvider = context.session.extendProvider,
            directSupertypeProvider = context.session.directSupertypeProviderOrNull,
            scopeKind = CfirClassMemberScopeKind.BODY_LOOKUP,
        )
    }
}

/**
 * 未实现成员义务的来源。
 *
 * 官方 `StructInheritanceChecker::DiagnoseForUnimplementedInterfaces` 按来源分流诊断：
 * 抽象成员没有实现走宿主 class/struct 的“缺少实现”诊断；接口要求未被满足走
 * `sema_interface_member_must_be_implemented`。两者不能共用一条诊断，否则
 * `class C1 <: I {}`（抽象成员）与接口默认成员冲突会得到相同的错误种类。
 */
private sealed class UnimplementedObligation {
    /** 触发义务的成员符号。 */
    abstract val symbol: CfirCallableSymbol<*>

    /**
     * 声明自身携带的抽象成员没有实现。
     */
    class AbstractDeclaration(override val symbol: CfirCallableSymbol<*>) : UnimplementedObligation()

    /**
     * 接口要求宿主实现该成员（含多个接口同签名成员无法择一的情况）。
     *
     * @property memberKind 诊断文案使用的成员种类，与 `RequirementMemberInfo.kind` 取同一套词表。
     */
    class InterfaceRequirement(
        override val symbol: CfirCallableSymbol<*>,
        val memberKind: String,
    ) : UnimplementedObligation()
}

/** 诊断文案中函数成员的种类文本。 */
private const val FUNCTION_MEMBER_KIND = "function"

/** 诊断文案中属性成员的种类文本。 */
private const val PROPERTY_MEMBER_KIND = "property"

/**
 * 判断 scope 中是否存在 owner 必须实现但尚未实现的成员，并返回其义务来源。
 */
private fun CfirTypeScope.findUnimplementedObligation(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
    accessContext: CfirAccessContext,
): UnimplementedObligation? {
    for (name in getCallableNames()) {
        val functionSymbols = mutableListOf<CfirFunctionSymbol<*>>()
        val propertySymbols = mutableListOf<CfirPropertySymbol>()
        fun collectSymbol(symbol: CfirCallableSymbol<*>) {
            when (symbol) {
                is CfirFunctionSymbol<*> -> functionSymbols += symbol
                is CfirPropertySymbol -> propertySymbols += symbol
                else -> Unit
            }
        }

        context.session.accessibilityChecker.processAccessibleCallablesByName(
            scope = this,
            name = name,
            context = accessContext,
        ) { candidate ->
            collectSymbol(candidate.symbol)
        }

        // 结构归并可以选中一个来自 extend 的实现，但使用点未导入对应接口时该实现
        // 不可见。必须从未归并输入保留原抽象要求，不能让过滤实现同时抹掉实现义务。
        fun collectAbstractRequirement(symbol: CfirCallableSymbol<*>, provenance: CfirCallableLookupProvenance) {
            if (!symbol.isAbstractLike(context)) return
            if (context.session.accessibilityChecker.checkCallable(symbol, accessContext, provenance) !is CfirAccessibilityResult.Accessible) return
            collectSymbol(symbol)
        }
        (this as? CfirFunctionInheritanceScope)?.processUnmergedInheritedFunctionsByNameWithProvenance(name) {
            collectAbstractRequirement(it.member, it.lookupProvenance)
        }
        (this as? CfirPropertyInheritanceScope)?.processUnmergedInheritedPropertiesByNameWithProvenance(name) {
            collectAbstractRequirement(it.member, it.lookupProvenance)
        }

        functionSymbols.distinct()
            .findUnimplementedObligationBySignature(ownerDeclaration, context, FUNCTION_MEMBER_KIND)
            ?.let { return it }
        propertySymbols.distinct()
            .findUnimplementedObligationBySignature(ownerDeclaration, context, PROPERTY_MEMBER_KIND)
            ?.let { return it }
    }
    return null
}

/**
 * 按 override signature 分组检查 callable 符号集合中是否存在未实现的成员义务。
 */
private fun <S : CfirCallableSymbol<*>> List<S>.findUnimplementedObligationBySignature(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
    memberKind: String,
): UnimplementedObligation? {
    if (isEmpty()) return null

    val visibleGroups = this
        .asSequence()
        .filter { it.isBound }
        .groupBy { it.overrideSignatureKey() }

    for ((_, symbols) in visibleGroups) {
        if (symbols.hasConcreteInterfaceImplementationConflict(ownerDeclaration, context)) {
            return UnimplementedObligation.InterfaceRequirement(symbols.first(), memberKind)
        }

        val abstractSymbols = symbols.filter { it.isAbstractLike(context) }
        if (abstractSymbols.isEmpty()) continue

        for (abstractSymbol in abstractSymbols) {
            // 集合已按使用点可见性过滤并按实现签名分组。具体成员即满足实现义务；
            // 实现自身的弱可见性等兼容性错误由继承检查报告，不能再归类为缺少实现。
            val hasConcreteImplementation = symbols.any { candidate ->
                candidate !== abstractSymbol &&
                    !candidate.isAbstractLike(context)
            }
            if (!hasConcreteImplementation) {
                val diagnosedAsIncompleteSuperExtend = ownerDeclaration is CfirClass && with(context) {
                    CfirInheritanceDeepChecker.hasConstraintInapplicableInheritedExtendImplementation(
                        declaration = ownerDeclaration,
                        requirementSymbol = abstractSymbol,
                    )
                }
                if (diagnosedAsIncompleteSuperExtend) continue
                return UnimplementedObligation.AbstractDeclaration(abstractSymbol)
            }
        }
    }

    return null
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
