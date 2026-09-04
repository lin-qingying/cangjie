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

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.AstLoadingFilter
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.utils.addDefaultBoundIfNecessary
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.CfirCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildSuperReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildBasicTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjNodeTypes.*
import org.cangnova.cangjie.source.*
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression as buildErrorExpressionNode

/** `CjPsiFactory.createCallArguments` 内部宿主文本 `let x = foo ` 的长度。 */
private const val PSI_SYNTHETIC_CALL_ARGUMENTS_PREFIX_LENGTH: Int = 12

/** macro-expression wrapper 文本中的半开区间。 */
private data class MacroExpressionTextRange(
    /** 起始偏移，相对 wrapper 文本。 */
    val startOffset: Int,
    /** 结束偏移，相对 wrapper 文本。 */
    val endOffset: Int,
)

/**
 * 当前 macro-expression wrapper 头部语法。
 *
 * PSI 的 [CjMacroExpression] 子树可能只保留 wrapper 名称，而把 `[attr]`
 * 留在原始文本中；raw CFIR 需要从当前 wrapper 头部恢复 annotation-site
 * 名称与属性区间，不能从 input declaration 子树泛化搜索。
 */
private data class MacroExpressionHeadSyntax(
    /** 扫描出的原始限定名文本。 */
    val rawName: String,
    /** 名称整体在 wrapper 文本中的起止区间。 */
    val nameRange: MacroExpressionTextRange,
    /** 本层 wrapper 的属性区间，包含左右方括号。 */
    val attrRange: MacroExpressionTextRange?,
)

/** macro-expression input 文本中恢复出的直接 annotation 语法。 */
private data class MacroExpressionInputAnnotationSyntax(
    /** 完整 annotation 文本，包含 `@` 前缀。 */
    val rawSyntax: String,
    /** annotation 本体在 wrapper 文本中的区间。 */
    val annotationRange: MacroExpressionTextRange,
    /** 标准 `(...)` 实参列表区间。 */
    val argumentRange: MacroExpressionTextRange?,
    /** 宏式 `[...]` attr 区间。 */
    val macroAttributeRange: MacroExpressionTextRange?,
)

/** 从 wrapper 文本重解析出的 PSI annotation 及其原始 source 映射。 */
private data class ReparsedMacroInputAnnotation(
    val annotation: CjAnnotation,
    val rawSyntax: String,
    val annotationSource: CjSourceElement,
    val sourceOffsetDelta: Int,
    val argumentListSource: CjSourceElement?,
    val macroAttributeText: String?,
    val macroAttributeStartOffset: Int?,
)

/**
 * `PSI -> Raw CFIR` 构建器，对齐 Kotlin 的 `PsiRawFirBuilder`。
 * 遍历 PSI 语法树，生成 Raw CFIR 中间表示。
 * 在 `RAW_CFIR` 阶段：
 * - 所有类型引用都保持为 `CfirUserTypeRef`，尚未解析
 * - 所有符号引用都保持为 `CfirNamedReference`，尚未绑定
 * - 不做类型推断和重载解析，这些工作留给 `CFIR_RESOLVE`
 *
 * @property baseScopeProvider PSI raw 构建使用的基础 scope provider。
 * @property bodyBuildingMode body 构建策略，用于普通构建与 lazy body 构建分流。
 */
