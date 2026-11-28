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

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * 隐式返回类型推断的计算状态。
 * 三态状态机 `NotComputed -> Computing -> Computed` 用于检测递归依赖并缓存结果。
 */
sealed class CfirImplicitBodyResolveComputationStatus {
    /** 尚未开始计算。 */
    object NotComputed : CfirImplicitBodyResolveComputationStatus()

    /** 正在计算中，用于递归检测。 */
    object Computing : CfirImplicitBodyResolveComputationStatus()

    /** 已完成计算，并缓存解析后的类型与声明。 */
    class Computed(
        /** 已解析返回类型引用。 */
        val resolvedTypeRef: CfirResolvedTypeRef,
        /** body resolve 后的 callable 声明。 */
        val transformedDeclaration: CfirCallableDeclaration,
    ) : CfirImplicitBodyResolveComputationStatus()

    /** 仅缓存推断类型；原声明及其 initializer 尚未经过正式、按源码顺序的 body resolve。 */
    class ComputedType(val resolvedTypeRef: CfirResolvedTypeRef) : CfirImplicitBodyResolveComputationStatus()
}

/**
 * 隐式类型推断计算会话。
 * 负责管理所有可调用声明的计算状态，并提供递归保护与结果缓存。
 */
open class  ImplicitBodyResolveComputationSession {

    /** callable symbol 到其隐式 body resolve 计算状态的映射。 */
    private val statusMap = HashMap<CfirCallableSymbol<*>, CfirImplicitBodyResolveComputationStatus>()

    /** 当前正在计算的符号栈，用于调试和错误报告。 */
    private val computingSymbolsStack = mutableListOf<CfirCallableSymbol<*>>()

    /** 参与非平凡递归环的符号集合。 */
    // 按真实循环保存来源；回滚子树时也能撤销该子树发现的父符号 loop 标记。
    private val nonTrivialLoops = mutableSetOf<List<CfirCallableSymbol<*>>>()

    /** 查询符号当前的计算状态。 */
    fun getStatus(symbol: CfirCallableSymbol<*>): CfirImplicitBodyResolveComputationStatus {
        return statusMap[symbol] ?: CfirImplicitBodyResolveComputationStatus.NotComputed
    }
    protected open fun <D : CfirCallableDeclaration> executeTransformation(symbol: CfirCallableSymbol<*>, transformation: () -> D): D {
        return transformation()
    }

    /**
     * 捕获当前隐式 body resolve 计算状态。
     *
     * overload-by-lambda 等推测式 body resolve 会在不同候选系统下重复解析同一段
     * lambda body；试跑产生的本地声明隐式类型缓存不能泄漏到下一候选或最终提交。
     */
    fun capture(symbols: Set<CfirCallableSymbol<*>>): CfirImplicitBodyResolveComputationSessionSnapshot =
        CfirImplicitBodyResolveComputationSessionSnapshot(
            owner = this,
            symbols = symbols.toSet(),
            statusMap = statusMap.filterKeys { it in symbols },
            computingSymbolsStack = computingSymbolsStack.toList(),
            nonTrivialLoops = nonTrivialLoops.filterTo(linkedSetOf()) { loop -> loop.any { it in symbols } },
        )

    /** 在写 AST 前验证原缓存 owner 和活动父栈；不得把另一条计算栈覆盖成旧栈。 */
    internal fun validateRestore(snapshot: CfirImplicitBodyResolveComputationSessionSnapshot) {
        check(snapshot.owner === this) { "Implicit body cache replay changed owner" }
        check(computingSymbolsStack == snapshot.computingSymbolsStack) { "Implicit body cache replay changed computing stack" }
    }

    /** 仅恢复与 AST 一起回滚的 symbols，包含试跑新建的子声明；范围外的正式结果保留。 */
    internal fun restore(snapshot: CfirImplicitBodyResolveComputationSessionSnapshot, affectedSymbols: Set<CfirCallableSymbol<*>>) {
        validateRestore(snapshot)
        check(affectedSymbols.containsAll(snapshot.symbols))
        affectedSymbols.forEach(statusMap::remove)
        statusMap.putAll(snapshot.statusMap)
        nonTrivialLoops.removeAll { loop -> loop.any { it in affectedSymbols } }
        nonTrivialLoops.addAll(snapshot.nonTrivialLoops)
    }

    /**
     * 执行计算并缓存结果。
     * @param symbol 被计算的符号
     * @param transformation 实际执行 body resolve 的变换闭包
     * @return 变换后的声明
     */
    fun <D : CfirCallableDeclaration> compute(
        symbol: CfirCallableSymbol<*>,
        transformation: () -> D,
    ): D = computeResult(symbol, { executeTransformation(symbol, transformation) }, ::declarationResult)

    /**
     * 计算仅含类型的结果，不把 initializer 试算误发布成已转换的声明。
     * 计算栈与声明计算共用，依赖跳转期间 Computing 状态必须完整保留。
     */
    fun computeType(
        symbol: CfirCallableSymbol<*>,
        computation: () -> CfirResolvedTypeRef,
    ): CfirResolvedTypeRef = computeResult(symbol, computation) { CfirImplicitBodyResolveComputationStatus.ComputedType(it) }

