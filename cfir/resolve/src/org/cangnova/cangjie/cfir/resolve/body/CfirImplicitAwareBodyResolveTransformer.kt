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

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/**
 * 感知隐式类型的 body resolve transformer。
 * 它包装 [CfirBodyResolveTransformer]，并借助
 * [CfirImplicitBodyResolveComputationSession] 做缓存和递归保护。
 */
open class CfirImplicitAwareBodyResolveTransformer(
    session: CfirSession,
    scopeSession: ScopeSession,
    /**
     * 隐式 body resolve 的跨声明计算状态。
     *
     * 该状态负责缓存已计算声明并阻断递归 implicit type 解析。
     */
    private val implicitBodyResolveComputationSession: CfirImplicitBodyResolveComputationSession,
    phase: CfirResolvePhase,
    implicitTypeOnly: Boolean,
    returnTypeCalculator: ReturnTypeCalculator,
    outerBodyResolveContext: BodyResolveContext? = null,
) : CfirBodyResolveTransformer(
    session = session,
    scopeSession = scopeSession,
    returnTypeCalculator = returnTypeCalculator,
    outerBodyResolveContext = outerBodyResolveContext,
    phase = phase,
    implicitTypeOnly = implicitTypeOnly,
) {

    /**
     * 解析普通函数声明，并在需要时复用隐式返回类型缓存。
     */
    override fun transformFunction(function: CfirFunction, data: ResolutionMode): CfirFunction {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(function) {
            super.transformFunction(function, data)
        } as CfirFunction
    }

    /**
     * 解析具名函数声明，并在跨声明隐式类型计算中登记结果。
     */
    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: ResolutionMode): CfirNamedFunction {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(namedFunction) {
            super.transformNamedFunction(namedFunction, data)
        } as CfirNamedFunction
    }

    /**
     * 解析主函数声明。
     */
    override fun transformMainFunction(mainFunction: CfirMainFunction, data: ResolutionMode): CfirMainFunction {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(mainFunction) {
            super.transformMainFunction(mainFunction, data)
        } as CfirMainFunction
    }

    /**
     * 解析宏声明的函数体与隐式返回类型。
     */
    override fun transformMacroDeclaration(
        macroDeclaration: CfirMacroDeclaration,
        data: ResolutionMode,
    ): CfirMacroDeclaration {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(macroDeclaration) {
            super.transformMacroDeclaration(macroDeclaration, data)
        } as CfirMacroDeclaration
    }

    /**
     * 解析 finalizer 声明，并复用可调用声明的隐式 body 缓存机制。
     */
    override fun transformFinalizer(finalizer: CfirFinalizer, data: ResolutionMode): CfirFinalizer {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(finalizer) {
            super.transformFinalizer(finalizer, data)
        } as CfirFinalizer
    }

    /**
     * 解析属性声明的初始化器、访问器和隐式类型。
     */
    override fun transformProperty(property: CfirProperty, data: ResolutionMode): CfirProperty {
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(property) {
            super.transformProperty(property, data)
        } as CfirProperty
    }

    /**
     * 解析字段变量；局部字段变量不进入跨声明隐式类型缓存。
     */
    override fun transformFieldVariable(fieldVariable: CfirFieldVariable, data: ResolutionMode): CfirFieldVariable {
        if (!fieldVariable.shouldUseImplicitBodyCache()) {
            return super.transformFieldVariable(fieldVariable, data)
        }
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(fieldVariable) {
            super.transformFieldVariable(fieldVariable, data)
        } as CfirFieldVariable
    }

    /**
     * 解析模式绑定变量；局部模式变量由当前 body resolve 直接处理。
     */
    override fun transformPatternVariable(patternVariable: CfirPatternVariable, data: ResolutionMode): CfirPatternVariable {
        if (!patternVariable.shouldUseImplicitBodyCache()) {
            return super.transformPatternVariable(patternVariable, data)
        }
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(patternVariable) {
            super.transformPatternVariable(patternVariable, data)
        } as CfirPatternVariable
    }

    /**
     * 解析普通变量声明；只有声明边界变量参与隐式类型缓存。
     */
    override fun transformVariable(variable: CfirVariable, data: ResolutionMode): CfirVariable {
        if (!variable.shouldUseImplicitBodyCache()) {
            return super.transformVariable(variable, data)
        }
        @Suppress("UNCHECKED_CAST")
        return computeCachedTransformationResult(variable) {
            super.transformVariable(variable, data)
        } as CfirVariable
    }

    /**
     * 局部变量是函数体内部语句，生命周期由当前 body resolve 负责；
     * 只有声明边界变量才进入跨声明隐式类型计算缓存。
     */
    private fun CfirVariable.shouldUseImplicitBodyCache(): Boolean = !isLocal

    /**
     * 通过状态机缓存变换结果。
     * 对可调用声明先查询缓存；对其他声明则直接执行变换。
     */
    private fun <D : CfirDeclaration> computeCachedTransformationResult(
        declaration: D,
        transformation: () -> CfirDeclaration,
    ): CfirDeclaration {
        if (!implicitTypeOnly && declaration is CfirCallableDeclaration && declaration.returnTypeRef is CfirResolvedTypeRef) {
            return transformation()
        }
        if (declaration !is CfirCallableDeclaration) {
            return transformation()
        }
        val symbol = declaration.symbol as? CfirCallableSymbol<*> ?: return transformation()

        return when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> {
                // 已缓存，直接返回
                status.transformedDeclaration
            }
            is CfirImplicitBodyResolveComputationStatus.Computing -> {
                // 递归访问时直接返回原声明
                declaration
            }
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> {
                // 未计算时，通过状态机执行
                implicitBodyResolveComputationSession.compute(symbol) {
                    @Suppress("UNCHECKED_CAST")
                    transformation() as CfirCallableDeclaration
                }
            }
        }
    }
}
