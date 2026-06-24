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

import org.cangnova.cangjie.cfir.CfirElement

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.createCurrentScopeList
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSyntheticCallGenerator
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractPhaseTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.util.PrivateForInline

/**
 * Body resolve transformer 的抽象基类。
 * 子类通过 `context` 与 `components` 协作，统一驱动 body resolve 流程。
 */
abstract class CfirAbstractBodyResolveTransformer(
    phase: CfirResolvePhase,
) : CfirAbstractPhaseTransformer<ResolutionMode>(phase) {

    /** 当前 body resolve 正在使用的声明、scope、CFG 与数据流上下文。 */
    abstract val context: BodyResolveContext

    /** 调用解析阶段共享的会话化上下文。 */
    abstract val resolutionContext: ResolutionContext

    /** 当前 transformer 暴露给 resolver、checker 与 inference 组件的服务集合。 */
    abstract val components: BodyResolveTransformerComponents

    /** 当前 body resolve 使用的数据流分析器。 */
    inline val dataFlowAnalyzer: CfirDataFlowAnalyzer
        get() = components.dataFlowAnalyzer

    /**
     * 是否只解析隐式类型所需的最小 body 片段。
     *
     * low-level resolver 使用该标志控制 partial body resolve，完整 body resolve 会临时关闭它。
     */
    @set:PrivateForInline
    abstract var implicitTypeOnly: Boolean
        internal set

    /** 当前 body resolve 使用的 CFIR 会话。 */
    final override val session: CfirSession get() = components.session

    /**
     * 在当前闭包内强制执行完整 body resolve。
     *
     * 如果当前处于隐式类型模式，闭包执行期间会临时切换为完整模式；
     * 结束后恢复原状态，保证嵌套调用不会污染外层 resolver。
     */
    @OptIn(PrivateForInline::class)
    internal inline fun <T> withFullBodyResolve(crossinline l: () -> T): T {
        val shouldSwitchMode = implicitTypeOnly
        if (shouldSwitchMode) {
            implicitTypeOnly = false
        }
        return try {
            l()
        } finally {
            if (shouldSwitchMode) {
                implicitTypeOnly = true
            }
        }
    }

    /**
     * 共享组件容器，集中持有 body resolve 所需的会话、上下文和服务。
     */
    open class BodyResolveTransformerComponents(
        /** 当前 body resolve 使用的 CFIR 会话。 */
        override val session: CfirSession,
        /** 当前 body resolve 共享的 scope 缓存会话。 */
        override val scopeSession: ScopeSession,
        /** 持有实际声明/表达式子 transformer 的 dispatcher。 */
        val transformer: CfirAbstractBodyResolveTransformerDispatcher,
        /** 当前 body resolve 的容器、scope、CFG 与数据流上下文。 */
        val context: BodyResolveContext,
        expandTypeAliases: Boolean,

    ) : BodyResolveComponents() {
        /** 当前声明路径上的容器声明列表。 */
        override val containingDeclarations: List<CfirDeclaration>
            get() = context.containers.toList()
        /** 当前文件的导入 scope；必要时从文件 imports 和默认 imports 现场构造。 */
        override val fileImportsScope: List<CfirScope>
            get() = context.fileImportsScope.ifEmpty { createImportingScopes(context.file) }
        /** tower resolve 使用的接收者、scope 与隐式值元素列表。 */
        override val towerDataElements: List<CfirTowerDataElement>
            get() = context.towerDataContext.towerDataElements

        /** tower resolve 的完整上下文对象。 */
        override val towerDataContext get() = context.towerDataContext
        /** 当前可见的局部 scope 栈。 */
        override val localScopes: CfirLocalScopes
            get() = context.towerDataContext.localScopes.toPersistentList()
        /** 缺失 expected type 时使用的错误类型引用占位。 */
        override val noExpectedType: CfirTypeRef
            get() = buildErrorTypeRef { diagnostic = org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("No expected type") }

        /** 当前声明返回类型计算器。 */
        override val returnTypeCalculator: ReturnTypeCalculator
            get() = context.returnTypeCalculator
        /** 当前 body resolve 共享的隐式值存储。 */
        override val implicitValueStorage
            get() = context.implicitValueStorage

        /** 当前会话的符号 provider。 */
        override val symbolProvider get() = session.symbolProvider
        /** SAM 解析服务，按需创建以避免普通 body resolve 路径的无用初始化。 */
        override val samResolver: CfirSamResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirSamResolver(session, scopeSession)
        }
        /** 当前正在解析的文件。 */
        override val file: CfirFile
            get() = context.file
        /** 当前最内层声明容器；文件级解析时退回到文件本身。 */
        override val container: CfirDeclaration
            get() = context.containerIfAny ?: context.file

        /** 调用解析阶段流水线执行器。 */
        override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner()






        /** 数据流分析器，绑定当前 components 和 body resolve context。 */
        override val dataFlowAnalyzer: CfirDataFlowAnalyzer by lazy(LazyThreadSafetyMode.NONE) {
            CfirDataFlowAnalyzer(this, context)
        }

        /** 整数字面量与操作符近似转换器。 */
        override val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer
            by lazy(LazyThreadSafetyMode.NONE) {
                IntegerLiteralAndOperatorApproximationTransformer(session, scopeSession)
            }

        /** 调用补全器，负责约束系统收敛和调用结果写回。 */
        override val callCompleter: CfirCallCompleter by lazy(LazyThreadSafetyMode.NONE) { CfirCallCompleter(transformer, this) }
        /** 合成调用生成器，用于补全操作符、访问器等派生调用。 */
        override val syntheticCallGenerator: CfirSyntheticCallGenerator by lazy(LazyThreadSafetyMode.NONE) {
            CfirSyntheticCallGenerator(this)
        }
        /** 当前处在 inline 函数体内时的最内层 inline 函数声明。 */
        override val inlineFunction: CfirFunction?
            get() = context.containers.lastOrNull() as? CfirFunction






        /** 当前 body resolve 使用的调用解析器。 */
        override val callResolver: CfirCallResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirCallResolver(this)
        }

        /**
         * 当前会话的 extend provider。
         *
         * 某些测试或低阶会话不注册 extend provider，因此这里只把缺失组件视为不可用服务。
         */
        val extendProvider: CfirExtendProvider? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                session.extendProvider
            } catch (_: Exception) {
                null
            }
        }

        /**
         * 为文件构造导入相关 scope 列表。
         *
         * 列表按低优先级到高优先级排列，tower resolver 反向遍历时能优先命中文件内声明和显式导入。
         */
        private fun createImportingScopes(file: CfirFile): List<CfirScope> {
            val imports = file.imports
            val resolvedImports = session.importBindingStoreOrNull?.getBindings(file)?.imports
            val defaultImportsProvider = session.defaultImportsProvider
            val defaultImports = defaultImportsProvider.getDefaultImports(includeLowPriorityImports = true)
                .filter { it.fqName !in defaultImportsProvider.excludedImports }
                .map { importPath ->
                    buildImport {
                        source = null
                        importedFqName = importPath.fqName
                        isAllUnder = importPath.isAllUnder
                        aliasName = importPath.alias
                        aliasSource = null
                    }
                }

            return buildList {
                // Scope 列表按低优先级到高优先级排列，tower 反向遍历时会先命中当前文件声明。
                add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
                add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
                add(CfirExplicitStarImportingScope(imports, symbolProvider, resolvedImports))
                add(CfirPackageMemberScope(file.packageDirective.packageFqName, session))
                add(CfirFileDeclaredTopLevelScope(file))
                add(CfirExplicitSimpleImportingScope(imports, symbolProvider, resolvedImports))
            }
        }

        /**
         * 创建一次调用解析使用的 [ResolutionContext]。
         *
         * [expectedType] 预留给与 Kotlin FIR 对齐的调用解析 API，目前上下文从 body resolve state 中读取。
         */
        fun createResolutionContext(@Suppress("UNUSED_PARAMETER") expectedType: org.cangnova.cangjie.cfir.types.ConeCangJieType? = null): ResolutionContext {
            return ResolutionContext(session, this, context)
        }
    }
}

