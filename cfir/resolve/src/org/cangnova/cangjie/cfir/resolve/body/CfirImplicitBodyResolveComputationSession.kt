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
        val resolvedTypeRef: CfirResolvedTypeRef,
        val transformedDeclaration: CfirCallableDeclaration,
    ) : CfirImplicitBodyResolveComputationStatus()
}

/**
 * 隐式类型推断计算会话。
 * 负责管理所有可调用声明的计算状态，并提供递归保护与结果缓存。
 */
class CfirImplicitBodyResolveComputationSession {

    private val statusMap = HashMap<CfirCallableSymbol<*>, CfirImplicitBodyResolveComputationStatus>()

    /** 当前正在计算的符号栈，用于调试和错误报告。 */
    private val computingSymbolsStack = mutableListOf<CfirCallableSymbol<*>>()

    /** 参与非平凡递归环的符号集合。 */
    private val nonTrivialLoops = mutableSetOf<CfirCallableSymbol<*>>()

    /** 查询符号当前的计算状态。 */
    fun getStatus(symbol: CfirCallableSymbol<*>): CfirImplicitBodyResolveComputationStatus {
        return statusMap[symbol] ?: CfirImplicitBodyResolveComputationStatus.NotComputed
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

    private var cycledSymbol: CfirCallableSymbol<*>? = null

    /**
     * 记录 jumping resolve 检测到递归时对应的符号。
     */
    fun pushCycledSymbol(symbol: CfirCallableSymbol<*>) {
        requireWithAttachment(cycledSymbol == null, { "Nested recursion is not allowed" })
        cycledSymbol = symbol
    }

    /**
     * 取出并清空 jumping resolve 检测到的递归符号。
     */
    fun popCycledSymbolIfExists(): CfirCallableSymbol<*>? {
        return cycledSymbol?.also {
            cycledSymbol = null
        }
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
