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

package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.cangnova.cangjie.cfir.calls.ExpressionReceiverValue
import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.calls.qualifierScopeOrNull
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.diagnostic.MemberLookupBlockedByDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.ConstructorFilter
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.isInstanceExtendMemberCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.processFunctionsAndConstructorsByName
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirMemberLookupCompletenessScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * 候选工厂与结果收集器组合。
 */
internal data class CandidateFactoriesAndCollectors(
    /**
     * 当前 tower level 使用的候选工厂。
     */
    val candidateFactory: CandidateFactory,
    /**
     * 当前 tower level 写入的候选收集器。
     */
    val resultCollector: CfirCandidateCollector,
)

/**
 * tower level 处理结果。
 */
internal enum class ProcessResult {
    /**
     * 当前作用域没有可处理候选。
     */
    SCOPE_EMPTY,
    /**
     * 当前作用域找到至少一个候选。
     */
    FOUND,
    ;

    /**
     * 合并两个 tower level 处理结果。
     */
    operator fun plus(other: ProcessResult): ProcessResult =
        if (this == FOUND || other == FOUND) FOUND else SCOPE_EMPTY
}

/**
 * 单个 tower level 的查找接口。
 */
internal interface CfirTowerLevel {
    /**
     * 按名称处理 callable 候选。
     */
    fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult
    /**
     * 按名称处理函数或构造器候选。
     */
    fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult
}

/**
 * 基于普通作用域的 tower level。
 */