    private fun <R> computeResult(
        symbol: CfirCallableSymbol<*>,
        computation: () -> R,
        completedStatus: (R) -> CfirImplicitBodyResolveComputationStatus,
    ): R {
        requireWithAttachment(statusMap[symbol] == null, { "Unexpected state in startComputing for $symbol: ${statusMap[symbol]}" })
        statusMap[symbol] = CfirImplicitBodyResolveComputationStatus.Computing
        computingSymbolsStack.add(symbol)

        try {
            val result = computation()
            val status = completedStatus(result)
            check(computingSymbolsStack.lastOrNull() == symbol)
            computingSymbolsStack.removeLast()
            statusMap[symbol] = status
            return result
        } catch (failure: Throwable) {
            // 未完成的计算不能留下 Computing，否则同一推断会话重试会变成伪递归。
            check(computingSymbolsStack.lastOrNull() == symbol)
            computingSymbolsStack.removeLast()
            statusMap.remove(symbol)
            throw failure
        }
    }

    /** 声明结果必须已经持有解析类型；仅含类型的结果走独立的 ComputedType 契约。 */
    private fun declarationResult(transformedDeclaration: CfirCallableDeclaration): CfirImplicitBodyResolveComputationStatus.Computed {
        val symbol = transformedDeclaration.symbol
        val returnTypeRef = transformedDeclaration.returnTypeRef
        requireWithAttachment(returnTypeRef is CfirResolvedTypeRef, {
            "Not CfirResolvedTypeRef (${transformedDeclaration.returnTypeRef}) in storeResult for: ${symbol.cfir}"
        })

        if (transformedDeclaration is CfirPatternVariable) {
            requireWithAttachment(transformedDeclaration.pattern.bindingVariables().all { it.returnTypeRef is CfirResolvedTypeRef }, {
                "Cannot cache a pattern declaration with deferred binding types: $symbol"
            })
        }

        return CfirImplicitBodyResolveComputationStatus.Computed(returnTypeRef, transformedDeclaration)
    }

    /** source preview 不发布完整声明；真正 final writer 完成之后才更新 Computed。 */
    internal fun previewResourceSource(resource: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable) {
        check(getStatus(resource.symbol) !is CfirImplicitBodyResolveComputationStatus.Computing)
        statusMap[resource.symbol] = CfirImplicitBodyResolveComputationStatus.ComputedType(resource.returnTypeRef as CfirResolvedTypeRef)
    }

    internal fun publishResourceSource(resource: org.cangnova.cangjie.cfir.declarations.CfirResourceVariable) {
        check(getStatus(resource.symbol) !is CfirImplicitBodyResolveComputationStatus.Computing)
        statusMap[resource.symbol] = declarationResult(resource)
    }

    internal fun validateDiscardResourceSource(symbol: CfirCallableSymbol<*>) {
        check(getStatus(symbol) !is CfirImplicitBodyResolveComputationStatus.Computing)
    }

    internal fun discardResourceSource(symbol: CfirCallableSymbol<*>) {
        validateDiscardResourceSource(symbol)
        statusMap.remove(symbol)
        nonTrivialLoops.removeAll { symbol in it }
    }

    /**
     * 计算并保存声明形成的非平凡递归环。
     */
    fun calculateAndStoreNonTrivialLoop(symbol: CfirCallableSymbol<*>) {
        val loopTail = computingSymbolsStack.takeLastWhile { it != symbol }.takeIf { it.isNotEmpty() } ?: return

        nonTrivialLoops += listOf(symbol) + loopTail
    }

    /**
     * 返回符号是否属于长度大于 1 的递归环。
     */
    fun belongToSomeNonTrivialLoop(symbol: CfirCallableSymbol<*>): Boolean {
        return nonTrivialLoops.any { symbol in it }
    }

}

/**
 * [ImplicitBodyResolveComputationSession] 的浅快照。
 *
 * 状态值本身绑定的是 CFIR symbol 与已转换声明对象；对象字段由外层 CFIR 快照负责恢复，
 * 这里仅恢复“哪些 symbol 已经被计算/正在计算”的事务边界。
 */
class CfirImplicitBodyResolveComputationSessionSnapshot internal constructor(
    /** 快照只可重放到产生它的实际 dispatcher cache owner。 */
    internal val owner: ImplicitBodyResolveComputationSession,
    /** 与对应 AST 快照同范围的真实 callable 身份。 */
    internal val symbols: Set<CfirCallableSymbol<*>>,
    /** 捕获时 callable symbol 到隐式 body resolve 状态的映射。 */
    internal val statusMap: Map<CfirCallableSymbol<*>, CfirImplicitBodyResolveComputationStatus>,
    /** 捕获时正在计算的 callable symbol 栈。 */
    internal val computingSymbolsStack: List<CfirCallableSymbol<*>>,
    /** 捕获时已识别出的非平凡递归环成员集合。 */
    internal val nonTrivialLoops: Set<List<CfirCallableSymbol<*>>>,
)