class PsiRawCfirBuilder(
    session: CfirSession,
    @Suppress("unused")
    /** PSI raw 构建使用的基础 scope provider。 */
    val baseScopeProvider: CfirScopeProvider = session.cangjieScopeProvider,
    /** body 构建策略，用于普通构建与 lazy body 构建分流。 */
    private val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) : AbstractRawCfirBuilder<PsiElement>(session) {
    /** 只覆盖 body 构建模式的便捷构造函数。 */
    constructor(
        session: CfirSession,
        bodyBuildingMode: BodyBuildingMode,
    ) : this(
        session = session,
        baseScopeProvider = session.cangjieScopeProvider,
        bodyBuildingMode = bodyBuildingMode,
    )

    /**
     * Macro construction-only surface 累加器（baseline Batch 4b）。
     *
     * 由 [Converter.convertMacroExpression] 在转换每个 `@Foo(...)` 调用时 push 一条
     * [MacroSurfaceExpr]；上层 raw-build 入口经
     * [consumeCollectedMacroSurfaces] 取出列表，并交给
     * `org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles`。
     *
     * expression surface 使用 typed error carrier 作为稳定替换锚点；
     * raw builder 不再为新 macro 调用生成旧 CFIR macro-expression carrier。
     */
    private val collectedMacroSurfaces: MutableList<MacroSurface> = mutableListOf()

    /** 提取并清空当前累加的 macro surface 列表。 */
    fun consumeCollectedMacroSurfaces(): List<MacroSurface> {
        val snapshot = collectedMacroSurfaces.toList()
        collectedMacroSurfaces.clear()
        return snapshot
    }

    // ===== AbstractRawCfirBuilder 抽象方法实现 =====

    /** 将 PSI 元素包装为真实 PSI source element。 */
    override fun PsiElement.toSourceElement(): AbstractCjSourceElement {
        return CjRealPsiSourceElement(this)
    }

    /** 返回 PSI 节点 element type。 */
    override fun PsiElement.elementType(): IElementType = node.elementType

    /** 返回 PSI 元素源码文本。 */
    override fun PsiElement.asText(): String = text

    /** 当前 body 构建模式；由构造参数初始化，对外只读。 */
    var mode: BodyBuildingMode = bodyBuildingMode
        private set

    /** 在当前 [mode] 下执行 [body]，lazy body 模式会禁止 AST 加载。 */
    private inline fun <T> runOnStubs(crossinline body: () -> T): T {
        return when (mode) {
            BodyBuildingMode.NORMAL -> body()
            BodyBuildingMode.LAZY_BODIES -> {
                AstLoadingFilter.disallowTreeLoading<T, Nothing> { body() }
            }
        }
    }

    /** 按当前 [mode] 在立即构建与 lazy 构建之间选择。 */
    private inline fun <T> buildOrLazy(build: () -> T, noinline lazy: () -> T): T {
        return when (mode) {
            BodyBuildingMode.NORMAL -> build()
            BodyBuildingMode.LAZY_BODIES -> runOnStubs(lazy)
        }
    }

    /** 构建 body block；lazy body 模式下返回 lazy block 占位。 */
    private inline fun buildOrLazyBlock(buildBlock: () -> CfirBlock?): CfirBlock? {
        return buildOrLazy(buildBlock) { buildLazyBlock() }
    }

    /** annotation macro surface 附着的语义目标。 */
    private enum class AnnotationSurfaceTarget {
        /** annotation 附着到声明。 */
        DECLARATION,
        /** annotation 附着到参数。 */
        PARAMETER,
    }

    /** PSI raw builder 的常量集合。 */
    private companion object {
        /** 内建 non-macro annotation `IfAvailable` 的短名。 */
        private const val IF_AVAILABLE_ANNOTATION_NAME: String = "IfAvailable"
        /** 不应送入 macro executor 的内建普通 annotation 名称集合。 */
        private val builtinAnnotationMacroNames: Set<Name> = setOf(
            Name.identifier("C"),
            Name.identifier("CallingConv"),
            Name.identifier("CJMapping"),
            Name.identifier("Deprecated"),
            Name.identifier("ForeignName"),
            Name.identifier("Frozen"),
            Name.identifier("Java"),
            Name.identifier("JavaImpl"),
            Name.identifier("JavaMirror"),
            Name.identifier("ObjCCJMapping"),
            Name.identifier("ObjCImpl"),
            Name.identifier("ObjCInit"),
            Name.identifier("ObjCMirror"),
        )
    }

    // ===== Public API =====

    /**
     * 构建 `CfirFile`，作为文件级入口点。
     */
    fun buildCfirFile(file: CjFile): CfirFile {
        return runOnStubs { file.accept(Visitor(), null) as CfirFile }
    }

    // ===== Visitor（私有访问器类型，对齐 Kotlin 的 PsiRawFirBuilder.Visitor）=====

    /** PSI visitor 分派器实例。 */
    private val visitor = Visitor()

    /** PSI 到 CFIR 的具体转换器实例。 */
    private val converter = Converter()

    /** 从通用 PSI 元素构建 CFIR 元素。 */
    override fun buildElement(element: PsiElement): CfirElement {
        val cjElement = element as? CjElement
            ?: error("Expected CjElement but was ${element::class.qualifiedName}")
        return cjElement.accept(visitor, null)
            ?: error("Unsupported PSI element: ${element::class.qualifiedName}")
    }

    /** 从通用 PSI 元素构建 CFIR 文件。 */
    override fun buildFile(file: PsiElement): CfirFile {
        val cjFile = file as? CjFile ?: error("Expected CjFile but was ${file::class.qualifiedName}")
        return buildFile(cjFile)
    }

    /** 从通用 PSI 元素构建 CFIR 声明。 */
    override fun buildDeclaration(declaration: PsiElement): CfirDeclaration {
        val cjDeclaration = declaration as? CjDeclaration
            ?: error("Expected CjDeclaration but was ${declaration::class.qualifiedName}")
        return converter.convertDeclaration(cjDeclaration)
    }

    /**
     * Macro fragment reparse 入口。
     *
     * 片段不是完整文件，必须显式继承原 macro 位点的包上下文，保证
     * fragment 内生成的 symbol/callableId 与宿主文件一致。
     */
    fun buildDeclarationInPackage(declaration: CjDeclaration, packageFqName: FqName): CfirDeclaration {
        return withPackageContext(packageFqName) {
            converter.convertDeclaration(declaration)
        }
    }

    /** 从通用 PSI 元素构建 CFIR 表达式。 */
    override fun buildExpression(expression: PsiElement): CfirExpression {
        val cjExpression = expression as? CjExpression
            ?: error("Expected CjExpression but was ${expression::class.qualifiedName}")
        return converter.convertExpression(cjExpression)
    }

    /**
     * Macro expression fragment reparse 入口。
     */
    fun buildExpressionInPackage(expression: CjExpression, packageFqName: FqName): CfirExpression {
        return withPackageContext(packageFqName) {
            converter.convertExpression(expression)
        }
    }

    /**
     * Macro parameter fragment reparse 入口。
     *
     * 参数 fragment 必须复用原宿主 callable symbol；不能通过临时 wrapper
     * 函数生成新的 containing symbol 后再放回原函数参数列表。
     */
    fun buildValueParameterInPackage(
        parameter: CjParameter,
        containingSymbol: CfirBasedSymbol<*>,
        packageFqName: FqName,
    ): CfirValueParameter {
        return withPackageContext(packageFqName) {
            converter.convertValueParameter(parameter, containingSymbol)
        }
    }

    /**
     * Macro custom annotation fragment reparse 入口。
     *
     * 只构造 annotation payload，不借用参数或声明 fragment 包装，确保
     * `CUSTOM_ANNOTATION` 消费的是 annotation slot snapshot 的完整语法。
     */
    fun buildAnnotationCallInPackage(
        annotation: CjAnnotation,
        containingSymbol: CfirBasedSymbol<*>,
        packageFqName: FqName,
        sourceOverride: CjSourceElement? = null,
        argumentListSourceOverride: CjSourceElement? = null,
    ): CfirAnnotationCall {
        return withPackageContext(packageFqName) {
            val sourceOffsetDelta = sourceOffsetDelta(sourceOverride, annotation)
            converter.convertAnnotationCall(
                annotation = annotation,
                containingSymbol = containingSymbol,
                sourceOverride = sourceOverride,
                typeRefOverride = annotation.typeReference?.let {
                    converter.buildAnnotationTypeRef(it, sourceOffsetDelta)
                },
                calleeReferenceSourceOverride = annotation.typeReference?.shiftedBy(sourceOffsetDelta)
                    ?: sourceOverride,
                argumentListSourceOverride = argumentListSourceOverride,
                macroAttributeOverride = PsiTreeUtil.findChildOfType(annotation, CjMacroAttr::class.java),
            )
        }
    }

    /** 使用 [symbol] 创建 source declaration，并保持符号与声明的类型关系。 */
    private inline fun <D : CfirDeclaration, S : CfirBasedSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)
        return declaration
    }

    /** 构建文件级 raw CFIR 根节点，并收集 package、import 与顶层声明。 */
    private fun buildFile(file: CjFile): CfirFile {
        return withPackageContext(file.packageFqName) {
            val symbol = CfirFileSymbol()
            buildSourceDeclaration(symbol) { fileSymbol ->
                buildFile {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = file.toCjPsiSourceElement()
                    this.symbol = fileSymbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    name = file.name
                    sourceFile = CjPsiSourceFile(file)
                    sourceFileLinesMapping = CjPsiSourceFileLinesMapping(file)
                    packageDirective = buildPackageDirective(file.packageDirective)
                    imports.addAll(this@PsiRawCfirBuilder.buildImports(file))
                    if (file is CjCodeFragment) {
                        declarations.add(converter.convertCodeFragment(file))
                    } else {
                        declarations.addAll(converter.convertFileDeclarations(file))
                    }
                }
            }
        }
    }

    /**
     * Visitor 负责把 PSI 节点分派转换为 CFIR 节点。
     * 对齐 Kotlin 的 `PsiRawFirBuilder.Visitor : KtVisitor<FirElement, FirElement?>`。
     */
    protected open inner class Visitor : CjVisitor<CfirElement, Unit?>() {
        /** 构建文件节点。 */
        override fun visitCjFile(file: CjFile, data: Unit?): CfirElement = buildFile(file)

        /** 构建声明节点。 */
        override fun visitDeclaration(dcl: CjDeclaration, data: Unit?): CfirElement = buildDeclaration(dcl)

        /** 构建表达式节点。 */
        override fun visitExpression(expression: CjExpression, data: Unit?): CfirElement = buildExpression(expression)
    }

    /**
     * PSI 到 raw CFIR 的主体转换器。
     *
     * 该类按语法类别拆分声明、表达式、类型、annotation、pattern 和辅助结构转换，
     * 所有输出都停留在 `RAW_CFIR` 阶段，不做符号解析或类型推断。
     */
    protected open inner class Converter {

        // ===== 声明转换 =====

        /** 转换文件中的顶层声明与顶层 macro declaration surface。 */
        fun convertFileDeclarations(file: CjFile): List<CfirDeclaration> {
            return buildList {
                for (child in file.children) {
                    when (child) {
                        is CjPackageDirective -> Unit
                        is CjDeclaration -> add(convertDeclaration(child))
                        is CjMacroExpression -> convertTopLevelMacroDeclaration(child)?.let(::add)
                    }
                }
            }
        }

        /** 转换 IDE code fragment，按 fragment 类型构造对应的 raw block。 */
        fun convertCodeFragment(file: CjCodeFragment): CfirCodeFragment {
            val symbol = CfirCodeFragmentSymbol()
            return buildCodeFragment {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = file.toCjPsiSourceElement()
                moduleData = baseModuleData
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                this.symbol = symbol
                block = buildOrLazyBlock {
                    withContainerSymbol(symbol) {
                        withLocalContext {
                            when (file) {
                                is CjExpressionCodeFragment -> {
                                    val expression = file.getContentElement()
                                    if (expression != null) {
                                        buildBlock {
                                            source = expression.toCjPsiSourceElement()
                                            statements.add(convertExpression(expression))
                                        }
                                    } else {
                                        buildBlock {
                                            source = file.toCjPsiSourceElement()
                                        }
                                    }
                                }

                                is CjBlockCodeFragment -> convertBlock(file.getContentElement())

                                is CjTypeCodeFragment -> buildBlock {
                                    source = file.getContentElement()?.toCjPsiSourceElement()
                                        ?: file.toCjPsiSourceElement()
                                }

                                else -> error("Unexpected code fragment type: ${file::class}")
                            }
                        }
                    }
                } ?: buildBlock {
                    source = file.toCjPsiSourceElement()
                }
            }
        }

        /** 按 PSI 声明具体类型分派为对应 raw CFIR 声明。 */
        fun convertDeclaration(psi: CjDeclaration): CfirDeclaration {
            val declaration = when (psi) {
                is CjClass -> convertClass(psi, CfirClassKind.CLASS)
                is CjInterface -> convertClass(psi, CfirClassKind.INTERFACE)
                is CjStruct -> convertClass(psi, CfirClassKind.STRUCT)
                is CjEnum -> convertClass(psi, CfirClassKind.ENUM)
                is CjExtend -> convertExtend(psi)
                is CjMainFunction -> convertMainFunction(psi)
                is CjMacroDeclaration -> convertMacroDeclaration(psi)
                is CjNamedFunction -> convertFunction(psi)
                is CjFinalizer -> convertFinalizer(psi)
                is CjProperty -> convertProperty(psi)
                is CjFieldVariable -> convertFieldVariable(psi)
                is CjPatternVariable -> convertPatternVariable(psi)
                is CjPrimaryConstructor -> convertConstructor(psi, isPrimary = true)
                is CjSecondaryConstructor -> convertConstructor(psi, isPrimary = false)
                is CjTypeAlias -> convertTypeAlias(psi)
                else -> buildSourceDeclaration(CfirInvalidDeclarationSymbol()) { symbol ->
                    buildInvalidDeclaration {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData

                        attributes = CfirDeclarationAttributes.EMPTY
                        reason = "Unsupported declaration: ${psi.javaClass.simpleName}"
                    }
                }
            }
            collectMacroAnnotationSurfaces(psi, AnnotationSurfaceTarget.DECLARATION, declaration)
            return declaration
        }

        /**
         * 文件级 `@Macro decl` 在 PSI 中是 [CjMacroExpression]，其 input
         * 承载真实声明。这里先构造 carrier 声明，再用同一个对象身份建立
         * declaration surface 的 stable replace handle。
         */
        private fun convertTopLevelMacroDeclaration(psi: CjMacroExpression): CfirDeclaration? {
            val chain = resolveMacroDeclarationChain(psi) ?: return null
            val (declaration, macroExpressions) = chain
            val carrier = convertDeclaration(declaration)
            macroExpressions.forEach { expression ->
                applyTopLevelMacroExpression(expression, carrier)
                collectMacroInputAnnotationSurfaces(expression, declaration, carrier)
            }
            return carrier
        }

        /**
         * 顶层 `@Anno @Anno decl` 在 PSI 上会形成多层 [CjMacroExpression] 链。
         * raw builder 必须沿链找到最终声明，并按源码顺序把每层 annotation-site
         * 回放到同一个 carrier 上，不能只消费最外层 wrapper。
         */
        private fun resolveMacroDeclarationChain(root: CjMacroExpression): Pair<CjDeclaration, List<CjMacroExpression>>? {
            val macroExpressions = mutableListOf<CjMacroExpression>()
            var current: CjMacroExpression? = root

            while (current != null) {
                macroExpressions += current
                val input = current.input ?: return null
                val next = input.children.firstOrNull { it is CjMacroExpression } as? CjMacroExpression
                if (next != null) {
                    current = next
                    continue
                }
                input.declarations?.let { declaration ->
                    return declaration to macroExpressions.toList()
                }
                current = null
            }

            return null
        }

        /**
         * 收集 macro wrapper input 中直接包裹 carrier 声明的普通 annotation。
         *
         * PSI 会把 `@Outer @Inner decl` 表达为 `Outer(input = @Inner decl)`：
         * 外层 wrapper 自身由 [applyTopLevelMacroExpression] 回放，input 中与
         * carrier 同层的普通 annotation 也必须挂到同一个 carrier 上，才能恢复
         * 官方 original macro-call forest。
         */
        private fun collectMacroInputAnnotationSurfaces(
            wrapper: CjMacroExpression,
            annotatedDeclaration: CjDeclaration,
            carrier: CfirDeclaration,
        ) {
            val directAnnotations = wrapper.input?.directAnnotationsBeforeCarrier().orEmpty()
            val reparsedAnnotations = wrapper.reparseInputAnnotationsBeforeCarrier(annotatedDeclaration)
            if (directAnnotations.isEmpty() && reparsedAnnotations.isEmpty()) return

            val attachedRanges = annotatedDeclaration.annotationEntries.mapTo(mutableSetOf()) { it.textRange }
            val detachedAnnotations = directAnnotations.filter { it.textRange !in attachedRanges }
            collectMacroAnnotationSurfaces(
                annotated = annotatedDeclaration,
                entries = detachedAnnotations,
                target = AnnotationSurfaceTarget.DECLARATION,
                carrier = carrier,
            )
            collectReparsedMacroInputAnnotationSurfaces(
                annotated = annotatedDeclaration,
                annotations = reparsedAnnotations,
                carrier = carrier,
            )
        }

        /**
         * 只读取当前 [CjMacroInput] 的直接 annotation 子节点。
         *
         * 遇到下一层 [CjMacroExpression] 或最终 [CjDeclaration] 后停止，禁止递归进入
         * class body / block，避免把内部声明 annotation 误绑定到外层 carrier。
         */
        private fun CjMacroInput.directAnnotationsBeforeCarrier(): List<CjAnnotation> {
            val result = mutableListOf<CjAnnotation>()
            for (child in children) {
                when (child) {
                    is CjAnnotations -> result += child.entries
                    is CjAnnotation -> result += child
                    is CjMacroExpression,
                    is CjDeclaration,
                        -> return result
                }
            }
            return result
        }

        /** 从 wrapper 原始文本中恢复 AST 未建模的 input annotation。 */
        private fun CjMacroExpression.reparseInputAnnotationsBeforeCarrier(
            annotatedDeclaration: CjDeclaration,
        ): List<ReparsedMacroInputAnnotation> {
            val headSyntax = macroExpressionHeadSyntax() ?: return emptyList()
            val rawText = text.orEmpty()
            val scanStart = headSyntax.attrRange?.endOffset ?: headSyntax.nameRange.endOffset
            val declarationStart = (annotatedDeclaration.textRange.startOffset - textRange.startOffset)
                .coerceIn(scanStart, rawText.length)
            val syntaxes = scanMacroExpressionInputAnnotationSyntax(rawText, scanStart, declarationStart)
            if (syntaxes.isEmpty()) return emptyList()

            val factory = CjPsiFactory.contextual(this)
            return syntaxes.mapNotNull { syntax ->
                val annotation = runCatching {
                    factory.createAnnotations(syntax.rawSyntax).entries.singleOrNull()
                }.getOrNull() ?: return@mapNotNull null
                val annotationSource = sliceMacroExpressionSource(syntax.annotationRange)
                val sourceOffsetDelta = sourceOffsetDelta(annotationSource, annotation)
                ReparsedMacroInputAnnotation(
                    annotation = annotation,
                    rawSyntax = syntax.rawSyntax,
                    annotationSource = annotationSource,
                    sourceOffsetDelta = sourceOffsetDelta,
                    argumentListSource = syntax.argumentRange?.let(::sliceMacroExpressionSource),
                    macroAttributeText = syntax.macroAttributeRange
                        ?.let { range -> rawText.substring(range.startOffset, range.endOffset) },
                    macroAttributeStartOffset = syntax.macroAttributeRange
                        ?.let { range -> textRange.startOffset + range.startOffset },
                )
            }
        }

        /** 将从 wrapper 文本重解析出的 annotation surface 写入同一个 carrier。 */
        private fun collectReparsedMacroInputAnnotationSurfaces(
            annotated: CjDeclaration,
            annotations: List<ReparsedMacroInputAnnotation>,
            carrier: CfirDeclaration,
        ) {
            if (annotations.isEmpty()) return
            val metadataRegistry = baseSession.ensureAnnotationMetadataRegistry()
            val modifiers = (annotated as? CjModifierListOwner)
                ?.modifierList
                ?.let(::collectModifierNames)
                .orEmpty()
            val carriedAnnotations = annotations.map { it.rawSyntax }
            val containerContext = macroContainerContext(annotated, AnnotationSurfaceTarget.DECLARATION)
            val containingSymbol = when (carrier) {
                is CfirValueParameter -> carrier.containingDeclarationSymbol
                else -> carrier.symbol
            }

            for (reparsed in annotations) {
                val annotation = reparsed.annotation
                val macroAttribute = PsiTreeUtil.findChildOfType(annotation, CjMacroAttr::class.java)
                val annotationCall = convertAnnotationCall(
                    annotation = annotation,
                    containingSymbol = containingSymbol,
                    sourceOverride = reparsed.annotationSource,
                    typeRefOverride = annotation.typeReference?.let {
                        buildAnnotationTypeRef(it, reparsed.sourceOffsetDelta)
                    },
                    calleeReferenceSourceOverride = annotation.typeReference?.shiftedBy(reparsed.sourceOffsetDelta)
                        ?: reparsed.annotationSource,
                    argumentListSourceOverride = reparsed.argumentListSource,
                    macroAttributeOverride = macroAttribute,
                    macroAttributeTextOverride = reparsed.macroAttributeText,
                    macroAttributeStartOffsetOverride = reparsed.macroAttributeStartOffset,
                )
                val annotationIndex = carrier.annotations.size
                carrier.replaceAnnotations(carrier.annotations + annotationCall)
                val isCompileTimeVisible = reparsed.rawSyntax.trimStart().startsWith("@!")
                val snapshot = CfirAnnotationSlotSnapshot(
                    owner = carrier,
                    annotationIndex = annotationIndex,
                    originalAnnotation = annotationCall,
                    rawSyntax = reparsed.rawSyntax,
                    forcedCustom = isCompileTimeVisible,
                    isCompileTimeVisible = isCompileTimeVisible,
                    annotationSource = reparsed.annotationSource,
                    qualifiedName = annotationQualifiedName(annotation),
                    argumentText = annotation.valueArgumentList?.text ?: reparsed.macroAttributeText,
                    tokens = MacroPayloadTokenizer.tokenize(
                        reparsed.rawSyntax,
                        reparsed.annotationSource.startOffset,
                    ).toMacroSurfaceTokens(),
                    callSite = MacroCallSite.DECLARATION,
                )
                val annotationCarrier = metadataRegistry.record(snapshot)
                collectedMacroSurfaces += buildMacroAnnotationSurface(
                    annotation = annotation,
                    target = AnnotationSurfaceTarget.DECLARATION,
                    carrier = carrier,
                    annotationCarrier = annotationCarrier,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    containerContext = containerContext,
                    sourceOverride = reparsed.annotationSource,
                    rawSyntaxOverride = reparsed.rawSyntax,
                    inputTokensOverride = declarationAnnotationMacroInputTokens(
                        annotated = annotated,
                        annotationEndOffset = reparsed.annotationSource.endOffset,
                        shortName = annotation.shortName?.asString(),
                    ),
                    attrTokensOverride = reparsed.macroAttributeText?.let { macroAttributeText ->
                        MacroPayloadTokenizer.tokenize(
                            macroAttributeText,
                            reparsed.macroAttributeStartOffset ?: 0,
                        ).toMacroSurfaceTokens()
                    },
                )
            }
        }

        /**
         * 将顶层 [CjMacroExpression] 回放为 declaration macro surface。
         *
         * 该函数先确保 builtin annotation macro 已经走普通 annotation 路径，
         * 其余 macro expression 才建立 [MacroSurfaceDecl] 与 stable replace handle。
         */
        private fun applyTopLevelMacroExpression(
            psi: CjMacroExpression,
            carrier: CfirDeclaration,
        ) {
            if (applyBuiltinAnnotationMacroExpression(psi, carrier)) return

            val surfaceId = MacroSurfaceIdGenerator.next()
            val text = psi.text.orEmpty()
            val currentPackage = context.packageFqName
            val qualifiedName = psi.macroExpressionQualifiedName(currentPackage)
            val source = psi.toCjPsiSourceElement()
            val input = psi.input
            val containingSymbol = when (carrier) {
                is CfirValueParameter -> carrier.containingDeclarationSymbol
                else -> carrier.symbol
            }
            val headSyntax = psi.macroExpressionHeadSyntax()
            val macroAttributeText = psi.macroAttributeText(headSyntax)
            val macroAttributeSource = psi.macroAttributeSourceElement(headSyntax)
            val macroAnnotation = psi.asDeclarationMacroAnnotation()
            val annotationCarrier = macroAnnotation?.let { annotation ->
                val annotationSource = psi.annotationSourceElement(headSyntax)
                val isCompileTimeVisible = text.trimStart().startsWith("@!")
                val annotationCall = convertAnnotationCall(
                    annotation = annotation,
                    containingSymbol = containingSymbol,
                    sourceOverride = annotationSource,
                    typeRefOverride = psi.toAnnotationTypeRefOverride(),
                    calleeReferenceSourceOverride = psi.referenceExpression?.toCjPsiSourceElement(),
                    argumentListSourceOverride = macroAttributeSource,
                    macroAttributeOverride = psi.attr,
                    macroAttributeTextOverride = macroAttributeText,
                    macroAttributeStartOffsetOverride = macroAttributeSource?.startOffset,
                )
                val annotationIndex = carrier.annotations.size
                carrier.replaceAnnotations(carrier.annotations + annotationCall)
                baseSession.ensureAnnotationMetadataRegistry().record(
                    CfirAnnotationSlotSnapshot(
                        owner = carrier,
                        annotationIndex = annotationIndex,
                        originalAnnotation = annotationCall,
                        rawSyntax = annotation.text,
                        forcedCustom = isCompileTimeVisible,
                        isCompileTimeVisible = isCompileTimeVisible,
                        annotationSource = annotationSource,
                        qualifiedName = qualifiedName,
                        argumentText = macroAttributeText,
                        tokens = MacroPayloadTokenizer.tokenize(
                            annotation.text,
                            psi.textRange.startOffset,
                        ).toMacroSurfaceTokens(),
                        callSite = MacroCallSite.DECLARATION,
                    )
                )
            }
            collectedMacroSurfaces += MacroSurfaceDecl(
                surfaceId = surfaceId,
                qualifiedName = qualifiedName,
                kind = if (text.startsWith("@!")) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
                hasParenthesis = input?.text?.trimStart()?.startsWith("(") == true,
                attrTokens = MacroPayloadTokenizer.tokenize(
                    macroAttributeText,
                    macroAttributeSource?.startOffset ?: 0,
                ).toMacroSurfaceTokens(),
                inputTokens = MacroPayloadTokenizer.tokenize(
                    input?.text,
                    input?.textRange?.startOffset ?: 0,
                ).toMacroSurfaceTokens(),
                sourceRange = MacroSurfaceSourceRange(
                    source = source,
                    startOffset = psi.textRange.startOffset,
                    endOffset = psi.textRange.endOffset,
                ),
                scopeContext = MacroSurfaceScopeContext(
                    packageFqName = currentPackage,
                    enclosingClassFqName = null,
                    enclosingFunctionName = null,
                ),
                modifiers = emptyList(),
                carriedAnnotations = emptyList(),
                capturedRawSyntax = text,
                containerContext = MacroSurfaceContainerContext(
                    outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL,
                    isInsidePrimaryConstructor = false,
                    isInsideEnumBody = false,
                    isInsideBlock = false,
                ),
                replaceHandle = CfirReplaceHandle(
                    handleId = surfaceId,
                    carrier = carrier,
                    annotationCarrier = annotationCarrier,
                ),
            )
        }

        /**
         * PSI 语法把文件级 `@Anno decl` 与 declaration macro 共用 [CjMacroExpression]。
         * raw CFIR 必须先为该 annotation-site 建立真实 annotation slot，后续
         * classification 再决定它是 declaration macro 还是 custom annotation。
         */
        private fun CjMacroExpression.asDeclarationMacroAnnotation(): CjAnnotation? {
            val headSyntax = macroExpressionHeadSyntax()
            val name = headSyntax?.rawName ?: macroReferenceText() ?: return null
            val prefix = if (text.orEmpty().trimStart().startsWith("@!")) "@!" else "@"
            val rawAnnotation = prefix + name + macroAttributeText(headSyntax).orEmpty()
            return CjPsiFactory.contextual(this).createAnnotations(rawAnnotation).entries.singleOrNull()
        }

        /** 提取 macro expression 的完整引用文本，保留包限定前缀。 */
        private fun CjMacroExpression.macroReferenceText(): String? {
            return macroExpressionHeadSyntax()?.rawName
                ?: referenceExpression?.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: shortName?.asString()?.takeIf { it.isNotBlank() }
        }

        /**
         * 从原始 macro expression 源码中提取 `@pkg.Name` 形式的限定 macro 名称。
         *
         * PSI parser 当前只把 `@` 后第一段建成 REFERENCE_EXPRESSION；这里在 raw builder
         * 层补齐语法前缀，供 macro construction surface 和 annotation metadata 使用同一全名。
         */
        private fun extractMacroReferencePrefix(rawText: String): String? {
            return scanMacroExpressionHeadSyntax(rawText)?.rawName
        }

        /** 将 macro expression 引用文本提升为 construction surface 使用的 FQN。 */
        private fun CjMacroExpression.macroExpressionQualifiedName(currentPackage: FqName): FqName? {
            val rawName = macroReferenceText() ?: return null
            if (rawName.contains('.')) return FqName(rawName)
            val name = Name.identifier(rawName)
            return if (currentPackage.isRoot) FqName.topLevel(name) else currentPackage.child(name)
        }

        /** 转换完全由 builtin annotation macro 组成的顶层 wrapper 链。 */
        private fun convertBuiltinAnnotationMacroDeclaration(psi: CjMacroExpression): CfirDeclaration? {
            val chain = resolveMacroDeclarationChain(psi) ?: return null
            val (declaration, macroExpressions) = chain
            if (macroExpressions.any { !it.isBuiltinAnnotationMacroExpression() }) return null

            val carrier = convertDeclaration(declaration)
            macroExpressions.forEach { expression ->
                applyBuiltinAnnotationMacroExpression(expression, carrier)
            }
            return carrier
        }

        /** 判定 [CjMacroExpression] 是否只是内建普通 annotation 的语法包装。 */
        private fun CjMacroExpression.isBuiltinAnnotationMacroExpression(): Boolean {
            val shortName = shortName ?: return false
            return !text.orEmpty().trimStart().startsWith("@!") && shortName in builtinAnnotationMacroNames
        }

        /** 把 builtin annotation macro expression 作为普通 annotation 写入 [carrier]。 */
        private fun applyBuiltinAnnotationMacroExpression(
            psi: CjMacroExpression,
            carrier: CfirDeclaration,
        ): Boolean {
            if (!psi.isBuiltinAnnotationMacroExpression()) return false
            val annotation = psi.asDeclarationMacroAnnotation() ?: return false
            val containingSymbol = when (carrier) {
                is CfirValueParameter -> carrier.containingDeclarationSymbol
                else -> carrier.symbol
            }
            val headSyntax = psi.macroExpressionHeadSyntax()
            val macroAttributeText = psi.macroAttributeText(headSyntax)
            val macroAttributeSource = psi.macroAttributeSourceElement(headSyntax)
            val annotationSource = psi.annotationSourceElement(headSyntax)
            val annotationCall = convertAnnotationCall(
                annotation = annotation,
                containingSymbol = containingSymbol,
                sourceOverride = annotationSource,
                typeRefOverride = psi.toAnnotationTypeRefOverride(),
                calleeReferenceSourceOverride = psi.referenceExpression?.toCjPsiSourceElement(),
                argumentListSourceOverride = macroAttributeSource,
                macroAttributeOverride = psi.attr,
                macroAttributeTextOverride = macroAttributeText,
                macroAttributeStartOffsetOverride = macroAttributeSource?.startOffset,
            )
            val annotationIndex = carrier.annotations.size
            carrier.replaceAnnotations(carrier.annotations + annotationCall)
            baseSession.ensureAnnotationMetadataRegistry().record(
                CfirAnnotationSlotSnapshot(
                    owner = carrier,
                    annotationIndex = annotationIndex,
                    originalAnnotation = annotationCall,
                    rawSyntax = annotation.text,
                    forcedCustom = false,
                    isCompileTimeVisible = false,
                    annotationSource = annotationSource,
                    qualifiedName = FqName.topLevel(psi.shortName!!),
                    argumentText = macroAttributeText,
                    tokens = MacroPayloadTokenizer.tokenize(
                        annotation.text,
                        psi.textRange.startOffset,
                    ).toMacroSurfaceTokens(),
                    callSite = MacroCallSite.DECLARATION,
                )
            )
            return true
        }

        /** 转换 class、interface、struct、enum 等 class-like 声明。 */
        private fun convertClass(psi: CjClassLikeDeclaration, classKind: CfirClassKind): CfirDeclaration {
            val name = psi.cfirNameAsSafeName
            if (!canDeclareTopLevelClassLike()) {
                return buildInvalidClassLikeDeclaration(
                    source = psi.toCjPsiSourceElement(),
                    kind = classKind.name.lowercase(),
                    name = name,
                )
            }

            val classId = topLevelClassId(name)
            return when (classKind) {
                CfirClassKind.CLASS -> buildSourceDeclaration(CfirClassSymbol(classId)) { symbol ->
                    buildClass {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (classTypeParameters, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = convertTypeParameters(psi, symbol)
                            val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
                                convertClassMembers(psi).toMutableList().also { declarations ->
                                    addPrimaryConstructorParameterProperties(psi, declarations)
                                    if (classKind != CfirClassKind.INTERFACE && declarations.none { it is CfirConstructor && !it.status.isStatic }) {
                                        declarations.add(0, buildImplicitPrimaryConstructor(psi))
                                    }
                                    if (psi is CjEnum) {
                                        declarations.addAll(
                                            0,
                                            psi.constructor.map { convertEnumConstructor(it) })
                                    }
                                }
                            }
                            typeParameters to declarations
                        }
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(psi)
                        status = convertDeclarationStatus(psi)
                        typeParameters.addAll(classTypeParameters)
                        superTypeRefs.addAll(convertSuperTypeRefs(psi))
                        declarations.addAll(classDeclarations)
                        this.name = name
                    }
                }

                CfirClassKind.INTERFACE -> buildSourceDeclaration(CfirInterfaceSymbol(classId)) { symbol ->
                    buildInterface {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (classTypeParameters, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = convertTypeParameters(psi, symbol)
                            val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
                                convertClassMembers(psi).toMutableList()
                            }
                            typeParameters to declarations
                        }
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(psi)
                        status = convertDeclarationStatus(psi)
                        typeParameters.addAll(classTypeParameters)
                        superTypeRefs.addAll(convertSuperTypeRefs(psi))
                        declarations.addAll(classDeclarations)
                        this.name = name
                    }
                }

                CfirClassKind.STRUCT -> buildSourceDeclaration(CfirStructSymbol(classId)) { symbol ->
                    buildStruct {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (classTypeParameters, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = convertTypeParameters(psi, symbol)
                            val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
                                convertClassMembers(psi).toMutableList().also { declarations ->
                                    addPrimaryConstructorParameterProperties(psi, declarations)
                                    if (declarations.none { it is CfirConstructor && !it.status.isStatic }) {
                                        declarations.add(0, buildImplicitPrimaryConstructor(psi))
                                    }
                                }
                            }
                            typeParameters to declarations
                        }
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(psi)
                        status = convertDeclarationStatus(psi)
                        typeParameters.addAll(classTypeParameters)
                        superTypeRefs.addAll(convertSuperTypeRefs(psi))
                        declarations.addAll(classDeclarations)
                        this.name = name
                    }
                }

                CfirClassKind.ENUM -> buildSourceDeclaration(CfirEnumSymbol(classId)) { symbol ->
                    buildEnum {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (classTypeParameters, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = convertTypeParameters(psi, symbol)
                            val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
                                convertClassMembers(psi).toMutableList().also { declarations ->
                                    addPrimaryConstructorParameterProperties(psi, declarations)
                                    if (declarations.none { it is CfirConstructor && !it.status.isStatic }) {
                                        declarations.add(0, buildImplicitPrimaryConstructor(psi))
                                    }
                                    if (psi is CjEnum) {
                                        declarations.addAll(
                                            0,
                                            psi.constructor.map { convertEnumConstructor(it) })
                                    }
                                }
                            }
                            typeParameters to declarations
                        }
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(psi)
                        status = convertDeclarationStatus(psi)
                        typeParameters.addAll(classTypeParameters)
                        superTypeRefs.addAll(convertSuperTypeRefs(psi))
                        declarations.addAll(classDeclarations)
                        this.name = name
                        this.isRefEnum = false
                        this.isNonExhaustive = (psi as? CjEnum)?.isNonExhaustive == true
                    }
                }
            }
        }

        /**
         * 主构造 `let/const/var` 参数同时声明同名成员。
         *
         * Kotlin raw FIR 在构建 class 时把 `val/var` 参数转换成 property 并加入声明树；
         * 仓颉 CFIR 的低层 API 已以 [CfirValueParameter.correspondingProperty] 作为这条关系的框架入口，
         * 因此这里在 raw 阶段创建对应成员，保证成员作用域和 LL 映射共享同一声明来源。
         */
        private fun addPrimaryConstructorParameterProperties(
            psi: CjClassLikeDeclaration,
            declarations: MutableList<CfirDeclaration>,
        ) {
            val typeStatement = psi as? CjTypeStatement ?: return
            val primaryConstructor = typeStatement.primaryConstructor ?: return
            val primaryConstructorIndex = declarations.indexOfFirst { it is CfirConstructor && it.isPrimary }
            if (primaryConstructorIndex < 0) return

            val cfirPrimaryConstructor = declarations[primaryConstructorIndex] as CfirConstructor
            val generatedProperties = primaryConstructor.valueParameters
                .zip(cfirPrimaryConstructor.valueParameters)
                .mapNotNull { (parameterPsi, valueParameter) ->
                    if (!parameterPsi.hasLetOrVar()) return@mapNotNull null
                    convertPrimaryConstructorParameterProperty(parameterPsi, valueParameter)
                }

            if (generatedProperties.isNotEmpty()) {
                declarations.addAll(primaryConstructorIndex + 1, generatedProperties)
            }
        }

        /** 为一个带 `let/var` 的主构造参数创建对应成员属性，并回填 parameter 关联。 */
        private fun convertPrimaryConstructorParameterProperty(
            psi: CjParameter,
            valueParameter: CfirValueParameter,
        ): CfirProperty {
            val name = psi.cfirNameAsSafeName
            val propertySource = psi.toCjPsiSourceElement().fakeElement(CjFakeSourceElementKind.PropertyFromParameter)
            val propertySymbol = CfirPropertySymbol(callableIdFor(name))
            // 参数 annotation 的语义同时落到合成 property；这里只复制节点和 metadata 槽位，
            // 不再次进入 macro surface 收集，避免为同一源码 annotation 创建第二个 construction surface。
            val parameterAnnotations = valueParameter.annotations.map { annotation ->
                check(annotation is CfirAnnotationCall) {
                    "Primary-constructor parameter annotations must be represented by CfirAnnotationCall."
                }
                annotation
            }
            val propertyAnnotations = parameterAnnotations.map { annotation ->
                buildAnnotationCallCopy(annotation) {
                    containingDeclarationSymbol = propertySymbol
                }
            }
            val propertyStatus = cloneDeclarationStatus(convertDeclarationStatus(psi)).also { status ->
                status.isMut = psi.isMutable
            }
            val defaultAccessorSource = propertySource.fakeElement(CjFakeSourceElementKind.DefaultAccessor)
            val getter = buildPrimaryConstructorParameterPropertyAccessor(
                source = defaultAccessorSource,
                accessorName = Name.special("<get-${name.asString()}>"),
                propertyTypeRef = valueParameter.returnTypeRef.copyWithNewSource(defaultAccessorSource),
                propertySymbol = propertySymbol,
                propertyStatus = propertyStatus,
                isGetter = true,
            )
            val setter = if (psi.isMutable) {
                buildPrimaryConstructorParameterPropertyAccessor(
                    source = defaultAccessorSource,
                    accessorName = Name.special("<set-${name.asString()}>"),
                    propertyTypeRef = valueParameter.returnTypeRef.copyWithNewSource(defaultAccessorSource),
                    propertySymbol = propertySymbol,
                    propertyStatus = propertyStatus,
                    isGetter = false,
                )
            } else {
                null
            }
            val property = buildSourceDeclaration(propertySymbol) { symbol ->
                buildProperty {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = propertySource
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    annotations.addAll(propertyAnnotations)
                    attributes = declarationAttributes(psi)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = propertyStatus
                    returnTypeRef = valueParameter.returnTypeRef.copyWithNewSource(propertySource)
                    this.name = name
                    this.getter = getter
                    this.setter = setter
                }
            }
            if (propertyAnnotations.isNotEmpty()) {
                val metadataRegistry = baseSession.ensureAnnotationMetadataRegistry()
                propertyAnnotations.forEachIndexed { annotationIndex, derivedAnnotation ->
                    metadataRegistry.recordDerivedSlot(
                        sourceAnnotation = parameterAnnotations[annotationIndex],
                        owner = property,
                        annotationIndex = annotationIndex,
                        derivedAnnotation = derivedAnnotation,
                    )
                }
            }
            valueParameter.correspondingProperty = property
            return property
        }

        /** 构造主构造参数属性的合成 getter 或 setter。 */
        private fun buildPrimaryConstructorParameterPropertyAccessor(
            source: CjSourceElement?,
            accessorName: Name,
            propertyTypeRef: CfirTypeRef,
            propertySymbol: CfirPropertySymbol,
            propertyStatus: CfirDeclarationStatus,
            isGetter: Boolean,
        ): CfirPropertyAccessor {
            val accessorSymbol = CfirPropertyAccessorSymbol()
            val valueParameters = if (isGetter) {
                emptyList()
            } else {
                listOf(
                    buildValueParameter {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        this.source = source
                        this.symbol = CfirValueParameterSymbol(callableIdFor(Name.identifier("value")))
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        attributes = CfirDeclarationAttributes.EMPTY
                        isLocal = false
                        isNamed = false
                        status = CfirDeclarationStatusImpl.DEFAULT
                        returnTypeRef = propertyTypeRef
                        name = Name.identifier("value")
                        defaultValue = null
                        containingDeclarationSymbol = accessorSymbol
                    }
                )
            }

            return buildSourceDeclaration(accessorSymbol) { symbol ->
                buildPropertyAccessor {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    this.source = source
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = propertyStatus
                    returnTypeRef = if (isGetter) {
                        propertyTypeRef
                    } else {
                        baseSession.builtinTypes.unitType.toCfirResolvedTypeRef(source)
                    }
                    this.propertySymbol = propertySymbol
                    this.isGetter = isGetter
                    this.valueParameters.addAll(valueParameters)
                    body = null
                }
            }
        }

        /** 转换仓颉 extend 声明，保留扩展目标类型、约束与成员声明。 */
        private fun convertExtend(psi: CjExtend): CfirExtend {
            val extendedTypeRef = convertTypeRef(psi.receiverTypeReceiver)
            val superTypes = psi.superTypeListEntries.map { convertTypeRef(it.typeReference) }

            return buildSourceDeclaration(CfirExtendSymbol()) { symbol ->
                buildExtend {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    val (typeParametersForExtend, members) = withContainerSymbol(symbol) {
                        val typeParameters = convertTypeParameters(psi, symbol)
                        val declarations = psi.body?.declarations?.map { convertDeclaration(it) } ?: emptyList()
                        typeParameters to declarations
                    }
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = declarationAttributes(psi)
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(typeParametersForExtend)
                    this.extendedTypeRef = extendedTypeRef
                    superTypeRefs.addAll(superTypes)
                    declarations.addAll(members)
                }
            }
        }

        /** 转换普通命名函数声明。 */
        fun convertFunction(psi: CjNamedFunction): CfirFunction {
            val name = psi.cfirNameAsSafeName
            val returnTypeRef = convertTypeRef(psi.typeReference)
            val funcSymbol = CfirNamedFunctionSymbol(callableIdFor(name))
            val valueParams = psi.valueParameters.map { convertValueParameter(it, funcSymbol) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = psi.buildCfirBody(functionTarget, funcSymbol)

            return buildSourceDeclaration(funcSymbol) { symbol ->
                buildNamedFunction {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    val typeParametersForFunction = convertFunctionTypeParameters(psi, symbol)
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = declarationAttributes(psi)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(typeParametersForFunction)
                    this.returnTypeRef = returnTypeRef
                    this.name = name
                    valueParameters.addAll(valueParams)
                    this.body = body
                    isMut = psi.isMut
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }

        /** 转换属性声明；无有效名称时显式构造 invalid declaration。 */
        private fun convertProperty(psi: CjProperty): CfirDeclaration {
            if (psi.name == null) {
                return buildSourceDeclaration(CfirInvalidDeclarationSymbol()) { symbol ->
                    buildInvalidDeclaration {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        attributes = CfirDeclarationAttributes.EMPTY
                        reason = "Property declaration has no valid name"
                    }
                }
            }

            val name = psi.cfirNameAsSafeName
            val typeRef = convertTypeRef(psi.typeReference)
            val propertySymbol = CfirPropertySymbol(callableIdFor(name))
            val getter = psi.getter?.let { accessor ->
                convertPropertyAccessor(
                    psi = accessor,
                    accessorName = Name.special("<get-${name.asString()}>"),
                    propertyTypeRef = typeRef,
                    propertySymbol = propertySymbol,
                )
            }
            val setter = psi.setter?.let { accessor ->
                convertPropertyAccessor(
                    psi = accessor,
                    accessorName = Name.special("<set-${name.asString()}>"),
                    propertyTypeRef = typeRef,
                    propertySymbol = propertySymbol,
                )
            }

            return buildSourceDeclaration(propertySymbol) { symbol ->
                buildProperty {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = declarationAttributes(psi)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    this.returnTypeRef = typeRef
                    this.name = name
                    this.getter = getter
                    this.setter = setter

                }
            }
        }

        /**
         * property accessor 在公开 Analysis API 中是独立 symbol，
         * 因此 Raw CFIR 必须为 getter / setter 保留明确的函数声明节点与 source PSI。
         */
        private fun convertPropertyAccessor(
            psi: CjPropertyAccessor,
            accessorName: Name,
            propertyTypeRef: CfirTypeRef,
            propertySymbol: CfirPropertySymbol,
        ): CfirPropertyAccessor {
            val accessorSymbol = CfirPropertyAccessorSymbol()
            val valueParams = psi.valueParameters.map { parameter ->
                convertValueParameter(parameter, accessorSymbol, requiresExplicitType = psi.isGetter)
            }
            if (!psi.isGetter) {
                valueParams.firstOrNull()
                    ?.takeIf { it.returnTypeRef is CfirImplicitTypeRef }
                    ?.replaceReturnTypeRef(propertyTypeRef)
            }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = psi.buildCfirBody(functionTarget, accessorSymbol)
            val source = psi.toCjPsiSourceElement()

            return buildSourceDeclaration(accessorSymbol) { symbol ->
                buildPropertyAccessor {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    this.source = source
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = declarationAttributes(psi)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = psi.returnTypeReference?.let(::convertTypeRef)
                        ?: if (psi.isGetter) propertyTypeRef else baseSession.builtinTypes.unitType.toCfirResolvedTypeRef(source)
                    this.propertySymbol = propertySymbol
                    this.isGetter = psi.isGetter
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }

        /** 转换字段变量声明。 */
        private fun convertFieldVariable(psi: CjFieldVariable): CfirFieldVariable {
            val name = psi.cfirNameAsSafeName
            return buildSourceDeclaration(CfirFieldVariableSymbol(callableIdFor(name))) { symbol ->
                buildFieldVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    this.name = name
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    this.returnTypeRef = convertTypeRef(psi.typeReference)

                    initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                        null
                    } else {
                        psi.initializer?.let { convertExpression(it) }
                    }
                    isVar = psi.isVar
                }
            }
        }

        /** 转换仓颉入口 main 函数声明。 */
        fun convertMainFunction(psi: CjMainFunction): CfirMainFunction {
            val mainFunctionSymbol = CfirMainFunctionSymbol(callableIdFor(Name.identifier("main")))
            val valueParams = psi.valueParameters.map { convertValueParameter(it, mainFunctionSymbol) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = psi.buildCfirBody(functionTarget, mainFunctionSymbol)

            return buildSourceDeclaration(mainFunctionSymbol) { symbol ->
                buildMainFunction {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertMainFunctionStatus(psi)
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }

        /** 转换 macro declaration；该声明只进入 macro symbol index，不进入最终 source provider。 */
        fun convertMacroDeclaration(psi: CjMacroDeclaration): CfirMacroDeclaration {
            val name = psi.cfirNameAsSafeName
            val macroSymbol = CfirMacroDeclarationSymbol(callableIdFor(name))
            val valueParams = psi.valueParameters.map { convertValueParameter(it, macroSymbol) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = psi.buildCfirBody(functionTarget, macroSymbol)

            return buildSourceDeclaration(macroSymbol) { symbol ->
                buildMacroDeclaration {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    this.name = name
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }

        /** 转换 finalizer 声明，返回类型固定为 Unit。 */
        fun convertFinalizer(psi: CjFinalizer): CfirFinalizer {
            val finalizerSymbol = CfirFinalizerSymbol(callableIdFor(SpecialNames.END_INIT))
            val typeParametersForFinalizer = convertFunctionTypeParameters(psi, finalizerSymbol)
            val valueParams = psi.valueParameters.map { convertValueParameter(it, finalizerSymbol) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = psi.buildCfirBody(functionTarget, finalizerSymbol)

            return buildSourceDeclaration(finalizerSymbol) { symbol ->
                buildFinalizer {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = declarationAttributes(psi)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(typeParametersForFinalizer)
                    returnTypeRef = baseSession.builtinTypes.unitType.toCfirResolvedTypeRef(source)
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }

        /** 转换 pattern variable 声明，保留 pattern 与 initializer。 */
        private fun convertPatternVariable(psi: CjPatternVariable): CfirPatternVariable {
            val status = convertDeclarationStatus(psi)
            val returnTypeRef = convertTypeRef(psi.typeReference)
            return buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(Name.special("<pattern-variable>")))) { symbol ->
                buildPatternVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = declarationAttributes(psi)
                    isLocal = context.inLocalContext
                    this.status = status
                    this.returnTypeRef = returnTypeRef
                    pattern = convertCasePattern(
                        pattern = psi.pattern,
                        ownerStatus = status,
                        ownerIsLocal = context.inLocalContext,
                        ownerIsVar = psi.isVar,
                    )
                    initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                        null
                    } else {
                        psi.initializer?.let { convertExpression(it) }
                    }
                    isVar = psi.isVar
                }
            }
        }

        /** 转换主构造或次构造函数声明。 */
        private fun convertConstructor(psi: CjConstructor<*>, isPrimary: Boolean): CfirConstructor {
            val constructorSymbol = CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))
            val typeParametersForConstructor = convertFunctionTypeParameters(psi, constructorSymbol)
            val valueParams = psi.valueParameters.map {
                convertValueParameter(it, constructorSymbol, requiresExplicitType = true)
            }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = psi.buildCfirBody(functionTarget, constructorSymbol)

            return buildSourceDeclaration(constructorSymbol) { symbol ->
                if (isPrimary) {
                    buildPrimaryConstructor {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData

                        attributes = declarationAttributes(psi)
                        isLocal = context.inLocalContext
                        dispatchReceiverType = currentDispatchReceiverType()
                        status = convertDeclarationStatus(psi)
                        typeParameters.addAll(typeParametersForConstructor)
                        returnTypeRef = buildImplicitTypeRef()
                        valueParameters.addAll(valueParams)
                        this.body = body
                    }
                } else {
                    buildConstructor {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData

                        attributes = declarationAttributes(psi)
                        isLocal = context.inLocalContext
                        dispatchReceiverType = currentDispatchReceiverType()
                        status = convertDeclarationStatus(psi)
                        typeParameters.addAll(typeParametersForConstructor)
                        returnTypeRef = buildImplicitTypeRef()
                        valueParameters.addAll(valueParams)
                        this.body = body
                    }
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }

        /** 转换 typealias 声明；非法嵌套时显式构造 invalid declaration。 */
        private fun convertTypeAlias(psi: CjTypeAlias): CfirDeclaration {
            val name = psi.cfirNameAsSafeName
            val expandedType = convertTypeRef(psi.getTypeReference())

            if (!canDeclareTopLevelClassLike()) {
                return buildInvalidClassLikeDeclaration(
                    source = psi.toCjPsiSourceElement(),
                    kind = "typealias",
                    name = name,
                )
            }

            return buildSourceDeclaration(CfirTypeAliasSymbol(topLevelClassId(name))) { symbol ->
                buildTypeAlias {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    val typeParametersForAlias = convertTypeAliasTypeParameters(psi, symbol)
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    scopeProvider = baseScopeProvider

                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(typeParametersForAlias)
                    this.name = name
                    expandedTypeRef = expandedType
                }
            }
        }

        /** 转换枚举构造项及其 payload 类型参数。 */
        private fun convertEnumConstructor(
            psi: CjEnumConstructor,
        ): CfirEnumConstructor {
            val enumConstructorName =
                psi.name?.let { Name.identifier(it) } ?: Name.special("<anonymous-enum-constructor>")
            val valueTypeRefs = psi.typeReferences.map { convertTypeRef(it) }
            val enumConstructor = buildSourceDeclaration(CfirEnumConstructorSymbol(callableIdFor(enumConstructorName))) { symbol ->
                val valueParameters = valueTypeRefs.mapIndexed { index, valueTypeRef ->
                    buildEnumConstructorValueParameter(
                        source = valueTypeRef.source ?: psi.toCjPsiSourceElement(),
                        name = enumConstructorPayloadParameterName(index),
                        returnTypeRef = valueTypeRef,
                        containingDeclarationSymbol = symbol,
                    )
                }
                buildEnumConstructor {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                    returnTypeRef = buildImplicitTypeRef()
                    this.valueParameters.addAll(valueParameters)
                    name = enumConstructorName
                }
            }
            // enum constructor 自身是 annotation metadata 与 macro surface 的唯一 owner；
            // 禁止依赖 class body 的 detached-annotation 回挂逻辑。
            collectMacroAnnotationSurfaces(psi, AnnotationSurfaceTarget.DECLARATION, enumConstructor)
            return enumConstructor
        }

        /** 为没有显式构造函数的 class-like 声明构造隐式主构造。 */
        private fun buildImplicitPrimaryConstructor(psi: CjClassLikeDeclaration): CfirConstructor {
            return buildSourceDeclaration(CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))) { symbol ->
                buildPrimaryConstructor {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = buildImplicitTypeRef()
                    body = null
                }
            }
        }

        // ===== 参数转换 =====

        /** 转换函数、构造函数、宏或 lambda 的值参数。 */
        fun convertValueParameter(
            psi: CjParameter,
            containingSymbol: CfirBasedSymbol<*>,
            requiresExplicitType: Boolean = true,
        ): CfirValueParameter {
            val parameterSource = psi.toCjPsiSourceElement()
            val parameterName = psi.cfirNameAsSafeName
            val parameter = buildSourceDeclaration(CfirValueParameterSymbol(callableIdFor(parameterName))) { symbol ->
                buildValueParameter {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = parameterSource
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    isNamed = psi.isNamed
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = when {
                        psi.typeReference != null -> convertTypeRef(psi.typeReference)
                        requiresExplicitType -> createNoTypeForParameterTypeRef(parameterSource)
                        else -> buildImplicitTypeRef()
                    }
                    name = parameterName
                    defaultValue = psi.defaultValue?.let { convertExpression(it) }
                    containingDeclarationSymbol = containingSymbol
                }
            }
            collectMacroAnnotationSurfaces(psi, AnnotationSurfaceTarget.PARAMETER, parameter)
            return parameter
        }

        /**
         * 声明和参数上的注解在 PSI raw builder 层提取为 macro construction surface，
         * 与 Kotlin raw FIR builder 在 raw builder helper 中集中采集 annotation 的层级保持一致。
         */
        private fun collectMacroAnnotationSurfaces(
            annotated: CjAnnotated,
            target: AnnotationSurfaceTarget,
            carrier: CfirDeclaration,
        ) {
            collectMacroAnnotationSurfaces(
                annotated = annotated,
                entries = annotated.annotationEntries,
                target = target,
                carrier = carrier,
            )
        }

        /** 使用显式 annotation 列表采集 macro annotation surface。 */
        private fun collectMacroAnnotationSurfaces(
            annotated: CjAnnotated,
            entries: List<CjAnnotation>,
            target: AnnotationSurfaceTarget,
            carrier: CfirDeclaration,
        ) {
            if (entries.isEmpty()) return
            val metadataRegistry = baseSession.ensureAnnotationMetadataRegistry()

            val modifiers = (annotated as? CjModifierListOwner)
                ?.modifierList
                ?.let(::collectModifierNames)
                .orEmpty()
            val carriedAnnotations = entries.map { it.text }
            val containerContext = macroContainerContext(annotated, target)

            for (annotation in entries) {
                val macroAttribute = PsiTreeUtil.findChildOfType(annotation, CjMacroAttr::class.java)
                val annotationCall = buildRawAnnotationCall(annotation, carrier)
                val annotationIndex = carrier.annotations.size
                carrier.replaceAnnotations(carrier.annotations + annotationCall)
                val isCompileTimeVisible = annotation.text.trimStart().startsWith("@!")
                val snapshot = CfirAnnotationSlotSnapshot(
                    owner = carrier,
                    annotationIndex = annotationIndex,
                    originalAnnotation = annotationCall,
                    rawSyntax = annotation.text,
                    forcedCustom = isCompileTimeVisible,
                    isCompileTimeVisible = isCompileTimeVisible,
                    annotationSource = annotation.toCjPsiSourceElement(),
                    qualifiedName = annotationQualifiedName(annotation),
                    argumentText = annotation.valueArgumentList?.text ?: macroAttribute?.text,
                    tokens = MacroPayloadTokenizer.tokenize(
                        annotation.text,
                        annotation.textRange.startOffset,
                    ).toMacroSurfaceTokens(),
                    callSite = when (target) {
                        AnnotationSurfaceTarget.DECLARATION -> MacroCallSite.DECLARATION
                        AnnotationSurfaceTarget.PARAMETER -> MacroCallSite.PARAMETER
                    },
                )
                val annotationCarrier = metadataRegistry.record(snapshot)
                collectedMacroSurfaces += buildMacroAnnotationSurface(
                    annotation = annotation,
                    target = target,
                    carrier = carrier,
                    annotationCarrier = annotationCarrier,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    containerContext = containerContext,
                )
            }
        }

        /**
         * Macro-expression wrapper input 中恢复出的 declaration annotation macro
         * 的 input payload 是当前 annotation 后方同一 carrier 声明的剩余源码，
         * 而不是 annotation 自身的 `(...)` 实参。
         *
         * 例如 `@A @B public class C {}` 中，`@A` 的 input 为
         * `@B public class C {}`，`@B` 的 input 为 `public class C {}`；
         * 后续 [MacroCallForestBuilder] 会按同 carrier source order 建立 child-first
         * wrapper 链并刷新父 payload。
         */
        private fun declarationAnnotationMacroInputTokens(
            annotated: CjAnnotated,
            annotationEndOffset: Int,
            shortName: String?,
            target: AnnotationSurfaceTarget = AnnotationSurfaceTarget.DECLARATION,
        ): List<MacroSurfaceToken>? {
            if (target != AnnotationSurfaceTarget.DECLARATION) return null
            if (shortName == IF_AVAILABLE_ANNOTATION_NAME) return null
            val owner = annotated as? PsiElement ?: return null
            return tokenizeSourceSlice(
                fileText = owner.containingFile?.text,
                startOffset = annotationEndOffset,
                endOffset = owner.textRange.endOffset,
            )
        }

        /** 按宿主文件绝对 offset 切片并保持 token offset 与原文件一致。 */
        private fun tokenizeSourceSlice(
            fileText: String?,
            startOffset: Int,
            endOffset: Int,
        ): List<MacroSurfaceToken>? {
            if (fileText == null) return null
            val start = startOffset.coerceIn(0, fileText.length)
            val end = endOffset.coerceIn(start, fileText.length)
            return MacroPayloadTokenizer.tokenize(
                fileText.substring(start, end),
                start,
            ).toMacroSurfaceTokens()
        }

        /** 转换单个 annotation call，供普通 annotation 与 macro custom annotation reparse 共用。 */
        fun convertAnnotationCall(
            annotation: CjAnnotation,
            containingSymbol: CfirBasedSymbol<*>,
            sourceOverride: CjSourceElement? = null,
            typeRefOverride: CfirTypeRef? = null,
            calleeReferenceSourceOverride: CjSourceElement? = null,
            argumentListSourceOverride: CjSourceElement? = null,
            macroAttributeOverride: CjMacroAttr? = null,
            macroAttributeTextOverride: String? = null,
            macroAttributeStartOffsetOverride: Int? = null,
        ): CfirAnnotationCall {
            val arguments = convertAnnotationArguments(
                annotation = annotation,
                macroAttribute = macroAttributeOverride,
                macroAttributeTextOverride = macroAttributeTextOverride,
                macroAttributeStartOffsetOverride = macroAttributeStartOffsetOverride,
            )
            return buildAnnotationCall {
                source = sourceOverride ?: annotation.toCjPsiSourceElement()
                typeRef = typeRefOverride ?: convertTypeRef(annotation.typeReference)
                this.arguments.addAll(arguments)
                argumentList = buildArgumentList {
                    source = argumentListSourceOverride ?: annotation.valueArgumentList?.toCjPsiSourceElement()
                    this.arguments.addAll(arguments)
                }
                calleeReference = buildNamedReference(
                    annotation.shortName ?: Name.identifier("<error>"),
                    // callee 只拥有注解名称；完整 annotation source 会被错误收集器识别为
                    // 注解节点，从而把名称未解析诊断当成重复错误吞掉。无名称的恢复节点
                    // 才保留 annotation 本身作为唯一可用的 source。
                    calleeReferenceSourceOverride
                        ?: annotation.typeReference?.toCjPsiSourceElement()
                        ?: annotation.toCjPsiSourceElement(),
                )
                containingDeclarationSymbol = containingSymbol
            }
        }

        /** 从 declaration macro expression 直接构造 annotation type ref override。 */
        private fun CjMacroExpression.toAnnotationTypeRefOverride(): CfirTypeRef? {
            val rawName = macroReferenceText() ?: return null
            val parts = rawName.split('.').filter(String::isNotBlank)
            if (parts.isEmpty()) return null
            val source = referenceExpression?.toCjPsiSourceElement() ?: return null
            return buildUserTypeRef {
                this.source = source
                qualifier += parts.map { part ->
                    buildQualifierPart {
                        this.source = source
                        name = Name.identifier(part)
                    }
                }
            }
        }

        /** 为 annotation type reference 构造不经普通类型解析的 user type ref。 */
        fun buildAnnotationTypeRef(typeReference: CjTypeReference, sourceOffsetDelta: Int = 0): CfirTypeRef {
            val rawName = typeReference.text.trim().takeIf(String::isNotEmpty) ?: return buildImplicitTypeRef()
            val parts = rawName.split('.').filter(String::isNotBlank)
            if (parts.isEmpty()) return buildImplicitTypeRef()
            val source = typeReference.shiftedBy(sourceOffsetDelta)
            return buildUserTypeRef {
                this.source = source
                qualifier += parts.map { part ->
                    buildQualifierPart {
                        this.source = source
                        name = Name.identifier(part)
                    }
                }
            }
        }

        /** 转换 annotation 参数；CallingConv 特殊语法转换为字符串 literal 参数。 */
        private fun convertAnnotationArguments(
            annotation: CjAnnotation,
            macroAttribute: CjMacroAttr? = null,
            macroAttributeTextOverride: String? = null,
            macroAttributeStartOffsetOverride: Int? = null,
        ): List<CfirExpression> {
            if (macroAttributeTextOverride != null && macroAttributeStartOffsetOverride != null) {
                val macroAttributeArguments = convertMacroAttributeArguments(
                    rawText = macroAttributeTextOverride,
                    startOffset = macroAttributeStartOffsetOverride,
                    factoryContext = annotation,
                )
                if (macroAttributeArguments.isNotEmpty()) return macroAttributeArguments
            }

            val valueArguments = annotation.valueArguments.mapNotNull(::convertCallArgument)
            if (valueArguments.isNotEmpty()) return valueArguments

            val macroAttributeArguments = convertMacroAttributeArguments(
                rawText = macroAttribute?.text,
                startOffset = macroAttribute?.textRange?.startOffset,
                factoryContext = annotation,
            )
            if (macroAttributeArguments.isNotEmpty()) return macroAttributeArguments

            val callingConvention = PsiTreeUtil.findChildOfType(annotation, CjAnnotationCallingConv::class.java)
            if (callingConvention != null) {
                return listOf(buildLiteralExpression {
                    source = callingConvention.toCjPsiSourceElement()
                    kind = CfirLiteralKind.STRING
                    value = callingConvention.getCallingConventionText()
                })
            }

            return emptyList()
        }

        /**
         * 将宏式 annotation attr `[a: b]` 解析成标准 call argument list。
         *
         * `CjMacroAttr` 和 macro-expression wrapper 头部 attr 的内部都是
         * quote-token 风格文本；这里复用仓颉调用参数
         * parser，使 `@!APILevel[since: "21"]` 和普通 annotation call 产出同一种
         * `CfirNamedArgumentExpression`，供 checker/resolve/import owner 统一消费。
         */
        private fun convertMacroAttributeArguments(
            rawText: String?,
            startOffset: Int?,
            factoryContext: PsiElement,
        ): List<CfirExpression> {
            if (rawText == null || startOffset == null) return emptyList()
            val openBracketIndex = rawText.indexOf('[')
            val closeBracketIndex = rawText.lastIndexOf(']')
            if (openBracketIndex < 0 || closeBracketIndex <= openBracketIndex) return emptyList()

            val content = rawText.substring(openBracketIndex + 1, closeBracketIndex)
            if (content.isBlank()) return emptyList()

            val contentStartOffset = startOffset + openBracketIndex + 1
            val padding = (contentStartOffset - PSI_SYNTHETIC_CALL_ARGUMENTS_PREFIX_LENGTH - 1)
                .coerceAtLeast(0)
            val argumentListText = buildString {
                repeat(padding) { append(' ') }
                append('(')
                append(content)
                append(')')
            }

            return runCatching {
                CjPsiFactory.contextual(factoryContext).createCallArguments(argumentListText)
                    .arguments
                    .mapNotNull(::convertCallArgument)
            }.getOrElse { emptyList() }
        }

        /** 基于 carrier 推导 containing symbol，并构造 raw annotation call。 */
        private fun buildRawAnnotationCall(
            annotation: CjAnnotation,
            carrier: CfirDeclaration,
        ): CfirAnnotationCall {
            val containingSymbol = when (carrier) {
                is CfirValueParameter -> carrier.containingDeclarationSymbol
                else -> carrier.symbol
            }
            return convertAnnotationCall(
                annotation = annotation,
                containingSymbol = containingSymbol,
                macroAttributeOverride = PsiTreeUtil.findChildOfType(annotation, CjMacroAttr::class.java),
            )
        }

        /** 构造 annotation-site macro surface，并区分 declaration、parameter 与 builtin non-macro。 */
        private fun buildMacroAnnotationSurface(
            annotation: CjAnnotation,
            target: AnnotationSurfaceTarget,
            carrier: CfirDeclaration,
            annotationCarrier: CfirAnnotationReplaceCarrier,
            modifiers: List<String>,
            carriedAnnotations: List<String>,
            containerContext: MacroSurfaceContainerContext,
            sourceOverride: CjSourceElement? = null,
            rawSyntaxOverride: String? = null,
            inputTokensOverride: List<MacroSurfaceToken>? = null,
            attrTokensOverride: List<MacroSurfaceToken>? = null,
        ): MacroSurface {
            val surfaceId = MacroSurfaceIdGenerator.next()
            val rawSyntax = rawSyntaxOverride ?: annotation.text
            val kind = if (rawSyntax.trimStart().startsWith("@!")) {
                MacroSurface.Kind.FORCED
            } else {
                MacroSurface.Kind.PLAIN
            }
            val qualifiedName = annotationQualifiedName(annotation)
            val valueArgumentList = annotation.valueArgumentList
            val inputTokens = inputTokensOverride ?: MacroPayloadTokenizer.tokenize(
                valueArgumentList?.text,
                valueArgumentList?.textRange?.startOffset ?: 0,
            ).toMacroSurfaceTokens()
            val attrTokens = attrTokensOverride ?: emptyList()
            val source = sourceOverride ?: annotation.toCjPsiSourceElement()
            val sourceRange = MacroSurfaceSourceRange(
                source = source,
                startOffset = source.startOffset,
                endOffset = source.endOffset,
            )
            val scopeContext = MacroSurfaceScopeContext(
                packageFqName = context.packageFqName,
                enclosingClassFqName = null,
                enclosingFunctionName = enclosingFunctionName(),
            )
            val replaceHandle = CfirReplaceHandle(
                handleId = surfaceId,
                carrier = carrier,
                annotationCarrier = annotationCarrier,
            )

            if (annotation.shortName?.asString() == IF_AVAILABLE_ANNOTATION_NAME) {
                return IfAvailableSurface(
                    surfaceId = surfaceId,
                    qualifiedName = qualifiedName,
                    kind = kind,
                    hasParenthesis = valueArgumentList != null,
                    attrTokens = attrTokens,
                    inputTokens = inputTokens,
                    sourceRange = sourceRange,
                    scopeContext = scopeContext,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    capturedRawSyntax = rawSyntax,
                    containerContext = containerContext,
                    replaceHandle = replaceHandle,
                    branchTokens = inputTokens,
                )
            }

            return when (target) {
                AnnotationSurfaceTarget.DECLARATION -> MacroSurfaceDecl(
                    surfaceId = surfaceId,
                    qualifiedName = qualifiedName,
                    kind = kind,
                    hasParenthesis = valueArgumentList != null,
                    attrTokens = attrTokens,
                    inputTokens = inputTokens,
                    sourceRange = sourceRange,
                    scopeContext = scopeContext,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    capturedRawSyntax = rawSyntax,
                    containerContext = containerContext,
                    replaceHandle = replaceHandle,
                )
                AnnotationSurfaceTarget.PARAMETER -> MacroSurfaceParam(
                    surfaceId = surfaceId,
                    qualifiedName = qualifiedName,
                    kind = kind,
                    hasParenthesis = valueArgumentList != null,
                    attrTokens = attrTokens,
                    inputTokens = inputTokens,
                    sourceRange = sourceRange,
                    scopeContext = scopeContext,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    capturedRawSyntax = rawSyntax,
                    containerContext = containerContext,
                    replaceHandle = replaceHandle,
                )
            }
        }

        /** 提取 annotation 的 macro surface 限定名。 */
        private fun annotationQualifiedName(annotation: CjAnnotation): FqName? {
            val rawName = annotation.typeReference?.text?.trim()?.takeIf(String::isNotEmpty)
            if (rawName != null && rawName.contains('.')) return FqName(rawName)
            return annotation.shortName?.let(::macroSurfaceQualifiedName)
        }

        /** 把短名按当前包上下文提升为 surface 使用的 FQN。 */
        private fun macroSurfaceQualifiedName(name: Name): FqName {
            return if (context.packageFqName.isRoot) {
                FqName.topLevel(name)
            } else {
                context.packageFqName.child(name)
            }
        }

        /** 从 modifier list 中按稳定顺序采集 modifier 名称。 */
        private fun collectModifierNames(modifierList: CjModifierList): List<String> {
            return CjTokens.MODIFIER_KEYWORDS_ARRAY
                .filter { modifierList.hasModifier(it) }
                .map { it.value }
        }

        /** 构造 macro surface 所需的语法容器上下文。 */
        private fun macroContainerContext(
            annotated: CjAnnotated,
            target: AnnotationSurfaceTarget,
        ): MacroSurfaceContainerContext {
            return MacroSurfaceContainerContext(
                outerDeclarationKind = macroOuterDeclarationKind(target),
                isInsidePrimaryConstructor = annotated.hasParentOfType<CjPrimaryConstructor>(),
                isInsideEnumBody = containerSymbolIfAny is CfirEnumSymbol || annotated.hasParentOfType<CjEnum>(),
                isInsideBlock = context.inLocalContext,
            )
        }

        /** 根据当前 raw builder 上下文推导 macro surface 的外层声明种类。 */
        private fun macroOuterDeclarationKind(target: AnnotationSurfaceTarget): MacroSurfaceContainerContext.OuterDeclarationKind {
            if (context.inLocalContext) {
                return MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY
            }

            return when (containerSymbolIfAny) {
                is CfirInterfaceSymbol -> MacroSurfaceContainerContext.OuterDeclarationKind.INTERFACE_BODY
                is CfirStructSymbol -> MacroSurfaceContainerContext.OuterDeclarationKind.STRUCT_BODY
                is CfirEnumSymbol -> MacroSurfaceContainerContext.OuterDeclarationKind.ENUM_BODY
                is CfirClassLikeSymbol<*> -> MacroSurfaceContainerContext.OuterDeclarationKind.CLASS_BODY
                else -> when (target) {
                    AnnotationSurfaceTarget.DECLARATION -> MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL
                    AnnotationSurfaceTarget.PARAMETER -> MacroSurfaceContainerContext.OuterDeclarationKind.NONE
                }
            }
        }

        /** 返回当前容器函数名，用于 macro surface scope context。 */
        private fun enclosingFunctionName(): Name? {
            return when (val symbol = containerSymbolIfAny) {
                is CfirNamedFunctionSymbol -> symbol.callableId.callableName
                is CfirMainFunctionSymbol -> symbol.callableId.callableName
                is CfirMacroDeclarationSymbol -> symbol.callableId.callableName
                is CfirPropertyAccessorSymbol -> symbol.callableId.callableName
                else -> null
            }
        }

        /** 判断当前 PSI 元素是否拥有指定类型的父节点。 */
        private inline fun <reified T : PsiElement> PsiElement.hasParentOfType(): Boolean {
            var current = parent
            while (current != null) {
                if (current is T) return true
                current = current.parent
            }
            return false
        }

        /** 转换单个类型参数，并合并 where/type constraint 收集到的额外上界。 */
        private fun convertTypeParameter(
            psi: CjTypeParameter,
            containingDeclarationSymbol: CfirBasedSymbol<*>,
            additionalBounds: List<CfirTypeRef> = emptyList(),
        ): CfirTypeParameter {
            val name = Name.identifier(psi.name ?: "<error>")

            return buildSourceDeclaration(CfirTypeParameterSymbol()) { symbol ->
                buildTypeParameter {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    this.containingDeclarationSymbol = containingDeclarationSymbol
                    this.name = name
                    this.bounds.addAll(additionalBounds)
                    addDefaultBoundIfNecessary()
                }
            }
        }

        // ===== 琛ㄨ揪寮忚浆鎹?=====

        /** 按 PSI 表达式具体类型分派到对应 raw CFIR 表达式构建函数。 */
        fun convertExpression(psi: CjExpression): CfirExpression = when (psi) {
            is CjBlockExpression -> convertBlock(psi)
            is CjConstantExpression -> convertLiteral(psi)
            is CjStringTemplateExpression -> convertStringTemplate(psi)
            is CjBinaryExpressionWithTypeRHS -> convertTypeOperator(psi)
            is CjBinaryExpression -> convertBinary(psi)
            is CjPrefixExpression -> convertPrefix(psi)
            is CjPostfixExpression -> convertPostfix(psi)
            is CjUnsafeExpression -> convertUnsafe(psi)
            is CjOptionalExpression -> convertOptionalExpression(psi)
            is CjOptionalChainExpression -> convertOptionalChainExpression(psi)
            is CjDotQualifiedExpression -> convertDotQualified(psi)
            is CjSafeQualifiedExpression -> convertDotQualified(psi)
            is CjSimpleNameExpression -> convertNameReference(psi)
            is CjIfExpression -> convertIf(psi)
            is CjMatchExpression -> convertMatch(psi)
            is CjLetExpression -> convertLetPatternExpression(psi)
            is CjForExpression -> convertFor(psi)
            is CjWhileExpression -> convertWhile(psi)
            is CjDoWhileExpression -> convertDoWhile(psi)
            is CjReturnExpression -> convertReturn(psi)
            is CjBreakExpression -> buildBreakExpression(psi.toCjPsiSourceElement())

            is CjContinueExpression -> buildContinueExpression(psi.toCjPsiSourceElement())

            is CjThrowExpression -> convertThrow(psi)
            is CjPerformExpression -> convertPerform(psi)
            is CjResumeExpression -> convertResume(psi)
            is CjTryExpression -> convertTry(psi)
            is CjLambdaExpression -> convertLambda(psi)
            is CjParenthesizedExpression -> psi.expression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Empty parenthesized expression")

            is CjArrayAccessExpression -> convertSubscript(psi)
            is CjCollectionLiteralExpression -> convertArrayLiteral(psi)
            is CjTupleExpression -> convertTupleLiteral(psi)
            is CjIsExpression -> convertTypeCheck(psi)
            is CjSynchronizedExpression -> convertSynchronized(psi)
            is CjQuoteExpression -> convertQuote(psi)
            is CjMacroExpression -> convertMacroExpression(psi)
            is CjCallExpression -> convertCall(psi)
            is CjThisExpression -> buildThisReceiverExpression {
                source = psi.toCjPsiSourceElement()
                calleeReference = buildThisReference {
                    source = psi.toCjPsiSourceElement()
                    isImplicit = false
                }
            }

            is CjSuperExpression -> buildSuperReceiverExpression {
                val sourceElement = psi.toCjPsiSourceElement()
                source = sourceElement
                calleeReference = buildSuperReference {
                    source = sourceElement.fakeElement(CjFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
                    superTypeRef = buildImplicitTypeRef()
                }
            }

            else -> buildErrorExpression(psi.toSourceElement(), "Unsupported expression: ${psi.javaClass.simpleName}")
        }

        /**
         * quest 后缀在 PSI 中先包装为 `CjOptionalExpression`。
         *
         * Raw CFIR 只承接这层语法包装，不在这里展开 optional chain 语义。
         */
        private fun convertOptionalExpression(psi: CjOptionalExpression): CfirExpression {
            val baseExpression = psi.children.filterIsInstance<CjExpression>().firstOrNull()
                ?: return buildErrorExpression(psi.toSourceElement(), "Malformed optional expression: missing base expression")
            return buildOptionalExpression {
                source = psi.toCjPsiSourceElement()
                expression = convertExpression(baseExpression)
            }
        }

        /**
         * 整条 optional chain 在 PSI 末尾统一封装为 `CjOptionalChainExpression`。
         *
         * 链内部的成员访问、调用、索引仍按普通表达式树构造，外层再由该节点统一承接。
         */
        private fun convertOptionalChainExpression(psi: CjOptionalChainExpression): CfirExpression {
            val chainExpression = psi.children.filterIsInstance<CjExpression>().firstOrNull()
                ?: return buildErrorExpression(psi.toSourceElement(), "Malformed optional chain expression: missing chain body")
            return buildOptionalChainExpression {
                source = psi.toCjPsiSourceElement()
                expression = convertExpression(chainExpression)
            }
        }

        /** 把声明或表达式 PSI 包装成可放入 block 的 CFIR statement。 */
        private inline fun CjElement.toCfirStatement(errorReasonLazy: () -> String): CfirStatement {
            val cfir = when (this) {
                is CjDeclaration -> convertDeclaration(this)
                is CjExpression -> convertExpression(this)
                else -> buildErrorExpressionNode {
                    source = toCjPsiSourceElement()
                    diagnostic = ConeSimpleDiagnostic(errorReasonLazy())
                }
            }

            return when (cfir) {
                is CfirStatement -> cfir
                else -> buildErrorExpressionNode {
                    source = toCjPsiSourceElement()
                    diagnostic = ConeSimpleDiagnostic(errorReasonLazy())
                    nonExpressionElement = cfir
                }
            }
        }

        /** 转换 block 表达式，并展开无 annotation 的普通嵌套 block。 */
        fun convertBlock(psi: CjBlockExpression): CfirBlock {
            val statements = withLocalContext {
                buildList {
                    for (stmt in psi.statements) {
                        val cfirStatement = stmt.toCfirStatement { "Statement expected: ${stmt.text}" }
                        val isForLoopBlock =
                            cfirStatement is CfirBlock && cfirStatement.source?.kind == CjFakeSourceElementKind.DesugaredForLoop
                        if (cfirStatement !is CfirBlock || isForLoopBlock || cfirStatement.annotations.isNotEmpty()) {
                            add(cfirStatement)
                        } else {
                            addAll(cfirStatement.statements)
                        }
                    }
                }
            }
            return buildBlock {
                source = psi.toCjPsiSourceElement()
                this.statements.addAll(statements)
            }
        }


        // ---- Literal ----

        /** 转换基础常量 literal 表达式。 */
        private fun convertLiteral(psi: CjConstantExpression): CfirLiteralExpression {
            val text = psi.text
            val elementType = psi.node.elementType
            val (kind, value) = when (elementType) {
                INTEGER_CONSTANT -> CfirLiteralKind.INT to text
                FLOAT_CONSTANT -> CfirLiteralKind.FLOAT to text
                RUNE_CONSTANT -> CfirLiteralKind.RUNE to text
                CHARACTER_BYTE_CONSTANT -> CfirLiteralKind.BYTE to byteLiteralCodePointOrNull(text)
                BOOLEAN_CONSTANT -> CfirLiteralKind.BOOLEAN to (text == "true")
                UNIT_CONSTANT -> CfirLiteralKind.UNIT to null
                else -> CfirLiteralKind.STRING to text
            }
            return buildLiteralExpression {
                source = psi.toCjPsiSourceElement()
                this.kind = kind
                this.value = value
            }
        }

        /** 转换字符串模板；有插值时构造 string interpolation 表达式。 */
        private fun convertStringTemplate(psi: CjStringTemplateExpression): CfirExpression {
            if (!psi.hasInterpolation()) {
                return buildLiteralExpression {
                    source = psi.toCjPsiSourceElement()
                    kind = CfirLiteralKind.STRING
                    value = psi.stringContent
                }
            }
            val parts = psi.entries.mapNotNull { entry ->
                when (entry) {
                    is CjStringTemplateEntryWithExpression ->
                        entry.expression?.let { convertExpression(it) }

                    else -> buildLiteralExpression {
                        source = entry.toCjPsiSourceElement()
                        kind = CfirLiteralKind.STRING
                        value = entry.text
                    }
                }
            }
            return buildStringInterpolation {
                source = psi.toCjPsiSourceElement()
                this.parts.addAll(parts)
            }
        }

        // ---- Binary & Unary ----

        /** 转换二元表达式、赋值表达式和可重载二元运算。 */
        private fun convertBinary(psi: CjBinaryExpression): CfirExpression {
            if (psi is CjRangeExpression) return convertRange(psi)

            val left = psi.left?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing left operand")
            val right = psi.right?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing right operand")
            val opToken = psi.operationToken

            if (opToken.isAssignmentToken()) {
                if (opToken == CjTokens.EQ) {
                    if (left is CfirTupleLiteral) {
                        return desugarDestructuringAssignment(psi, left, right)
                    }
                    return buildAssignment {
                        source = psi.toCjPsiSourceElement()
                        lValue = left
                        rValue = right
                    }
                }
                val operation = opToken.toCompoundAssignName() ?: Name.identifier("<error>")
                return buildAugmentedAssignment {
                    source = psi.toCjPsiSourceElement()
                    this.operation = operation
                    operationSource = psi.operationReference.toCjPsiSourceElement()
                    leftArgument = left
                    rightArgument = right
                }
            }

            // 閫昏緫/绌哄悎/绠￠亾
            opToken.toBinaryOpKind()?.let { kind ->
                return buildBinaryOp {
                    source = psi.toCjPsiSourceElement()
                    this.kind = kind
                    this.left = left
                    this.right = right
                }
            }

            // 姣旇緝
            opToken.toComparisonOp()?.let { op ->
                return buildComparisonExpression {
                    source = psi.toCjPsiSourceElement()
                    operation = op
                    this.left = left
                    this.right = right
                }
            }

            // 可重载运算符转换为函数调用
            val operatorName = opToken.toBinaryName() ?: Name.identifier("<op:$opToken>")
            return buildFunctionCall {
                source = psi.toCjPsiSourceElement()
                calleeReference = buildNamedReference(operatorName, psi.toCjPsiSourceElement())
                argumentList = buildArgumentList {
                    arguments.add(right)
                }
                explicitReceiver = left
                origin = CfirFunctionCallOrigin.Operator
            }
        }

        /**
         * 把元组解构赋值脱糖为「临时绑定 + 逐元素赋值」的 block。
         *
         * `(a, b) = rhs` 展开为：
         * ```
         * let <destructuring-0> = rhs
         * a = <destructuring-0>[0]
         * b = <destructuring-0>[1]
         * ```
         *
         * 引入临时绑定是为了让右值只求值一次；每条合成赋值的 source 锚定在对应左值元素上，
         * 使左值可写性、初始化、const 求值等诊断落在元素本身而不是整条赋值上。
         *
         * 脱糖在建树期完成，因此所有下游消费方（赋值合法性检查、初始化流分析、const 求值、
         * DFA）看到的都是普通赋值，无需各自重新实现元组左值语义。
         */
        private fun desugarDestructuringAssignment(
            psi: CjBinaryExpression,
            targets: CfirTupleLiteral,
            rValue: CfirExpression,
        ): CfirExpression {
            val fakeSource = psi.toCjPsiSourceElement()
                .fakeElement(CjFakeSourceElementKind.DesugaredDestructuringAssignment)
            val statements = mutableListOf<CfirStatement>()
            expandDestructuringTargets(targets, rValue, fakeSource, statements, nextTemporaryId = 0)
            return buildBlock {
                source = fakeSource
                this.statements.addAll(statements)
            }
        }

        /**
         * 逐层展开解构目标，把生成的临时绑定与赋值语句追加到 [out]。
         *
         * 嵌套元组各自分配临时绑定；名字必须逐层唯一，否则内层绑定会遮蔽外层，
         * 导致外层后续元素的下标读取解析到错误的绑定上。返回下一个可用的临时绑定编号。
         */
        private fun expandDestructuringTargets(
            targets: CfirTupleLiteral,
            rValue: CfirExpression,
            fakeSource: CjSourceElement,
            out: MutableList<CfirStatement>,
            nextTemporaryId: Int,
        ): Int {
            val temporaryName = Name.special("<destructuring-$nextTemporaryId>")
            var temporaryId = nextTemporaryId + 1
            out.add(buildDestructuringTemporary(temporaryName, rValue, fakeSource))

            targets.elements.forEachIndexed { index, target ->
                val elementRead = buildSubscriptExpression {
                    source = fakeSource
                    receiver = buildNamedAccessExpression {
                        source = fakeSource
                        calleeReference = buildNamedReference(temporaryName, fakeSource)
                    }
                    indices.add(
                        buildLiteralExpression {
                            source = fakeSource
                            kind = CfirLiteralKind.INT
                            value = index.toString()
                        }
                    )
                }

                if (target is CfirTupleLiteral) {
                    temporaryId = expandDestructuringTargets(target, elementRead, fakeSource, out, temporaryId)
                } else {
                    out.add(
                        buildAssignment {
                            // 锚点落在左值元素上，而不是整条 `(a, b) = rhs`
                            source = target.source ?: fakeSource
                            this.lValue = target
                            this.rValue = elementRead
                        }
                    )
                }
            }
            return temporaryId
        }

        /**
         * 构造承载解构右值的不可变合成局部绑定。
         *
         * 仓颉的局部 `let` 是模式绑定，名字由 binding pattern 承载，
         * 因此这里构造 [CfirPatternVariable] 而非 property，
         * 使其能像普通局部变量一样在 body resolve 阶段进入作用域并被后续下标读取引用。
         */
        private fun buildDestructuringTemporary(
            name: Name,
            rValue: CfirExpression,
            fakeSource: CjSourceElement,
        ): CfirPatternVariable {
            val temporaryStatus = cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT)
            return buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(name))) { symbol ->
                buildPatternVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = fakeSource
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    status = temporaryStatus
                    returnTypeRef = buildImplicitTypeRef()
                    pattern = buildBindingPattern {
                        source = fakeSource
                        this.name = name
                        bindingVariable = createPatternBindingVariable(
                            source = fakeSource,
                            name = name,
                            status = temporaryStatus,
                            isLocal = true,
                            isVar = false,
                            returnTypeRef = buildImplicitTypeRef(),
                        )
                    }
                    initializer = rValue
                    isVar = false
                }
            }
        }


        private fun convertRange(psi: CjRangeExpression): CfirRangeExpression {
            val start = psi.left?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing range start")
            val end = psi.right?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing range end")
            val step = psi.step?.let { convertExpression(it) }
            return buildRangeExpression {
                source = psi.toCjPsiSourceElement()
                this.start = start
                this.end = end
                this.step = step
                isInclusive = psi.operationToken == CjTokens.RANGEEQ
            }
        }

        /** 转换前缀一元表达式，递增/递减使用专用节点，其余转为 operator call。 */
        private fun convertPrefix(psi: CjPrefixExpression): CfirExpression {
            val base = psi.baseExpression?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing prefix operand")
            val opName = psi.operationToken.toPrefixUnaryName() ?: Name.identifier("<prefix>")
            if (psi.operationToken == CjTokens.PLUSPLUS || psi.operationToken == CjTokens.MINUSMINUS) {
                return buildIncrementDecrementExpression {
                    source = psi.toCjPsiSourceElement()
                    isPrefix = true
                    operationName = opName
                    expression = base
                    operationSource = psi.operationReference.toCjPsiSourceElement()
                }
            }
            return buildFunctionCall {
                source = psi.toCjPsiSourceElement()
                calleeReference = buildNamedReference(opName, psi.toCjPsiSourceElement())
                argumentList = buildArgumentList()
                explicitReceiver = base
                origin = CfirFunctionCallOrigin.Operator
            }
        }

        /** 转换后缀一元表达式，递增/递减使用专用节点，其余转为 operator call。 */
        private fun convertPostfix(psi: CjPostfixExpression): CfirExpression {
            val base = psi.baseExpression?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing postfix operand")
            val opName = psi.operationToken.toPostfixUnaryName() ?: Name.identifier("<postfix>")
            if (psi.operationToken == CjTokens.PLUSPLUS || psi.operationToken == CjTokens.MINUSMINUS) {
                return buildIncrementDecrementExpression {
                    source = psi.toCjPsiSourceElement()
                    isPrefix = false
                    operationName = opName
                    expression = base
                    operationSource = psi.operationReference.toCjPsiSourceElement()
                }
            }
            return buildFunctionCall {
                source = psi.toCjPsiSourceElement()
                calleeReference = buildNamedReference(opName, psi.toCjPsiSourceElement())
                argumentList = buildArgumentList()
                explicitReceiver = base
                origin = CfirFunctionCallOrigin.Operator
            }
        }

        // ---- Call & Access ----

        /** 转换调用表达式、spawn 表达式和仅带类型实参的 named access。 */
        private fun convertCall(psi: CjCallExpression): CfirExpression {
            if (psi is CjSpawnExpression) {
                val lambda = psi.lambdaExpression
                val body = lambda?.bodyExpression?.let { convertBlock(it) } ?: buildBlock {
                    source = (lambda ?: psi).toCjPsiSourceElement()
                }
                val ctxArg = psi.valueArgumentList?.arguments?.firstOrNull()?.getArgumentExpression()
                    ?.let { convertExpression(it) }
                return buildSpawnExpression {
                    source = psi.toCjPsiSourceElement()
                    this.body = body
                    this.threadContextArgument = ctxArg
                }
            }

            val callee = psi.calleeExpression

            /**
             * 对齐 Kotlin FIR raw builder：
             *
             * `f(1) { ... }` 语义上仍然是一次对 `f` 的调用，
             * 尾随 lambda 只是最后一个实参，不应该在 CFIR 中退化成
             * “把 `f(1)` 当作 callee 名称的另一层调用”。
             *
             * 仓颉当前 PSI 把这类语法表示成：
             *   outerCall(callee = innerCall, lambdaArguments = [...])
             * 因此这里需要显式扁平化，把内层普通实参与外层尾随 lambda
             * 合并成同一个调用的 argument list。
             */
            if (callee is CjCallExpression && psi.valueArgumentList == null && psi.lambdaArguments.isNotEmpty()) {
                val flattenedCallee = callee.calleeExpression
                val flattenedArguments = callee.valueArguments.mapNotNull(::convertCallArgument) +
                        psi.lambdaArguments.mapNotNull(::convertCallArgument)
                val flattenedTypeArguments = extractCallTypeArguments(callee, flattenedCallee)
                val (receiver, reference) = resolveCalleeReference(flattenedCallee)
                val varraySizeLiteral = extractVArraySizeLiteral(callee, flattenedCallee)

                return buildFunctionCall {
                    source = callee.toCjPsiSourceElement()
                    calleeReference = reference
                    argumentList = buildArgumentList {
                        source = callee.valueArgumentList?.toCjPsiSourceElement()
                        arguments.addAll(flattenedArguments)
                    }
                    explicitReceiver = receiver
                    typeArguments.addAll(flattenedTypeArguments)
                    origin = callOriginFor(flattenedCallee)
                    hasTrailingLambda = true
                    this.varraySizeLiteral = varraySizeLiteral
                }
            }

            val typeArgs = extractCallTypeArguments(psi, callee)
            val varraySizeLiteral = extractVArraySizeLiteral(psi, callee)

            tryBuildTypeConversion(psi, callee, typeArgs)?.let { return it }

            // valueArguments 已经包含了 lambdaArguments，不需要再单独处理
            val allArgs = psi.valueArguments.mapNotNull(::convertCallArgument)
            val (receiver, reference) = resolveCalleeReference(callee)

            if (psi.valueArgumentList == null && psi.lambdaArguments.isEmpty() && typeArgs.isNotEmpty()) {
                return buildNamedAccessExpression {
                    source = psi.toCjPsiSourceElement()
                    calleeReference = reference
                    explicitReceiver = receiver
                    typeArguments.addAll(typeArgs)
                }
            }

            return buildFunctionCall {
                source = psi.callSourceWithoutTrailingLambda()
                calleeReference = reference
                argumentList = buildArgumentList {
                    source = psi.valueArgumentList?.toCjPsiSourceElement()
                    arguments.addAll(allArgs)
                }
                explicitReceiver = receiver
                typeArguments.addAll(typeArgs)
                origin = callOriginFor(callee)
                hasTrailingLambda = psi.lambdaArguments.isNotEmpty()
                this.varraySizeLiteral = varraySizeLiteral
            }
        }

        /**
         * 仓颉尾随 lambda 在 PSI 中属于外层 CALL_EXPRESSION，但 CFIR 的调用主体
         * source 应对应“callee + 普通实参/类型实参”本身；lambda 已作为独立实参保留。
         */
        private fun CjCallExpression.callSourceWithoutTrailingLambda(): CjSourceElement {
            if (lambdaArguments.isEmpty()) return toCjPsiSourceElement()

            val startOffset = textRange.startOffset
            val fileText = containingFile?.text ?: return toCjPsiSourceElement()
            var endOffset = lambdaArguments.firstOrNull()
                ?.asElement()
                ?.textRange
                ?.startOffset
                ?: return toCjPsiSourceElement()

            while (endOffset > startOffset && fileText.getOrNull(endOffset - 1)?.isWhitespace() == true) {
                endOffset--
            }
            if (endOffset <= startOffset) return toCjPsiSourceElement()

            return toCjPsiSourceElement().fakeElement(
                CjFakeSourceElementKind.SyntheticCall,
                CjSourceElementOffsetStrategy.Custom.Initialized(startOffset, endOffset),
            )
        }

        /** 将调用实参转换为结构化的 named/inout 包装表达式。 */
        private fun convertCallArgument(argument: ValueArgument): CfirExpression? {
            val convertedExpression = when (argument) {
                is CjLambdaArgument -> argument.getLambdaExpression()?.let { lambda ->
                    convertLambda(lambda).also { anonymousFunctionExpression ->
                        anonymousFunctionExpression.replaceIsTrailingLambda(true)
                    }
                }
                else -> argument.getArgumentExpression()?.let { convertExpression(it) }
            } ?: return null

            val isInout = (argument as? CjValueArgument)?.isInout == true
            val wrapped = if (isInout) {
                buildInoutArgumentExpression {
                    source = argument.getArgumentExpression()?.toCjPsiSourceElement()
                        ?: argument.asElement().toCjPsiSourceElement()
                    expression = convertedExpression
                }
            } else convertedExpression

            val argumentName = argument.getArgumentName() ?: return wrapped
            return buildNamedArgumentExpression {
                source = argument.asElement().toCjPsiSourceElement()
                this.argumentName = argumentName.asName
                nameSource = argumentName.referenceExpression?.toCjPsiSourceElement()
                expression = wrapped
            }
        }

        /** 尝试把 `Int(x)` 等基础类型调用直接构造成类型转换表达式。 */
        private fun tryBuildTypeConversion(
            psi: CjCallExpression,
            callee: CjExpression?,
            typeArgs: List<CfirTypeRef>,
        ): CfirExpression? {
            val targetKind = (callee as? CjNameBasicReferenceExpression)
                ?.primitiveTypeConversionKindOrNull()
                ?: return null
            if (psi.valueArgumentList == null || psi.lambdaArguments.isNotEmpty() || typeArgs.isNotEmpty()) {
                return buildErrorExpression(psi.toSourceElement(), "Malformed primitive type conversion")
            }

            val valueArgument = psi.valueArguments.singleOrNull()
                ?: return buildErrorExpression(psi.toSourceElement(), "Malformed primitive type conversion")
            if (valueArgument.isNamed()) {
                return buildErrorExpression(psi.toSourceElement(), "Malformed primitive type conversion")
            }
            val argumentExpression = valueArgument.getArgumentExpression()
                ?: return buildErrorExpression(
                    valueArgument.asElement().toSourceElement(),
                    "Missing primitive type conversion argument"
                )

            return buildTypeConversion {
                source = psi.toCjPsiSourceElement()
                argument = convertExpression(argumentExpression)
                targetTypeRef = buildBasicTypeRef {
                    source = callee.toCjPsiSourceElement()
                    name = Name.identifier(targetKind.typeName)
                }
            }
        }

        /** 识别可由调用语法触发的基础类型转换目标。 */
        private fun CjNameBasicReferenceExpression.primitiveTypeConversionKindOrNull(): PrimitiveTypeKind? =
            PrimitiveTypeKind.entries.firstOrNull {
                it.isExposedBuiltinClassifier && it.typeName == referencedName
            }

        /** 把 callee 表达式拆成显式 receiver 与待解析的具名引用。 */
        private fun resolveCalleeReference(callee: CjExpression?): Pair<CfirExpression?, CfirNamedReference> {
            return when (callee) {
                is CjSimpleNameExpression -> null to buildNamedReference(
                    callee.cfirReferencedNameAsName,
                    callee.toCjPsiSourceElement()
                )

                is CjThisExpression -> null to buildNamedReference(
                    Name.identifier("this"),
                    callee.toCjPsiSourceElement()
                )

                is CjSuperExpression -> null to buildNamedReference(
                    Name.identifier("super"),
                    callee.toCjPsiSourceElement()
                )

                is CjDotQualifiedExpression -> {
                    val selector = callee.selectorExpression
                    if (selector is CjCallExpression) {
                        return convertExpression(callee) to buildNamedReference(
                            OperatorNameConventions.INVOKE,
                            callee.toCjPsiSourceElement(),
                        )
                    }

                    val recv = convertExpression(callee.receiverExpression)
                    val ref = when (selector) {
                        is CjSimpleNameExpression ->
                            buildNamedReference(selector.cfirReferencedNameAsName, selector.toCjPsiSourceElement())

                        else -> buildNamedReference(Name.identifier("<error>"), selector?.toCjPsiSourceElement())
                    }
                    recv to ref
                }

                null -> null to buildNamedReference(Name.identifier("<error>"))

                else -> convertExpression(callee) to buildNamedReference(
                    OperatorNameConventions.INVOKE,
                    callee.toCjPsiSourceElement(),
                )
            }
        }

        /**
         * 仓颉把构造器 delegation 写在 constructor body 里，但它仍然不是普通函数调用。
         * Raw CFIR 阶段先把这层语义入口显式编码到 origin 中，后续 resolve/checker 才能分流。
         */
        private fun callOriginFor(callee: CjExpression?): CfirFunctionCallOrigin = when (callee) {
            is CjThisExpression -> CfirFunctionCallOrigin.ConstructorDelegationThis
            is CjSuperExpression -> CfirFunctionCallOrigin.ConstructorDelegationSuper
            is CjSimpleNameExpression ->
                if (callee.referencedName in setOf("createMock", "createSpy")) {
                    CfirFunctionCallOrigin.MockIntrinsic
                } else {
                    CfirFunctionCallOrigin.Regular
                }
            is CjQualifiedExpression -> {
                val selector = callee.selectorExpression as? CjSimpleNameExpression
                if (selector?.referencedName in setOf("createMock", "createSpy")) {
                    CfirFunctionCallOrigin.MockIntrinsic
                } else {
                    CfirFunctionCallOrigin.Regular
                }
            }
            else -> CfirFunctionCallOrigin.Regular
        }

        /** 从调用表达式或 callee selector 中提取显式类型实参。 */
        private fun extractCallTypeArguments(
            callExpression: CjCallExpression,
            calleeExpression: CjExpression?,
        ): List<CfirTypeRef> {
            val directTypeArguments = callExpression.typeArguments.map { convertTypeRef(it.typeReference) }
            if (directTypeArguments.isNotEmpty()) return directTypeArguments

            val calleeTypeArguments = when (calleeExpression) {
                is CjSimpleNameExpression ->
                    calleeExpression.getTypeArguments().map { convertTypeRef(it.typeReference) }

                is CjQualifiedExpression -> {
                    val selectorName = calleeExpression.selectorExpression as? CjSimpleNameExpression
                    selectorName?.getTypeArguments()?.map { convertTypeRef(it.typeReference) }.orEmpty()
                }

                else -> emptyList()
            }
            return calleeTypeArguments
        }

        /** 提取 `VArray` 构造调用携带的字面量大小。 */
        private fun extractVArraySizeLiteral(
            callExpression: CjCallExpression,
            calleeExpression: CjExpression?,
        ): String? {
            if ((calleeExpression as? CjSimpleNameExpression)?.referencedName != "VArray") return null
            return callExpression.typeArgumentList?.varrayLiteral?.text
                ?: calleeExpression.getTypeArgumentList()?.varrayLiteral?.text
        }

        /** 转换点访问或安全访问表达式。 */
        private fun convertDotQualified(psi: CjQualifiedExpression): CfirExpression {
            val receiver = convertExpression(psi.receiverExpression)
            val selector = psi.selectorExpression
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing selector")

            if (selector is CjCallExpression) {
                val callArguments = selector.valueArguments.mapNotNull(::convertCallArgument)
                val typeArgs = extractCallTypeArguments(selector, selector.calleeExpression)
                val callee = selector.calleeExpression
                val ref = if (callee is CjSimpleNameExpression) {
                    buildNamedReference(callee.cfirReferencedNameAsName, callee.toCjPsiSourceElement())
                } else {
                    buildNamedReference(Name.identifier(callee?.text ?: "<error>"), callee?.toCjPsiSourceElement())
                }

                return buildFunctionCall {
                    source = psi.toCjPsiSourceElement()
                    calleeReference = ref
                    argumentList = buildArgumentList {
                        source = selector.valueArgumentList?.toCjPsiSourceElement()
                        arguments.addAll(callArguments)
                    }
                    explicitReceiver = receiver
                    typeArguments.addAll(typeArgs)
                    origin = CfirFunctionCallOrigin.Regular
                    hasTrailingLambda = selector.lambdaArguments.isNotEmpty()
                }
            }

            if (selector is CjSimpleNameExpression) {
                val typeArgs = selector.getTypeArguments().map { convertTypeRef(it.typeReference) }
                if (typeArgs.isNotEmpty()) {
                    return buildNamedAccessExpression {
                        source = psi.toCjPsiSourceElement()
                        calleeReference =
                            buildNamedReference(selector.cfirReferencedNameAsName, selector.toCjPsiSourceElement())
                        explicitReceiver = receiver
                        typeArguments.addAll(typeArgs)
                    }
                }
                return buildNamedAccessExpression {
                    source = psi.toCjPsiSourceElement()
                    calleeReference =
                        buildNamedReference(selector.cfirReferencedNameAsName, selector.toCjPsiSourceElement())
                    explicitReceiver = receiver
                }
            }

            return buildErrorExpression(psi.toSourceElement(), "Unsupported selector: ${selector.javaClass.simpleName}")
        }

        /** 转换裸名称引用和带类型实参的名称访问。 */
        private fun convertNameReference(psi: CjSimpleNameExpression): CfirExpression {
            val referencedName = psi.cfirReferencedNameAsName
            val typeArguments = psi.getTypeArguments()
            if (referencedName.asString() == "this" && typeArguments.isEmpty()) {
                return buildThisReceiverExpression {
                    source = psi.toCjPsiSourceElement()
                    calleeReference = buildThisReference {
                        source = psi.toCjPsiSourceElement()
                        isImplicit = false
                    }
                }
            }

            return buildNamedAccessExpression {
                source = psi.toCjPsiSourceElement()
                calleeReference = buildNamedReference(referencedName, psi.toCjPsiSourceElement())
                this.typeArguments.addAll(typeArguments.map { convertTypeRef(it.typeReference) })
            }
        }

        // ---- Control Flow ----

        /** 转换 if 表达式，支持 let-pattern condition。 */
        private fun convertIf(psi: CjIfExpression): CfirIfExpression {
            val condition = psi.letExpression?.let { convertLetPatternExpression(it) }
                ?: psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing if condition")
            val thenBranch = psi.then?.let { toBlock(it) } ?: buildBlock {
                source = psi.toCjPsiSourceElement()
            }
            val elseBranch = psi.`else`?.let { convertExpression(it) }

            return buildIfExpression {
                source = psi.toCjPsiSourceElement()
                this.condition = condition
                this.thenBranch = thenBranch
                this.elseBranch = elseBranch
            }
        }

        /** 转换 if/while 条件中的 let-pattern 表达式。 */
        private fun convertLetPatternExpression(psi: CjLetExpression): CfirLetPatternExpression {
            val status = cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT)
            val convertedPatterns = psi.patterns.map {
                convertCasePattern(
                    pattern = it,
                    ownerStatus = status,
                    ownerIsLocal = true,
                    ownerIsVar = false,
                )
            }
            val pattern = when (convertedPatterns.size) {
                0 -> buildWildcardPattern { source = psi.toCjPsiSourceElement() }
                1 -> convertedPatterns.single()
                else -> buildOrPattern {
                    source = psi.toCjPsiSourceElement()
                    alternatives.addAll(convertedPatterns)
                }
            }
            val initializer = psi.expression?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing let-pattern initializer")

            return buildLetPatternExpression {
                source = psi.toCjPsiSourceElement()
                this.initializer = initializer
                this.pattern = pattern
            }
        }

        /** 转换 match 表达式，区分有主语模式匹配与无主语条件分支。 */
        private fun convertMatch(psi: CjMatchExpression): CfirMatchExpression {
            val subject = psi.subjectExpression?.let { convertExpression(it) }
            val hasSubject = subject != null

            val branches = psi.entries.map { entry ->
                val (pattern, guard) = when {
                    // ── case _ ────────────────────────────────────────────────────────
                    entry.isElse -> {
                        val conditions = entry.conditions.toList()
                        val p = if (hasSubject && conditions.size == 1) {
                            convertCasePattern(conditions.first())
                        } else {
                            buildWildcardPattern {
                                source = conditions.firstOrNull()?.toCjPsiSourceElement()
                                    ?: entry.toCjPsiSourceElement()
                            }
                        }
                        val g = entry.patternGuard
                            ?.children?.filterIsInstance<CjExpression>()?.firstOrNull()
                            ?.let { convertExpression(it) }
                        p to g
                    }

                    // ── 有主语：模式匹配，conditions 是 | 分隔的多个 pattern ──────────
                    hasSubject -> {
                        val conditions = entry.conditions.toList()
                        val p = when {
                            conditions.isEmpty() -> buildWildcardPattern {
                                source = entry.toCjPsiSourceElement()
                            }

                            conditions.size == 1 -> convertCasePattern(conditions.first())
                            else -> buildOrPattern {
                                source = entry.toCjPsiSourceElement()
                                alternatives.addAll(conditions.map { convertCasePattern(it) })
                            }
                        }
                        val g = entry.patternGuard
                            ?.children?.filterIsInstance<CjExpression>()?.firstOrNull()
                            ?.let { convertExpression(it) }
                        p to g
                    }

                    // ── 无主语：条件表达式包装成 ExpressionPattern ────────────────────
                    else -> {
                        val conditions = entry.conditions.toList()
                        val p = if (conditions.isEmpty()) {
                            buildWildcardPattern { source = entry.toCjPsiSourceElement() }
                        } else {
                            val expr = (conditions.first() as? CjMatchConditionWithExpression)?.expression
                            if (expr != null) {
                                buildExpressionPattern {
                                    source = entry.toCjPsiSourceElement()
                                    expression = convertExpression(expr)
                                }
                            } else {
                                buildWildcardPattern { source = entry.toCjPsiSourceElement() }
                            }
                        }
                        val g = entry.patternGuard
                            ?.children?.filterIsInstance<CjExpression>()?.firstOrNull()
                            ?.let { convertExpression(it) }
                        p to g
                    }
                }

                val body = entry.expression?.let { convertBlock(it) }
                    ?: entry.body?.let { convertBlock(it) }
                    ?: buildBlock { source = entry.toCjPsiSourceElement() }

                buildMatchBranch {
                    source = entry.toCjPsiSourceElement()
                    this.pattern = pattern
                    this.guard = guard
                    this.body = body
                }
            }

            return buildMatchExpression {
                source = psi.toCjPsiSourceElement()
                this.subject = subject
                this.branches.addAll(branches)
            }
        }
        // ---- Loops ----

        /** 转换 for-in 循环，构造模式变量、iterable 与 loop target。 */
        private fun convertFor(psi: CjForExpression): CfirForInExpression {
            val loopStatus = cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT)
            val loopPattern = psi.pattern?.let { pattern ->
                convertCasePattern(
                    pattern = pattern,
                    ownerStatus = loopStatus,
                    ownerIsLocal = true,
                    ownerIsVar = false,
                )
            } ?: buildWildcardPattern {
                source = psi.toCjPsiSourceElement()
            }
            val variable = buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(Name.special("<pattern-variable>")))) { symbol ->
                buildPatternVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = (psi.pattern ?: psi).toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    status = loopStatus
                    returnTypeRef = buildImplicitTypeRef()
                    pattern = loopPattern
                    isVar = false
                }
            }
            val iterable = psi.loopRange?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing for-in iterable")
            val patternGuard = psi.patternGuard
                ?.children?.filterIsInstance<CjExpression>()?.firstOrNull()
                ?.let { convertExpression(it) }
            val loop = buildForInExpression {
                source = psi.toCjPsiSourceElement()
                this.condition = buildLiteralExpression {
                    source = psi.toCjPsiSourceElement()
                    kind = CfirLiteralKind.BOOLEAN
                    value = true
                }
                this.isDoWhile = false
                this.variable = variable
                this.iterable = iterable
                this.patternGuard = patternGuard
                this.body = psi.body?.let { toBlock(it) } ?: buildBlock {
                    source = psi.toCjPsiSourceElement()
                }
            }
            return loop
        }

        /** 转换 while 循环，支持 let-pattern condition。 */
        private fun convertWhile(psi: CjWhileExpression): CfirLoopExpression {
            val condition = psi.letExpression?.let { convertLetPatternExpression(it) }
                ?: psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing while condition")
            return buildLoopExpression {
                source = psi.toCjPsiSourceElement()
                this.condition = condition
                this.body = psi.body?.let { toBlock(it) } ?: buildBlock {
                    source = psi.toCjPsiSourceElement()
                }
                isDoWhile = false
            }
        }

        /** 转换 do-while 循环。 */
        private fun convertDoWhile(psi: CjDoWhileExpression): CfirLoopExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing do-while condition")
            return buildLoopExpression {
                source = psi.toCjPsiSourceElement()
                this.condition = condition
                this.body = psi.body?.let { toBlock(it) } ?: buildBlock {
                    source = psi.toCjPsiSourceElement()
                }
                isDoWhile = true
            }
        }

        // ---- Jump & Exception ----

        /** 转换 return 表达式，并绑定当前函数 target。 */
        private fun convertReturn(psi: CjReturnExpression): CfirReturnExpression {
            return buildReturnExpressionWithCurrentFunctionTarget(
                source = psi.toCjPsiSourceElement(),
                result = psi.returnedExpression?.let { convertExpression(it) },
            )
        }

        /** 转换 throw 表达式。 */
        private fun convertThrow(psi: CjThrowExpression): CfirThrowExpression {
            val exception = psi.thrownExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing thrown expression")
            return buildThrowExpression {
                source = psi.toCjPsiSourceElement()
                this.exception = exception
            }
        }

        /** 转换 effect perform 表达式。 */
        private fun convertPerform(psi: CjPerformExpression): CfirPerformExpression {
            val effectExpression = psi.expression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing performed expression")
            return buildPerformExpression {
                source = psi.toCjPsiSourceElement()
                expression = effectExpression
            }
        }

        /** 转换 effect resume 表达式。 */
        private fun convertResume(psi: CjResumeExpression): CfirResumeExpression {
            return buildResumeExpression {
                source = psi.toCjPsiSourceElement()
                withExpression = psi.withExpression?.let(::convertExpression)
                throwingExpression = psi.throwingExpression?.let(::convertExpression)
            }
        }

        /** 转换 try handle 分支中的 command type pattern。 */
        private fun convertCommandTypePattern(psi: CjCommandTypePattern): CfirCommandTypePattern {
            return buildCommandTypePattern {
                source = psi.toCjPsiSourceElement()
                bindingName = psi.bindingName?.let(Name::identifier)
                isWildcard = psi.isWildcard
                typeRefs.addAll(psi.typeReferences.map(::convertTypeRef))
            }
        }

        /** 转换 try-with-resource 资源声明为局部 field variable。 */
        private fun convertTryResource(psi: CjTryResource): CfirFieldVariable {
            val parameter = psi.parameter
            val resourceName = parameter?.cfirNameAsSafeName ?: Name.special("<error>")
            val resourceStatus = cloneDeclarationStatus(CfirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL))
            return buildSourceDeclaration(CfirFieldVariableSymbol(callableIdFor(resourceName))) { symbol ->
                buildFieldVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    dispatchReceiverType = null
                    status = resourceStatus
                    returnTypeRef = convertTypeRef(parameter?.typeReference)
                    name = resourceName
                    initializer = psi.expression?.let(::convertExpression)
                    isVar = false
                }
            }
        }

        /** 转换 try 表达式，包括 resource、handle、catch 与 finally 分支。 */
        private fun convertTry(psi: CjTryExpression): CfirTryExpression {
            val resources = psi.tryResourceList?.resources?.map(::convertTryResource).orEmpty()
            val tryBlock = convertBlock(psi.tryBlock)
            val handlers = psi.handleClauses.map { clause ->
                val body = clause.handleBody?.let(::convertBlock)
                    ?: buildBlock { source = clause.toCjPsiSourceElement() }
                val commandPattern = clause.commandPattern?.let(::convertCommandTypePattern)
                    ?: buildCommandTypePattern {
                        source = clause.toCjPsiSourceElement()
                        bindingName = null
                        isWildcard = false
                    }
                buildHandleClause {
                    source = clause.toCjPsiSourceElement()
                    this.commandPattern = commandPattern
                    this.body = body
                }
            }
            val catches = psi.catchClauses.map { clause ->
                val body = clause.catchBody?.let {
                    if (it is CjBlockExpression) convertBlock(it) else buildBlock {
                        source = it.toCjPsiSourceElement()
                    }
                }
                    ?: buildBlock { source = clause.toCjPsiSourceElement() }
                buildCatch {
                    source = clause.toCjPsiSourceElement()
                    pattern = convertCatchPattern(clause)
                    this.body = body
                }
            }
            val finallyBlock = psi.finallyBlock?.let { section ->
                val expr = section.finalExpression
                if (expr is CjBlockExpression) convertBlock(expr) else null
            }

            return buildTryExpression {
                source = psi.toCjPsiSourceElement()
                this.resources.addAll(resources)
                this.tryBlock = tryBlock
                this.handlers.addAll(handlers)
                this.catches.addAll(catches)
                this.finallyBlock = finallyBlock
            }
        }

        // ---- Lambda ----

        /** 转换 lambda 表达式及其匿名函数声明。 */
        private fun convertLambda(psi: CjLambdaExpression): CfirAnonymousFunctionExpression {
            val anonymousFunctionSymbol = CfirAnonymousFunctionSymbol()
            val valueParams = psi.valueParameters.map {
                convertValueParameter(it, anonymousFunctionSymbol, requiresExplicitType = false).also { parameter ->
                    if (it.typeReference == null) {
                        parameter.isLambdaParameterTypeOmitted = true
                    }
                }
            }
            val hasExplicitParameterList = psi.valueParameters.isNotEmpty()
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = true)
            val body = withContainerSymbol(anonymousFunctionSymbol) {
                withFunctionTarget(functionTarget) {
                    psi.bodyExpression?.let { convertBlock(it) }
                }
            }

            val anonymousFunction = buildSourceDeclaration(anonymousFunctionSymbol) { symbol ->
                buildAnonymousFunction {
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = buildImplicitTypeRef()
                    valueParameters.addAll(valueParams)
                    this.body = body
                    this.hasExplicitParameterList = hasExplicitParameterList
                    isLambda = true
                    typeRef = buildImplicitTypeRef()
                }
            }.also { bindFunctionTarget(functionTarget, it) }
            return buildAnonymousFunctionExpression {
                source = psi.toCjPsiSourceElement()
                this.anonymousFunction = anonymousFunction
                isTrailingLambda = false
            }
        }

        // ---- Misc ----

        /** 转换数组或下标访问表达式。 */
        private fun convertSubscript(psi: CjArrayAccessExpression): CfirSubscriptExpression {
            val receiver = psi.arrayExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing subscript receiver")
            return buildSubscriptExpression {
                source = psi.toCjPsiSourceElement()
                this.receiver = receiver
                indices.addAll(psi.indexExpressions.map { convertExpression(it) })
            }
        }

        /** 转换数组字面量。 */
        private fun convertArrayLiteral(psi: CjCollectionLiteralExpression): CfirArrayLiteral {
            return buildArrayLiteral {
                source = psi.toCjPsiSourceElement()
                elements.addAll(psi.innerExpressions.map { convertExpression(it) })
            }
        }

        /** 转换 tuple 字面量。 */
        private fun convertTupleLiteral(psi: CjTupleExpression): CfirTupleLiteral {
            return buildTupleLiteral {
                source = psi.toCjPsiSourceElement()
                elements.addAll(psi.expressions.map { convertExpression(it) })
            }
        }

        /** 转换 `is` 类型检查表达式。 */
        private fun convertTypeCheck(psi: CjIsExpression): CfirTypeOperator {
            val argument = psi.leftHandSide.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing is-check operand")
            return buildTypeOperator {
                source = psi.toCjPsiSourceElement()
                operation = CfirTypeOperationKind.IS
                this.argument = argument
                typeRef = convertTypeRef(psi.typeReference)
            }
        }

        /** 转换 `as` 等带类型 RHS 的类型操作表达式。 */
        private fun convertTypeOperator(psi: CjBinaryExpressionWithTypeRHS): CfirTypeOperator {
            val operation = when (psi.operationReference.referencedNameElementType) {
                CjTokens.AS_KEYWORD -> CfirTypeOperationKind.AS
                else -> error("Unexpected binary type operator: ${psi.operationReference.referencedNameElementType}")
            }
            return buildTypeOperator {
                source = psi.toCjPsiSourceElement()
                this.operation = operation
                argument = convertExpression(psi.left)
                typeRef = convertTypeRef(psi.right)
            }
        }

        /** 转换 catch clause 的绑定与类型 pattern。 */
        private fun convertCatchPattern(clause: CjCatchClause): CfirCatchPattern {
            val parameter = clause.catchParameter
            val bindingName = parameter?.name?.let(Name::identifier)
            val typeRefs = parameter?.typeReferences?.map(::convertTypeRef).orEmpty()
            val bindingStatus = cloneDeclarationStatus(CfirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL))

            return buildCatchPattern {
                source = (parameter ?: clause).toCjPsiSourceElement()
                this.bindingName = bindingName
                isWildcard = bindingName == null
                this.typeRefs.addAll(typeRefs)
                bindingVariable = bindingName?.let { name ->
                    createPatternBindingVariable(
                        source = parameter?.toCjPsiSourceElement(),
                        name = name,
                        status = bindingStatus,
                        isLocal = true,
                        isVar = false,
                        returnTypeRef = buildImplicitTypeRef(),
                    )
                }
            }
        }

        /** 转换 synchronized 表达式。 */
        private fun convertSynchronized(psi: CjSynchronizedExpression): CfirExpression {
            val mutex = psi.expression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing synchronized mutex expression")
            val body = psi.blockExpression?.let { convertBlock(it) } ?: buildBlock {
                source = psi.toCjPsiSourceElement()
            }
            return buildSynchronizedExpression {
                source = psi.toCjPsiSourceElement()
                monitor = mutex
                this.body = body
            }
        }

        /** 转换 unsafe 表达式。 */
        private fun convertUnsafe(psi: CjUnsafeExpression): CfirExpression {
            val block = psi.block
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing unsafe block")
            val bodyExpression = when (block) {
                is CjBlockExpression -> convertBlock(block)
                else -> convertExpression(block)
            }
            return buildUnsafeExpression {
                source = psi.toCjPsiSourceElement()
                body = bodyExpression
            }
        }

        /** 转换 quote 表达式并保留插值表达式列表。 */
        private fun convertQuote(psi: CjQuoteExpression): CfirExpression {
            return buildQuoteExpression {
                source = psi.toCjPsiSourceElement()
                rawText = psi.text
                interpolations.addAll(
                    psi.quoteInterpolates.mapNotNull { interpolate ->
                        interpolate.expression?.let { convertExpression(it) }
                    }
                )
            }
        }

        /** 转换表达式位置的 macro call，并收集 [MacroSurfaceExpr]。 */
        private fun convertMacroExpression(psi: CjMacroExpression): CfirExpression {
            val surfaceId = MacroSurfaceIdGenerator.next()
            val text = psi.text.orEmpty()
            val isForced = text.startsWith("@!")
            val currentPackage = this@PsiRawCfirBuilder.context.packageFqName
            val source = psi.toCjPsiSourceElement()
            val carrier = buildErrorExpressionNode {
                this.source = source
                diagnostic = ConeSimpleDiagnostic(
                    "Macro expression `$text` is a construction-only surface and must be replaced before final provider registration.",
                )
            }
            val qualifiedName = psi.shortName?.let {
                if (currentPackage.isRoot) FqName.topLevel(it) else currentPackage.child(it)
            }
            collectedMacroSurfaces += MacroSurfaceExpr(
                surfaceId = surfaceId,
                qualifiedName = qualifiedName,
                kind = if (isForced) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
                hasParenthesis = psi.input != null || text.hasMacroInputParentheses(),
                attrTokens = MacroPayloadTokenizer.tokenize(
                    psi.attr?.text,
                    psi.attr?.textRange?.startOffset ?: 0,
                ).toMacroSurfaceTokens(),
                inputTokens = MacroPayloadTokenizer.tokenize(
                    psi.input?.text,
                    psi.input?.textRange?.startOffset ?: 0,
                ).toMacroSurfaceTokens(),
                sourceRange = MacroSurfaceSourceRange(
                    source = source,
                    startOffset = psi.textRange.startOffset,
                    endOffset = psi.textRange.endOffset,
                ),
                scopeContext = MacroSurfaceScopeContext(
                    packageFqName = currentPackage,
                    enclosingClassFqName = null,
                    enclosingFunctionName = null,
                ),
                modifiers = emptyList(),
                carriedAnnotations = emptyList(),
                capturedRawSyntax = text,
                containerContext = MacroSurfaceContainerContext(
                    outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.NONE,
                    isInsidePrimaryConstructor = false,
                    isInsideEnumBody = false,
                    isInsideBlock = false,
                ),
                replaceHandle = CfirReplaceHandle(handleId = surfaceId, carrier = carrier),
            )
            return carrier
        }

        /** 在 raw 文本中判断 macro input 是否显式包含括号。 */
        private fun String.hasMacroInputParentheses(): Boolean {
            val open = indexOf('(')
            return open >= 0 && indexOf(')', startIndex = open + 1) >= 0
        }

        /**
         * 把 PSI pattern 构造成 CFIR pattern，同时为所有具名绑定同步创建独立的 binding variable。
         *
         * 外层 `CfirPatternVariable` 只保留容器职责，真正进入作用域的是这里生成的
         * `CfirPatternBindingVariable`。
         */
        private fun convertCasePattern(
            pattern: CjCasePatternElement?,
            ownerStatus: CfirDeclarationStatus = cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT),
            ownerIsLocal: Boolean = true,
            ownerIsVar: Boolean = false,
        ): CfirPattern {
            return when (pattern) {
                is CjBindingPattern -> buildBindingPattern {
                    source = pattern.toCjPsiSourceElement()
                    name = pattern.cfirNameAsSafeName
                    bindingVariable = createPatternBindingVariable(
                        source = pattern.toCjPsiSourceElement(),
                        name = pattern.cfirNameAsSafeName,
                        status = ownerStatus,
                        isLocal = ownerIsLocal,
                        isVar = ownerIsVar,
                        returnTypeRef = buildImplicitTypeRef(),
                    )
                }

                is CjTypePattern -> buildTypePattern {
                    source = pattern.toCjPsiSourceElement()
                    typeRef = convertTypeRef(pattern.typeReference)
                    bindingName = pattern.cfirNameAsName
                    bindingVariable = pattern.cfirNameAsName?.let { name ->
                        createPatternBindingVariable(
                            source = (pattern.nameIdentifier ?: pattern.reference ?: pattern).toCjPsiSourceElement(),
                            name = name,
                            status = ownerStatus,
                            isLocal = ownerIsLocal,
                            isVar = ownerIsVar,
                            returnTypeRef = typeRef,
                        )
                    }
                }

                is CjVarOrEnumPattern -> buildVarOrEnumPattern {
                    source = pattern.toCjPsiSourceElement()
                    name = pattern.cfirNameAsSafeName
                    bindingVariable = createPatternBindingVariable(
                        source = pattern.toCjPsiSourceElement(),
                        name = pattern.cfirNameAsSafeName,
                        status = ownerStatus,
                        isLocal = ownerIsLocal,
                        isVar = ownerIsVar,
                        returnTypeRef = buildImplicitTypeRef(),
                    )
                }

                is CjTuplePattern -> buildTuplePattern {
                    source = pattern.toCjPsiSourceElement()
                    elements.addAll(
                        pattern.patterns.map {
                            convertCasePattern(it, ownerStatus, ownerIsLocal, ownerIsVar)
                        }
                    )
                }

                is CjEnumPattern -> buildEnumPattern {
                    source = pattern.toCjPsiSourceElement()
                    val refText = pattern.expression?.text ?: "<enum-pattern>"
                    constructorReference = buildNamedReference(
                        if (refText.startsWith("<")) Name.special(refText) else Name.identifier(refText),
                        pattern.expression?.toCjPsiSourceElement() ?: pattern.toCjPsiSourceElement(),
                    )
                    arguments.addAll(
                        pattern.patterns.map {
                            convertCasePattern(it, ownerStatus, ownerIsLocal, ownerIsVar)
                        }
                    )
                }

                is CjConstantPattern -> buildConstPattern {
                    source = pattern.toCjPsiSourceElement()
                    expression = pattern.expression?.let { convertExpression(it) }
                        ?: buildErrorExpression(pattern.toSourceElement(), "Missing constant pattern expression")
                }

                is CjWildcardPattern -> buildWildcardPattern { source = pattern.toCjPsiSourceElement() }
                else -> buildWildcardPattern { source = pattern?.toCjPsiSourceElement() }
            }
        }

        /** 为 pattern 中的具名绑定创建独立 binding variable 声明。 */
        private fun createPatternBindingVariable(
            source: CjSourceElement?,
            name: Name,
            status: CfirDeclarationStatus,
            isLocal: Boolean,
            isVar: Boolean,
            returnTypeRef: CfirTypeRef,
        ): CfirPatternBindingVariable {
            val bindingStatus = cloneDeclarationStatus(status)
            return buildSourceDeclaration(CfirPatternBindingSymbol(callableIdFor(name))) { symbol ->
                org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    this.source = source?.fakeElement(CjFakeSourceElementKind.PatternBindingVariable)
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    this.isLocal = isLocal
                    dispatchReceiverType = null
                    this.status = bindingStatus
                    initializer = null
                    this.isVar = isVar
                    this.returnTypeRef = returnTypeRef
                    this.name = name
                }
            }
        }

        /** 深拷贝声明状态，避免复用可变 status 对象造成后续阶段串改。 */
        private fun cloneDeclarationStatus(status: CfirDeclarationStatus): CfirDeclarationStatusImpl {
            return CfirDeclarationStatusImpl(
                visibility = status.visibility,
                modality = status.modality,
            ).also { copied ->
                copied.isVisibilityExplicit = status.isVisibilityExplicit
                copied.isModalityExplicit = status.isModalityExplicit
                copied.isAbstractExplicit = status.isAbstractExplicit
                copied.isOverride = status.isOverride
                copied.isOperator = status.isOperator
                copied.isStatic = status.isStatic
                copied.isConst = status.isConst
                copied.isMut = status.isMut
                copied.isUnsafe = status.isUnsafe
                copied.isForeign = status.isForeign
                copied.isCommon = status.isCommon
                copied.isSpecific = status.isSpecific
                copied.isRedef = status.isRedef
                copied.isDefault = status.isDefault
                copied.isAbstract = status.isAbstract
                copied.isOpen = status.isOpen
                copied.isSealed = status.isSealed
            }
        }

        // ===== 辅助方法 =====

        /** 构建带 body 声明的函数体 block，并绑定函数 target 与容器符号。 */
        private fun CjDeclarationWithBody.buildCfirBody(
            functionTarget: CfirFunctionTarget,
            containingDeclarationSymbol: CfirBasedSymbol<*>,
        ): CfirBlock? {
            if (!hasBody()) return null

            return buildOrLazyBlock {
                withContainerSymbol(containingDeclarationSymbol) {
                    withFunctionTarget(functionTarget) {
                        bodyExpression?.let(::toBlock)
                    }
                }
            }
        }

        /** 将任意表达式包装成 block；block 表达式直接复用 [convertBlock]。 */
        private fun toBlock(psi: CjExpression): CfirBlock {
            if (psi is CjBlockExpression) return convertBlock(psi)
            return buildBlock {
                source = psi.toCjPsiSourceElement()
                statements.add(convertExpression(psi))
            }
        }

        /** 转换类型引用；缺失时生成 implicit type ref。 */
        private fun convertTypeRef(psi: CjTypeReference?): CfirTypeRef {
            return psi.toCfirOrImplicitTypeRef { it.toSourceElement() }
        }

        /** 收集 where/type constraint 语法中每个类型参数的额外上界。 */
        private fun collectTypeConstraintBounds(owner: CjTypeParameterListOwner): Map<Name, List<CfirTypeRef>> {
            if (owner.typeConstraints.isEmpty()) return emptyMap()

            val boundsByParameter = linkedMapOf<Name, MutableList<CfirTypeRef>>()
            for (constraint in owner.typeConstraints) {
                val parameterName = constraint.subjectTypeParameterName?.cfirReferencedNameAsName ?: continue
                val boundRefs = constraint.boundTypeReferences
                if (boundRefs.isEmpty()) continue

                boundsByParameter.getOrPut(parameterName) { mutableListOf() }
                    .addAll(boundRefs.map(::convertTypeRef))
            }

            return boundsByParameter
        }

        /** 收集 type constraint 诊断定位数据，供 checker 报告重复/非法约束。 */
        private fun collectTypeConstraintDiagnosticData(owner: CjTypeParameterListOwner): CfirTypeConstraintDiagnosticData? {
            val typeConstraints = owner.typeConstraints.mapNotNull { constraint ->
                val parameterName = constraint.subjectTypeParameterName?.cfirReferencedNameAsName ?: return@mapNotNull null
                val parameterSource =
                    constraint.subjectTypeParameterName?.toCjPsiSourceElement() ?: return@mapNotNull null
                CfirTypeConstraintReference(
                    parameterName = parameterName,
                    source = parameterSource,
                    boundTypeRefs = constraint.boundTypeReferences.map(::convertTypeRef),
                    constraintSource = constraint.toCjPsiSourceElement(),
                )
            }

            if (typeConstraints.isEmpty()) return null

            return CfirTypeConstraintDiagnosticData(
                typeConstraints = typeConstraints,
            )
        }

        /** 收集函数体中额外参数列表的诊断定位数据。 */
        private fun collectFunctionBodyDiagnosticData(owner: CjElement): CfirFunctionBodyDiagnosticData? {
            val parameterLists = owner.children
                .filterIsInstance<CjParameterList>()
                .map { parameterList ->
                    CfirValueParameterListReference(
                        source = parameterList.toCjPsiSourceElement(),
                    )
                }

            if (parameterLists.size <= 1) return null

            return CfirFunctionBodyDiagnosticData(
                valueParameterLists = parameterLists,
            )
        }

        /** 汇总声明附加属性，包括 type constraint 与函数体诊断定位数据。 */
        private fun declarationAttributes(owner: CjElement?): CfirDeclarationAttributes {
            var hasAttributes = false
            val attributes = CfirDeclarationAttributes()

            (owner as? CjTypeParameterListOwner)?.let { typeParameterOwner ->
                collectTypeConstraintDiagnosticData(typeParameterOwner)?.let { diagnosticData ->
                    attributes.typeConstraintDiagnosticData = diagnosticData
                    hasAttributes = true
                }
            }

            val functionBodyDiagnosticOwner = when (owner) {
                is CjFunction -> owner
                // PSI 中 property accessor 不是 CjFunction，但官方 PropDecl 的 getter/setter
                // 以 FuncDecl/FuncBody 保存参数列表，需要进入同一套函数体诊断数据。
                is CjPropertyAccessor -> owner
                else -> null
            }
            functionBodyDiagnosticOwner?.let { diagnosticOwner ->
                collectFunctionBodyDiagnosticData(diagnosticOwner)?.let { diagnosticData ->
                    attributes.functionBodyDiagnosticData = diagnosticData
                    hasAttributes = true
                }
            }

            return if (hasAttributes) attributes else CfirDeclarationAttributes.EMPTY
        }

        /** 转换 class-like 声明的类型参数列表。 */
        private fun convertTypeParameters(
            psi: CjClassLikeDeclaration,
            containingSymbol: CfirBasedSymbol<*>,
        ): List<CfirTypeParameter> {
            val owner = psi as? CjTypeParameterListOwner ?: return emptyList()
            val typeConstraintBounds = collectTypeConstraintBounds(owner)
            return owner.typeParameters.map { typeParameter ->
                convertTypeParameter(
                    typeParameter,
                    containingSymbol,
                    typeConstraintBounds[typeParameter.cfirNameAsSafeName].orEmpty()
                )
            }
        }

        /** 转换 extend 声明的类型参数列表。 */
        private fun convertTypeParameters(
            psi: CjExtend,
            containingSymbol: CfirBasedSymbol<*>,
        ): List<CfirTypeParameter> {
            val owner = psi as? CjTypeParameterListOwner ?: return emptyList()
            val typeConstraintBounds = collectTypeConstraintBounds(owner)
            return owner.typeParameters.map { typeParameter ->
                convertTypeParameter(
                    typeParameter,
                    containingSymbol,
                    typeConstraintBounds[typeParameter.cfirNameAsSafeName].orEmpty()
                )
            }
        }

        /** 转换 typealias 声明的类型参数列表。 */
        private fun convertTypeAliasTypeParameters(
            psi: CjTypeAlias,
            containingSymbol: CfirBasedSymbol<*>,
        ): List<CfirTypeParameter> {
            val owner = psi as? CjTypeParameterListOwner ?: return emptyList()
            val typeConstraintBounds = collectTypeConstraintBounds(owner)
            return owner.typeParameters.map { typeParameter ->
                convertTypeParameter(
                    typeParameter,
                    containingSymbol,
                    typeConstraintBounds[typeParameter.cfirNameAsSafeName].orEmpty()
                )
            }
        }

        /** 转换函数、构造、finalizer、macro declaration 的类型参数列表。 */
        private fun convertFunctionTypeParameters(
            psi: CjFunction,
            containingDeclarationSymbol: CfirBasedSymbol<*>,
        ): List<CfirTypeParameter> {
            val typeConstraintBounds = collectTypeConstraintBounds(psi)
            return psi.typeParameters.map { typeParameter ->
                convertTypeParameter(
                    typeParameter,
                    containingDeclarationSymbol,
                    typeConstraintBounds[typeParameter.cfirNameAsSafeName].orEmpty()
                )
            }
        }

        /** 转换 class-like 声明的直接父类型引用列表。 */
        private fun convertSuperTypeRefs(psi: CjClassLikeDeclaration): List<CfirTypeRef> {
            val typeStatement = psi as? CjTypeStatement ?: return emptyList()
            return typeStatement.superTypeListEntries.map { convertTypeRef(it.typeReference) }
        }

        /** 转换 class-like body 内成员，并处理分离 annotation 与 builtin annotation macro。 */
        private fun convertClassMembers(psi: CjClassLikeDeclaration): List<CfirDeclaration> {
            val typeStatement = psi as? CjTypeStatement ?: return emptyList()
            val body = typeStatement.body ?: return emptyList()
            val declarations = mutableListOf<CfirDeclaration>()
            val pendingAnnotations = mutableListOf<CjAnnotation>()
            for (child in body.children) {
                when (child) {
                    is CjAnnotations -> pendingAnnotations += child.entries
                    is CjDeclaration -> {
                        val declaration = convertDeclaration(child)
                        collectDetachedClassMemberAnnotations(child, pendingAnnotations, declaration)
                        pendingAnnotations.clear()
                        declarations += declaration
                    }
                    is CjMacroExpression -> {
                        convertBuiltinAnnotationMacroDeclaration(child)?.let { declaration ->
                            val annotatedDeclaration = child.input?.declarations
                            if (annotatedDeclaration != null) {
                                collectDetachedClassMemberAnnotations(annotatedDeclaration, pendingAnnotations, declaration)
                            }
                            pendingAnnotations.clear()
                            declarations += declaration
                        }
                    }
                }
            }
            return declarations
        }

        /** 将 class body 中与声明分离的 annotation 回挂到对应 carrier 声明。 */
        private fun collectDetachedClassMemberAnnotations(
            annotated: CjDeclaration,
            pendingAnnotations: List<CjAnnotation>,
            carrier: CfirDeclaration,
        ) {
            if (pendingAnnotations.isEmpty()) return
            val attachedRanges = annotated.annotationEntries.mapTo(mutableSetOf()) { it.textRange }
            val detachedAnnotations = pendingAnnotations.filter { it.textRange !in attachedRanges }
            collectMacroAnnotationSurfaces(
                annotated = annotated,
                entries = detachedAnnotations,
                target = AnnotationSurfaceTarget.DECLARATION,
                carrier = carrier,
            )
        }

        /**
         * 仓颉的 class-like 声明只允许出现在文件顶层。
         * 当 PSI 恢复出非法嵌套/局部 class-like 时，Raw CFIR 必须显式标记为 invalid，
         * 不能继续为其制造 `ClassId` 并伪装成合法声明。
         */
        private fun buildInvalidClassLikeDeclaration(
            source: AbstractCjSourceElement,
            kind: String,
            name: Name,
        ): CfirDeclaration {
            return buildSourceDeclaration(CfirInvalidDeclarationSymbol()) { symbol ->
                buildInvalidDeclaration {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    this.source = source as? CjSourceElement
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    reason =
                        "Cangjie only supports top-level $kind declarations, but found illegal non-top-level declaration: $name"
                }
            }
        }

        /** 转换普通声明的 status，非 modifier owner 使用默认 status。 */
        private fun convertDeclarationStatus(psi: CjDeclaration): CfirDeclarationStatus {
            val owner = psi as? CjModifierListOwner ?: return CfirDeclarationStatusImpl.DEFAULT
            return convertDeclarationStatus(owner, psi)
        }

        /** 转换 main 函数 status，默认可见性为 public。 */
        private fun convertMainFunctionStatus(psi: CjMainFunction): CfirDeclarationStatus {
            return convertDeclarationStatus(psi, psi, defaultVisibility = Visibilities.Public)
        }

        /** 根据 modifier list、声明种类和上下文构造声明 status。 */
        private fun convertDeclarationStatus(
            owner: CjModifierListOwner,
            declaration: CjDeclaration,
            defaultVisibility: Visibility? = null,
        ): CfirDeclarationStatus {
            fun hasModifier(token: org.cangnova.cangjie.lexer.CjKeywordToken): Boolean = owner.hasModifier(token)

            val isVisibilityExplicit =
                hasModifier(CjTokens.PUBLIC_KEYWORD) ||
                        hasModifier(CjTokens.PRIVATE_KEYWORD) ||
                        hasModifier(CjTokens.PROTECTED_KEYWORD) ||
                        hasModifier(CjTokens.INTERNAL_KEYWORD)
            val isModalityExplicit =
                hasModifier(CjTokens.ABSTRACT_KEYWORD) ||
                        hasModifier(CjTokens.OPEN_KEYWORD) ||
                        hasModifier(CjTokens.SEALED_KEYWORD)
            val effectiveDefaultVisibility = defaultVisibility ?: when {
                context.inLocalContext -> Visibilities.Local
                containerSymbolIfAny is CfirInterfaceSymbol -> Visibilities.Public
                else -> Visibilities.Internal
            }

            return buildDeclarationStatus(
                visibility = when {
                    hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
                    hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
                    hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
                    hasModifier(CjTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
                    else -> effectiveDefaultVisibility
                },
                isVisibilityExplicit = isVisibilityExplicit,
                isModalityExplicit = isModalityExplicit,
                isAbstractExplicit = hasModifier(CjTokens.ABSTRACT_KEYWORD),
                isAbstract = hasModifier(CjTokens.ABSTRACT_KEYWORD) ||
                        isImplicitAbstractClassLikeMember(declaration, owner),
                isOpen = hasModifier(CjTokens.OPEN_KEYWORD),
                isSealed = hasModifier(CjTokens.SEALED_KEYWORD),
                isStatic = hasModifier(CjTokens.STATIC_KEYWORD),
                isConst = hasModifier(CjTokens.CONST_KEYWORD) ||
                        declaration.hasConstDeclarationKeyword(),
                isMut = hasModifier(CjTokens.MUT_KEYWORD),
                isOverride = hasModifier(CjTokens.OVERRIDE_KEYWORD),
                isRedef = hasModifier(CjTokens.REDEF_KEYWORD),
                isOperator = hasModifier(CjTokens.OPERATOR_KEYWORD),
                isUnsafe = hasModifier(CjTokens.UNSAFE_KEYWORD),
                isForeign = hasModifier(CjTokens.FOREIGN_KEYWORD),
                isDefault = isDefaultInterfaceMember(declaration),
            )
        }

        /** 判断声明语法本身是否带 const 声明关键字。 */
        private fun CjDeclaration.hasConstDeclarationKeyword(): Boolean = when (this) {
            is CjPatternVariable -> isConst
            is CjFieldVariable -> isConst
            else -> false
        }

        /**
         * 官方 parser 在 class/interface 体内把无 body 函数、无属性体且无 getter/setter 的属性标记为 abstract。
         * 这里处于 PSI -> Raw CFIR 层，应承接 parser 产物，而不是等待后续 checker 猜测。
         */
        private fun isImplicitAbstractClassLikeMember(
            declaration: CjDeclaration,
            owner: CjModifierListOwner,
        ): Boolean {
            if (containerSymbolIfAny !is CfirClassSymbol && containerSymbolIfAny !is CfirInterfaceSymbol) return false
            if (owner.hasModifier(CjTokens.FOREIGN_KEYWORD)) return false

            return when (declaration) {
                is CjNamedFunction -> !declaration.hasBody()
                is CjProperty -> declaration.body == null && declaration.getter == null && declaration.setter == null
                else -> false
            }
        }

        /** 判断 interface 成员是否应标记为 default 实现。 */
        private fun isDefaultInterfaceMember(declaration: CjDeclaration): Boolean {
            if (containerSymbolIfAny !is CfirInterfaceSymbol) return false

            return when (declaration) {
                is CjNamedFunction -> !declaration.hasModifier(CjTokens.FOREIGN_KEYWORD) && declaration.hasBody()
                is CjProperty -> declaration.hasBody()
                is CjPropertyAccessor -> declaration.hasBody()
                else -> false
            }
        }
    }

    // ===== 文件级构建辅助 =====

    /** 构造文件 package directive，缺失时使用 root package。 */
    private fun buildPackageDirective(psi: CjPackageDirective?): CfirPackageDirective {
        val fqName = psi?.fqName ?: FqName.ROOT
        return buildPackageDirective {
            source = psi?.toCjPsiSourceElement()
            packageFqName = fqName
            isMacroPackage = psi?.isMacroPackage == true
        }
    }

    /** 构造文件 import 列表。 */
    private fun buildImports(file: CjFile): List<CfirImport> {
        val importDirectives = file.importDirectives
        return importDirectives.flatMap { directive ->
            directive.importItems.mapNotNull { item ->
                val fqName = item.importedFqName ?: return@mapNotNull null
                buildImport {
                    source = item.toCjPsiSourceElement()
                    importedFqName = fqName
                    isAllUnder = item.isAllUnder
                    aliasName = item.aliasName?.let { Name.identifier(it) }
                }
            }
        }
    }


}