internal class ScopeBasedTowerLevel(
    /**
     * 当前 body resolve 组件。
     */
    private val components: BodyResolveComponents,
    /**
     * 要查询的作用域。
     */
    private val scope: CfirScope,
    /**
     * 可选 dispatch receiver。
     */
    private val dispatchReceiver: ReceiverValue? = null,
    /**
     * 可选 extension receiver。
     */
    private val givenExtensionReceiver: ReceiverValue? = null,
) : CfirTowerLevel {
    /**
     * 当前名称在 scope 中没有候选时，把 lookup completeness blocker 转发给最终规约。
     *
     * blocker 不改变 tower level 的 FOUND/SCOPE_EMPTY 结果，也不创建伪候选。
     */
    private fun ProcessResult.forwardMemberLookupBlockers(
        processor: TowerLevelProcessor,
    ): ProcessResult {
        if (this != ProcessResult.SCOPE_EMPTY) return this
        val completenessScope = scope as? CfirMemberLookupCompletenessScope ?: return this
        for (blocker in completenessScope.memberLookupBlockers) {
            processor.resultCollector.addForwardedDiagnostic(
                MemberLookupBlockedByDeclaredSupertype(
                    ownerSymbol = blocker.ownerSymbol,
                    rootDiagnostic = blocker.rootDiagnostic,
                )
            )
        }
        return this
    }

    /**
     * 对齐 Kotlin `TowerLevels.consumeCallableCandidate`：
     * candidate 在进入 argument mapping / checking 之前，先推进到 TYPES。
     */
    private fun consumeCallableCandidate(
        symbol: CfirCallableSymbol<*>,
        processor: TowerLevelProcessor,
    ) {
        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        if (givenExtensionReceiver != null && !symbol.isInstanceExtendMemberCandidate(components.session)) {
            return
        }
        processor.consumeCandidate(symbol, scope, dispatchReceiver, givenExtensionReceiver)
    }

    /**
     * 从作用域中处理 callable 候选。
     */
    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        var result = ProcessResult.SCOPE_EMPTY
        scope.processCallablesByName(info.name) { symbol ->
            // 仓颉 enum constructor 在普通名字访问中不是普通 callable。
            // 裸 enum 值访问由 CfirCallResolver 的 EnumConstructorCall fallback 单独进入，
            // 否则同名顶层变量/函数会被错误地与 enum constructor 合并成歧义。
            if (info.callKind == CallKind.NamedValueAccess && symbol is CfirEnumConstructorSymbol) {
                return@processCallablesByName
            }
            result = ProcessResult.FOUND
            consumeCallableCandidate(symbol, processor)
        }
        return result.forwardMemberLookupBlockers(processor)
    }

    /**
     * 从作用域中处理函数、构造器或函数值候选。
     */
    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        if (info.callKind == CallKind.EnumConstructorCall) {
            var result = ProcessResult.SCOPE_EMPTY
            val lexicalOwnerClassId = lexicalEnumOwnerClassId(info)
            // 目标 enum owner 只细化无 receiver 的 enum sugar；显式 `E.C` 已由 qualifier 确定 owner。
            val expectedOwnerClassId = if (info.explicitReceiver == null) {
                (info.resolutionMode as? ResolutionMode.WithExpectedType)
                    ?.expectedType
                    ?.fullyExpandedType(components.session)
                    ?.classIdOrPrimitiveClassId
                    ?.takeIf { classId ->
                        components.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir is CfirEnum
                    }
            } else {
                null
            }
            val enumConstructorCandidates = mutableListOf<CfirEnumConstructorSymbol>()
            scope.processCallablesByName(info.name) { symbol ->
                val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return@processCallablesByName
                // enum 体内的词法遮蔽是硬性可见性规则，不参与目标类型细化的回滚。
                if (lexicalOwnerClassId != null &&
                    enumConstructorSymbol.enumOwnerClassId() != lexicalOwnerClassId
                ) {
                    return@processCallablesByName
                }
                enumConstructorCandidates += enumConstructorSymbol
            }
            for (enumConstructorSymbol in enumConstructorCandidates.refineByExpectedEnumOwner(expectedOwnerClassId)) {
                result = ProcessResult.FOUND
                consumeCallableCandidate(enumConstructorSymbol, processor)
            }
            return result.forwardMemberLookupBlockers(processor)
        }

        var result = ProcessResult.SCOPE_EMPTY
        val constructorFilter = when (givenExtensionReceiver) {
            null -> ConstructorFilter.OnlyNested
            else -> ConstructorFilter.Both
        }
        scope.processFunctionsAndConstructorsByName(info, components, constructorFilter) { symbol ->
            result = ProcessResult.FOUND
            consumeCallableCandidate(symbol, processor)
        }

        if (info.callKind == CallKind.Function && result == ProcessResult.SCOPE_EMPTY) {
            val consumedCallableValues = linkedSetOf<CfirCallableSymbol<*>>()

            fun consumeCallableValue(symbol: CfirCallableSymbol<*>) {
                if (symbol is CfirFunctionSymbol<*>) return
                if (!symbol.isDirectCallableValueCandidate()) return
                if (!consumedCallableValues.add(symbol)) return

                result = ProcessResult.FOUND
                consumeCallableCandidate(symbol, processor)
            }

            if (scope is CfirLocalScope) {
                scope.processVariablesByName(info.name, ::consumeCallableValue)
            }

            scope.processCallablesByName(info.name) { symbol ->
                consumeCallableValue(symbol)
            }
        }

        return result.forwardMemberLookupBlockers(processor)
    }

    /** 查找裸 enum constructor 访问的最近 lexical enum owner。 */
    private fun lexicalEnumOwnerClassId(info: CallInfo): org.cangnova.cangjie.name.ClassId? {
        if (info.explicitReceiver != null) return null

        for (declaration in info.containingDeclarations.asReversed()) {
            when (declaration) {
                is CfirEnum -> return declaration.symbol.classId
                is CfirExtend -> {
                    val ownerClassId = declaration.extendedTypeRef.coneTypeOrNull
                        ?.fullyExpandedType(components.session)
                        ?.classIdOrPrimitiveClassId
                    val owner = ownerClassId?.let(components.session.symbolProvider::getClassLikeSymbolByClassId)
                    return owner?.classId?.takeIf { owner.cfir is CfirEnum }
                }
                is CfirClassLikeDeclaration -> return null
                else -> Unit
            }
        }
        return null
    }

    /**
     * 目标类型对 enum sugar 候选只做优先级细化，不是可见性过滤。
     *
     * 官方 `EnumSugarTargetsFinder::RefineTargets` 在按目标类型细化后候选为空时会回滚候选集：
     * 优先恢复当前包（非导入）候选，候选全部来自导入时恢复全部。因此目标类型属于其它 enum
     * 的写法（`let c: A = None1`、`let f: Option1<Int64> = None`）仍能解析到 enum constructor，
     * 由后续裸泛型访问 checker 或类型不匹配检查报告真实语义错误，而不是退化成 unresolved。
     */
    private fun List<CfirEnumConstructorSymbol>.refineByExpectedEnumOwner(
        expectedOwnerClassId: org.cangnova.cangjie.name.ClassId?,
    ): List<CfirEnumConstructorSymbol> {
        if (expectedOwnerClassId == null || isEmpty()) return this
        val refined = filter { it.enumOwnerClassId() == expectedOwnerClassId }
        if (refined.isNotEmpty()) return refined
        return filter { it.isDeclaredInSource() }.ifEmpty { this }
    }

    /** 读取 enum constructor 所属 enum 的 class id。 */
    private fun CfirEnumConstructorSymbol.enumOwnerClassId(): org.cangnova.cangjie.name.ClassId? =
        components.session.cfirProvider.getContainingClass(this)?.classId

    /** 判断 enum constructor 是否来自源码而非导入产物，对齐官方 `Attribute::IMPORTED` 判定。 */
    private fun CfirEnumConstructorSymbol.isDeclaredInSource(): Boolean =
        takeIf { it.isBound }?.cfir?.origin?.fromSource == true

    /**
     * 判断变量符号是否可以作为直接 callable value 调用。
     */
    private fun CfirCallableSymbol<*>.isDirectCallableValueCandidate(): Boolean {
        if (!isBound) return false
        val declaration = cfir as? CfirVariable ?: return false
        return declaration.returnTypeRef.coneTypeOrNull.recoverableFunctionTypeOrNull() != null
    }
}

