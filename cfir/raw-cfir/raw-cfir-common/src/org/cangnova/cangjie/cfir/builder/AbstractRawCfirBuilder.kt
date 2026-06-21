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

package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression as buildErrorExpressionNode
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference as buildNamedReferenceNode
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef as buildImplicitTypeRefNode

abstract class AbstractRawCfirBuilder<T : Any>(
    val baseSession: CfirSession,
    val context: Context<T> = Context(),
) {
    val baseModuleData: CfirModuleData = baseSession.moduleData

    protected var packageFqName: FqName
        get() = context.packageFqName
        set(value) {
            context.packageFqName = value
        }

    protected fun <R> withPackageContext(fqName: FqName, block: () -> R): R = context.withPackage(fqName, block)

    protected fun <R> withLocalContext(block: () -> R): R = context.withLocalContext(block)

    protected val inLocalContext: Boolean
        get() = context.inLocalContext

    protected fun pushContainerSymbol(symbol: CfirBasedSymbol<*>) = context.pushContainerSymbol(symbol)

    protected fun popContainerSymbol(symbol: CfirBasedSymbol<*>) = context.popContainerSymbol(symbol)

    protected val containerSymbolIfAny: CfirBasedSymbol<*>?
        get() = context.containerSymbolIfAny

    protected val containerSymbol: CfirBasedSymbol<*>
        get() = context.containerSymbol

    protected inline fun <R> withContainerSymbol(symbol: CfirBasedSymbol<*>, block: () -> R): R {
        pushContainerSymbol(symbol)
        return try {
            block()
        } finally {
            popContainerSymbol(symbol)
        }
    }

    protected inline fun <R> withDispatchReceiverType(type: ConeSimpleCangJieType, block: () -> R): R {
        context.pushDispatchReceiverType(type)
        return try {
            block()
        } finally {
            context.popDispatchReceiverType()
        }
    }

    protected inline fun <R> withFunctionTarget(target: CfirFunctionTarget, block: () -> R): R {
        context.enterFunction(target)
        return try {
            block()
        } finally {
            context.exitFunction()
        }
    }

    protected open fun bindFunctionTarget(target: CfirFunctionTarget, function: CfirFunction) {
        target.bind(function)
    }

    protected inline fun <R> withLoopTarget(target: CfirLoopTarget, block: () -> R): R {
        context.enterLoop(target)
        return try {
            block()
        } finally {
            context.exitLoop()
        }
    }

    protected fun callableIdFor(name: Name): CallableId {
        if (context.inLocalContext) return CallableId(name)

        val containingClass = containerSymbolIfAny as? CfirClassLikeSymbol<*>
        return if (containingClass != null) {
            CallableId(containingClass.classId, name)
        } else {
            CallableId(packageFqName, name)
        }
    }

    /**
     * 仓颉只有顶层 class-like 声明具备稳定的 `ClassId`。
     * 一旦位于局部作用域或另一个 class-like 容器内，就不应再构造 `ClassId`。
     */
    protected fun canDeclareTopLevelClassLike(): Boolean {
        return !context.inLocalContext && containerSymbolIfAny !is CfirClassLikeSymbol<*>
    }

    /**
     * 为顶层 class-like 声明创建 `ClassId`。
     * 调用方必须先通过 [canDeclareTopLevelClassLike] 校验语境。
     */
    protected fun topLevelClassId(name: Name): ClassId {
        check(canDeclareTopLevelClassLike()) {
            "Only top-level class-like declarations may have a ClassId in Cangjie: $name"
        }
        return ClassId(packageFqName, name)
    }

    protected fun currentDispatchReceiverType(): ConeSimpleCangJieType? {
        if (context.inLocalContext) return null
        context.currentDispatchReceiverType()?.let { return it }

        val containingClass = containerSymbolIfAny as? CfirClassLikeSymbol<*> ?: return null
        return when (containingClass) {
            is CfirInterfaceSymbol -> ConeClassLikeType(containingClass.classId.toLookupTag(), isInterface = true)
            is CfirStructSymbol -> ConeStructType(containingClass.classId.toLookupTag())
            is CfirEnumSymbol -> ConeEnumType(
                containingClass.classId.toLookupTag(),
                isRefEnum = containingClass.isRefEnum,
            )
            else -> ConeClassLikeType(containingClass.classId.toLookupTag())
        }
    }

    /**
     * 构造 class-like 内部成员声明看到的 self type。
     *
     * Kotlin raw FIR 在进入 class 时把 `C<T>` 压入 dispatch receiver 栈；
     * CFIR 同样必须保留这些类型实参，供成员签名和 substitution scope 使用。
     */
    protected fun CfirClassLikeSymbol<*>.rawDispatchReceiverType(
        typeParameters: List<CfirTypeParameter>,
    ): ConeSimpleCangJieType {
        val typeArguments = typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag()) }
        return constructType(typeArguments) as ConeSimpleCangJieType
    }



    abstract fun T.toSourceElement(): AbstractCjSourceElement

    abstract fun T.elementType(): IElementType

    abstract fun T.asText(): String

    open fun buildElement(element: T): CfirElement {
        error("Unsupported build element entry: ${element::class.qualifiedName}")
    }

    open fun buildFile(file: T): CfirFile {
        error("Unsupported build file entry: ${file::class.qualifiedName}")
    }

    open fun buildDeclaration(declaration: T): CfirDeclaration {
        error("Unsupported build declaration entry: ${declaration::class.qualifiedName}")
    }

    open fun buildExpression(expression: T): CfirExpression {
        error("Unsupported build expression entry: ${expression::class.qualifiedName}")
    }

    protected open fun buildDeclarationStatus(
        visibility: Visibility,
        isVisibilityExplicit: Boolean = false,
        isModalityExplicit: Boolean = false,
        isAbstract: Boolean = false,
        isOpen: Boolean = false,
        isSealed: Boolean = false,
        isStatic: Boolean = false,
        isConst: Boolean = false,
        isMut: Boolean = false,
        isOverride: Boolean = false,
        isRedef: Boolean = false,
        isOperator: Boolean = false,
        isUnsafe: Boolean = false,
        isForeign: Boolean = false,
        isDefault: Boolean = false,
    ): CfirDeclarationStatus {
        val status = CfirDeclarationStatusImpl(
            visibility = visibility,
            modality = Modality.convertFromFlags(isSealed, isAbstract, isOpen),
        )
        status.isAbstract = isAbstract
        status.isOpen = isOpen
        status.isSealed = isSealed
        status.isVisibilityExplicit = isVisibilityExplicit
        status.isModalityExplicit = isModalityExplicit
        status.isStatic = isStatic
        status.isConst = isConst
        status.isMut = isMut
        status.isOverride = isOverride
        status.isRedef = isRedef
        status.isOperator = isOperator
        status.isUnsafe = isUnsafe
        status.isForeign = isForeign
        status.isDefault = isDefault
        return status
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun buildNamedReference(name: Name, source: AbstractCjSourceElement? = null): CfirNamedReference {
        return buildNamedReferenceNode {
            this.source = source as? CjSourceElement
            this.name = name
        }
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun buildErrorExpression(source: AbstractCjSourceElement? = null, reason: String): CfirErrorExpression {
        return buildErrorExpressionNode {
            this.source = source as? CjSourceElement
            this.diagnostic = object : ConeDiagnostic {
                override val reason: String = reason
            }
        }
    }

    protected fun buildImplicitTypeRef(): CfirTypeRef {
        return buildImplicitTypeRefNode {
            customRenderer = false
        }
    }

    /**
     * 对语法上必须显式声明类型的值参数，缺失类型时直接构造 error type，
     * 避免普通函数/构造器参数继续以 implicit type 进入后续 resolve。
     */
    protected fun createNoTypeForParameterTypeRef(parameterSource: CjSourceElement): CfirTypeRef {
        return buildErrorTypeRef {
            source = parameterSource
            diagnostic = ConeSimpleDiagnostic("No type for parameter")
        }
    }

    /**
     * 参考 Kotlin FIR raw builder：
     * `break/continue` 在构建阶段就要绑定到“当前函数内最近的循环”。
     *
     * 仓颉当前还没有公开的显式 loop target 语义，因此这里先只实现隐式最近循环绑定。
     * 同时对齐 Kotlin FIR，把 `break` / `continue` 拆成不同节点，而不是再依赖枚举字段。
     *
     * 若当前函数内没有可见循环，就直接在 jump 自身上挂 `JumpOutsideLoop` 诊断。
     */
    protected fun buildBreakExpressionWithImplicitLoopTarget(
        source: CjSourceElement?,
    ): CfirBreakExpression {
        return buildLoopJumpWithImplicitLoopTarget(source) { target, diagnostic ->
            buildBreakExpression {
                this.source = source
                this.target = target
                this.coneTypeOrNull = diagnostic?.let(::ConeErrorType)
            }
        }
    }

    protected fun buildContinueExpressionWithImplicitLoopTarget(
        source: CjSourceElement?,
    ): CfirContinueExpression {
        return buildLoopJumpWithImplicitLoopTarget(source) { target, diagnostic ->
            buildContinueExpression {
                this.source = source
                this.target = target
                this.coneTypeOrNull = diagnostic?.let(::ConeErrorType)
            }
        }
    }

    /**
     * 统一封装 loop jump 的隐式 target 绑定策略。
     *
     * target 负责回答“跳到哪个循环”，具体的 break/continue 形态
     * 由调用方提供的 concrete builder 决定。
     */
    private inline fun <TJump : CfirLoopJump> buildLoopJumpWithImplicitLoopTarget(
        source: CjSourceElement?,
        buildJump: (target: CfirLoopTarget, diagnostic: ConeSimpleDiagnostic?) -> TJump,
    ): TJump {
        val currentTarget = context.currentLoopTargetInCurrentFunction()
        val diagnostic = if (currentTarget == null) {
            ConeSimpleDiagnostic(
                reason = "'break' or 'continue' must be used inside a loop",
                kind = DiagnosticKind.JumpOutsideLoop,
            )
        } else {
            null
        }
        val target = currentTarget ?: buildErrorLoopTarget(source, diagnostic!!)
        return buildJump(target, diagnostic)
    }

    protected fun buildErrorLoopTarget(
        source: CjSourceElement?,
        diagnostic: ConeSimpleDiagnostic,
    ): CfirLoopTarget {
        val target = CfirLoopTarget(labelName = null)
        val errorLoop: CfirLoopExpression = buildLoopExpression {
            this.source = source
            this.condition = buildErrorExpression(source as? AbstractCjSourceElement, diagnostic.reason)
            this.body = buildBlock {
                this.source = source
            }
            this.isDoWhile = false
            this.coneTypeOrNull = ConeErrorType(diagnostic)
        }
        target.bind(errorLoop)
        return target
    }

    /**
     * 对齐 Kotlin FIR：`return` 必须绑定到当前函数 target，而不是依赖最近语法块猜测。
     *
     * 当前仓颉还没有公开的显式 `return@label` 语法，因此这里只实现“返回到当前最近函数”。
     * 若当前不在函数体中，则把 return 绑定到错误 target，后续统一经诊断流水线报告。
     */
    protected fun buildReturnExpressionWithCurrentFunctionTarget(
        source: CjSourceElement?,
        result: CfirExpression?,
    ): CfirReturnExpression {
        val functionTarget = context.currentFunctionTarget()
            ?: CfirFunctionTarget(labelName = null, isLambda = false).apply {
                bind(
                    buildErrorFunctionTarget(
                        source = source,
                        diagnostic = ConeSimpleDiagnostic(
                            reason = "`return` must be used inside a function",
                            kind = DiagnosticKind.ReturnNotAllowed,
                        ),
                    )
                )
            }

        return buildReturnExpression {
            this.source = source
            this.target = functionTarget
            this.result = result ?: buildLiteralExpression {
                this.source = source?.fakeElement(CjFakeSourceElementKind.ImplicitUnit.Return) ?: source
                kind = CfirLiteralKind.UNIT
                value = null
            }
        }
    }

    private fun buildErrorFunctionTarget(
        source: CjSourceElement?,
        diagnostic: ConeSimpleDiagnostic,
    ): CfirFunction {
        return org.cangnova.cangjie.cfir.declarations.builder.buildErrorFunction {
            this.source = source
            moduleData = baseModuleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            this.diagnostic = diagnostic
            symbol = org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol()
            attributes = CfirDeclarationAttributes.EMPTY
            status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
            dispatchReceiverType = null
            body = null
        }
    }

    /**
     * enum constructor 在语法上只声明 payload 类型，不显式声明形参名。
     *
     * 这里统一为每个 payload 类型生成稳定的合成 `valueParameter`，
     * 让后续调用解析、冲突检测和模式匹配都读取同一份参数结构。
     */
    protected fun buildEnumConstructorValueParameter(
        source: CjSourceElement?,
        name: Name,
        returnTypeRef: CfirTypeRef,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): CfirValueParameter {
        return buildValueParameter {
            this.source = source
            moduleData = baseModuleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = context.inLocalContext
            isNamed = false
            dispatchReceiverType = null
            symbol = CfirValueParameterSymbol(callableIdFor(name))
            status = CfirDeclarationStatusImpl.DEFAULT
            this.returnTypeRef = returnTypeRef
            this.name = name
            defaultValue = null
            this.containingDeclarationSymbol = containingDeclarationSymbol
        }
    }
}