/** 计算 reparse 后 PSI 与原始 source override 之间的偏移差。 */
private fun sourceOffsetDelta(sourceOverride: CjSourceElement?, reparsedPsi: PsiElement): Int {
    return sourceOverride?.let { it.startOffset - reparsedPsi.textRange.startOffset } ?: 0
}

/**
 * 返回 macro-expression wrapper 中完整 annotation 的精确 source，不包含其输入声明。
 */
private fun CjMacroExpression.annotationSourceElement(
    headSyntax: MacroExpressionHeadSyntax? = macroExpressionHeadSyntax(),
): CjSourceElement {
    val wrapperSource = toCjPsiSourceElement()
    val annotationEndOffset = headSyntax?.let { syntax ->
        wrapperSource.startOffset + (syntax.attrRange?.endOffset ?: syntax.nameRange.endOffset)
    } ?: attr?.textRange?.endOffset
        ?: referenceExpression?.textRange?.endOffset
        ?: error("Macro annotation must contain a reference expression")
    check(annotationEndOffset in wrapperSource.startOffset..wrapperSource.endOffset) {
        "Macro annotation source must stay inside its wrapper source range."
    }
    return CjLightSourceElement(
        lighterASTNode = wrapperSource.lighterASTNode,
        startOffset = wrapperSource.startOffset,
        endOffset = annotationEndOffset,
        treeStructure = wrapperSource.treeStructure,
        kind = wrapperSource.kind,
    )
}