/**
 * 错误类型携带函数 delegated type 时仍可作为函数值进入调用解析。
 */
private fun ConeCangJieType?.recoverableFunctionTypeOrNull(): ConeFunctionType? =
    when (this) {
        is ConeFunctionType -> this
        is ConeErrorType -> delegatedType as? ConeFunctionType
        else -> null
    }

/**
 * 基于 dispatch receiver 成员作用域的 tower level。
 */
internal class DispatchReceiverMemberScopeTowerLevel(
    /**
     * 当前 body resolve 组件。
     */
    private val components: BodyResolveComponents,
    /**
     * dispatch receiver。
     */
    private val dispatchReceiver: ReceiverValue,
    /**
     * 可选 extension receiver。
     */
    private val givenExtensionReceiver: ReceiverValue? = null,
) : CfirTowerLevel {
    /**
     * receiver 成员作用域及有效 dispatch receiver。
     */
    private data class MemberScopeData(
        /**
         * receiver 成员作用域。
         */
        val scope: CfirScope,
        /**
         * 实际写入候选的 dispatch receiver。
         */
        val dispatchReceiver: ReceiverValue?,
    )

    /**
     * 计算 receiver 的成员作用域。
     */
    private fun memberScope(): MemberScopeData? {
        val scope = dispatchReceiver.scope(components) ?: return null
        val effectiveDispatchReceiver = dispatchReceiver.takeUnless {
            it is ExpressionReceiverValue && it.receiverExpression.qualifierScopeOrNull(
                components.session,
                components.scopeSession,
            ) != null
        }
        return MemberScopeData(scope, effectiveDispatchReceiver)
    }

    /**
     * 从 receiver 成员作用域处理 callable 候选。
     */
    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        val scopeData = memberScope() ?: return ProcessResult.SCOPE_EMPTY
        return ScopeBasedTowerLevel(
            components,
            scopeData.scope,
            scopeData.dispatchReceiver,
            givenExtensionReceiver,
        ).processCallablesByName(info, processor)
    }

    /**
     * 从 receiver 成员作用域处理函数候选。
     */
    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        val scopeData = memberScope() ?: return ProcessResult.SCOPE_EMPTY
        return ScopeBasedTowerLevel(
            components,
            scopeData.scope,
            scopeData.dispatchReceiver,
            givenExtensionReceiver,
        ).processFunctionsByName(info, processor)
    }
}

/**
 * tower level 处理入口。
 */
