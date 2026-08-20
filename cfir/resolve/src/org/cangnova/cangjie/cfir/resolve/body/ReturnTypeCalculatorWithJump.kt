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

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 带 designated resolve“跳转”能力的返回类型计算器。
 *
 * 对齐 Kotlin K2 `ReturnTypeCalculatorWithJump`：普通隐式返回类型通过 designated body resolve
 * 按需推进；callable copy 的延迟返回类型通过 [CallableCopyTypeCalculator] 委托回同一条计算路径。
 */
open class ReturnTypeCalculatorWithJump(

    /** 当前 scope session。 */
    protected val scopeSession: ScopeSession,
    /** 隐式 body resolve 计算会话。 */
    val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
) : ReturnTypeCalculator() {
    /** callable copy 延迟返回类型计算器。 */
    override val callableCopyTypeCalculator: CallableCopyTypeCalculator = CallableCopyTypeCalculatorWithJump()

    /**
     * 尝试计算 callable 声明的返回类型。
     *
     * 本入口优先复用已解析类型，处理 pattern binding 与局部声明特殊路径，
     * 对可延迟计算的 callable copy 则委托 [callableCopyTypeCalculator]。
     */
    override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        if (declaration is CfirPatternBindingVariable) {
            calculatePatternBindingReturnTypeOrNull(declaration)?.let { return it }
        }

        if (declaration.isLocal) {
            return ReturnTypeCalculatorForFullBodyResolve.Default.tryCalculateReturnType(declaration)
        }

        if (declaration is CfirValueParameter && declaration.returnTypeRef is CfirImplicitTypeRef) {
            declaration.replaceReturnTypeRef(
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported: implicit value parameter type",
                        DiagnosticKind.InferenceError,
                    )
                }
            )
        }

        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is CfirResolvedTypeRef) return returnTypeRef

        if (declaration.canHaveDeferredReturnTypeCalculation) {
            val resolvedTypeRef = callableCopyTypeCalculator.computeReturnType(declaration)
            requireWithAttachment(
                resolvedTypeRef is CfirResolvedTypeRef,
                { "Unexpected return type: ${resolvedTypeRef?.let { it::class.simpleName }}" },
            ) {
                withCfirEntry("declaration", declaration)
            }

            return resolvedTypeRef
        }

        return computeReturnTypeRef(declaration)
    }

    /**
     * 构造递归隐式类型的瞬时返回值，并把参与递归的符号记录到计算会话。
     *
     * `Computing` 说明当前调用依赖尚未完成的 callable；占位类型必须立刻发布给所有
     * 依赖调用，令它们在后续的解析与诊断中稳定地观察同一个递归失败。函数 body 的
     * 共享推断 owner 再负责让已有的源码表达式错误优先于该占位错误。
     */
    protected fun recursionInImplicitTypeRef(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        val errorTypeRef = buildErrorTypeRef {
            source = declaration.returnTypeRef.source
            diagnostic = ConeSimpleDiagnostic("Recursive implicit type", DiagnosticKind.RecursionInImplicitTypes)
        }
        if (declaration.returnTypeRef !is CfirResolvedTypeRef) {
            declaration.replaceReturnTypeRef(errorTypeRef)
        }
        implicitBodyResolveComputationSession.calculateAndStoreNonTrivialLoop(declaration.symbol)
        return errorTypeRef
    }

    /** 执行普通隐式返回类型计算，并处理计算状态缓存和递归检测。 */
    private fun computeReturnTypeRef(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        val symbol = declaration.symbol
        val computedReturnType = when (val status = implicitBodyResolveComputationSession.getStatus(symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> status.resolvedTypeRef
            is CfirImplicitBodyResolveComputationStatus.Computing -> recursionInImplicitTypeRef(declaration)
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> null
        }

        (computedReturnType ?: declaration.returnTypeRef as? CfirResolvedTypeRef)?.let { return it }
        require(!declaration.isCopyCreatedInScope) {
            "callableCopySubstitution was not calculated for callable copy: " +
                    "$symbol with origin ${declaration.origin} and return type ${declaration.returnTypeRef}"
        }

        resolveDeclaration(declaration)
        return declaration.returnTypeRef as? CfirResolvedTypeRef
            ?: errorWithAttachment("${this::class.simpleName}: Return type cannot be calculated for ${declaration::class.simpleName}") {
                withCfirEntry("declaration", declaration)
            }
    }

    /**
     * 通过 designated body resolve 推进声明并解析返回类型。
     *
     * 该方法先恢复文件、外层 class-like 和 extend designation，再只解析目标 callable 的必要路径。
     */
    protected open fun resolveDeclaration(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        val session = declaration.moduleData.session

        val file = session.cfirProvider.getContainingFile(declaration.symbol)
        val containingExtend = session.extendProviderOrNull?.getContainingExtend(declaration.symbol)
        val containingClassLookupTag = declaration.symbol.containingClassLookupTag()
        val outerClasses = generateSequence(containingClassLookupTag) { lookupTag ->
            lookupTag.toSymbol(session)?.getContainingClassSymbol()?.toLookupTag()
        }.mapTo(mutableListOf()) { lookupTag ->
            lookupTag.toSymbol(session)?.cfir as? CfirClassLikeDeclaration
        }

        if (file == null || outerClasses.any { it == null }) {
            return buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic(
                    "Cannot calculate return type (local class/object?)",
                    DiagnosticKind.InferenceError,
                )
            }
        }

        val designation = listOf(file) + outerClasses.filterNotNull().asReversed() + listOfNotNull(containingExtend)
        val transformer = CfirDesignatedBodyResolveTransformerForReturnTypeCalculator(
            designation = (designation.drop(1) + declaration).iterator(),
            session = session,
            scopeSession = scopeSession,
            implicitBodyResolveComputationSession = implicitBodyResolveComputationSession,
            returnTypeCalculator = this,
        )

        designation.first().transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextDependent)

        val transformedDeclaration = transformer.lastResult as? CfirCallableDeclaration
            ?: error("Unexpected lastResult: ${transformer.lastResult}")

        val newReturnTypeRef = transformedDeclaration.returnTypeRef
        require(newReturnTypeRef is CfirResolvedTypeRef) { transformedDeclaration }
        return newReturnTypeRef
    }

    /**
     * Pattern binding 的名字解析 symbol 与隐式类型推断 owner 不同：
     * binding 自身没有 initializer，类型由外层 pattern variable 推断后投影写回。
     */
    private fun calculatePatternBindingReturnTypeOrNull(
        declaration: CfirPatternBindingVariable,
    ): CfirResolvedTypeRef? {
        (declaration.returnTypeRef as? CfirResolvedTypeRef)?.let { return it }
        val session = declaration.moduleData.session

        val owner = session.cfirProvider.getCfirPatternVariableForBinding(declaration.symbol) ?: return null
        if (implicitBodyResolveComputationSession.getStatus(owner.symbol) is CfirImplicitBodyResolveComputationStatus.Computing) {
            return recursionInImplicitTypeRef(declaration)
        }

        tryCalculateReturnTypeOrNull(owner)
        if (declaration.returnTypeRef !is CfirResolvedTypeRef && owner.returnTypeRef is CfirResolvedTypeRef) {
            resolveDeclaration(owner)
        }
        return declaration.returnTypeRef as? CfirResolvedTypeRef
    }

    private inner class CallableCopyTypeCalculatorWithJump : CallableCopyTypeCalculator.DeferredCallableCopyTypeCalculator() {
        /** callable copy 需要解析返回类型时回到外层 jump calculator 的计算路径。 */
        override fun CfirCallableDeclaration.getResolvedTypeRef(): CfirResolvedTypeRef {
            return this@ReturnTypeCalculatorWithJump.computeReturnTypeRef(this)
        }
    }
}