/** 扫描当前 PSI macro-expression wrapper 的头部语法。 */
private fun CjMacroExpression.macroExpressionHeadSyntax(): MacroExpressionHeadSyntax? =
    scanMacroExpressionHeadSyntax(text.orEmpty())

/** 返回当前 wrapper 头部 attr 文本，优先使用源码扫描结果。 */
private fun CjMacroExpression.macroAttributeText(
    headSyntax: MacroExpressionHeadSyntax? = macroExpressionHeadSyntax(),
): String? {
    val rawText = text.orEmpty()
    return headSyntax
        ?.attrRange
        ?.let { range -> rawText.substring(range.startOffset, range.endOffset) }
        ?: attr?.text
}

/** 返回当前 wrapper 头部 attr source，优先使用源码扫描结果。 */
private fun CjMacroExpression.macroAttributeSourceElement(
    headSyntax: MacroExpressionHeadSyntax? = macroExpressionHeadSyntax(),
): CjSourceElement? {
    return headSyntax
        ?.attrRange
        ?.let(::sliceMacroExpressionSource)
        ?: attr?.toCjPsiSourceElement()
}

/** 构造 wrapper 内局部文本区间对应的 source。 */
private fun CjMacroExpression.sliceMacroExpressionSource(range: MacroExpressionTextRange): CjSourceElement {
    val wrapperSource = toCjPsiSourceElement()
    return CjLightSourceElement(
        lighterASTNode = wrapperSource.lighterASTNode,
        startOffset = wrapperSource.startOffset + range.startOffset,
        endOffset = wrapperSource.startOffset + range.endOffset,
        treeStructure = wrapperSource.treeStructure,
        kind = wrapperSource.kind,
    )
}