internal class TowerLevelHandler {
    /**
     * 按调用种类将 tower level 处理结果写入候选收集器。
     */
    fun handleLevel(
        collector: CfirCandidateCollector,
        candidateFactory: CandidateFactory,
        info: CallInfo,
        explicitReceiverKind: ExplicitReceiverKind,
        group: CfirTowerGroup,
        towerLevel: CfirTowerLevel,
        context: ResolutionContext,
    ): ProcessResult {
        val processor = TowerLevelProcessor(
            callInfo = info,
            explicitReceiverKind = explicitReceiverKind,
            resultCollector = collector,
            candidateFactory = candidateFactory,
            group = group,
            context = context,
        )

        return when (info.callKind) {
            CallKind.NamedValueAccess -> towerLevel.processCallablesByName(info, processor)
            CallKind.Function,
            CallKind.DelegatingConstructorCall,
            CallKind.EnumConstructorCall,
            -> towerLevel.processFunctionsByName(info, processor)
        }
    }
}

/**
 * tower level 中消费符号并创建候选的处理器。
 */
internal class TowerLevelProcessor(
    /**
     * 当前调用信息。
     */
    val callInfo: CallInfo,
    /**
     * 显式接收者种类。
     */
    val explicitReceiverKind: ExplicitReceiverKind,
    /**
     * 结果候选收集器。
     */
    val resultCollector: CfirCandidateCollector,
    /**
     * 候选工厂。
     */
    val candidateFactory: CandidateFactory,
    /**
     * 当前 tower group。
     */
    val group: CfirTowerGroup,
    /**
     * 当前解析上下文。
     */
    val context: ResolutionContext,
) {
    /**
     * 消费普通 callable 符号并创建候选。
     */
    fun consumeCandidate(
        symbol: CfirCallableSymbol<*>,
        scope: CfirScope?,
        dispatchReceiver: ReceiverValue? = null,
        givenExtensionReceiver: ReceiverValue? = null,
    ): CandidateApplicability {
        return resultCollector.consumeCandidate(
            group,
            candidateFactory.createCandidate(
                callInfo = callInfo,
                symbol = symbol,
                originScope = scope,
                explicitReceiverKind = explicitReceiverKind,
                dispatchReceiver = dispatchReceiver,
                givenExtensionReceiver = givenExtensionReceiver,
            ),
            context,
        )
    }

    /**
     * 消费函数类型接收者的 synthetic invoke 候选。
     */
    fun consumeFunctionTypeInvokeCandidate(
        receiverExpression: org.cangnova.cangjie.cfir.expressions.CfirExpression,
        dispatchReceiver: ReceiverValue,
    ): CandidateApplicability {
        val functionType = receiverExpression.resolvedType.recoverableFunctionTypeOrNull()
            ?: error("Expected function type or recoverable error function type receiver")

        return resultCollector.consumeCandidate(
            group,
            candidateFactory.createFunctionTypeInvokeCandidate(
                callInfo = callInfo,
                functionType = functionType,
                receiverExpression = receiverExpression,
                explicitReceiverKind = explicitReceiverKind,
                dispatchReceiver = dispatchReceiver,
            ),
            context,
        )
    }
}

/**
 * 函数类型接收者的 `invoke` tower level。
 */
internal class FunctionTypeInvokeTowerLevel(
    /**
     * 函数类型接收者表达式。
     */
    private val receiverExpression: org.cangnova.cangjie.cfir.expressions.CfirExpression,
) : CfirTowerLevel {
    /**
     * callable 访问委托到函数调用处理。
     */
    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult =
        processFunctionsByName(info, processor)

    /**
     * 当名称为 `invoke` 且 receiver 类型为函数类型时创建 synthetic invoke 候选。
     */
    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        if (info.callKind != CallKind.Function || info.name != OperatorNameConventions.INVOKE) {
            return ProcessResult.SCOPE_EMPTY
        }
        if (receiverExpression.resolvedType.recoverableFunctionTypeOrNull() == null) return ProcessResult.SCOPE_EMPTY

        val functionTypeReceiver = ExpressionReceiverValue(receiverExpression)
        processor.consumeFunctionTypeInvokeCandidate(receiverExpression, functionTypeReceiver)
        return ProcessResult.FOUND
    }
}

/**
 * 判断作用域是否可能包含指定名称。
 */
internal fun CfirScope.mayContainName(name: org.cangnova.cangjie.name.Name): Boolean {
    if (this is CfirContainingNamesAwareScope) {
        if (name in getCallableNames() || name in getClassifierNames()) return true
    }

    var found = false
    processCallablesByName(name) { found = true }
    if (found) return true
    processFunctionsByName(name) { found = true }
    if (found) return true
    processClassifiersByName(name) { found = true }
    return found
}
