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

/**
 * `PSI -> Raw CFIR` 构建器，对齐 Kotlin 的 `PsiRawFirBuilder`。
 * 遍历 PSI 语法树，生成 Raw CFIR 中间表示。
 * 在 `RAW_CFIR` 阶段：
 * - 所有类型引用都保持为 `CfirUserTypeRef`，尚未解析
 * - 所有符号引用都保持为 `CfirNamedReference`，尚未绑定
 * - 不做类型推断和重载解析，这些工作留给 `CFIR_RESOLVE`
 */
class PsiRawCfirBuilder(
    session: CfirSession,
    @Suppress("unused")
    val baseScopeProvider: CfirScopeProvider = session.cangjieScopeProvider,
    private val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) : AbstractRawCfirBuilder<PsiElement>(session) {
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

    override fun PsiElement.toSourceElement(): AbstractCjSourceElement {
        return CjRealPsiSourceElement(this)
    }

    override fun PsiElement.elementType(): IElementType = node.elementType

    override fun PsiElement.asText(): String = text
    var mode: BodyBuildingMode = bodyBuildingMode
        private set

    private inline fun <T> runOnStubs(crossinline body: () -> T): T {
        return when (mode) {
            BodyBuildingMode.NORMAL -> body()
            BodyBuildingMode.LAZY_BODIES -> {
                AstLoadingFilter.disallowTreeLoading<T, Nothing> { body() }
            }
        }
    }

    private inline fun <T> buildOrLazy(build: () -> T, noinline lazy: () -> T): T {
        return when (mode) {
            BodyBuildingMode.NORMAL -> build()
            BodyBuildingMode.LAZY_BODIES -> runOnStubs(lazy)
        }
    }

    private inline fun buildOrLazyBlock(buildBlock: () -> CfirBlock?): CfirBlock? {
        return buildOrLazy(buildBlock) { buildLazyBlock() }
    }

    private enum class AnnotationSurfaceTarget {
        DECLARATION,
        PARAMETER,
    }

    private companion object {
        private const val IF_AVAILABLE_ANNOTATION_NAME: String = "IfAvailable"
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

    private val visitor = Visitor()
    private val converter = Converter()

    override fun buildElement(element: PsiElement): CfirElement {
        val cjElement = element as? CjElement
            ?: error("Expected CjElement but was ${element::class.qualifiedName}")
        return cjElement.accept(visitor, null)
            ?: error("Unsupported PSI element: ${element::class.qualifiedName}")
    }

    override fun buildFile(file: PsiElement): CfirFile {
        val cjFile = file as? CjFile ?: error("Expected CjFile but was ${file::class.qualifiedName}")
        return buildFile(cjFile)
    }

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
            converter.convertAnnotationCall(
                annotation = annotation,
                containingSymbol = containingSymbol,
                sourceOverride = sourceOverride,
                argumentListSourceOverride = argumentListSourceOverride,
            )
        }
    }

    private inline fun <D : CfirDeclaration, S : CfirBasedSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)
        return declaration
    }

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
        override fun visitCjFile(file: CjFile, data: Unit?): CfirElement = buildFile(file)

        override fun visitDeclaration(dcl: CjDeclaration, data: Unit?): CfirElement = buildDeclaration(dcl)

        override fun visitExpression(expression: CjExpression, data: Unit?): CfirElement = buildExpression(expression)
    }

    protected open inner class Converter {

        // ===== 声明转换 =====

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
                input.declarations?.let { declaration ->
                    return declaration to macroExpressions.toList()
                }
                current = input.children.firstOrNull { it is CjMacroExpression } as? CjMacroExpression
            }

            return null
        }

        private fun applyTopLevelMacroExpression(
            psi: CjMacroExpression,
            carrier: CfirDeclaration,
        ) {
            if (applyBuiltinAnnotationMacroExpression(psi, carrier)) return

            val surfaceId = MacroSurfaceIdGenerator.next()
            val text = psi.text.orEmpty()
            val currentPackage = context.packageFqName
            val source = psi.toCjPsiSourceElement()
            val input = psi.input
            val containingSymbol = when (carrier) {
                is CfirValueParameter -> carrier.containingDeclarationSymbol
                else -> carrier.symbol
            }
            val macroAnnotation = psi.asDeclarationMacroAnnotation()
            val annotationCarrier = macroAnnotation?.let { annotation ->
                val annotationCall = convertAnnotationCall(
                    annotation = annotation,
                    containingSymbol = containingSymbol,
                    sourceOverride = source,
                    argumentListSourceOverride = psi.attr?.toCjPsiSourceElement(),
                )
                val annotationIndex = carrier.annotations.size
                carrier.replaceAnnotations(carrier.annotations + annotationCall)
                baseSession.ensureAnnotationMetadataRegistry().record(
                    CfirAnnotationSlotSnapshot(
                        owner = carrier,
                        annotationIndex = annotationIndex,
                        originalAnnotation = annotationCall,
                        rawSyntax = annotation.text,
                        forcedCustom = text.trimStart().startsWith("@!"),
                        qualifiedName = psi.shortName?.let {
                            if (currentPackage.isRoot) FqName.topLevel(it) else currentPackage.child(it)
                        },
                        argumentText = psi.attr?.text,
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
                qualifiedName = psi.shortName?.let {
                    if (currentPackage.isRoot) FqName.topLevel(it) else currentPackage.child(it)
                },
                kind = if (text.startsWith("@!")) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
                hasParenthesis = input?.text?.trimStart()?.startsWith("(") == true,
                attrTokens = MacroPayloadTokenizer.tokenize(
                    psi.attr?.text,
                    psi.attr?.textRange?.startOffset ?: 0,
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
            val name = shortName?.asString()?.takeIf { it.isNotBlank() } ?: return null
            val prefix = if (text.orEmpty().trimStart().startsWith("@!")) "@!" else "@"
            val rawAnnotation = prefix + name + attr?.text.orEmpty()
            return CjPsiFactory.contextual(this).createAnnotations(rawAnnotation).entries.singleOrNull()
        }

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

        private fun CjMacroExpression.isBuiltinAnnotationMacroExpression(): Boolean {
            val shortName = shortName ?: return false
            return !text.orEmpty().trimStart().startsWith("@!") && shortName in builtinAnnotationMacroNames
        }

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
            val annotationCall = convertAnnotationCall(
                annotation = annotation,
                containingSymbol = containingSymbol,
                sourceOverride = psi.toCjPsiSourceElement(),
                argumentListSourceOverride = psi.attr?.toCjPsiSourceElement(),
            )
            carrier.replaceAnnotations(carrier.annotations + annotationCall)
            return true
        }

        private fun convertClass(psi: CjClassLikeDeclaration, classKind: CfirClassKind): CfirDeclaration {
            val name = psi.nameAsSafeName
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
                            val declarations = convertClassMembers(psi).toMutableList().also { declarations ->
                                addPrimaryConstructorParameterProperties(psi, declarations)
                                if (classKind != CfirClassKind.INTERFACE && declarations.none { it is CfirConstructor }) {
                                    declarations.add(0, buildImplicitPrimaryConstructor(psi))
                                }
                                if (psi is CjEnum) {
                                    declarations.addAll(
                                        0,
                                        psi.constructor.map { convertEnumConstructor(it, typeParameters) })
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
                            convertTypeParameters(psi, symbol) to convertClassMembers(psi).toMutableList()
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
                            val declarations = convertClassMembers(psi).toMutableList().also { declarations ->
                                addPrimaryConstructorParameterProperties(psi, declarations)
                                if (declarations.none { it is CfirConstructor }) {
                                    declarations.add(0, buildImplicitPrimaryConstructor(psi))
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
                            val declarations = convertClassMembers(psi).toMutableList().also { declarations ->
                                addPrimaryConstructorParameterProperties(psi, declarations)
                                if (declarations.none { it is CfirConstructor }) {
                                    declarations.add(0, buildImplicitPrimaryConstructor(psi))
                                }
                                if (psi is CjEnum) {
                                    declarations.addAll(
                                        0,
                                        psi.constructor.map { convertEnumConstructor(it, typeParameters) })
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

        private fun convertPrimaryConstructorParameterProperty(
            psi: CjParameter,
            valueParameter: CfirValueParameter,
        ): CfirProperty {
            val name = psi.nameAsSafeName
            val propertySource = psi.toCjPsiSourceElement().fakeElement(CjFakeSourceElementKind.PropertyFromParameter)
            val propertySymbol = CfirPropertySymbol(callableIdFor(name))
            val property = buildSourceDeclaration(propertySymbol) { symbol ->
                buildProperty {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = propertySource
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = valueParameter.returnTypeRef.copyWithNewSource(propertySource)
                    this.name = name
                    getter = null
                    setter = null
                }
            }
            valueParameter.correspondingProperty = property
            return property
        }

        private fun convertExtend(psi: CjExtend): CfirExtend {
            val extendedTypeRef = convertTypeRef(psi.receiverTypeReceiver)
            val superTypes = psi.superTypeListEntries.map { convertTypeRef(it.typeReference) }
            val members = psi.body?.declarations?.map { convertDeclaration(it) } ?: emptyList()

            return buildSourceDeclaration(CfirExtendSymbol()) { symbol ->
                buildExtend {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    val typeParametersForExtend = convertTypeParameters(psi, symbol)
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

        fun convertFunction(psi: CjNamedFunction): CfirFunction {
            val name = psi.nameAsSafeName
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

        private fun convertProperty(psi: CjProperty): CfirProperty {
            val name = psi.nameAsSafeName
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

                    attributes = CfirDeclarationAttributes.EMPTY
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

                    attributes = CfirDeclarationAttributes.EMPTY
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

        private fun convertFieldVariable(psi: CjFieldVariable): CfirFieldVariable {
            val name = psi.nameAsSafeName
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

        fun convertMacroDeclaration(psi: CjMacroDeclaration): CfirMacroDeclaration {
            val name = psi.nameAsSafeName
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

        private fun convertTypeAlias(psi: CjTypeAlias): CfirDeclaration {
            val name = psi.nameAsSafeName
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

        private fun convertEnumConstructor(
            psi: CjEnumConstructor,
            ownerTypeParameters: List<CfirTypeParameter>,
        ): CfirEnumConstructor {
            val enumConstructorName =
                psi.name?.let { Name.identifier(it) } ?: Name.special("<anonymous-enum-constructor>")
            val valueTypeRefs = psi.typeReferences.map { convertTypeRef(it) }
            return buildSourceDeclaration(CfirEnumConstructorSymbol(callableIdFor(enumConstructorName))) { symbol ->
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
                    typeParameters.addAll(ownerTypeParameters)
                    returnTypeRef = buildImplicitTypeRef()
                    this.valueParameters.addAll(valueParameters)
                    name = enumConstructorName
                }
            }
        }

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

        fun convertValueParameter(
            psi: CjParameter,
            containingSymbol: CfirBasedSymbol<*>,
            requiresExplicitType: Boolean = true,
        ): CfirValueParameter {
            val parameterSource = psi.toCjPsiSourceElement()
            val parameter = buildSourceDeclaration(CfirValueParameterSymbol(callableIdFor(psi.nameAsSafeName))) { symbol ->
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
                    name = psi.nameAsSafeName
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
                val annotationCall = buildRawAnnotationCall(annotation, carrier)
                val annotationIndex = carrier.annotations.size
                carrier.replaceAnnotations(carrier.annotations + annotationCall)
                val snapshot = CfirAnnotationSlotSnapshot(
                    owner = carrier,
                    annotationIndex = annotationIndex,
                    originalAnnotation = annotationCall,
                    rawSyntax = annotation.text,
                    forcedCustom = annotation.text.trimStart().startsWith("@!"),
                    qualifiedName = annotationQualifiedName(annotation),
                    argumentText = annotation.valueArgumentList?.text,
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

        fun convertAnnotationCall(
            annotation: CjAnnotation,
            containingSymbol: CfirBasedSymbol<*>,
            sourceOverride: CjSourceElement? = null,
            argumentListSourceOverride: CjSourceElement? = null,
        ): CfirAnnotationCall {
            val arguments = convertAnnotationArguments(annotation)
            return buildAnnotationCall {
                source = sourceOverride ?: annotation.toCjPsiSourceElement()
                typeRef = convertTypeRef(annotation.typeReference)
                this.arguments.addAll(arguments)
                argumentList = buildArgumentList {
                    source = argumentListSourceOverride ?: annotation.valueArgumentList?.toCjPsiSourceElement()
                    this.arguments.addAll(arguments)
                }
                calleeReference = buildNamedReference(
                    annotation.shortName ?: Name.identifier("<error>"),
                    annotation.toCjPsiSourceElement(),
                )
                containingDeclarationSymbol = containingSymbol
            }
        }

        private fun convertAnnotationArguments(annotation: CjAnnotation): List<CfirExpression> {
            val valueArguments = annotation.valueArguments.mapNotNull(::convertCallArgument)
            if (valueArguments.isNotEmpty()) return valueArguments

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

        private fun buildRawAnnotationCall(
            annotation: CjAnnotation,
            carrier: CfirDeclaration,
        ): CfirAnnotationCall {
            val containingSymbol = when (carrier) {
                is CfirValueParameter -> carrier.containingDeclarationSymbol
                else -> carrier.symbol
            }
            return convertAnnotationCall(annotation, containingSymbol)
        }

        private fun buildMacroAnnotationSurface(
            annotation: CjAnnotation,
            target: AnnotationSurfaceTarget,
            carrier: CfirDeclaration,
            annotationCarrier: CfirAnnotationReplaceCarrier,
            modifiers: List<String>,
            carriedAnnotations: List<String>,
            containerContext: MacroSurfaceContainerContext,
        ): MacroSurface {
            val surfaceId = MacroSurfaceIdGenerator.next()
            val kind = if (annotation.text.startsWith("@!")) {
                MacroSurface.Kind.FORCED
            } else {
                MacroSurface.Kind.PLAIN
            }
            val qualifiedName = annotationQualifiedName(annotation)
            val valueArgumentList = annotation.valueArgumentList
            val inputTokens = MacroPayloadTokenizer.tokenize(
                valueArgumentList?.text,
                valueArgumentList?.textRange?.startOffset ?: 0,
            ).toMacroSurfaceTokens()
            val sourceRange = MacroSurfaceSourceRange(
                source = annotation.toCjPsiSourceElement(),
                startOffset = annotation.textRange.startOffset,
                endOffset = annotation.textRange.endOffset,
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
                    attrTokens = emptyList(),
                    inputTokens = inputTokens,
                    sourceRange = sourceRange,
                    scopeContext = scopeContext,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    capturedRawSyntax = annotation.text,
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
                    attrTokens = emptyList(),
                    inputTokens = inputTokens,
                    sourceRange = sourceRange,
                    scopeContext = scopeContext,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    capturedRawSyntax = annotation.text,
                    containerContext = containerContext,
                    replaceHandle = replaceHandle,
                )
                AnnotationSurfaceTarget.PARAMETER -> MacroSurfaceParam(
                    surfaceId = surfaceId,
                    qualifiedName = qualifiedName,
                    kind = kind,
                    hasParenthesis = valueArgumentList != null,
                    attrTokens = emptyList(),
                    inputTokens = inputTokens,
                    sourceRange = sourceRange,
                    scopeContext = scopeContext,
                    modifiers = modifiers,
                    carriedAnnotations = carriedAnnotations,
                    capturedRawSyntax = annotation.text,
                    containerContext = containerContext,
                    replaceHandle = replaceHandle,
                )
            }
        }

        private fun annotationQualifiedName(annotation: CjAnnotation): FqName? {
            val rawName = annotation.typeReference?.text?.trim()?.takeIf(String::isNotEmpty)
            if (rawName != null && rawName.contains('.')) return FqName(rawName)
            return annotation.shortName?.let(::macroSurfaceQualifiedName)
        }

        private fun macroSurfaceQualifiedName(name: Name): FqName {
            return if (context.packageFqName.isRoot) {
                FqName.topLevel(name)
            } else {
                context.packageFqName.child(name)
            }
        }

        private fun collectModifierNames(modifierList: CjModifierList): List<String> {
            return CjTokens.MODIFIER_KEYWORDS_ARRAY
                .filter { modifierList.hasModifier(it) }
                .map { it.value }
        }

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

        private fun enclosingFunctionName(): Name? {
            return when (val symbol = containerSymbolIfAny) {
                is CfirNamedFunctionSymbol -> symbol.callableId.callableName
                is CfirMainFunctionSymbol -> symbol.callableId.callableName
                is CfirMacroDeclarationSymbol -> symbol.callableId.callableName
                is CfirPropertyAccessorSymbol -> symbol.callableId.callableName
                else -> null
            }
        }

        private inline fun <reified T : PsiElement> PsiElement.hasParentOfType(): Boolean {
            var current = parent
            while (current != null) {
                if (current is T) return true
                current = current.parent
            }
            return false
        }

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

        fun convertExpression(psi: CjExpression): CfirExpression = when (psi) {
            is CjBlockExpression -> convertBlock(psi)
            is CjConstantExpression -> convertLiteral(psi)
            is CjStringTemplateExpression -> convertStringTemplate(psi)
            is CjBinaryExpression -> convertBinary(psi)
            is CjPrefixExpression -> convertPrefix(psi)
            is CjPostfixExpression -> convertPostfix(psi)
            is CjUnsafeExpression -> convertUnsafe(psi)
            is CjOptionalExpression -> convertOptionalExpression(psi)
            is CjOptionalChainExpression -> convertOptionalChainExpression(psi)
            is CjDotQualifiedExpression -> convertDotQualified(psi)
            is CjSafeQualifiedExpression -> convertDotQualified(psi)
            is CjNameReferenceExpression -> convertNameReference(psi)
            is CjIfExpression -> convertIf(psi)
            is CjMatchExpression -> convertMatch(psi)
            is CjForExpression -> convertFor(psi)
            is CjWhileExpression -> convertWhile(psi)
            is CjDoWhileExpression -> convertDoWhile(psi)
            is CjReturnExpression -> convertReturn(psi)
            is CjBreakExpression -> buildBreakExpressionWithImplicitLoopTarget(psi.toCjPsiSourceElement())

            is CjContinueExpression -> buildContinueExpressionWithImplicitLoopTarget(psi.toCjPsiSourceElement())

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

        private fun convertLiteral(psi: CjConstantExpression): CfirLiteralExpression {
            val text = psi.text
            val elementType = psi.node.elementType
            val (kind, value) = when (elementType) {
                INTEGER_CONSTANT -> CfirLiteralKind.INT to text
                FLOAT_CONSTANT -> CfirLiteralKind.FLOAT to text
                RUNE_CONSTANT -> CfirLiteralKind.RUNE to text
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

        private fun convertBinary(psi: CjBinaryExpression): CfirExpression {
            if (psi is CjRangeExpression) return convertRange(psi)

            val left = psi.left?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing left operand")
            val right = psi.right?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing right operand")
            val opToken = psi.operationToken

            if (opToken.isAssignmentToken()) {
                if (opToken == CjTokens.EQ) {
                    return buildAssignment {
                        source = psi.toCjPsiSourceElement()
                        lValue = left
                        rValue = right
                    }
                }
                val opName = opToken.toCompoundAssignName()?.asString() ?: "<error>"
                return buildAssignment {
                    source = psi.toCjPsiSourceElement()
                    lValue = left
                    rValue = buildFunctionCall {
                        source = psi.toCjPsiSourceElement()
                        calleeReference = buildNamedReference(Name.identifier(opName), psi.toCjPsiSourceElement())
                        argumentList = buildArgumentList {
                            arguments.add(right)
                        }
                        explicitReceiver = left
                        origin = CfirFunctionCallOrigin.Operator
                    }
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

        private fun convertPrefix(psi: CjPrefixExpression): CfirExpression {
            val base = psi.baseExpression?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing prefix operand")
            val opName = psi.operationToken.toPrefixUnaryName() ?: Name.identifier("<prefix>")
            return buildFunctionCall {
                source = psi.toCjPsiSourceElement()
                calleeReference = buildNamedReference(opName, psi.toCjPsiSourceElement())
                argumentList = buildArgumentList()
                explicitReceiver = base
                origin = CfirFunctionCallOrigin.Operator
            }
        }

        private fun convertPostfix(psi: CjPostfixExpression): CfirExpression {
            val base = psi.baseExpression?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing postfix operand")
            val opName = psi.operationToken.toPostfixUnaryName() ?: Name.identifier("<postfix>")
            return buildFunctionCall {
                source = psi.toCjPsiSourceElement()
                calleeReference = buildNamedReference(opName, psi.toCjPsiSourceElement())
                argumentList = buildArgumentList()
                explicitReceiver = base
                origin = CfirFunctionCallOrigin.Operator
            }
        }

        // ---- Call & Access ----

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
            val lambdaArgs = psi.lambdaArguments.mapNotNull { lambdaArgument ->
                lambdaArgument.getLambdaExpression()?.let { lambda ->
                    convertLambda(lambda).also { anonymousFunctionExpression ->
                        anonymousFunctionExpression.replaceIsTrailingLambda(true)
                    }
                }
            }

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
            if (callee is CjCallExpression && psi.valueArgumentList == null && lambdaArgs.isNotEmpty()) {
                val flattenedCallee = callee.calleeExpression
                val flattenedArguments = callee.valueArguments.mapNotNull(::convertCallArgument) + lambdaArgs
                val flattenedTypeArguments = extractCallTypeArguments(callee, flattenedCallee)
                val (receiver, reference) = resolveCalleeReference(flattenedCallee)

                return buildFunctionCall {
                    source = psi.toCjPsiSourceElement()
                    calleeReference = reference
                    argumentList = buildArgumentList {
                        arguments.addAll(flattenedArguments)
                    }
                    explicitReceiver = receiver
                    typeArguments.addAll(flattenedTypeArguments)
                    origin = callOriginFor(flattenedCallee)
                    hasTrailingLambda = true
                }
            }

            val typeArgs = extractCallTypeArguments(psi, callee)

            tryBuildTypeConversion(psi, callee, typeArgs)?.let { return it }

            // valueArguments 已经包含了 lambdaArguments，不需要再单独处理
            val allArgs = psi.valueArguments.mapNotNull(::convertCallArgument)
            val (receiver, reference) = resolveCalleeReference(callee)

            if (psi.valueArgumentList == null && lambdaArgs.isEmpty() && typeArgs.isNotEmpty()) {
                return buildNamedAccessExpression {
                    source = psi.toCjPsiSourceElement()
                    calleeReference = reference
                    explicitReceiver = receiver
                    typeArguments.addAll(typeArgs)
                }
            }

            return buildFunctionCall {
                source = psi.toCjPsiSourceElement()
                calleeReference = reference
                argumentList = buildArgumentList {
                    arguments.addAll(allArgs)
                }
                explicitReceiver = receiver
                typeArguments.addAll(typeArgs)
                origin = callOriginFor(callee)
                hasTrailingLambda = psi.lambdaArguments.isNotEmpty()
            }
        }

        /**
         * 参数映射阶段需要知道调用实参的外层语法（特别是 named argument 前缀），
         * 因此对命名实参保留一层单表达式 block，避免在 Raw CFIR 阶段把整段 value-argument source 丢掉。
         * 这里复用现有 block 节点，是为了让后续 checker / reporter / positioning 链路保持统一，
         * 而不是额外引入只服务于参数绑定的专用表达式分支。
         */
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

            if (!argument.isNamed()) return wrapped

            return buildBlock {
                source = argument.asElement().toCjPsiSourceElement()
                statements.add(wrapped)
            }
        }

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

        private fun CjNameBasicReferenceExpression.primitiveTypeConversionKindOrNull(): PrimitiveTypeKind? =
            PrimitiveTypeKind.entries.firstOrNull {
                it.isExposedBuiltinClassifier && it.typeName == referencedName
            }

        private fun resolveCalleeReference(callee: CjExpression?): Pair<CfirExpression?, CfirNamedReference> {
            return when (callee) {
                is CjSimpleNameExpression -> null to buildNamedReference(
                    callee.referencedNameAsName,
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
                            buildNamedReference(selector.referencedNameAsName, selector.toCjPsiSourceElement())

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

        private fun convertDotQualified(psi: CjQualifiedExpression): CfirExpression {
            val receiver = convertExpression(psi.receiverExpression)
            val selector = psi.selectorExpression
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing selector")

            if (selector is CjCallExpression) {
                val callArguments = selector.valueArguments.mapNotNull(::convertCallArgument)
                val typeArgs = extractCallTypeArguments(selector, selector.calleeExpression)
                val callee = selector.calleeExpression
                val ref = if (callee is CjSimpleNameExpression) {
                    buildNamedReference(callee.referencedNameAsName, callee.toCjPsiSourceElement())
                } else {
                    buildNamedReference(Name.identifier(callee?.text ?: "<error>"), callee?.toCjPsiSourceElement())
                }
                val lambdaArgs =
                    selector.lambdaArguments.mapNotNull { lambdaArgument ->
                        lambdaArgument.getLambdaExpression()?.let { lambda ->
                            convertLambda(lambda).also { anonymousFunctionExpression ->
                                anonymousFunctionExpression.replaceIsTrailingLambda(true)
                            }
                        }
                    }

                return buildFunctionCall {
                    source = psi.toCjPsiSourceElement()
                    calleeReference = ref
                    argumentList = buildArgumentList {
                        arguments.addAll(callArguments + lambdaArgs)
                    }
                    explicitReceiver = receiver
                    typeArguments.addAll(typeArgs)
                    origin = CfirFunctionCallOrigin.Regular
                }
            }

            if (selector is CjSimpleNameExpression) {
                val typeArgs = selector.getTypeArguments().map { convertTypeRef(it.typeReference) }
                if (typeArgs.isNotEmpty()) {
                    return buildNamedAccessExpression {
                        source = psi.toCjPsiSourceElement()
                        calleeReference =
                            buildNamedReference(selector.referencedNameAsName, selector.toCjPsiSourceElement())
                        explicitReceiver = receiver
                        typeArguments.addAll(typeArgs)
                    }
                }
                return buildNamedAccessExpression {
                    source = psi.toCjPsiSourceElement()
                    calleeReference =
                        buildNamedReference(selector.referencedNameAsName, selector.toCjPsiSourceElement())
                    explicitReceiver = receiver
                }
            }

            return buildErrorExpression(psi.toSourceElement(), "Unsupported selector: ${selector.javaClass.simpleName}")
        }

        private fun convertNameReference(psi: CjNameReferenceExpression): CfirExpression {
            val referencedName = psi.referencedNameAsName
            if (referencedName.asString() == "this" && psi.typeArguments.isEmpty()) {
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
                typeArguments.addAll(psi.typeArguments.map { convertTypeRef(it.typeReference) })
            }
        }

        // ---- Control Flow ----

        private fun convertIf(psi: CjIfExpression): CfirIfExpression {
            val condition = psi.condition?.let { convertExpression(it) }
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

        private fun convertMatch(psi: CjMatchExpression): CfirMatchExpression {
            val subject = psi.subjectExpression?.let { convertExpression(it) }
            val hasSubject = subject != null

            val branches = psi.entries.map { entry ->
                val (pattern, guard) = when {
                    // ── case _ ────────────────────────────────────────────────────────
                    entry.isElse -> {
                        val p = buildWildcardPattern { source = entry.toCjPsiSourceElement() }
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
            val target = CfirLoopTarget(labelName = null)
            val loop = withLoopTarget(target) {
                val body = psi.body?.let { toBlock(it) } ?: buildBlock {
                    source = psi.toCjPsiSourceElement()
                }

                buildForInExpression {
                    source = psi.toCjPsiSourceElement()
                    this.condition = buildLiteralExpression {
                        source = psi.toCjPsiSourceElement()
                        kind = CfirLiteralKind.BOOLEAN
                        value = true
                    }
                    this.isDoWhile = false
                    this.variable = variable
                    this.iterable = iterable
                    this.body = body
                }
            }
            target.bind(loop)
            return loop
        }

        private fun convertWhile(psi: CjWhileExpression): CfirLoopExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing while condition")
            val target = CfirLoopTarget(labelName = null)
            val loop = withLoopTarget(target) {
                buildLoopExpression {
                    source = psi.toCjPsiSourceElement()
                    this.condition = condition
                    this.body = psi.body?.let { toBlock(it) } ?: buildBlock {
                        source = psi.toCjPsiSourceElement()
                    }
                    isDoWhile = false
                }
            }
            target.bind(loop)
            return loop
        }

        private fun convertDoWhile(psi: CjDoWhileExpression): CfirLoopExpression {
            val target = CfirLoopTarget(labelName = null)
            val loop = withLoopTarget(target) {
                val condition = psi.condition?.let { convertExpression(it) }
                    ?: buildErrorExpression(reason = "Missing do-while condition")
                buildLoopExpression {
                    source = psi.toCjPsiSourceElement()
                    this.condition = condition
                    this.body = psi.body?.let { toBlock(it) } ?: buildBlock {
                        source = psi.toCjPsiSourceElement()
                    }
                    isDoWhile = true
                }
            }
            target.bind(loop)
            return loop
        }

        // ---- Jump & Exception ----

        private fun convertReturn(psi: CjReturnExpression): CfirReturnExpression {
            return buildReturnExpressionWithCurrentFunctionTarget(
                source = psi.toCjPsiSourceElement(),
                result = psi.returnedExpression?.let { convertExpression(it) },
            )
        }

        private fun convertThrow(psi: CjThrowExpression): CfirThrowExpression {
            val exception = psi.thrownExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing thrown expression")
            return buildThrowExpression {
                source = psi.toCjPsiSourceElement()
                this.exception = exception
            }
        }

        private fun convertPerform(psi: CjPerformExpression): CfirPerformExpression {
            val effectExpression = psi.expression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing performed expression")
            return buildPerformExpression {
                source = psi.toCjPsiSourceElement()
                expression = effectExpression
            }
        }

        private fun convertResume(psi: CjResumeExpression): CfirResumeExpression {
            return buildResumeExpression {
                source = psi.toCjPsiSourceElement()
                withExpression = psi.withExpression?.let(::convertExpression)
                throwingExpression = psi.throwingExpression?.let(::convertExpression)
            }
        }

        private fun convertCommandTypePattern(psi: CjCommandTypePattern): CfirCommandTypePattern {
            return buildCommandTypePattern {
                source = psi.toCjPsiSourceElement()
                bindingName = psi.bindingName?.let(Name::identifier)
                isWildcard = psi.isWildcard
                typeRefs.addAll(psi.typeReferences.map(::convertTypeRef))
            }
        }

        private fun convertTryResource(psi: CjTryResource): CfirFieldVariable {
            val parameter = psi.parameter
            val resourceName = parameter?.nameAsSafeName ?: Name.special("<error>")
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
                val catchParam = clause.catchParameter
                val catchParamName = catchParam?.name?.let { Name.identifier(it) } ?: Name.special("<error>")
                val catchStatus = cloneDeclarationStatus(CfirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL))
                val parameter =
                    buildSourceDeclaration(CfirPropertySymbol(callableIdFor(catchParamName))) { symbol ->
                        buildProperty {
                            resolvePhase = CfirResolvePhase.RAW_CFIR
                            source = (catchParam ?: clause).toCjPsiSourceElement()
                            this.symbol = symbol
                            origin = CfirDeclarationOrigin.Source
                            moduleData = baseModuleData

                            attributes = CfirDeclarationAttributes.EMPTY
                            isLocal = true
                            dispatchReceiverType = null
                            status = catchStatus
                            returnTypeRef =
                                catchParam?.typeReferences?.firstOrNull()?.let(::convertTypeRef) ?: buildImplicitTypeRef()
                            name = catchParamName
                        }
                    }.also { it.isCatchParameter = true }
                val body = clause.catchBody?.let {
                    if (it is CjBlockExpression) convertBlock(it) else buildBlock {
                        source = it.toCjPsiSourceElement()
                    }
                }
                    ?: buildBlock { source = clause.toCjPsiSourceElement() }
                buildCatch {
                    source = clause.toCjPsiSourceElement()
                    this.parameter = parameter
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

        private fun convertLambda(psi: CjLambdaExpression): CfirAnonymousFunctionExpression {
            val anonymousFunctionSymbol = CfirAnonymousFunctionSymbol()
            val valueParams = psi.valueParameters.map {
                convertValueParameter(it, anonymousFunctionSymbol, requiresExplicitType = false)
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

        private fun convertSubscript(psi: CjArrayAccessExpression): CfirSubscriptExpression {
            val receiver = psi.arrayExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing subscript receiver")
            return buildSubscriptExpression {
                source = psi.toCjPsiSourceElement()
                this.receiver = receiver
                indices.addAll(psi.indexExpressions.map { convertExpression(it) })
            }
        }

        private fun convertArrayLiteral(psi: CjCollectionLiteralExpression): CfirArrayLiteral {
            return buildArrayLiteral {
                source = psi.toCjPsiSourceElement()
                elements.addAll(psi.innerExpressions.map { convertExpression(it) })
            }
        }

        private fun convertTupleLiteral(psi: CjTupleExpression): CfirTupleLiteral {
            return buildTupleLiteral {
                source = psi.toCjPsiSourceElement()
                elements.addAll(psi.expressions.map { convertExpression(it) })
            }
        }

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
                    name = pattern.nameAsSafeName
                    bindingVariable = createPatternBindingVariable(
                        source = pattern.toCjPsiSourceElement(),
                        name = pattern.nameAsSafeName,
                        status = ownerStatus,
                        isLocal = ownerIsLocal,
                        isVar = ownerIsVar,
                        returnTypeRef = buildImplicitTypeRef(),
                    )
                }

                is CjTypePattern -> buildTypePattern {
                    source = pattern.toCjPsiSourceElement()
                    typeRef = convertTypeRef(pattern.typeReference)
                    bindingName = pattern.nameAsName
                    bindingVariable = pattern.nameAsName?.let { name ->
                        createPatternBindingVariable(
                            source = pattern.toCjPsiSourceElement(),
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
                    name = pattern.nameAsSafeName
                    bindingVariable = createPatternBindingVariable(
                        source = pattern.toCjPsiSourceElement(),
                        name = pattern.nameAsSafeName,
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

        private fun cloneDeclarationStatus(status: CfirDeclarationStatus): CfirDeclarationStatusImpl {
            return CfirDeclarationStatusImpl(
                visibility = status.visibility,
                modality = status.modality,
            ).also { copied ->
                copied.isVisibilityExplicit = status.isVisibilityExplicit
                copied.isModalityExplicit = status.isModalityExplicit
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
                copied.isAbstract = status.isAbstract
                copied.isOpen = status.isOpen
                copied.isSealed = status.isSealed
            }
        }

        // ===== 辅助方法 =====

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

        private fun toBlock(psi: CjExpression): CfirBlock {
            if (psi is CjBlockExpression) return convertBlock(psi)
            return buildBlock {
                source = psi.toCjPsiSourceElement()
                statements.add(convertExpression(psi))
            }
        }

        private fun convertTypeRef(psi: CjTypeReference?): CfirTypeRef {
            return psi.toCfirOrImplicitTypeRef { it.toSourceElement() }
        }

        private fun collectTypeConstraintBounds(owner: CjTypeParameterListOwner): Map<Name, List<CfirTypeRef>> {
            if (owner.typeConstraints.isEmpty()) return emptyMap()

            val boundsByParameter = linkedMapOf<Name, MutableList<CfirTypeRef>>()
            for (constraint in owner.typeConstraints) {
                val parameterName = constraint.subjectTypeParameterName?.referencedNameAsName ?: continue
                val boundRefs = constraint.boundTypeReferences
                if (boundRefs.isEmpty()) continue

                boundsByParameter.getOrPut(parameterName) { mutableListOf() }
                    .addAll(boundRefs.map(::convertTypeRef))
            }

            return boundsByParameter
        }

        private fun collectTypeConstraintDiagnosticData(owner: CjTypeParameterListOwner): CfirTypeConstraintDiagnosticData? {
            val typeConstraints = owner.typeConstraints.mapNotNull { constraint ->
                val parameterName = constraint.subjectTypeParameterName?.referencedNameAsName ?: return@mapNotNull null
                val parameterSource =
                    constraint.subjectTypeParameterName?.toCjPsiSourceElement() ?: return@mapNotNull null
                CfirTypeConstraintReference(
                    parameterName = parameterName,
                    source = parameterSource,
                    constraintSource = constraint.toCjPsiSourceElement(),
                )
            }

            if (typeConstraints.isEmpty()) return null

            return CfirTypeConstraintDiagnosticData(
                typeConstraints = typeConstraints,
            )
        }

        private fun collectFunctionBodyDiagnosticData(owner: CjFunction): CfirFunctionBodyDiagnosticData? {
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

        private fun declarationAttributes(owner: CjElement?): CfirDeclarationAttributes {
            var hasAttributes = false
            val attributes = CfirDeclarationAttributes()

            (owner as? CjTypeParameterListOwner)?.let { typeParameterOwner ->
                collectTypeConstraintDiagnosticData(typeParameterOwner)?.let { diagnosticData ->
                    attributes.typeConstraintDiagnosticData = diagnosticData
                    hasAttributes = true
                }
            }

            (owner as? CjFunction)?.let { function ->
                collectFunctionBodyDiagnosticData(function)?.let { diagnosticData ->
                    attributes.functionBodyDiagnosticData = diagnosticData
                    hasAttributes = true
                }
            }

            return if (hasAttributes) attributes else CfirDeclarationAttributes.EMPTY
        }

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
                    typeConstraintBounds[typeParameter.nameAsSafeName].orEmpty()
                )
            }
        }

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
                    typeConstraintBounds[typeParameter.nameAsSafeName].orEmpty()
                )
            }
        }

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
                    typeConstraintBounds[typeParameter.nameAsSafeName].orEmpty()
                )
            }
        }

        private fun convertFunctionTypeParameters(
            psi: CjFunction,
            containingDeclarationSymbol: CfirBasedSymbol<*>,
        ): List<CfirTypeParameter> {
            val typeConstraintBounds = collectTypeConstraintBounds(psi)
            return psi.typeParameters.map { typeParameter ->
                convertTypeParameter(
                    typeParameter,
                    containingDeclarationSymbol,
                    typeConstraintBounds[typeParameter.nameAsSafeName].orEmpty()
                )
            }
        }

        private fun convertSuperTypeRefs(psi: CjClassLikeDeclaration): List<CfirTypeRef> {
            val typeStatement = psi as? CjTypeStatement ?: return emptyList()
            return typeStatement.superTypeListEntries.map { convertTypeRef(it.typeReference) }
        }

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

        private fun convertDeclarationStatus(psi: CjDeclaration): CfirDeclarationStatus {
            val owner = psi as? CjModifierListOwner ?: return CfirDeclarationStatusImpl.DEFAULT
            return convertDeclarationStatus(owner, psi)
        }

        private fun convertMainFunctionStatus(psi: CjMainFunction): CfirDeclarationStatus {
            return convertDeclarationStatus(psi, psi, defaultVisibility = Visibilities.Public)
        }

        private fun convertDeclarationStatus(
            owner: CjModifierListOwner,
            declaration: CjDeclaration,
            defaultVisibility: Visibility? = null,
        ): CfirDeclarationStatus {
            val modifiers = owner.modifierList
            val isVisibilityExplicit = modifiers?.let {
                it.hasModifier(CjTokens.PUBLIC_KEYWORD) ||
                        it.hasModifier(CjTokens.PRIVATE_KEYWORD) ||
                        it.hasModifier(CjTokens.PROTECTED_KEYWORD) ||
                        it.hasModifier(CjTokens.INTERNAL_KEYWORD)
            } == true
            val isModalityExplicit = modifiers?.let {
                it.hasModifier(CjTokens.ABSTRACT_KEYWORD) ||
                        it.hasModifier(CjTokens.OPEN_KEYWORD) ||
                        it.hasModifier(CjTokens.SEALED_KEYWORD)
            } == true
            val effectiveDefaultVisibility = defaultVisibility ?: when {
                context.inLocalContext -> Visibilities.Local
                containerSymbolIfAny is CfirInterfaceSymbol -> Visibilities.Public
                else -> Visibilities.Internal
            }

            return buildDeclarationStatus(
                visibility = when {
                    modifiers?.hasModifier(CjTokens.PUBLIC_KEYWORD) == true -> Visibilities.Public
                    modifiers?.hasModifier(CjTokens.PRIVATE_KEYWORD) == true -> Visibilities.Private
                    modifiers?.hasModifier(CjTokens.PROTECTED_KEYWORD) == true -> Visibilities.Protected
                    modifiers?.hasModifier(CjTokens.INTERNAL_KEYWORD) == true -> Visibilities.Internal
                    else -> effectiveDefaultVisibility
                },
                isVisibilityExplicit = isVisibilityExplicit,
                isModalityExplicit = isModalityExplicit,
                isAbstract = modifiers?.hasModifier(CjTokens.ABSTRACT_KEYWORD) == true,
                isOpen = modifiers?.hasModifier(CjTokens.OPEN_KEYWORD) == true,
                isSealed = modifiers?.hasModifier(CjTokens.SEALED_KEYWORD) == true,
                isStatic = modifiers?.hasModifier(CjTokens.STATIC_KEYWORD) == true,
                isConst = modifiers?.hasModifier(CjTokens.CONST_KEYWORD) == true,
                isMut = modifiers?.hasModifier(CjTokens.MUT_KEYWORD) == true,
                isOverride = modifiers?.hasModifier(CjTokens.OVERRIDE_KEYWORD) == true,
                isRedef = modifiers?.hasModifier(CjTokens.REDEF_KEYWORD) == true,
                isOperator = modifiers?.hasModifier(CjTokens.OPERATOR_KEYWORD) == true,
                isUnsafe = modifiers?.hasModifier(CjTokens.UNSAFE_KEYWORD) == true,
                isForeign = modifiers?.hasModifier(CjTokens.FOREIGN_KEYWORD) == true,
            )
        }
    }

    // ===== 文件级构建辅助 =====

    private fun buildPackageDirective(psi: CjPackageDirective?): CfirPackageDirective {
        val fqName = psi?.fqName ?: FqName.ROOT
        return buildPackageDirective {
            source = psi?.toCjPsiSourceElement()
            packageFqName = fqName
            isMacroPackage = psi?.isMacroPackage == true
        }
    }

    private fun buildImports(file: CjFile): List<CfirImport> {
        val importDirectives = file.importDirectives
        return importDirectives.flatMap { directive ->
            directive.importItems.mapNotNull { item ->
                val fqName = item.importedFqName ?: return@mapNotNull null
                buildImport {
                    source = item.toCjPsiSourceElement()
                    importedFqName = normalizeImportFqName(fqName)
                    isAllUnder = item.isAllUnder
                    aliasName = item.aliasName?.let { Name.identifier(it) }
                }
            }
        }
    }

    private fun normalizeImportFqName(fqName: FqName): FqName {
        val segments = fqName.pathSegments().map { it.asString() }
        for (prefixLength in 1..(segments.size / 2)) {
            val firstPrefix = segments.subList(0, prefixLength)
            val secondPrefix = segments.subList(prefixLength, prefixLength * 2)
            if (firstPrefix == secondPrefix) {
                return FqName((firstPrefix + segments.drop(prefixLength * 2)).joinToString("."))
            }
        }
        return fqName
    }


}

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