/**
 * 从当前 macro-expression wrapper 文本开头扫描 annotation 名称与 attr。
 *
 * 扫描只消费 `@` / `@!` 后的限定名以及紧随其后的 `[attr]`，
 * 不进入 input declaration，因此可恢复 `@!APILevel[since: "21"] @M class A`
 * 的外层 attr，而不会误读内层 `@M`。
 */
private fun scanMacroExpressionHeadSyntax(rawText: String): MacroExpressionHeadSyntax? {
    var index = rawText.indexOf('@')
    if (index < 0) return null
    index++
    if (rawText.getOrNull(index) == '!') index++
    while (index < rawText.length && rawText[index].isWhitespace()) index++

    val nameStart = index
    var expectIdentifier = true
    var lastIdentifierEnd = -1
    while (index < rawText.length) {
        val current = rawText[index]
        when {
            current.isMacroIdentifierStart() -> {
                index++
                while (index < rawText.length && rawText[index].isMacroIdentifierPart()) index++
                lastIdentifierEnd = index
                expectIdentifier = false
            }
            current == '.' && !expectIdentifier -> {
                index++
                expectIdentifier = true
            }
            else -> break
        }
    }

    if (lastIdentifierEnd <= nameStart || expectIdentifier) return null

    while (index < rawText.length && rawText[index].isWhitespace()) index++
    val attrRange = if (rawText.getOrNull(index) == '[') {
        scanMacroAttributeRange(rawText, index)
    } else null

    return MacroExpressionHeadSyntax(
        rawName = rawText.substring(nameStart, lastIdentifierEnd),
        nameRange = MacroExpressionTextRange(nameStart, lastIdentifierEnd),
        attrRange = attrRange,
    )
}