/**
 * Body resolve dispatcher 的抽象基类。
 * 具体 dispatcher 通过它把各种 `transformXxx` 分发给对应子 transformer。
 */
abstract class CfirAbstractBodyResolveTransformerDispatcher(
    phase: CfirResolvePhase,
    /** 是否只解析隐式类型所需的 body。 */
    override var implicitTypeOnly: Boolean = false,
) : CfirAbstractBodyResolveTransformer(phase) {
    /**
     * 对齐 Kotlin `FirAbstractBodyResolveTransformerDispatcher.typeResolverTransformer`。
     *
     * `prepareSignatureForBodyResolve(...)` 等路径会直接把 declaration 的 typeRef 交给 dispatcher，
     * 因而 dispatcher 必须像 Kotlin 一样承担 body resolve 内的显式类型解析职责。
     */
    protected val typeResolverTransformer: CfirSpecificTypeResolverTransformer by lazy(LazyThreadSafetyMode.NONE) {
        CfirSpecificTypeResolverTransformer(session)
    }

    /**
     * 对齐 Kotlin `FirAbstractBodyResolveTransformerDispatcher.preserveCFGForClasses`。
     * 主干 body resolve 默认保留 class CFG，low-level resolver 可覆写关闭。
     */
    open val preserveCFGForClasses: Boolean
        get() = !implicitTypeOnly

    /**
     * 对齐 Kotlin `FirAbstractBodyResolveTransformerDispatcher.buildCfgForFiles`。
     * 主干 body resolve 默认构建 file CFG，low-level resolver 可覆写关闭，
     * 再由 LL resolver 在 designated 路径上单独计算 file CFG。
     */
    open val buildCfgForFiles: Boolean
        get() = !implicitTypeOnly

    /** 当前 dispatcher 共享的 body resolve 上下文。 */
    abstract override val context: BodyResolveContext

    /** 从当前 components 创建的调用解析上下文。 */
    final override val resolutionContext: ResolutionContext
        get() = components.createResolutionContext()

    /** 当前 dispatcher 暴露给声明和表达式子 transformer 的组件集合。 */
    abstract override val components: BodyResolveTransformerComponents

    /** 负责表达式节点 body resolve 的子 transformer。 */
    abstract val expressionsTransformer: CfirExpressionsResolveTransformer

    /** 负责声明节点 body resolve 的子 transformer。 */
    abstract val declarationsTransformer: CfirDeclarationsResolveTransformer

    /**
     * 在指定声明内容已经被选中时继续转换声明子树。
     *
     * designated body resolve 使用该入口避免重新进入具体 `transformXxx` 分发。
     */
    open fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: ResolutionMode,
    ): CfirDeclaration {
        // 对齐 Kotlin K2：declaration-content 钩子只负责继续向下遍历当前声明的 children，
        // 让 designated body resolve 可以在“容器已选定”的前提下接管后续子树，而不是重新走一遍具体 transformXxx 分发。
        @Suppress("UNCHECKED_CAST")
        return transformElement(declaration, data) as CfirDeclaration
    }

    /** 默认元素转换：继续遍历子节点并返回原元素。 */
    override fun <E : CfirElement> transformElement(element: E, data: ResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        element.transformChildren(this, data)
        return element
    }

    /**
     * 在 body resolve 上下文中解析显式类型引用。
     *
     * 已解析的错误类型会继续补解析内部部分，未解析类型使用当前 scope 和容器类型参数完成解析。
     */
    override fun transformTypeRef(typeRef: CfirTypeRef, data: ResolutionMode): CfirResolvedTypeRef {
        val resolvedTypeRef = if (typeRef is CfirResolvedTypeRef) {
            if (typeRef is CfirErrorTypeRef) {
                typeRef.transformPartiallyResolvedTypeRef(this, data)
            }
            typeRef
        } else {
            typeResolverTransformer.transformTypeRef(
                typeRef,
                CfirTypeResolutionConfiguration(
                    scopes = components.createCurrentScopeList(),
                    containingClassDeclarations = context.containingClassDeclarations.toList(),
                    useSiteFile = context.file,
                    topContainer = context.containerIfAny,
                ).withAdditionalTypeParameters(context.containers.flatMap(::extractTypeParameters)),
            ) as CfirResolvedTypeRef
        }

        return resolvedTypeRef.transformAnnotations(this, data) as CfirResolvedTypeRef
    }

    /** 在显式要求时用新类型引用替换隐式类型引用。 */
    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: ResolutionMode): CfirTypeRef {
        if (data !is ResolutionMode.UpdateImplicitTypeRef) {
            return implicitTypeRef
        }

        return data.newTypeRef.transformSingle(this, data)
    }

    /** 转换文件 body，并在文件作用域内建立可访问性上下文。 */
    override fun transformFile(file: CfirFile, data: ResolutionMode): CfirFile {
        checkSessionConsistency(file)
        return CfirAccessibilityFileScope.with(file) {
            declarationsTransformer.transformFile(file, data)
        }
    }

    /** 将代码片段 body resolve 分发给声明 transformer。 */
    override fun transformCodeFragment(codeFragment: CfirCodeFragment, data: ResolutionMode): CfirCodeFragment {
        return declarationsTransformer.transformCodeFragment(codeFragment, data)
    }

    /** 将 class body resolve 分发给声明 transformer。 */
    override fun transformClass(klass: CfirClass, data: ResolutionMode): CfirClass {
        return declarationsTransformer.transformClass(klass, data)
    }

    /** 将 interface body resolve 分发给声明 transformer。 */
    override fun transformInterface(interfaceDeclaration: CfirInterface, data: ResolutionMode): CfirInterface {
        return declarationsTransformer.transformInterface(interfaceDeclaration, data)
    }

    /** 将 struct body resolve 分发给声明 transformer。 */
    override fun transformStruct(struct: CfirStruct, data: ResolutionMode): CfirStruct {
        return declarationsTransformer.transformStruct(struct, data)
    }

    /** 将 enum body resolve 分发给声明 transformer。 */
    override fun transformEnum(enum: CfirEnum, data: ResolutionMode): CfirEnum {
        return declarationsTransformer.transformEnum(enum, data)
    }

    /** 将 extend body resolve 分发给声明 transformer。 */
    override fun transformExtend(extend: CfirExtend, data: ResolutionMode): CfirExtend {
        return declarationsTransformer.transformExtend(extend, data)
    }

    /** 将普通函数 body resolve 分发给声明 transformer。 */
    override fun transformFunction(function: CfirFunction, data: ResolutionMode): CfirFunction {
        return declarationsTransformer.transformFunction(function, data)
    }

    /** 将构造器 body resolve 分发给声明 transformer。 */
    override fun transformConstructor(constructor: CfirConstructor, data: ResolutionMode): CfirConstructor {
        return declarationsTransformer.transformConstructor(constructor, data)
    }

    /** 将 enum constructor body resolve 分发给声明 transformer。 */
    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: ResolutionMode): CfirEnumConstructor {
        return declarationsTransformer.transformEnumConstructor(enumConstructor, data)
    }

    /** 将具名函数 body resolve 分发给声明 transformer。 */
    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: ResolutionMode): CfirNamedFunction {
        return declarationsTransformer.transformNamedFunction(namedFunction, data)
    }

    /** 将 main 函数 body resolve 分发给声明 transformer。 */
    override fun transformMainFunction(mainFunction: CfirMainFunction, data: ResolutionMode): CfirMainFunction {
        return declarationsTransformer.transformMainFunction(mainFunction, data)
    }

    /** 将宏声明 body resolve 分发给声明 transformer。 */
    override fun transformMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: ResolutionMode): CfirMacroDeclaration {
        return declarationsTransformer.transformMacroDeclaration(macroDeclaration, data)
    }

    /** 将 finalizer body resolve 分发给声明 transformer。 */
    override fun transformFinalizer(finalizer: CfirFinalizer, data: ResolutionMode): CfirFinalizer {
        return declarationsTransformer.transformFinalizer(finalizer, data)
    }

    /** 将属性 body resolve 分发给声明 transformer。 */
    override fun transformProperty(property: CfirProperty, data: ResolutionMode): CfirProperty {
        return declarationsTransformer.transformProperty(property, data)
    }

    /** 将属性访问器 body resolve 分发给声明 transformer。 */
    override fun transformPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: ResolutionMode): CfirPropertyAccessor {
        return declarationsTransformer.transformPropertyAccessor(propertyAccessor, data)
    }

    /** 将字段变量 body resolve 分发给声明 transformer。 */
    override fun transformFieldVariable(fieldVariable: CfirFieldVariable, data: ResolutionMode): CfirFieldVariable {
        return declarationsTransformer.transformFieldVariable(fieldVariable, data)
    }

    /**
     * Body resolve 内部的显式类型引用可能出现在函数体、局部声明、默认参数等位置。
     * 这些位置必须继承当前 container 链上所有可见类型参数，保持与 Kotlin
     * `FirMemberTypeParameterScope` 相同的类型解析上下文。
     */
    private fun extractTypeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> {
        return when (declaration) {
            is CfirClass -> declaration.typeParameters
            is CfirInterface -> declaration.typeParameters
            is CfirStruct -> declaration.typeParameters
            is CfirEnum -> declaration.typeParameters
            is CfirConstructor -> declaration.typeParameters
            is CfirProperty -> declaration.typeParameters
            is CfirFieldVariable -> declaration.typeParameters
            is CfirValueParameter -> declaration.typeParameters
            is CfirExtend -> declaration.typeParameters
            is CfirTypeAlias -> declaration.typeParameters
            is CfirPatternVariable -> declaration.typeParameters
            is CfirMacroDeclaration -> declaration.typeParameters
            is CfirMainFunction -> declaration.typeParameters
            is CfirFinalizer -> declaration.typeParameters
            is CfirFunction -> declaration.typeParameters
            is CfirCodeFragment -> emptyList()
            is CfirEnumConstructor -> declaration.typeParameters
            else -> emptyList()
        }
    }

    /** 将模式绑定变量 body resolve 分发给声明 transformer。 */
    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: ResolutionMode,
    ): CfirPatternBindingVariable {
        return declarationsTransformer.transformPatternBindingVariable(patternBindingVariable, data)
    }

    /** 将通用变量 body resolve 分发给声明 transformer。 */
    override fun transformVariable(variable: CfirVariable, data: ResolutionMode): CfirVariable {
        return declarationsTransformer.transformVariable(variable, data)
    }

    /** 将模式变量 body resolve 分发给声明 transformer。 */
    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: ResolutionMode,
    ): CfirPatternVariable {
        return declarationsTransformer.transformPatternVariable(patternVariable, data)
    }

    /** 将通用声明 body resolve 分发给声明 transformer。 */
    override fun transformDeclaration(declaration: CfirDeclaration, data: ResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformDeclaration(declaration, data)
    }

    /** 将 block body resolve 分发给声明 transformer，因为 block 会引入局部声明作用域。 */
    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression {
        return declarationsTransformer.transformBlock(block, data)
    }

    /** 将通用表达式 body resolve 分发给表达式 transformer。 */
    override fun transformExpression(expression: CfirExpression, data: ResolutionMode): CfirExpression {
        return expressionsTransformer.transformExpression(expression, data) as CfirExpression
    }

    /** 将 wrapped expression body resolve 分发给表达式 transformer。 */
    override fun transformWrappedExpression(
        wrappedExpression: CfirWrappedExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformWrappedExpression(wrappedExpression, data)
    }

    /** 将可选表达式 body resolve 分发给表达式 transformer。 */
    override fun transformOptionalExpression(
        optionalExpression: CfirOptionalExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformOptionalExpression(optionalExpression, data)
    }

    /** 将可选链表达式 body resolve 分发给表达式 transformer。 */
    override fun transformOptionalChainExpression(
        optionalChainExpression: CfirOptionalChainExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformOptionalChainExpression(optionalChainExpression, data)
    }

    /** 将字面量表达式 body resolve 分发给表达式 transformer。 */
    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLiteralExpression(literalExpression, data)
    }

    /** 将命名访问表达式 body resolve 分发给表达式 transformer。 */
    override fun transformNamedAccessExpression(
        namedAccess: CfirNamedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformNamedAccessExpression(namedAccess, data)
    }

    /** 将 super receiver 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformSuperReceiverExpression(
        superReceiverExpression: CfirSuperReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSuperReceiverExpression(superReceiverExpression, data)
    }

    /** 将限定访问表达式 body resolve 分发给表达式 transformer。 */
    override fun transformQualifiedAccessExpression(
        qualifiedAccess: CfirQualifiedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformQualifiedAccessExpression(qualifiedAccess, data)
    }

    /** 将函数调用表达式 body resolve 分发给表达式 transformer。 */
    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformFunctionCall(functionCall, data)
    }

    /** 将自增自减表达式 body resolve 分发给表达式 transformer。 */
    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: CfirIncrementDecrementExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformIncrementDecrementExpression(incrementDecrementExpression, data)
    }

    /** 将 if 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformIfExpression(ifExpression, data)
    }

    /** 将 let pattern 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformLetPatternExpression(
        letPatternExpression: CfirLetPatternExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLetPatternExpression(letPatternExpression, data)
    }

    /** 将 return 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformReturnExpression(returnExpression, data)
    }

    /** 将循环跳转表达式 body resolve 分发给表达式 transformer。 */
    override fun transformLoopJump(
        jumpExpression: CfirLoopJump,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLoopJump(jumpExpression, data)
    }

    /** 将 break 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformBreakExpression(
        breakExpression: CfirBreakExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformBreakExpression(breakExpression, data)
    }

    /** 将 continue 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformContinueExpression(
        continueExpression: CfirContinueExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformContinueExpression(continueExpression, data)
    }

    /** 将赋值表达式 body resolve 分发给表达式 transformer。 */
    override fun transformAssignment(
        assignment: CfirAssignment,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformAssignment(assignment, data)
    }

    /** 将 tuple 字面量 body resolve 分发给表达式 transformer。 */
    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTupleLiteral(tupleLiteral, data)
    }

    /** 将数组字面量 body resolve 分发给表达式 transformer。 */
    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformArrayLiteral(arrayLiteral, data)
    }

    /** 将字符串插值表达式 body resolve 分发给表达式 transformer。 */
    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformStringInterpolation(stringInterpolation, data)
    }

    /** 将 match 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformMatchExpression(matchExpression, data)
    }

    /** 将错误表达式 body resolve 分发给表达式 transformer。 */
    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformErrorExpression(errorExpression, data)
    }

    /** 将比较表达式 body resolve 分发给表达式 transformer。 */
    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformComparisonExpression(comparisonExpression, data)
    }

    /** 将二元操作表达式 body resolve 分发给表达式 transformer。 */
    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformBinaryOp(binaryOp, data)
    }

    /** 将类型操作表达式 body resolve 分发给表达式 transformer。 */
    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTypeOperator(typeOperator, data)
    }

    /** 将类型转换表达式 body resolve 分发给表达式 transformer。 */
    override fun transformTypeConversion(
        typeConversion: CfirTypeConversion,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTypeConversion(typeConversion, data)
    }

    /** 将 for-in 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformForInExpression(forInExpression, data)
    }

    /** 将循环表达式 body resolve 分发给表达式 transformer。 */
    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLoopExpression(loopExpression, data)
    }

    /** 将 throw 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformThrowExpression(throwExpression, data)
    }

    /** 将 perform 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformPerformExpression(
        performExpression: CfirPerformExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformPerformExpression(performExpression, data)
    }

    /** 将 resume 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformResumeExpression(
        resumeExpression: CfirResumeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformResumeExpression(resumeExpression, data)
    }

    /** 将 effect handle clause body resolve 分发给表达式 transformer。 */
    override fun transformHandleClause(
        handleClause: CfirHandleClause,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformHandleClause(handleClause, data)
    }

    /** 将 try 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTryExpression(tryExpression, data)
    }

    /** 将 catch 子句 body resolve 分发给表达式 transformer。 */
    override fun transformCatch(
        catch: CfirCatch,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformCatch(catch, data)
    }

    /** 将下标表达式 body resolve 分发给表达式 transformer。 */
    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSubscriptExpression(subscriptExpression, data)
    }

    /** 将匿名函数表达式 body resolve 分发给表达式 transformer。 */
    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformAnonymousFunctionExpression(anonymousFunctionExpression, data)
    }

    /** 将 range 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformRangeExpression(rangeExpression, data)
    }

    /** 将 spawn 表达式 body resolve 分发给表达式 transformer。 */
    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSpawnExpression(spawnExpression, data)
    }
}
