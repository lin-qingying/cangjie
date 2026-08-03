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
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
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
    private val nonTrivialLoops = mutableSetOf<CfirCallableSymbol<*>>()

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
    fun capture(): CfirImplicitBodyResolveComputationSessionSnapshot =
        CfirImplicitBodyResolveComputationSessionSnapshot(
            statusMap = HashMap(statusMap),
            computingSymbolsStack = computingSymbolsStack.toList(),
            nonTrivialLoops = nonTrivialLoops.toSet(),
        )

    /**
     * 恢复由 [capture] 捕获的计算状态。
     */
    fun restore(snapshot: CfirImplicitBodyResolveComputationSessionSnapshot) {
        statusMap.clear()
        statusMap.putAll(snapshot.statusMap)
        computingSymbolsStack.clear()
        computingSymbolsStack.addAll(snapshot.computingSymbolsStack)
        nonTrivialLoops.clear()
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
    ): D {
        requireWithAttachment(statusMap[symbol] == null, { "Unexpected state in startComputing for $symbol: ${statusMap[symbol]}" })
        statusMap[symbol] = CfirImplicitBodyResolveComputationStatus.Computing
        computingSymbolsStack.add(symbol)

        val result = transformation()
        storeResult(symbol, result)
        return result
    }

    /** 保存单个 callable 的已解析返回类型与变换后声明。 */
    private fun storeResult(
        symbol: CfirCallableSymbol<*>,
        transformedDeclaration: CfirCallableDeclaration,
    ) {
        requireWithAttachment(
            statusMap[symbol] == CfirImplicitBodyResolveComputationStatus.Computing,
            { "Unexpected state in storeResult for $symbol: ${statusMap[symbol]}" },
        )

        val returnTypeRef = transformedDeclaration.returnTypeRef
        requireWithAttachment(returnTypeRef is CfirResolvedTypeRef, {
            "Not CfirResolvedTypeRef (${transformedDeclaration.returnTypeRef}) in storeResult for: ${symbol.cfir}"
        })

        computingSymbolsStack.removeLast()
        statusMap[symbol] = CfirImplicitBodyResolveComputationStatus.Computed(returnTypeRef, transformedDeclaration)
    }

    /**
     * 计算并保存声明形成的非平凡递归环。
     */
    fun calculateAndStoreNonTrivialLoop(symbol: CfirCallableSymbol<*>) {
        val loopTail = computingSymbolsStack.takeLastWhile { it != symbol }.takeIf { it.isNotEmpty() } ?: return

        nonTrivialLoops += symbol
        nonTrivialLoops += loopTail
    }

    /**
     * 返回符号是否属于长度大于 1 的递归环。
     */
    fun belongToSomeNonTrivialLoop(symbol: CfirCallableSymbol<*>): Boolean {
        return symbol in nonTrivialLoops
    }

    /** 从变换后的声明中提取已解析的返回类型。 */
    private fun extractResolvedType(declaration: CfirCallableDeclaration): org.cangnova.cangjie.cfir.types.ConeCangJieType {
        val typeRef = when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirFunction -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> declaration.returnTypeRef
            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> declaration.returnTypeRef
            else -> return org.cangnova.cangjie.cfir.types.ConeErrorType(
                ConeSimpleDiagnostic("unsupported declaration for implicit type")
            )
        }
        return if (typeRef is org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef) {
            typeRef.coneType
        } else {
            org.cangnova.cangjie.cfir.types.ConeErrorType(
                ConeSimpleDiagnostic("type not resolved after transformation")
            )
        }
    }
}

/**
 * [ImplicitBodyResolveComputationSession] 的浅快照。
 *
 * 状态值本身绑定的是 CFIR symbol 与已转换声明对象；对象字段由外层 CFIR 快照负责恢复，
 * 这里仅恢复“哪些 symbol 已经被计算/正在计算”的事务边界。
 */
class CfirImplicitBodyResolveComputationSessionSnapshot internal constructor(
    /** 捕获时 callable symbol 到隐式 body resolve 状态的映射。 */
    internal val statusMap: Map<CfirCallableSymbol<*>, CfirImplicitBodyResolveComputationStatus>,
    /** 捕获时正在计算的 callable symbol 栈。 */
    internal val computingSymbolsStack: List<CfirCallableSymbol<*>>,
    /** 捕获时已识别出的非平凡递归环成员集合。 */
    internal val nonTrivialLoops: Set<CfirCallableSymbol<*>>,
)