/**
 * 扫描 macro-expression wrapper input 中位于 carrier 声明前的直接 annotation 序列。
 *
 * 该扫描只在调用方给定的 `[startOffset, endOffset)` 区间内推进；该区间由
 * wrapper 头部结束位置和最终 carrier 声明起点构成，因此不会进入声明体。
 */
private fun scanMacroExpressionInputAnnotationSyntax(
    rawText: String,
    startOffset: Int,
    endOffset: Int,
): List<MacroExpressionInputAnnotationSyntax> {
    val result = mutableListOf<MacroExpressionInputAnnotationSyntax>()
    var index = startOffset.coerceIn(0, rawText.length)
    val limit = endOffset.coerceIn(index, rawText.length)

    while (index < limit) {
        while (index < limit && rawText[index].isWhitespace()) index++
        if (index >= limit || rawText[index] != '@') break

        val annotationStart = index
        index++
        if (rawText.getOrNull(index) == '!') index++
        while (index < limit && rawText[index].isWhitespace()) index++

        val nameStart = index
        var expectIdentifier = true
        var lastIdentifierEnd = -1
        while (index < limit) {
            val current = rawText[index]
            when {
                current.isMacroIdentifierStart() -> {
                    index++
                    while (index < limit && rawText[index].isMacroIdentifierPart()) index++
                    lastIdentifierEnd = index
                    expectIdentifier = false
                }
                current == '.' && !expectIdentifier -> {
                    index++
                    expectIdentifier = true
                }
                else -> break
            }
        }
        if (lastIdentifierEnd <= nameStart || expectIdentifier) break

        while (index < limit && rawText[index].isWhitespace()) index++
        var argumentRange: MacroExpressionTextRange? = null
        var macroAttributeRange: MacroExpressionTextRange? = null
        when (rawText.getOrNull(index)) {
            '(' -> {
                argumentRange = scanBalancedMacroRange(rawText, index, '(', ')') ?: break
                index = argumentRange.endOffset
            }
            '[' -> {
                macroAttributeRange = scanMacroAttributeRange(rawText, index) ?: break
                index = macroAttributeRange.endOffset
            }
        }

        result += MacroExpressionInputAnnotationSyntax(
            rawSyntax = rawText.substring(annotationStart, index),
            annotationRange = MacroExpressionTextRange(annotationStart, index),
            argumentRange = argumentRange,
            macroAttributeRange = macroAttributeRange,
        )
    }

    return result
}

/** 扫描 wrapper 头部的平衡方括号 attr 区间，跳过字符串 literal 内部括号。 */
private fun scanMacroAttributeRange(rawText: String, openBracketIndex: Int): MacroExpressionTextRange? {
    return scanBalancedMacroRange(rawText, openBracketIndex, '[', ']')
}

/** 扫描平衡括号区间，跳过字符串 literal 内部括号。 */
private fun scanBalancedMacroRange(
    rawText: String,
    openIndex: Int,
    openChar: Char,
    closeChar: Char,
): MacroExpressionTextRange? {
    if (rawText.getOrNull(openIndex) != openChar) return null
    var index = openIndex
    var bracketDepth = 0
    var quote: Char? = null
    var escaped = false
    while (index < rawText.length) {
        val current = rawText[index]
        if (quote != null) {
            when {
                escaped -> escaped = false
                current == '\\' -> escaped = true
                current == quote -> quote = null
            }
            } else {
                when (current) {
                    '"', '\'' -> quote = current
                    openChar -> bracketDepth++
                    closeChar -> {
                        bracketDepth--
                        if (bracketDepth == 0) {
                            return MacroExpressionTextRange(openIndex, index + 1)
                        }
                    }
                }
            }
            index++
    }
    return null
}

/** macro / annotation 名称首字符。 */
private fun Char.isMacroIdentifierStart(): Boolean = this == '_' || isLetter()

/** macro / annotation 名称后续字符。 */
private fun Char.isMacroIdentifierPart(): Boolean = this == '_' || isLetterOrDigit()

/** 将 PSI source element 按 [delta] 平移，用于 macro fragment reparse 后恢复原始位置。 */
private fun PsiElement.shiftedBy(delta: Int): CjSourceElement {
    val source = toCjPsiSourceElement()
    if (delta == 0) return source
    return CjLightSourceElement(
        lighterASTNode = source.lighterASTNode,
        startOffset = source.startOffset + delta,
        endOffset = source.endOffset + delta,
        treeStructure = source.treeStructure,
        kind = source.kind,
    )
}

/** 将 raw-cfir-common 的 macro payload token 映射为 providers 层 macro surface token。 */
private fun List<org.cangnova.cangjie.cfir.builder.macro.MacroPayloadToken>.toMacroSurfaceTokens(): List<MacroSurfaceToken> {
    return map { token ->
        MacroSurfaceToken(
            text = token.text,
            startOffset = token.startOffset,
            endOffset = token.endOffset,
            kindName = token.kindName,
        )
    }
}
