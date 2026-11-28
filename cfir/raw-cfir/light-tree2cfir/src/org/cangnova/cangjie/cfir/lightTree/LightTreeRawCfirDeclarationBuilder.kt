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

package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.parsing.isParsingErrorElementType

import org.cangnova.cangjie.cfir.builder.initializeRawAnnotationSyntax
import org.cangnova.cangjie.cfir.builder.initConstructorCjmpStatus
import org.cangnova.cangjie.cfir.builder.hasCjmpCommonDefault
import org.cangnova.cangjie.cfir.builder.withEmptyFunctionBodyReturnType
import org.cangnova.cangjie.cfir.builder.toParsingContext

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirFeaturesDirective
import org.cangnova.cangjie.cfir.builder.AbstractRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.captureDefaultAssignmentSyntaxFacts
import org.cangnova.cangjie.cfir.builder.CfirImplicitAbstractContainerKind
import org.cangnova.cangjie.cfir.builder.CfirImplicitAbstractMemberFacts
import org.cangnova.cangjie.cfir.builder.CfirImplicitAbstractMemberKind
import org.cangnova.cangjie.cfir.builder.CfirImplicitAbstractStatusResolver
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.Context
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.builder.buildFeaturesDirective
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.builder.macro.MacroExpressionHeadScanner
import org.cangnova.cangjie.cfir.builder.macro.MacroExpressionHeadSyntax as MacroExpressionNameScan
import org.cangnova.cangjie.cfir.builder.macro.MacroExpressionTextRange
import org.cangnova.cangjie.cfir.builder.bindMemberOwners
import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.recordMemberParameterFieldInfo
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.utils.addDefaultBoundIfNecessary
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.CangJieUnicode.sourceIdentifier
import org.cangnova.cangjie.name.CangJieUnicode.normalizeIdentifier
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CjMemberParsingContext
import org.cangnova.cangjie.parsing.CjParameterGrammar
import org.cangnova.cangjie.parsing.CjVariadicParameterRules
import org.cangnova.cangjie.parsing.CjmpModifierDeclarationKind
import org.cangnova.cangjie.parsing.CjmpNominalSyntaxFacts
import org.cangnova.cangjie.parsing.MacroParsingFacts
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.ImportPathPrefix
import org.cangnova.cangjie.resolveImportPath
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.fakeElement

/**
 * LightTree → Raw CFIR 声明构建器（对齐 PsiRawCfirBuilder 的声明转换部分）。
 *
 * 继承 [AbstractRawCfirBuilder]，实现三个模板方法，
 * 通过 `when(node.tokenType)` 手动分发代替 PSI Visitor 模式。
 */
/**
 * LightTree 到 raw CFIR 的声明构建器。
 *
 * 该 builder 对齐 PSI raw builder 的声明转换部分，负责文件、声明、
 * annotation/macro surface、import/package 与声明状态构造。
 *
 * @property baseScopeProvider class-like 声明使用的基础 scope provider。
 * @property bodyBuildingMode body 构建策略。
 */
class LightTreeRawCfirDeclarationBuilder(
    session: CfirSession,
    /** class-like 声明使用的基础 scope provider。 */
    internal val baseScopeProvider: CfirScopeProvider,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    context: Context<LighterASTNode> = Context(),
    /** body 构建策略。 */
    val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
    /**
     * 被解析代码**所属源文件**的种类。
     *
     * 声明文件（`.cj.d`）的语义差异（4.5.2 护栏 1）由它决定：无体函数**不得**被推断为隐式 abstract。
     * 与 [bodyBuildingMode] 正交 —— 后者是"体延迟构建"的性能策略，本参数是"体是否存在"的语言语义。
     *
     * 片段重解析（宏展开产物）默认 [CjSourceKind.SOURCE]：片段不是文件，其种类由来源文件决定，
     * 由调用方按需传入。
     */
    val sourceKind: CjSourceKind = CjSourceKind.SOURCE,
    /** 与当前 LightTree 同次 parser 根冻结的真实 invocation facts。 */
    internal val macroFacts: Map<Int, MacroParsingFacts>,
    /** 与当前 LightTree 同次解析的容器关键字事实。 */
    val nominalFacts: Map<Int, CjmpNominalSyntaxFacts>,
    /** 原始 prefix 与检查三态不能代替声明文法发布的有效属性。 */
    val declarationModifiers: Map<Int, org.cangnova.cangjie.parsing.CjParsedDeclarationModifiers>,
    /** 与当前文件及其宏片段共享的私有 metadata 事务。 */
    val annotationMetadata: CfirAnnotationMetadataBuilder,
) : AbstractLightTreeRawCfirBuilder(session, tree, source, context) {
    /** 返回属性已经包含声明实际接收的集合/宏前缀，不从外层 owner 补授。 */
    private fun modifierListFor(node: LighterASTNode, isPrimaryConstructorParameter: Boolean? = null): LightTreeModifierList {
        return LightTreeModifierList.from(tree, node, declarationModifiers, isPrimaryConstructorParameter)
    }

    /**
     * Macro construction-only surface 累加器（baseline Batch 4b）。
     *
     * 由 [LightTreeRawCfirExpressionBuilder.convertMacroExpression] push；
     * 上层 raw-build 入口经 [consumeCollectedMacroSurfaces] 取出并交给
     * `org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles`。
     */
    internal val collectedMacroSurfaces: MutableList<MacroSurface> = mutableListOf()

    /** 提取并清空当前累加的 macro surface 列表。 */
    fun consumeCollectedMacroSurfaces(): List<MacroSurface> {
        val snapshot = collectedMacroSurfaces.toList()
        collectedMacroSurfaces.clear()
        return snapshot
    }

    /** 按当前 local/interface 上下文把 LightTree modifier list 转换为声明状态。 */
    private fun LightTreeModifierList.toDeclarationStatusForCurrentContext(
        defaultVisibility: Visibility? = null,
        isDefault: Boolean = false,
        isImplicitAbstract: Boolean = false,
    ): CfirDeclarationStatus {
        val inInterfaceContext = !context.inLocalContext && context.containingClassSymbolIfAny is CfirInterfaceSymbol
        return toDeclarationStatus(
            context.inLocalContext,
            inInterfaceContext,
            defaultVisibility,
            isDefault,
            isImplicitAbstract,
            isSpecificDeclaration = cjmpDeclarationRole().isSpecific,
        )
    }

    /** 按当前语言版本把早期 platform 与稳定 specific 修饰符归一到共享声明角色。 */
    private fun LightTreeModifierList.cjmpDeclarationRole() = declarationRole

    /** macro annotation surface 的宿主种类。 */
    private enum class MacroSurfaceOwnerKind {
        /** annotation 附着在声明上。 */
        DECLARATION,
        /** annotation 附着在参数上。 */
        PARAMETER,
    }

    // ===== AbstractRawCfirBuilder 抽象方法实现 =====

    // ===== 表达式构建器（延迟初始化，解决循环依赖） =====

    /** 表达式构建器，延迟初始化以避免声明/表达式 builder 循环依赖。 */
    val expressionBuilder: LightTreeRawCfirExpressionBuilder by lazy {
        LightTreeRawCfirExpressionBuilder(baseSession, tree, source, context, this)
    }

    // ===== Public API =====

    /** 完整文件构建必须经过 LightTree2Cfir 的 parser 结果入口。 */
    override fun buildFile(file: LighterASTNode): CfirFile {
        error("Use LightTree2Cfir with ParsedCangJieLightFile for LightTree file conversion")
    }

    /** 仅文件级入口可以调用；由它把同次 parser facts 一次发布到生成文件。 */
    internal fun buildCfirFile(
        file: LighterASTNode,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): CfirFile {
        // 解析包名
        val packageNode = tree.findChildByType(file, CjNodeTypes.PACKAGE_DIRECTIVE)
        val packageFqName = packageNode?.let { extractPackageFqName(it) } ?: FqName.ROOT
        val packageDirective = buildPackageDirectiveNode(packageNode, packageFqName)
        val sourceModuleName = BuiltInAnnotationRegistry.sourceModuleName(packageFqName, packageDirective.organizationName)

        return context.withSourceModuleName(sourceModuleName) {
            withPackageContext(packageFqName) {
                val symbol = CfirFileSymbol()
                buildSourceDeclaration(symbol) { fileSymbol ->
                    buildFile {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = file.toSource()
                        this.symbol = fileSymbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        attributes = CfirDeclarationAttributes.EMPTY
                        name = sourceFile.name
                        this.sourceFile = sourceFile
                        this.sourceFileLinesMapping = linesMapping

                        featuresDirective = tree.findChildByType(file, CjNodeTypes.FEATURES_DIRECTIVE)
                            ?.let { buildFeaturesDirectiveNode(it, fileSymbol) }
                        this.packageDirective = packageDirective
                        imports.addAll(buildImportsFromFile(file))
                        declarations.addAll(buildFileDeclarations(file))
                    }
                }
            }
        }
    }

    /** 从 LightTree 声明节点构建 raw CFIR 声明。 */
    override fun buildDeclaration(declaration: LighterASTNode): CfirDeclaration =
        convertDeclaration(declaration)

    /** 将 LightTree 文件前导 `features` 转换为独立 CFIR metadata 节点。 */
    private fun buildFeaturesDirectiveNode(
        node: LighterASTNode,
        containingSymbol: CfirBasedSymbol<*>,
    ): CfirFeaturesDirective = buildFeaturesDirective {
        source = node.toSource()

        val annotationsNode = tree.findChildByType(node, CjStubElementTypes.ANNOTATIONS)
        if (annotationsNode != null) {
            tree.forEachChildren(annotationsNode) { annotation ->
                if (annotation.tokenType != CjStubElementTypes.ANNOTATION) return@forEachChildren
                val rawName = annotationNameInfo(annotation)?.rawName ?: return@forEachChildren
                annotations.add(buildRawAnnotationCall(annotation, rawName, containingSymbol))
            }
        }

        val featureSet = tree.findChildByType(node, CjNodeTypes.FEATURES_SET)
        if (featureSet != null) {
            tree.forEachChildren(featureSet) { featureId ->
                if (featureId.tokenType == CjNodeTypes.FEATURE_ID) {
                    featureIds.add(featureId.asText().filterNot(Char::isWhitespace))
                }
            }
        }
    }

    /**
     * 宏返回的声明片段从根节点收集，不能通过递归寻找终端声明丢弃外层注解包装。
     * 片段的新 token 没有 parser 模块，符号名称仍使用宿主包。
     */
    fun buildDeclarationFragment(file: LighterASTNode, ownerSnapshot: MacroRawOwnerSnapshot): CfirDeclaration? =
        context.withMacroOwner(ownerSnapshot) {
            buildFileDeclarations(file).map { it.bindMacroFragmentOwner() }.singleOrNull()
        }

    /**
     * 转换原生 macro fragment 的全部直接 LightTree 根，按 parser 顺序保留类型。
     * 参数签名取自实际替换槽；声明和表达式都在宿主 owner 语境下构造。
     */
    internal fun buildMacroFragmentPayload(
        roots: List<LighterASTNode>,
        parsingInfo: org.cangnova.cangjie.parsing.CjMacroFragmentParseInfo,
        ownerSnapshot: MacroRawOwnerSnapshot,
    ): MacroFragmentPayload? = context.withMacroOwner(ownerSnapshot) {
        val entryRole = requireNotNull(parsingInfo.entryFileRole) { "Macro fragment has no verified entry role." }
        val targetContext = baseSession.cjmpSettings.toParsingContext(baseSession.languageVersionSettings)
            .copy(initialFileRole = entryRole)
        val parsedContext = parsingInfo.parsingContext as? org.cangnova.cangjie.parsing.CjmpParsingContext.Compilation
        require(parsedContext?.copy(initialFileRole = entryRole) == targetContext) {
            "Macro fragment must be parsed in the target session context before raw construction."
        }
        val convertedRoots = mutableListOf<MacroFragmentPayload.Root>()
        for ((index, root) in roots.withIndex()) {
            val converted = when {
                root.tokenType == CjNodeTypes.VALUE_PARAMETER -> {
                    val signature = ownerSnapshot.signature as? MacroParameterSignature.Declaration
                        ?: return@withMacroOwner null
                    MacroFragmentPayload.Root.Parameter(index, convertValueParameter(root, signature.owner, signature))
                }
                LightTreeRawCfirExpressionBuilder.isDeclarationToken(root.tokenType) ->
                    MacroFragmentPayload.Root.Declaration(
                        index,
                        convertDeclaration(root).bindMacroFragmentOwner(),
                    )
                LightTreeRawCfirExpressionBuilder.isExpressionToken(root.tokenType) ->
                    MacroFragmentPayload.Root.Expression(index, expressionBuilder.convertExpression(root))
                else -> return@withMacroOwner null
            }
            convertedRoots += converted
        }
        MacroFragmentPayload(convertedRoots, parsingInfo)
    }

    /** 从 LightTree 表达式节点构建 raw CFIR 表达式。 */
    override fun buildExpression(expression: LighterASTNode): CfirExpression =
        expressionBuilder.convertExpression(expression)

    /**
     * Macro expression fragment reparse 入口。
     */
    fun buildExpressionFragment(expression: LighterASTNode, ownerSnapshot: MacroRawOwnerSnapshot): CfirExpression {
        return context.withMacroOwner(ownerSnapshot) {
            expressionBuilder.convertExpression(expression)
        }
    }

    /**
     * Macro parameter fragment reparse 入口。
     *
     * 参数 fragment 必须复用原宿主 callable symbol；不能把 wrapper 函数的
     * containing symbol 带入最终 CFIR。
     */
    fun buildValueParameterFragment(
        parameter: LighterASTNode,
        ownerSnapshot: MacroRawOwnerSnapshot,
    ): CfirValueParameter {
        return context.withMacroOwner(ownerSnapshot) {
            val signature = checkNotNull(ownerSnapshot.signature) { "A parameter fragment must have its actual signature owner." }
            require(signature is MacroParameterSignature.Declaration) { "Lambda parameters have no declaration macro grammar." }
            convertValueParameter(parameter, signature.owner, signature)
        }
    }

    /**
     * Macro custom annotation fragment reparse 入口。
     *
     * 直接从 annotation light-tree node 构造 [CfirAnnotationCall]，不通过
     * 临时参数 fragment 间接提取 annotation，避免 annotation slot 语法被参数语法改写。
     */
    fun buildAnnotationCallFragment(
        annotation: LighterASTNode,
        containingSymbol: CfirBasedSymbol<*>,
        ownerSnapshot: MacroRawOwnerSnapshot,
        sourceOverride: CjSourceElement? = null,
        argumentListSourceOverride: CjSourceElement? = null,
    ): CfirAnnotationCall? = context.withMacroOwner(ownerSnapshot) {
        buildAnnotationCall(annotation, containingSymbol, sourceOverride, argumentListSourceOverride)
    }

    /** 普通 raw annotation/lambda 构建复用当前语境，不能重装 fragment 的宿主快照。 */
    internal fun buildAnnotationCall(
        annotation: LighterASTNode,
        containingSymbol: CfirBasedSymbol<*>,
        sourceOverride: CjSourceElement? = null,
        argumentListSourceOverride: CjSourceElement? = null,
    ): CfirAnnotationCall? {
        val annotationName = annotationNameInfo(annotation) ?: return null
        val sourceOffsetDelta = sourceOffsetDelta(sourceOverride, annotation)
        return buildRawAnnotationCall(
            annotation = annotation,
            rawName = annotationName.rawName,
            containingSymbol = containingSymbol,
            sourceOverride = sourceOverride,
            typeRefOverride = buildAnnotationTypeRef(annotationName, sourceOffsetDelta),
            calleeReferenceSourceOverride = annotationName.calleeReferenceSource.shiftedBy(sourceOffsetDelta),
            argumentListSourceOverride = argumentListSourceOverride,
        )
    }

    // ===== 声明转换入口 =====

    /** 按 LightTree 声明节点 token type 分派到具体声明转换函数。 */
    fun convertDeclaration(node: LighterASTNode): CfirDeclaration = convertDeclaration(node, emptyList())

    private fun convertDeclaration(node: LighterASTNode, annotationWrappers: List<LighterASTNode>): CfirDeclaration {
        val declaration = when (node.tokenType) {
            CjNodeTypes.INVALID_DECLARATION -> buildSourceDeclaration(CfirInvalidDeclarationSymbol()) { symbol ->
                buildInvalidDeclaration {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    reason = "Invalid declaration in source syntax"
                }
            }
            CjNodeTypes.CLASS -> convertClass(node, CfirClassKind.CLASS)
            CjNodeTypes.INTERFACE -> convertClass(node, CfirClassKind.INTERFACE)
            CjNodeTypes.STRUCT -> convertClass(node, CfirClassKind.STRUCT)
            CjNodeTypes.ENUM -> convertClass(node, CfirClassKind.ENUM)
            CjNodeTypes.EXTEND -> convertExtend(node)
            CjNodeTypes.FUNC -> convertFunction(node)
            CjNodeTypes.MAIN_FUNC -> convertMainFunction(node)
            CjNodeTypes.MACRO -> convertMacroDeclaration(node)
            CjNodeTypes.FINALIZER -> convertFinalizer(node)
            CjNodeTypes.PROPERTY -> convertProperty(node)
            CjNodeTypes.FIELD -> convertFieldVariable(node)
            CjNodeTypes.VARIABLE -> convertPatternVariable(node)
            CjNodeTypes.PRIMARY_CONSTRUCTOR -> convertConstructor(node, isPrimary = true)
            CjNodeTypes.SECONDARY_CONSTRUCTOR -> convertConstructor(node, isPrimary = false)
            CjNodeTypes.TYPEALIAS -> convertTypeAlias(node)
            CjNodeTypes.ENUM_CONSTRUCTOR -> convertEnumConstructor(node)
            else -> buildSourceDeclaration(CfirInvalidDeclarationSymbol()) { symbol ->
                buildInvalidDeclaration {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    reason = "Unsupported declaration: ${node.tokenType}"
                }
            }
        }
        collectDeclarationAnnotationSurfaces(node, declaration, annotationWrappers)
        return declaration
    }

    /** 按源码顺序一次登记包装层前缀、宏自身及最终声明注解，禁止重解析已有 input 子树。 */
    private fun collectDeclarationAnnotationSurfaces(
        node: LighterASTNode,
        declaration: CfirDeclaration,
        annotationWrappers: List<LighterASTNode>,
    ) {
        for (wrapper in annotationWrappers) {
            collectMacroSurfacesFromAnnotations(
                wrapper, modifierListFor(wrapper), MacroSurfaceOwnerKind.DECLARATION, declaration,
            )
            applyTopLevelMacroExpression(wrapper, node, declaration)
        }
        collectMacroSurfacesFromAnnotations(
            node, modifierListFor(node), MacroSurfaceOwnerKind.DECLARATION, declaration,
        )
    }

    /**
     * 将 block 中的 annotation/declaration macro 恢复为 declaration carrier。
     *
     * LightTree 的 `MACRO_EXPRESSION` 同时用于表达式 macro 和 `@Anno decl`。
     * statement builder 必须先尝试完整 declaration chain，才能为 local declaration
     * 建立 annotation slot snapshot；无法形成 declaration chain 时由调用方继续按
     * expression macro 处理。
     */
    fun convertMacroDeclarationIfPresent(node: LighterASTNode): CfirDeclaration? {
        if (node.tokenType != CjNodeTypes.MACRO_EXPRESSION) return null
        return convertTopLevelMacroDeclaration(node)
    }

    // ===== 类/接口/结构体/枚举 =====

    /** 转换 class、interface、struct、enum 等 class-like 声明。 */
    private fun convertClass(node: LighterASTNode, classKind: CfirClassKind): CfirDeclaration {
        val name = extractName(node)
        val modifiers = modifierListFor(node)
        val superTypes = extractSuperTypeRefs(node)

        if (!canDeclareTopLevelClassLike()) {
            return buildInvalidClassLikeDeclaration(
                source = node.toSource(),
                kind = classKind.name.lowercase(),
                name = name,
            )
        }

        val classId = topLevelClassId(name)
        return context.withNominalSyntax({
            CjMemberParsingContext(
                kind = when (classKind) {
                    CfirClassKind.CLASS -> CjMemberParsingContext.Kind.CLASS
                    CfirClassKind.STRUCT -> CjMemberParsingContext.Kind.STRUCT
                    CfirClassKind.INTERFACE -> CjMemberParsingContext.Kind.INTERFACE
                    CfirClassKind.ENUM -> CjMemberParsingContext.Kind.ENUM
                },
                declarationName = tree.findChildByType(node, CjTokens.IDENTIFIER)?.asText(),
            )
        }) {
            when (classKind) {
                CfirClassKind.CLASS -> buildSourceDeclaration(CfirClassSymbol(classId)) { symbol ->
                    buildClass {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (typeParams, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = extractTypeParameters(node, symbol)
                            val declarations = withClassLikeMembers(
                                symbol.rawDispatchReceiverType(typeParameters), node.toSource(), typeParameters,
                                isConstructible = true,
                            ) {
                                withEnclosingClassModifiers(modifiers) { extractClassMembers(node) }.toMutableList().also { declarations ->
                                    if (shouldSynthesizeImplicitPrimaryConstructor(
                                            isSpecific = modifiers.cjmpDeclarationRole().isSpecific,
                                            declarations = declarations,
                                        )
                                    ) {
                                        declarations.add(buildImplicitPrimaryConstructor(node))
                                    }
                                }
                            }
                            typeParameters to declarations
                        }
                        source = node.toSource()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(node)
                        status = modifiers.toDeclarationStatusForCurrentContext()
                        this.typeParameters.addAll(typeParams)
                        this.superTypeRefs.addAll(superTypes)
                        this.declarations.addAll(classDeclarations)
                        this.name = name
                    }
                }
                CfirClassKind.INTERFACE -> buildSourceDeclaration(CfirInterfaceSymbol(classId)) { symbol ->
                    buildInterface {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (typeParams, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = extractTypeParameters(node, symbol)
                            val declarations = withClassLikeMembers(
                                symbol.rawDispatchReceiverType(typeParameters), node.toSource(), typeParameters,
                                isConstructible = false,
                            ) {
                                extractClassMembers(node).toMutableList()
                            }
                            typeParameters to declarations
                        }
                        source = node.toSource()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(node)
                        status = modifiers.toDeclarationStatusForCurrentContext()
                        this.typeParameters.addAll(typeParams)
                        this.superTypeRefs.addAll(superTypes)
                        this.declarations.addAll(classDeclarations)
                        this.name = name
                    }
                }
                CfirClassKind.STRUCT -> buildSourceDeclaration(CfirStructSymbol(classId)) { symbol ->
                    buildStruct {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (typeParams, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = extractTypeParameters(node, symbol)
                            val declarations = withClassLikeMembers(
                                symbol.rawDispatchReceiverType(typeParameters), node.toSource(), typeParameters,
                                isConstructible = true,
                            ) {
                                extractClassMembers(node).toMutableList().also { declarations ->
                                    if (shouldSynthesizeImplicitPrimaryConstructor(
                                            isSpecific = modifiers.cjmpDeclarationRole().isSpecific,
                                            declarations = declarations,
                                        )
                                    ) {
                                        declarations.add(buildImplicitPrimaryConstructor(node))
                                    }
                                }
                            }
                            typeParameters to declarations
                        }
                        source = node.toSource()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(node)
                        status = modifiers.toDeclarationStatusForCurrentContext()
                        this.typeParameters.addAll(typeParams)
                        this.superTypeRefs.addAll(superTypes)
                        this.declarations.addAll(classDeclarations)
                        this.name = name
                    }
                }
                CfirClassKind.ENUM -> buildSourceDeclaration(CfirEnumSymbol(classId)) { symbol ->
                    buildEnum {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        val (typeParams, classDeclarations) = withContainerSymbol(symbol) {
                            val typeParameters = extractTypeParameters(node, symbol)
                            // 官方 EnumDecl 只有命名构造项，没有 init；不合成隐式主构造。
                            val declarations = withClassLikeMembers(
                                symbol.rawDispatchReceiverType(typeParameters), node.toSource(), typeParameters,
                                isConstructible = true,
                            ) {
                                extractClassMembers(node).toMutableList()
                            }
                            typeParameters to declarations
                        }
                        source = node.toSource()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        scopeProvider = baseScopeProvider
                        attributes = declarationAttributes(node)
                        status = modifiers.toDeclarationStatusForCurrentContext()
                        this.typeParameters.addAll(typeParams)
                        this.superTypeRefs.addAll(superTypes)
                        this.declarations.addAll(classDeclarations)
                        this.name = name
                        this.isRefEnum = false
                        val enumBody = tree.findChildByType(node, CjNodeTypes.ENUM_BODY)
                        this.isNonExhaustive = enumBody != null && tree.findChildByType(enumBody, CjTokens.ELLIPSIS) != null
                    }.also { it.initConstructorCjmpStatus() }
                }
            }
        }
    }

    /** 为没有显式构造函数的 class-like 声明构造隐式主构造。 */
    private fun buildImplicitPrimaryConstructor(ownerNode: LighterASTNode): CfirConstructor {
        val constructedClassTypeParameters = constructorTypeParametersFromConstructedClass()
        return buildSourceDeclaration(CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))) { symbol ->
            org.cangnova.cangjie.cfir.builder.buildImplicitPrimaryConstructor(symbol,
                ownerNode.toSource().fakeElement(CjFakeSourceElementKind.ImplicitConstructor), baseModuleData,
                context.inLocalContext, currentDispatchReceiverType(), constructedClassTypeParameters)
        }.also { it.initContainingClassAttr() }
    }

    // ===== Extend =====

    /** 转换 extend 声明，保留扩展目标类型、约束与成员声明。 */
    private fun convertExtend(node: LighterASTNode): CfirExtend {
        val modifiers = modifierListFor(node)

        // 提取 extendedTypeRef（第一个 TYPE_REFERENCE）
        var extendedTypeRefNode: LighterASTNode? = null
        val superTypeNodes = mutableListOf<LighterASTNode>()

        val superTypeList = tree.findChildByType(node, CjNodeTypes.SUPER_TYPE_LIST)
        if (superTypeList != null) {
            tree.forEachChildren(superTypeList) { child ->
                if (child.tokenType == CjNodeTypes.SUPER_TYPE_ENTRY) {
                    val typeRef = tree.findChildByType(child, CjNodeTypes.TYPE_REFERENCE)
                    if (typeRef != null) superTypeNodes.add(typeRef)
                }
            }
        }

        // 查找 extend 的目标类型（第一个 TYPE_REFERENCE，在 SUPER_TYPE_LIST 之前）
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjNodeTypes.TYPE_REFERENCE && extendedTypeRefNode == null) {
                extendedTypeRefNode = child
            }
        }

        val extendedType = convertTypeRef(extendedTypeRefNode)
        val superTypes = superTypeNodes.map { convertTypeRef(it) }

        val extend = buildSourceDeclaration(CfirExtendSymbol()) { symbol ->
            buildExtend {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                val (typeParams, members) = withContainerSymbol(symbol) {
                    context.withNominalSyntax({ CjMemberParsingContext(CjMemberParsingContext.Kind.EXTEND, null) }) {
                        val typeParameters = extractTypeParameters(node, symbol)
                        val declarations = withCapturedTypeParameters(false, node.toSource(), typeParameters) {
                            extractClassMembers(node)
                        }
                        typeParameters to declarations
                    }
                }
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = declarationAttributes(node)
                status = modifiers.toDeclarationStatusForCurrentContext()
                this.typeParameters.addAll(typeParams)
                this.extendedTypeRef = extendedType
                this.superTypeRefs.addAll(superTypes)
                this.declarations.addAll(members)
            }
        }
        extend.bindMemberOwners()
        return extend
    }

    // ===== 函数 =====

    /** 转换普通命名函数声明。 */
    private fun convertFunction(node: LighterASTNode): CfirFunction {
        val name = extractFunctionName(node, countValueParameters(node))
        val functionSymbol = CfirNamedFunctionSymbol(callableIdFor(name))
        return withContainerSymbol(functionSymbol) {
            val typeParams = extractFunctionTypeParameters(node, functionSymbol)
            val valueParams = extractValueParameters(node, functionSymbol, MacroParameterSignature.Declaration(containerSymbol, CjParameterGrammar.REGULAR_TYPED))
            val hasVariableLenArg = hasVariableLengthArgument(node)
            val modifiers = modifierListFor(node)
            val returnTypeRef = extractReturnTypeRef(node).withEmptyFunctionBodyReturnType(hasEmptyFunctionBlock(node))
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = withCapturedTypeParameters(true, node.toSource(), typeParams) {
                withFunctionTarget(functionTarget) { extractBody(node) }
            }

            buildSourceDeclaration(functionSymbol) { symbol ->
                buildNamedFunction {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = declarationAttributes(node)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = run {
                        // DEFAULT 与隐式 abstract 同源（护栏 2）：官方 SetDefaultFunc 读的是
                        // CheckFuncBody 的结果，因此这里只算一次，两边共用。
                        val implicitAbstract = isImplicitAbstractClassLikeFunction(node, modifiers)
                        modifiers.toDeclarationStatusForCurrentContext(
                            isDefault = isDefaultInterfaceFunction(node, modifiers, implicitAbstract),
                            isImplicitAbstract = implicitAbstract,
                        )
                    }
                    this.typeParameters.addAll(typeParams)
                    this.returnTypeRef = returnTypeRef
                    this.name = name
                    this.valueParameters.addAll(valueParams)
                    this.hasVariableLenArg = hasVariableLenArg
                    this.body = body
                    isMut = modifiers.isMut
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }
    }

    /** 转换仓颉入口 main 函数声明。 */
    private fun convertMainFunction(node: LighterASTNode): CfirMainFunction {
        val modifiers = modifierListFor(node)
        val functionSymbol = CfirMainFunctionSymbol(callableIdFor(Name.identifier("main")))
        return withContainerSymbol(functionSymbol) {
            val valueParams = extractValueParameters(node, functionSymbol, MacroParameterSignature.Declaration(containerSymbol, CjParameterGrammar.REGULAR_TYPED))
            val returnTypeRef = extractReturnTypeRef(node)
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = withCapturedTypeParameters(true, node.toSource(), emptyList()) {
                withFunctionTarget(functionTarget) { extractBody(node) }
            }

            buildSourceDeclaration(functionSymbol) { symbol ->
                buildMainFunction {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = modifiers.toDeclarationStatusForCurrentContext(defaultVisibility = Visibilities.Public)
                    this.returnTypeRef = returnTypeRef
                    this.valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }
    }

    /** 转换 macro declaration。 */
    private fun convertMacroDeclaration(node: LighterASTNode): CfirMacroDeclaration {
        val name = extractName(node)
        val modifiers = modifierListFor(node)
        val functionSymbol = CfirMacroDeclarationSymbol(callableIdFor(name))
        return withContainerSymbol(functionSymbol) {
            val valueParams = extractValueParameters(node, functionSymbol, MacroParameterSignature.Declaration(containerSymbol, CjParameterGrammar.REGULAR_TYPED))
            val returnTypeRef = extractReturnTypeRef(node)
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = withCapturedTypeParameters(true, node.toSource(), emptyList()) {
                withFunctionTarget(functionTarget) { extractBody(node) }
            }

            buildSourceDeclaration(functionSymbol) { symbol ->
                buildMacroDeclaration {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = modifiers.toDeclarationStatusForCurrentContext()
                    this.returnTypeRef = returnTypeRef
                    this.name = name
                    this.valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }
    }

    /** 转换 finalizer 声明。 */
    private fun convertFinalizer(node: LighterASTNode): CfirFinalizer {
        val modifiers = modifierListFor(node)
        val functionSymbol = CfirFinalizerSymbol(callableIdFor(SpecialNames.END_INIT))
        return withContainerSymbol(functionSymbol) {
            val typeParams = extractFunctionTypeParameters(node, functionSymbol)
            val valueParams = extractValueParameters(node, functionSymbol, MacroParameterSignature.Declaration(containerSymbol, CjParameterGrammar.REGULAR_TYPED))
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = withCapturedTypeParameters(true, node.toSource(), typeParams) {
                withFunctionTarget(functionTarget) { extractBody(node) }
            }

            buildSourceDeclaration(functionSymbol) { symbol ->
                buildFinalizer {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = declarationAttributes(node)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = modifiers.toDeclarationStatusForCurrentContext()
                    this.typeParameters.addAll(typeParams)
                    returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef(source)
                    this.valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }
    }

    // ===== 属性/字段/变量 =====

    /** 转换属性声明；无有效名称时显式构造 invalid declaration。 */
    private fun convertProperty(node: LighterASTNode): CfirDeclaration {
        val name = extractPropertyName(node)
        if (name.isSpecial) {
            return buildSourceDeclaration(CfirInvalidDeclarationSymbol()) { symbol ->
                buildInvalidDeclaration {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    reason = "Property declaration has no valid name"
                }
            }
        }

        val modifiers = modifierListFor(node)
        val typeRef = extractReturnTypeRef(node)
        val accessors = extractPropertyAccessorNodes(node)
        val propertySymbol = CfirPropertySymbol(callableIdFor(name))
        return withContainerSymbol(propertySymbol) {
            val getter = accessors.firstOrNull(::isGetterAccessor)?.let { accessorNode ->
                convertPropertyAccessor(
                    node = accessorNode,
                    accessorName = Name.special("<get-${name.asString()}>"),
                    propertyTypeRef = typeRef,
                    propertySymbol = propertySymbol,
                )
            }
            val setter = accessors.firstOrNull { accessorNode -> !isGetterAccessor(accessorNode) }?.let { accessorNode ->
                convertPropertyAccessor(
                    node = accessorNode,
                    accessorName = Name.special("<set-${name.asString()}>"),
                    propertyTypeRef = typeRef,
                    propertySymbol = propertySymbol,
                )
            }

            buildSourceDeclaration(propertySymbol) { symbol ->
                buildProperty {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = declarationAttributes(node)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = run {
                        // 同函数侧：DEFAULT 与隐式 abstract 同源（护栏 2）。
                        val implicitAbstract = isImplicitAbstractClassLikeProperty(node, modifiers, accessors)
                        modifiers.toDeclarationStatusForCurrentContext(
                            isDefault = isDefaultInterfaceProperty(modifiers, implicitAbstract),
                            isImplicitAbstract = implicitAbstract,
                        )
                    }
                    this.returnTypeRef = typeRef
                    this.name = name
                    this.getter = getter
                    this.setter = setter
                }
            }
        }
    }

    /**
     * LightTree 路径同样需要为 getter / setter 保留独立的函数声明节点，
     * 以支撑公开 accessor symbol 的 PSI 入口与 pointer/originalPsi 协议。
     */
    private fun convertPropertyAccessor(
        node: LighterASTNode,
        accessorName: Name,
        propertyTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
    ): CfirPropertyAccessor {
        val modifiers = modifierListFor(node)
        val accessorSymbol = CfirPropertyAccessorSymbol()
        return withContainerSymbol(accessorSymbol) {
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = withFunctionTarget(functionTarget) { extractBody(node) }
            val isGetter = isGetterAccessor(node)
            val explicitReturnTypeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)?.let(::convertTypeRef)
            val valueParameters = extractValueParameters(
                node,
                accessorSymbol,
                MacroParameterSignature.Declaration(containerSymbol, CjParameterGrammar.ACCESSOR_NAME_ONLY),
            )
            if (!isGetter) {
                valueParameters.firstOrNull()
                    ?.takeIf { it.returnTypeRef is CfirImplicitTypeRef }
                    ?.replaceReturnTypeRef(propertyTypeRef)
            }
            val source = node.toSource()

            buildSourceDeclaration(accessorSymbol) { symbol ->
                buildPropertyAccessor {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    this.source = source
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = declarationAttributes(node)
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = modifiers.toDeclarationStatusForCurrentContext(
                        isDefault = isDefaultInterfaceAccessor(node, modifiers),
                    )
                    returnTypeRef = explicitReturnTypeRef
                        ?: if (isGetter) propertyTypeRef else ConePrimitiveType.UNIT.toCfirResolvedTypeRef(source)
                    this.propertySymbol = propertySymbol
                    this.isGetter = isGetter
                    this.valueParameters.addAll(valueParameters)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }
    }

    /** 转换字段变量声明。 */
    private fun convertFieldVariable(node: LighterASTNode): CfirFieldVariable {
        val name = extractName(node)
        val modifiers = modifierListFor(node)
        val typeRef = extractReturnTypeRef(node)
        val initializer = buildVariableInitializer(node)
        val isVar = hasVarKeyword(node)

        return buildSourceDeclaration(CfirFieldVariableSymbol(callableIdFor(name))) { symbol ->
            buildFieldVariable {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = modifiers.toDeclarationStatusForCurrentContext()
                    .withConstDeclarationKeyword(hasConstKeyword(node))
                this.returnTypeRef = typeRef
                this.isTypeImplicit = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE) == null
                this.name = name
                this.initializer = initializer
                this.isVar = isVar
            }
        }
    }

    /** 转换 pattern variable 声明。 */
    private fun convertPatternVariable(node: LighterASTNode): CfirPatternVariable {
        val modifiers = modifierListFor(node)
        val typeRef = extractReturnTypeRef(node)
        val patternNode = findPatternChild(node)
        val status = modifiers.toDeclarationStatusForCurrentContext()
            .withConstDeclarationKeyword(hasConstKeyword(node))
        val pattern = patternNode?.let {
            expressionBuilder.convertPattern(
                node = it,
                ownerStatus = status,
                ownerIsLocal = context.inLocalContext,
                ownerIsVar = hasVarKeyword(node),
            )
        }
            ?: org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern { source = node.toSource() }
        val initializer = buildVariableInitializer(node)
        val isVar = hasVarKeyword(node)

        return buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(Name.special("<pattern-variable>")))) { symbol ->
            buildPatternVariable {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = declarationAttributes(node)
                isLocal = context.inLocalContext
                this.status = status
                this.returnTypeRef = typeRef
                this.isTypeImplicit = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE) == null
                this.pattern = pattern
                this.initializer = initializer
                this.isVar = isVar
            }
        }
    }

    // ===== 构造器 =====

    /** 转换主构造或次构造函数声明。 */
    private fun convertConstructor(node: LighterASTNode, isPrimary: Boolean): CfirConstructor {
        val modifiers = modifierListFor(node)
        val constructorSymbol = CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))
        // 实例构造器借用所属名义类型的参数；static init 不借用。须在自身容器帧入栈前读取。
        val constructedClassTypeParameters = if (modifiers.isStatic) emptyList()
            else constructorTypeParametersFromConstructedClass()
        return withContainerSymbol(constructorSymbol) {
            // 自有泛型是 cjc sema_forbid_generic_constructor 的错误输入；保留声明以供解析与诊断。
            val typeParams = constructedClassTypeParameters + extractFunctionTypeParameters(node, constructorSymbol)
            val valueParams = extractValueParameters(node, constructorSymbol, context.constructorParameterSignature(containerSymbol, isPrimary))
            val hasVariableLenArg = hasVariableLengthArgument(node)
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = withFunctionTarget(functionTarget) { extractBody(node) }

            buildSourceDeclaration(constructorSymbol) { symbol ->
                if (isPrimary) {
                    buildPrimaryConstructor {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = node.toSource()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        attributes = declarationAttributes(node)
                        isLocal = context.inLocalContext
                        dispatchReceiverType = currentDispatchReceiverType()
                        status = modifiers.toDeclarationStatusForCurrentContext()
                        this.typeParameters.addAll(typeParams)
                        returnTypeRef = buildImplicitTypeRef()
                        this.valueParameters.addAll(valueParams)
                        this.hasVariableLenArg = hasVariableLenArg
                        this.body = body
                    }
                } else {
                    buildConstructor {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = node.toSource()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        attributes = declarationAttributes(node)
                        isLocal = context.inLocalContext
                        dispatchReceiverType = currentDispatchReceiverType()
                        status = modifiers.toDeclarationStatusForCurrentContext()
                        this.typeParameters.addAll(typeParams)
                        returnTypeRef = buildImplicitTypeRef()
                        this.valueParameters.addAll(valueParams)
                        this.hasVariableLenArg = hasVariableLenArg
                        this.body = body
                    }
                }
            }.also {
                it.initContainingClassAttr()
                bindFunctionTarget(functionTarget, it)
            }
        }
    }

    // ===== 类型别名 =====

    /** 转换 typealias 声明；非法嵌套时显式构造 invalid declaration。 */
    private fun convertTypeAlias(node: LighterASTNode): CfirDeclaration {
        val name = extractName(node)
        val modifiers = modifierListFor(node)
        val expandedType = extractReturnTypeRef(node)

        if (!canDeclareTopLevelClassLike()) {
            return buildInvalidClassLikeDeclaration(
                source = node.toSource(),
                kind = "typealias",
                name = name,
            )
        }

        return buildSourceDeclaration(CfirTypeAliasSymbol(topLevelClassId(name))) { symbol ->
            buildTypeAlias {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                val typeParams = extractTypeParameters(node, symbol)
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                scopeProvider = baseScopeProvider
                attributes = declarationAttributes(node)
                status = modifiers.toDeclarationStatusForCurrentContext()
                this.typeParameters.addAll(typeParams)
                this.name = name
                expandedTypeRef = expandedType
            }
        }
    }

    // ===== 枚举构造器 =====

    /** 转换枚举构造项及其 payload 类型参数。 */
    private fun convertEnumConstructor(
        node: LighterASTNode,
    ): CfirEnumConstructor {
        val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
            ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
        val enumName = if (nameNode != null) {
            Name.identifier(sourceIdentifier(nameNode.asText()))
        } else {
            Name.special("<anonymous-enum-constructor>")
        }

        // 类型参数在 TYPE_LIST 子节点中
        val typeListNode = tree.findChildByType(node, CjNodeTypes.TYPE_LIST)
        val valueTypeRefs = if (typeListNode != null) {
            tree.getChildrenByType(typeListNode, CjNodeTypes.TYPE_REFERENCE).map { convertTypeRef(it) }
        } else {
            emptyList()
        }
        val constructedClassTypeParameters = constructorTypeParametersFromConstructedClass()
        val enumConstructor = buildSourceDeclaration(CfirEnumConstructorSymbol(callableIdFor(enumName))) { symbol ->
            val valueParameters = valueTypeRefs.mapIndexed { index, valueTypeRef ->
                buildEnumConstructorValueParameter(
                    source = valueTypeRef.source ?: node.toSource(),
                    name = enumConstructorPayloadParameterName(index),
                    returnTypeRef = valueTypeRef,
                    containingDeclarationSymbol = symbol,
                )
            }
            buildEnumConstructor {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = context.inLocalContext
                status = CfirDeclarationStatusImpl.DEFAULT
                typeParameters.addAll(constructedClassTypeParameters)
                returnTypeRef = buildImplicitTypeRef()
                this.valueParameters.addAll(valueParameters)
                name = enumName
            }
        }
        enumConstructor.initContainingClassAttr()
        enumConstructor.exportFacts = enumConstructor.exportFacts.copy(enumConstructorHasParameterList = typeListNode != null)
        return enumConstructor
    }

    // ===== 值参数 =====

    /** 转换函数、构造函数、宏或 lambda 的值参数。 */
    fun convertValueParameter(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        signature: MacroParameterSignature,
    ): CfirValueParameter = withNonFunctionContext {
        context.withParameterSignature(signature) {
            // 参数结构归属由构造入口显式提供，不从用于宏的词法签名推导。
            val structuralOwner = containingDeclarationSymbol
            val requiresExplicitType = when (signature) {
                is MacroParameterSignature.Declaration -> signature.grammar != CjParameterGrammar.ACCESSOR_NAME_ONLY
                is MacroParameterSignature.Lambda -> false
            }
            val isPrimaryConstructorParameter = signature is MacroParameterSignature.Declaration && signature.grammar.isPrimaryConstructor
            val modifiers = modifierListFor(node, isPrimaryConstructorParameter)

            val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
            val paramName = if (nameNode != null) Name.identifier(sourceIdentifier(nameNode.asText())) else Name.special("<error>")
            val typeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)

            val isNamed = tree.findChildByType(node, CjTokens.EXCL) != null
            val parameterSource = node.toSource()

            // 默认值是最后一个表达式子节点
            var defaultExpr: CfirExpression? = null
            var defaultSyntax: CfirDefaultAssignmentSyntaxFacts? = null
            tree.forEachChildren(node) { child ->
                if (LightTreeRawCfirExpressionBuilder.isExpressionToken(child.tokenType)) {
                    defaultSyntax = captureDefaultAssignmentSyntaxFacts(
                        child,
                        typeOf = { it.tokenType },
                        childrenOf = { tree.getChildrenArray(it).filterNotNull() },
                        isExpression = { LightTreeRawCfirExpressionBuilder.isExpressionToken(it.tokenType) },
                    ).copy(source = child.toSource())
                    defaultExpr = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                        buildLazyExpression { source = child.toSource() }
                    } else {
                        expressionBuilder.convertExpression(child)
                    }
                }
            }

            val parameter = buildSourceDeclaration(CfirValueParameterSymbol(callableIdFor(paramName))) { symbol ->
                buildValueParameter {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = parameterSource
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    this.isNamed = isNamed
                    returnTypeRef = when {
                        typeRef != null -> convertTypeRef(typeRef)
                        requiresExplicitType -> createNoTypeForParameterTypeRef(parameterSource)
                        else -> buildImplicitTypeRef()
                    }
                    name = paramName
                    defaultValue = defaultExpr
                    this.containingDeclarationSymbol = structuralOwner
                }
            }
            parameter.defaultAssignmentSyntaxFacts = defaultSyntax
            if (isPrimaryConstructorParameter &&
                tree.childrenThroughErrors(node).any { it.tokenType == CjTokens.LET_KEYWORD || it.tokenType == CjTokens.VAR_KEYWORD }
            ) {
                parameter.recordMemberParameterFieldInfo(
                    isVar = hasVarKeyword(node),
                    status = modifiers.toDeclarationStatusForCurrentContext().withConstDeclarationKeyword(hasConstKeyword(node)),
                )
            }
            collectMacroSurfacesFromAnnotations(node, modifiers, MacroSurfaceOwnerKind.PARAMETER, parameter)
            parameter
        }
    }

    /**
     * LightTree 声明/参数层的 annotation surface 采集入口。
     *
     * 先把 annotation 构造成 [CfirAnnotationCall] 并 append 到 owner.annotations，
     * 再基于该 slot identity 建 construction-only surface 与 metadata。
     */
    private fun collectMacroSurfacesFromAnnotations(
        ownerNode: LighterASTNode,
        modifiers: LightTreeModifierList,
        ownerKind: MacroSurfaceOwnerKind,
        carrier: CfirDeclaration,
    ) {
        if (modifiers.annotations.isEmpty()) return
        val metadataRegistry = annotationMetadata

        val carriedAnnotations = modifiers.annotations.map { it.asText() }
        modifiers.annotations.forEach { annotation ->
            val annotationName = annotationNameInfo(annotation) ?: return@forEach
            val rawName = annotationName.rawName
            val qualifiedName = macroSurfaceQualifiedName(annotationName)
            val annotationCall = buildRawAnnotationCall(
                annotation = annotation,
                rawName = rawName,
                carrier = carrier,
                annotationName = annotationName,
            )
            val annotationIndex = carrier.annotations.size
            carrier.replaceAnnotations(carrier.annotations + annotationCall)
            val valueArgumentList = findFirstDescendantByType(annotation, CjNodeTypes.VALUE_ARGUMENT_LIST)
            val macroAttribute = findFirstDescendantByType(annotation, CjNodeTypes.MACRO_ATTR)
            val rawSyntax = annotation.asText()
            val annotationSource = annotation.toSource()
            val isCompileTimeVisible = tree.findChildByType(annotation, CjTokens.ATEXCL) != null
            val snapshot = CfirAnnotationSlotSnapshot(
                owner = carrier,
                annotationIndex = annotationIndex,
                originalAnnotation = annotationCall,
                rawSyntax = rawSyntax,
                forcedCustom = isCompileTimeVisible,
                isCompileTimeVisible = isCompileTimeVisible,
                annotationSource = annotationSource,
                qualifiedName = qualifiedName,
                argumentText = valueArgumentList?.asText() ?: macroAttribute?.asText(),
                tokens = tokenizeFullAnnotation(annotation, annotationSource),
                callSite = when (ownerKind) {
                    MacroSurfaceOwnerKind.DECLARATION -> MacroCallSite.DECLARATION
                    MacroSurfaceOwnerKind.PARAMETER -> MacroCallSite.PARAMETER
                },
            )
            val annotationCarrier = metadataRegistry.record(snapshot)
            buildMacroSurfaceFromAnnotation(
                ownerNode = ownerNode,
                annotation = annotation,
                ownerKind = ownerKind,
                carrier = carrier,
                annotationCarrier = annotationCarrier,
                modifiers = modifiers.modifierTexts,
                carriedAnnotations = carriedAnnotations,
            )?.let { collectedMacroSurfaces += it }
        }
    }

    /** 基于 annotation 节点本身构造 raw annotation call。 */
    private fun buildRawAnnotationCall(
        annotation: LighterASTNode,
        rawName: String,
        carrier: CfirDeclaration,
        annotationName: AnnotationNameInfo? = null,
    ): CfirAnnotationCall {
        val containingSymbol = when (carrier) {
            is CfirValueParameter -> carrier.containingDeclarationSymbol
            else -> carrier.symbol
        }
        return buildRawAnnotationCall(
            annotation = annotation,
            rawName = rawName,
            containingSymbol = containingSymbol,
            typeRefOverride = annotationName?.let { buildAnnotationTypeRef(it) },
            calleeReferenceSourceOverride = annotationName?.calleeReferenceSource,
        )
    }

    /** 基于已提取的 annotation 名称构造 raw annotation call。 */
    private fun buildRawAnnotationCall(
        annotation: LighterASTNode,
        rawName: String,
        containingSymbol: CfirBasedSymbol<*>,
        sourceOverride: CjSourceElement? = null,
        typeRefOverride: CfirTypeRef? = null,
        calleeReferenceSourceOverride: CjSourceElement? = null,
        argumentListSourceOverride: CjSourceElement? = null,
        macroAttributeOverride: LighterASTNode? = null,
        macroAttributeTextOverride: String? = null,
        macroAttributeStartOffsetOverride: Int? = null,
    ): CfirAnnotationCall {
        val sourceOffsetDelta = sourceOffsetDelta(sourceOverride, annotation)
        val valueArgumentList = if (annotation.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
            null
        } else {
            findFirstDescendantByType(annotation, CjNodeTypes.VALUE_ARGUMENT_LIST)
        }
        val macroAttribute = macroAttributeOverride ?: if (annotation.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
            tree.findChildByType(annotation, CjNodeTypes.MACRO_ATTR)
        } else {
            findFirstDescendantByType(annotation, CjNodeTypes.MACRO_ATTR)
        }
        val arguments = convertAnnotationArguments(
            annotation = annotation,
            annotationName = rawName,
            valueArgumentList = valueArgumentList,
            macroAttribute = macroAttribute,
            macroAttributeTextOverride = macroAttributeTextOverride,
            macroAttributeStartOffsetOverride = macroAttributeStartOffsetOverride,
            sourceOffsetDelta = sourceOffsetDelta,
        )
        val compileTimeVisible = tree.findChildByType(annotation, CjTokens.ATEXCL) != null
        val annotationTypeRef = typeRefOverride ?: buildAnnotationTypeRef(annotation)
        val calleeName = when (annotationTypeRef) {
            is CfirUserTypeRef -> annotationTypeRef.qualifier.last().name
            else -> Name.identifier("<error>")
        }
        return buildAnnotationCall {
            source = sourceOverride ?: annotation.toSource()
            typeRef = annotationTypeRef
            initializeRawAnnotationSyntax(compileTimeVisible, rawName, context.sourceModuleName)
            argumentList = buildArgumentList {
                source = argumentListSourceOverride ?: valueArgumentList?.toSource()
                this.arguments.addAll(arguments)
            }
            calleeReference = buildNamedReference(
                calleeName,
                calleeReferenceSourceOverride ?: annotationCalleeReferenceSource(annotation),
            )
            containingDeclarationSymbol = containingSymbol
        }
    }

    /** 转换 annotation 实参列表。 */
    private fun convertAnnotationArguments(
        annotation: LighterASTNode,
        annotationName: String,
        valueArgumentList: LighterASTNode?,
        macroAttribute: LighterASTNode? = null,
        macroAttributeTextOverride: String? = null,
        macroAttributeStartOffsetOverride: Int? = null,
        sourceOffsetDelta: Int = 0,
    ): List<CfirExpression> {
        if (macroAttributeTextOverride != null && macroAttributeStartOffsetOverride != null) {
            return convertMacroAttributeArguments(
                rawText = macroAttributeTextOverride,
                startOffset = macroAttributeStartOffsetOverride,
                annotationName = annotationName,
            )
        }

        if (valueArgumentList != null) return expressionBuilder.convertValueArguments(valueArgumentList)

        if (macroAttribute != null) {
            return convertMacroAttributeArguments(
                macroAttribute.asText(),
                macroAttribute.startOffset + sourceOffsetDelta,
                annotationName,
            )
        }

        // Language builtins are only the unqualified source surface.  Do not
        // truncate a qualified custom annotation (`user.Attribute`) to its
        // short name here; PSI and the parser deliberately keep it custom.
        when (BuiltInAnnotationRegistry.findLanguageBuiltIn(annotationName)?.kind) {
            BuiltInAnnotationKind.ATTRIBUTE -> {
                val attributeNode = findFirstDescendantByType(annotation, CjNodeTypes.ANNTATION_ATTR_ATTRIBUTE)
                    ?: return emptyList()
                val arguments = mutableListOf<CfirExpression>()
                tree.forEachChildren(attributeNode) { child ->
                    when (child.tokenType) {
                        CjTokens.IDENTIFIER -> arguments += buildLiteralExpression {
                            source = child.toSource()
                            kind = CfirLiteralKind.STRING
                            value = child.asText()
                        }
                        CjNodeTypes.STRING_TEMPLATE -> arguments += expressionBuilder.convertExpression(child)
                    }
                }
                return arguments
            }

            BuiltInAnnotationKind.NUMERIC_OVERFLOW -> {
                val strategyNode = findFirstDescendantByType(annotation, CjNodeTypes.ANNOTATION_OVERFLOW_STRATEGY)
                    ?: return emptyList()
                val strategyName = strategyNode.asText().trim().takeIf(String::isNotEmpty)
                    ?: return emptyList()
                return listOf(buildNamedAccessExpression {
                    source = strategyNode.toSource()
                    calleeReference = buildNamedReference(
                        Name.identifier(strategyName),
                        strategyNode.toSource(),
                    )
                })
            }

            BuiltInAnnotationKind.WHEN -> {
                val conditionNode = findFirstDescendantByType(annotation, CjNodeTypes.ANNOTATION_WHEN_CONDITION)
                    ?: return emptyList()
                val expressionNode = expressionBuilder.findFirstExpression(conditionNode)
                    ?: return emptyList()
                return listOf(expressionBuilder.convertExpression(expressionNode))
            }

            else -> Unit
        }

        val callingConvention = findFirstDescendantByType(annotation, CjNodeTypes.ANNOTATION_CALLING_CONV)
        if (callingConvention != null) {
            val source = callingConvention.toSource()
            return listOf(buildNamedAccessExpression {
                this.source = source
                calleeReference = buildNamedReference(
                    Name.identifier(callingConvention.asText()),
                    source,
                )
            })
        }

        return emptyList()
    }

    /**
     * 将宏式 annotation attr `[a: b]` 重解析成标准调用实参。
     *
     * LightTree 的 [CjNodeTypes.MACRO_ATTR] 内部是 quote token 容器，不产生
     * `VALUE_ARGUMENT` 子树；平台注解、custom annotation 与 import 使用分析都
     * 需要同一种结构化 argument IR，因此这里用仓颉 LightTree parser 重建
     * synthetic call，再交给现有 expression builder 转换。
     */
    private fun convertMacroAttributeArguments(
        rawText: String?,
        startOffset: Int?,
        annotationName: String,
    ): List<CfirExpression> {
        if (rawText == null || startOffset == null) return emptyList()
        val openBracketIndex = rawText.indexOf('[')
        val closeBracketIndex = rawText.lastIndexOf(']')
        if (openBracketIndex < 0 || closeBracketIndex <= openBracketIndex) return emptyList()

        val content = rawText.substring(openBracketIndex + 1, closeBracketIndex)
        if (content.isBlank()) return emptyList()

        val prefix = "@$annotationName"
        val fragmentText = buildString {
            repeat(startOffset + openBracketIndex - prefix.length) { append(' ') }
            append(prefix)
            append(rawText.substring(openBracketIndex, closeBracketIndex + 1))
            append("\nfunc __annotation_arguments__() {}")
        }
        val psiBuilder = CangJieLightParser.createBuilder(fragmentText)
        val parsedFragment = CangJieLightParser.parseAnnotationOnly(
            psiBuilder,
            languageModuleName = context.sourceModuleName,
        )
        val parsedTree = parsedFragment.tree
        val valueArgumentList = parsedTree.findFirstParsedNode(CjNodeTypes.VALUE_ARGUMENT_LIST)
            ?: return emptyList()
        val parsedBuilder = LightTreeRawCfirDeclarationBuilder(
            annotationMetadata = annotationMetadata,
            session = baseSession,
            baseScopeProvider = baseScopeProvider,
            tree = parsedTree,
            source = fragmentText,
            context = Context(),
            bodyBuildingMode = bodyBuildingMode,
            macroFacts = parsedFragment.macroFacts,
            nominalFacts = parsedFragment.cjmpNominalFacts,
            declarationModifiers = parsedFragment.declarationModifiers,
        )
        return parsedBuilder.context.withSourceModuleName(context.sourceModuleName) {
            parsedBuilder.withPackageContext(packageFqName) {
                parsedBuilder.expressionBuilder.convertValueArguments(valueArgumentList)
            }
        }
    }

    /** 根据语法名称段构造 annotation 的 user type ref。 */
    private fun buildAnnotationTypeRef(annotation: LighterASTNode): CfirTypeRef =
        annotationNameInfo(annotation)?.let { buildAnnotationTypeRef(it) } ?: buildImplicitTypeRef()

    /** 根据 annotation 名称信息构造可重定位的 user type ref。 */
    private fun buildAnnotationTypeRef(annotationName: AnnotationNameInfo, sourceOffsetDelta: Int = 0): CfirTypeRef {
        val parts = annotationName.segmentTexts
        check(parts.size == annotationName.segmentSources.size && parts.isNotEmpty())
        val nameSource = annotationName.nameSource.shiftedBy(sourceOffsetDelta)
        val segmentSources = annotationName.segmentSources.map { it.shiftedBy(sourceOffsetDelta) }
        return buildUserTypeRef {
            source = nameSource
            qualifier += parts.mapIndexed { index, part ->
                buildQualifierPart {
                    source = segmentSources[index]
                    name = Name.identifier(sourceIdentifier(part))
                }
            }
        }
    }

    /** 从 annotation 或 macro-expression annotation 包装构造 macro surface。 */
    private fun buildMacroSurfaceFromAnnotation(
        ownerNode: LighterASTNode,
        annotation: LighterASTNode,
        ownerKind: MacroSurfaceOwnerKind,
        carrier: CfirDeclaration,
        annotationCarrier: CfirAnnotationReplaceCarrier,
        modifiers: List<String>,
        carriedAnnotations: List<String>,
    ): MacroSurface? {
        val annotationName = annotationNameInfo(annotation) ?: return null
        val rawName = annotationName.rawName
        val qualifiedName = macroSurfaceQualifiedName(annotationName)
        val surfaceId = MacroSurfaceIdGenerator.next()
        val attrNode = tree.findChildByType(annotation, CjNodeTypes.VALUE_ARGUMENT_LIST)
        val attrTokens = tokenizeSurfacePayload(attrNode)
        val inputTokens = tokenizeSourceSlice(annotation.endOffset, ownerNode.endOffset)
        val annotationSource = annotation.toSource()
        val invocationSource = CjLightSourceElement(
            lighterASTNode = annotationSource.lighterASTNode,
            startOffset = annotation.startOffset,
            endOffset = ownerNode.endOffset,
            treeStructure = annotationSource.treeStructure,
            kind = annotationSource.kind,
        )
        val isForced = tree.findChildByType(annotation, CjTokens.ATEXCL) != null
        val common = MacroSurfaceCommon(
            surfaceId = surfaceId,
            qualifiedName = qualifiedName,
            kind = if (isForced) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
            parsingFacts = macroParsingFacts(annotation),
            hasParenthesis = false,
            attrTokens = attrTokens,
            inputTokens = inputTokens,
            sourceRange = MacroSurfaceSourceRange(
                source = invocationSource,
                startOffset = annotation.startOffset,
                endOffset = ownerNode.endOffset,
            ),
            scopeContext = macroSurfaceScopeContext(),
            modifiers = modifiers,
            carriedAnnotations = carriedAnnotations,
            capturedRawSyntax = source.subSequence(annotation.startOffset, ownerNode.endOffset).toString(),
            containerContext = macroSurfaceContainerContext(ownerNode),
            replaceHandle = CfirReplaceHandle(
                handleId = surfaceId,
                ownerSnapshot = context.snapshotMacroOwner(),
                carrier = carrier,
                annotationCarrier = annotationCarrier,
            ),
        )

        return when (ownerKind) {
            MacroSurfaceOwnerKind.DECLARATION -> MacroSurfaceDecl(
                surfaceId = common.surfaceId,
                qualifiedName = common.qualifiedName,
                isQualifiedName = annotationName.segmentTexts.size > 1,
                kind = common.kind,
                parsingFacts = common.parsingFacts,
                hasParenthesis = common.hasParenthesis,
                attrTokens = common.attrTokens,
                inputTokens = common.inputTokens,
                sourceRange = common.sourceRange,
                scopeContext = common.scopeContext,
                modifiers = common.modifiers,
                carriedAnnotations = common.carriedAnnotations,
                capturedRawSyntax = common.capturedRawSyntax,
                containerContext = common.containerContext,
                replaceHandle = common.replaceHandle,
            )
            MacroSurfaceOwnerKind.PARAMETER -> MacroSurfaceParam(
                surfaceId = common.surfaceId,
                qualifiedName = common.qualifiedName,
                isQualifiedName = annotationName.segmentTexts.size > 1,
                kind = common.kind,
                parsingFacts = common.parsingFacts,
                hasParenthesis = common.hasParenthesis,
                attrTokens = common.attrTokens,
                inputTokens = common.inputTokens,
                sourceRange = common.sourceRange,
                scopeContext = common.scopeContext,
                modifiers = common.modifiers,
                carriedAnnotations = common.carriedAnnotations,
                capturedRawSyntax = common.capturedRawSyntax,
                containerContext = common.containerContext,
                replaceHandle = common.replaceHandle,
            )
        }
    }

    /**
     * 构造 macro surface 时共享的字段集合。
     *
     * @property surfaceId construction 期唯一 surface id。
     * @property kind macro 调用形态。
     * @property qualifiedName surface 限定名。
     * @property inputTokens input payload token 流。
     * @property sourceRange surface 源码范围。
     * @property scopeContext surface 作用域上下文。
     * @property replaceHandle stable splice 句柄。
     */
    private data class MacroSurfaceCommon(
        /** construction 期唯一 surface id。 */
        val surfaceId: Long,
        /** surface 限定名。 */
        val qualifiedName: FqName?,
        /** macro 调用形态。 */
        val kind: MacroSurface.Kind,
        /** 原 parser 根冻结的 scope、直接 parent 与外层声明身份。 */
        val parsingFacts: MacroParsingFacts?,
        /** surface 语法中是否带括号。 */
        val hasParenthesis: Boolean,
        /** attribute token 流。 */
        val attrTokens: List<MacroSurfaceToken>,
        /** input payload token 流。 */
        val inputTokens: List<MacroSurfaceToken>,
        /** surface 源码范围。 */
        val sourceRange: MacroSurfaceSourceRange,
        /** surface 作用域上下文。 */
        val scopeContext: MacroSurfaceScopeContext,
        /** surface 上携带的修饰符文本。 */
        val modifiers: List<String>,
        /** 随 surface 携带的 annotation 文本。 */
        val carriedAnnotations: List<String>,
        /** 捕获到的原始语法文本。 */
        val capturedRawSyntax: String,
        /** surface 所处容器上下文。 */
        val containerContext: MacroSurfaceContainerContext,
        /** stable splice 句柄。 */
        val replaceHandle: CfirReplaceHandle,
    )

    /** 对 surface payload 节点执行 lexer tokenization。 */
    private fun tokenizeSurfacePayload(node: LighterASTNode?): List<MacroSurfaceToken> {
        return tokenizeSurfacePayload(
            payload = node?.asText(),
            baseOffset = node?.startOffset ?: 0,
        )
    }

    /** 对已恢复的 surface payload 文本执行 lexer tokenization。 */
    private fun tokenizeSurfacePayload(payload: String?, baseOffset: Int): List<MacroSurfaceToken> {
        return MacroPayloadTokenizer.tokenize(payload = payload, baseOffset = baseOffset).map { token ->
            MacroSurfaceToken(
                text = token.text,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
                kindName = token.kindName,
                isSourceToken = true,
            )
        }
    }

    /** 按宿主文件绝对 offset 切片并保持 token offset 与原文件一致。 */
    private fun tokenizeSourceSlice(startOffset: Int, endOffset: Int): List<MacroSurfaceToken> {
        val start = startOffset.coerceIn(0, source.length)
        val end = endOffset.coerceIn(start, source.length)
        return tokenizeSurfacePayload(source.subSequence(start, end).toString(), start)
    }

    /** 对完整 annotation 文本执行 lexer tokenization。 */
    private fun tokenizeFullAnnotation(node: LighterASTNode, source: CjSourceElement): List<MacroSurfaceToken> {
        return MacroPayloadTokenizer.tokenize(
            payload = node.asText(),
            baseOffset = node.startOffset,
        ).map { token ->
            MacroSurfaceToken(
                text = token.text,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
                kindName = token.kindName,
                sourceRange = MacroSurfaceSourceRange(source, token.startOffset, token.endOffset),
                isSourceToken = true,
            )
        }
    }

    /** 从 annotation 节点提取名称文本。 */
    private fun extractAnnotationNameText(annotation: LighterASTNode): String? {
        return annotationNameInfo(annotation)?.rawName
    }

    /**
     * annotation 名称与各段 source 信息。
     *
     * @property rawName annotation 原始限定名文本。
     * @property calleeReferenceSource callee reference 的 source。
     * @property segmentSources 限定名每一段对应的 source。
     */
    private data class AnnotationNameInfo(
        /** annotation 原始限定名文本。 */
        val rawName: String,
        /** 按语法 token 保留的每个名称段。 */
        val segmentTexts: List<String>,
        /** annotation 名称整体 source。 */
        val nameSource: CjSourceElement,
        /** 限定名每一段对应的 source。 */
        val segmentSources: List<CjSourceElement>,
    ) {
        /** callee reference 的 source，优先使用最后一段名称。 */
        val calleeReferenceSource: CjSourceElement
            get() = segmentSources.lastOrNull() ?: nameSource
    }

    /** 提取普通 annotation 节点的名称信息。 */
    private fun annotationNameInfo(annotation: LighterASTNode): AnnotationNameInfo? {
        val nameNode = findAnnotationNameNode(annotation) ?: return null
        val rawName = nameNode.asText().trim().takeIf { it.isNotEmpty() } ?: return null
        val segments = collectAnnotationNameSegmentSources(nameNode)
        return AnnotationNameInfo(
            rawName = rawName,
            segmentTexts = segments.map { source.subSequence(it.startOffset, it.endOffset).toString() },
            nameSource = nameNode.toSource(),
            segmentSources = segments,
        )
    }

    /** 提取 macro-expression annotation 包装的名称信息。 */
    private fun macroExpressionAnnotationNameInfo(node: LighterASTNode): AnnotationNameInfo? {
        val scan = macroExpressionHeadSyntax(node) ?: return null
        val wrapperSource = node.toSource()
        return AnnotationNameInfo(
            rawName = scan.rawName,
            segmentTexts = scan.segmentRanges.map { range -> node.asText().substring(range.startOffset, range.endOffset) },
            nameSource = wrapperSource.sliceMacroExpressionSource(scan.nameRange),
            segmentSources = scan.segmentRanges.map { range -> wrapperSource.sliceMacroExpressionSource(range) },
        )
    }

    /** 仅从当前 wrapper 的直接宏前缀解析本层名称与属性。 */
    private fun macroExpressionHeadSyntax(node: LighterASTNode): MacroExpressionNameScan? {
        val prefix = tree.findChildByType(node, CjTokens.AT) ?: tree.findChildByType(node, CjTokens.ATEXCL) ?: return null
        return MacroExpressionHeadScanner.scan(node.asText(), prefix.startOffset - node.startOffset)
    }

    private fun CjSourceElement.sliceMacroExpressionSource(range: MacroExpressionTextRange): CjSourceElement {
        check(this is CjLightSourceElement) {
            "Macro expression source must be a LightTree source element."
        }
        return CjLightSourceElement(
            lighterASTNode = lighterASTNode,
            startOffset = startOffset + range.startOffset,
            endOffset = startOffset + range.endOffset,
            treeStructure = treeStructure,
            kind = kind,
        )
    }

    /** 返回当前 wrapper 头部 attr 文本，优先使用源码扫描结果。 */
    private fun macroExpressionAttributeText(
        rawText: String,
        scan: MacroExpressionNameScan,
        attrNode: LighterASTNode?,
    ): String? {
        return scan.attrRange
            ?.let { range -> rawText.substring(range.startOffset, range.endOffset) }
            ?: attrNode?.asText()
    }

    /** 返回当前 wrapper 头部 attr source，优先使用源码扫描结果。 */
    private fun macroExpressionAttributeSource(
        node: LighterASTNode,
        scan: MacroExpressionNameScan,
        attrNode: LighterASTNode?,
    ): CjSourceElement? {
        return scan.attrRange
            ?.let { range -> node.toSource().sliceMacroExpressionSource(range) }
            ?: attrNode?.toSource()
    }

    /**
     * 返回 macro-expression wrapper 中完整 annotation 的精确 source，不包含其输入声明。
     */
    private fun macroExpressionAnnotationSource(
        node: LighterASTNode,
        annotationName: AnnotationNameInfo,
        attrSource: CjSourceElement?,
    ): CjSourceElement {
        val wrapperSource = node.toSource()
        val annotationEndOffset = attrSource?.endOffset ?: annotationName.nameSource.endOffset
        check(annotationEndOffset in wrapperSource.startOffset..wrapperSource.endOffset) {
            "Macro annotation source must stay inside its wrapper source range."
        }
        return CjLightSourceElement(
            lighterASTNode = wrapperSource.lighterASTNode,
            startOffset = checkNotNull(tree.findChildByType(node, CjTokens.AT) ?: tree.findChildByType(node, CjTokens.ATEXCL)).startOffset,
            endOffset = annotationEndOffset,
            treeStructure = wrapperSource.treeStructure,
            kind = wrapperSource.kind,
        )
    }

    /** 查找普通 annotation 的名称节点。 */
    private fun findAnnotationNameNode(annotation: LighterASTNode): LighterASTNode? {
        if (annotation.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
            return findMacroExpressionAnnotationNameNode(annotation)
        }

        return findAnnotationConstructorCalleeNameNode(annotation)
    }

    /**
     * 注解 callee source 必须来自 parser 产生的 constructor-callee 类型结构。
     *
     * Kotlin light-tree 也固定沿 CONSTRUCTOR_CALLEE -> TYPE_REFERENCE -> USER_TYPE
     * -> REFERENCE_EXPRESSION 取 annotation callee，不能在整棵 annotation 子树里
     * 泛化搜索任意引用，否则会把参数、声明输入或恢复节点中的引用误当成注解名。
     */
    private fun findAnnotationConstructorCalleeNameNode(annotation: LighterASTNode): LighterASTNode? {
        val constructorCallee = tree.findChildByType(annotation, CjStubElementTypes.CONSTRUCTOR_CALLEE)
            ?: return null
        val typeRef = tree.findChildByType(constructorCallee, CjNodeTypes.TYPE_REFERENCE)
            ?: return null
        return tree.findChildByType(typeRef, CjNodeTypes.USER_TYPE)
    }

    /** 查找 macro-expression annotation 包装中的名称节点。 */
    private fun findMacroExpressionAnnotationNameNode(node: LighterASTNode): LighterASTNode? {
        return findAnnotationConstructorCalleeNameNode(node)
            ?: findMacroExpressionNameNode(node)
    }

    /** 构造 annotation callee reference 的 source。 */
    private fun annotationCalleeReferenceSource(annotation: LighterASTNode): CjSourceElement? {
        return annotationNameInfo(annotation)?.calleeReferenceSource
    }

    /** 计算 reparse 后 LightTree 节点与原始 source override 之间的偏移差。 */
    private fun sourceOffsetDelta(sourceOverride: CjSourceElement?, reparsedNode: LighterASTNode): Int {
        return sourceOverride?.let { it.startOffset - reparsedNode.toSource().startOffset } ?: 0
    }

    /** 将 source element 按 [delta] 平移。 */
    private fun CjSourceElement.shiftedBy(delta: Int): CjSourceElement {
        if (delta == 0) return this
        return CjLightSourceElement(
            lighterASTNode = lighterASTNode,
            startOffset = startOffset + delta,
            endOffset = endOffset + delta,
            treeStructure = treeStructure,
            kind = kind,
        )
    }

    /** 收集 annotation 限定名每一段对应的 source。 */
    private fun collectAnnotationNameSegmentSources(nameNode: LighterASTNode): List<CjSourceElement> {
        check(
            nameNode.tokenType == CjNodeTypes.USER_TYPE ||
                nameNode.tokenType == CjNodeTypes.DOT_QUALIFIED_EXPRESSION ||
                nameNode.tokenType == CjNodeTypes.REFERENCE_EXPRESSION,
        ) {
            "Annotation name traversal must start from USER_TYPE, DOT_QUALIFIED_EXPRESSION, or REFERENCE_EXPRESSION."
        }
        val sources = mutableListOf<CjSourceElement>()

        /**
         * 名称主干只由 USER_TYPE、DOT_QUALIFIED_EXPRESSION 与 REFERENCE_EXPRESSION
         * 组成。只有前两者是复合节点，REFERENCE_EXPRESSION 直接作为一个名称段收集；
         * 点号等叶子 token 直接跳过，不能交给 LightTree 的子节点遍历 API。
         */
        fun visitNameNode(node: LighterASTNode) {
            when (node.tokenType) {
                CjNodeTypes.REFERENCE_EXPRESSION -> sources += node.toSource()
                CjNodeTypes.USER_TYPE,
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION -> node.forEachChildren { child ->
                    when (child.tokenType) {
                        CjNodeTypes.USER_TYPE,
                        CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                        CjNodeTypes.REFERENCE_EXPRESSION -> visitNameNode(child)
                        CjTokens.DOT -> Unit
                        // 其它节点（例如类型参数或错误恢复节点）不是名称主干，
                        // 且可能是叶子节点；不递归，避免误取参数中的引用。
                        else -> Unit
                    }
                }
                else -> error(
                    "Unsupported annotation name node: ${node.tokenType}."
                )
            }
        }

        visitNameNode(nameNode)
        check(sources.isNotEmpty()) {
            "Annotation name `${nameNode.asText()}` has no reference-expression segments."
        }
        return sources
    }

    /** 查找第一个 token type 为 [tokenType] 的后代节点。 */
    private fun findFirstDescendantByType(
        node: LighterASTNode,
        tokenType: IElementType,
    ): LighterASTNode? {
        if (node.tokenType == tokenType) return node
        tree.forEachChildren(node) { child ->
            findFirstDescendantByType(child, tokenType)?.let { return it }
        }
        return null
    }

    /** 在指定 parsed tree 中查找第一个 token type 为 [tokenType] 的节点。 */
    private fun FlyweightCapableTreeStructure<LighterASTNode>.findFirstParsedNode(
        tokenType: IElementType,
    ): LighterASTNode? {
        fun visit(node: LighterASTNode): LighterASTNode? {
            if (node.tokenType == tokenType) return node
            val childrenRef = Ref<Array<LighterASTNode?>>()
            getChildren(node, childrenRef)
            val children = childrenRef.get() ?: return null
            for (child in children) {
                if (child == null) break
                visit(child)?.let { return it }
            }
            return null
        }
        return visit(root)
    }

    /** 将 surface 原始名称提升为 FQN。 */
    private fun macroSurfaceQualifiedName(annotationName: AnnotationNameInfo): FqName {
        val names = annotationName.segmentTexts.map(::sourceIdentifier)
        if (names.size > 1) return FqName.fromSegments(names)
        val name = Name.identifier(names.single())
        return if (packageFqName.isRoot) {
            FqName.topLevel(name)
        } else {
            packageFqName.child(name)
        }
    }

    /** 构造 macro surface 的 scope context。 */
    private fun macroSurfaceScopeContext(): MacroSurfaceScopeContext {
        val classFqName = context.containingClassSymbolIfAny?.classId?.asSingleFqName()
        val functionName = (containerSymbolIfAny as? CfirCallableSymbol<*>)?.name
        return MacroSurfaceScopeContext(
            packageFqName = packageFqName,
            sourceModuleName = context.sourceModuleName,
            enclosingClassFqName = classFqName,
            enclosingFunctionName = functionName,
        )
    }

    /** 构造 macro surface 的语法容器上下文。 */
    private fun macroSurfaceContainerContext(ownerNode: LighterASTNode): MacroSurfaceContainerContext {
        return MacroSurfaceContainerContext(
            outerDeclarationKind = outerDeclarationKind(ownerNode),
            isInsidePrimaryConstructor = ownerNode.tokenType == CjNodeTypes.PRIMARY_CONSTRUCTOR ||
                    hasAncestor(ownerNode, CjNodeTypes.PRIMARY_CONSTRUCTOR),
            isInsideEnumBody = ownerNode.tokenType == CjNodeTypes.ENUM_BODY || hasAncestor(ownerNode, CjNodeTypes.ENUM_BODY),
            isInsideBlock = hasAncestor(ownerNode, CjNodeTypes.BLOCK) || hasAncestor(ownerNode, CjNodeTypes.CASE_BLOCK),
            commaListPosition = commaListPosition(ownerNode),
        )
    }

    /** 根据 owner 与当前容器符号推导外层声明种类。 */
    private fun outerDeclarationKind(ownerNode: LighterASTNode): MacroSurfaceContainerContext.OuterDeclarationKind {
        var current = ownerNode.getParent()
        while (current != null) {
            when (current.tokenType) {
                CjNodeTypes.ENUM_BODY -> return MacroSurfaceContainerContext.OuterDeclarationKind.ENUM_BODY
                CjNodeTypes.INTERFACE_BODY -> return MacroSurfaceContainerContext.OuterDeclarationKind.INTERFACE_BODY
                CjNodeTypes.CLASS_BODY -> return when (context.containingClassSymbolIfAny) {
                    is CfirStructSymbol -> MacroSurfaceContainerContext.OuterDeclarationKind.STRUCT_BODY
                    is CfirEnumSymbol -> MacroSurfaceContainerContext.OuterDeclarationKind.ENUM_BODY
                    is CfirInterfaceSymbol -> MacroSurfaceContainerContext.OuterDeclarationKind.INTERFACE_BODY
                    else -> MacroSurfaceContainerContext.OuterDeclarationKind.CLASS_BODY
                }
                CjNodeTypes.PROPERTY_BODY -> return MacroSurfaceContainerContext.OuterDeclarationKind.PROPERTY_BODY
                CjNodeTypes.BLOCK,
                CjNodeTypes.CASE_BLOCK,
                -> return MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY
            }
            current = current.getParent()
        }

        return if (context.inLocalContext) {
            MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY
        } else {
            MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL
        }
    }

    /** 判断 [node] 是否存在指定 token type 的祖先节点。 */
    private fun hasAncestor(node: LighterASTNode, tokenType: com.intellij.psi.tree.IElementType): Boolean {
        var current = node.getParent()
        while (current != null) {
            if (current.tokenType == tokenType) return true
            current = current.getParent()
        }
        return false
    }

    /** 计算节点在逗号分隔列表中的位置；当前未能判定时返回 null。 */
    private fun commaListPosition(node: LighterASTNode): Int? {
        val parent = node.getParent()?.takeIf { it.tokenType == CjNodeTypes.VALUE_PARAMETER_LIST } ?: return null
        return tree.getChildrenByType(parent, CjNodeTypes.VALUE_PARAMETER).indexOf(node).takeIf { it >= 0 }
    }

    // ===== 文件级构建辅助 =====

    /** 从 package directive 节点提取包名。 */
    private fun extractPackageFqName(packageNode: LighterASTNode): FqName {
        // PACKAGE_DIRECTIVE 内部可能包含 DOT_QUALIFIED_EXPRESSION 或 REFERENCE_EXPRESSION
        val text = buildString {
            tree.forEachChildren(packageNode) { child ->
                when (child.tokenType) {
                    CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                    CjNodeTypes.REFERENCE_EXPRESSION -> append(child.asText())
                }
            }
        }
        return organizationQualifiedPath(text).fqName
    }

    private data class OrganizationQualifiedPath(
        val fqName: FqName,
        val organizationName: Name?,
    )

    /** 将官方 `organization::package` 路径拆成组织元数据和普通包名。 */
    private fun organizationQualifiedPath(text: String): OrganizationQualifiedPath {
        val separator = text.indexOf("::")
        if (separator <= 0) return OrganizationQualifiedPath(FqName(normalizeIdentifier(text)), null)
        return OrganizationQualifiedPath(
            fqName = FqName(normalizeIdentifier(text.substring(separator + 2).replace("::", "."))),
            organizationName = Name.identifier(sourceIdentifier(text.substring(0, separator).trim())),
        )
    }

    /** 构造 CFIR package directive。 */
    private fun buildPackageDirectiveNode(
        packageNode: LighterASTNode?,
        fqName: FqName,
    ): CfirPackageDirective {
        return buildPackageDirective {
            source = packageNode?.toSource()
            packageFqName = fqName
            organizationName = packageNode
                ?.let { extractPackageDirectiveText(it) }
                ?.let(::organizationQualifiedPath)
                ?.organizationName
            isMacroPackage = packageNode?.let { containsChildByType(it, CjTokens.MACRO_KEYWORD) } == true
        }
    }

    private fun extractPackageDirectiveText(packageNode: LighterASTNode): String = buildString {
        tree.forEachChildren(packageNode) { child ->
            when (child.tokenType) {
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                CjNodeTypes.REFERENCE_EXPRESSION -> append(child.asText())
            }
        }
    }

    /** 判断节点是否包含直接子节点 [type]。 */
    private fun containsChildByType(node: LighterASTNode, type: com.intellij.psi.tree.IElementType): Boolean {
        tree.forEachChildren(node) { child ->
            if (child.tokenType == type || containsChildByType(child, type)) return true
        }
        return false
    }

    /** 从文件节点构造 import 列表。 */
    private fun buildImportsFromFile(file: LighterASTNode): List<CfirImport> {
        val imports = mutableListOf<CfirImport>()
        tree.forEachChildren(file) { child ->
            if (child.tokenType == CjNodeTypes.IMPORT_LIST) {
                tree.forEachChildren(child) { directive ->
                    if (directive.tokenType == CjNodeTypes.IMPORT_DIRECTIVE && !directive.hasImportContainerErrors()) {
                        val condition = directive.whenConditionExpressionOrNull()
                        val visibility = when {
                            tree.findChildByType(directive, CjTokens.PUBLIC_KEYWORD) != null -> Visibilities.Public
                            tree.findChildByType(directive, CjTokens.PROTECTED_KEYWORD) != null -> Visibilities.Protected
                            tree.findChildByType(directive, CjTokens.INTERNAL_KEYWORD) != null -> Visibilities.Internal
                            else -> Visibilities.Private
                        }
                        tree.forEachChildren(directive) contentLoop@{ content ->
                            when (content.tokenType) {
                                CjNodeTypes.IMPORT_ITEM ->
                                    convertImportItem(content, null, condition, visibility)?.let(imports::add)
                                CjNodeTypes.IMPORT_GROUP -> {
                                    if (content.hasImportContainerErrors()) return@contentLoop
                                    val prefix = content.extractImportPath(allowOrganizationOnly = true)
                                    if (prefix == null && content.hasImportPathExpression()) return@contentLoop
                                    tree.forEachChildren(content) { item ->
                                        if (item.tokenType == CjNodeTypes.IMPORT_ITEM) {
                                            convertImportItem(item, prefix, condition, visibility)?.let(imports::add)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return imports
    }

    /** 只读取当前 group/item 的路径，组织引用与包路径由真实 `::` token 分隔。 */
    private fun LighterASTNode.extractImportPath(allowOrganizationOnly: Boolean = false): ImportPathPrefix? {
        val expressions = mutableListOf<LighterASTNode>()
        var hasOrganizationSeparator = false
        tree.forEachChildren(this) { child ->
            when (child.tokenType) {
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                CjNodeTypes.REFERENCE_EXPRESSION -> expressions += child
                CjTokens.DOUBLE_COLON -> hasOrganizationSeparator = true
            }
        }
        if (expressions.isEmpty()) return null
        val organizationName = if (hasOrganizationSeparator) {
            expressions.removeAt(0).importPathSegments()?.singleOrNull() ?: return null
        } else null
        val fqName = when (expressions.size) {
            0 -> if (allowOrganizationOnly && organizationName != null) FqName.ROOT else return null
            1 -> {
                val segments = expressions.single().importPathSegments() ?: return null
                segments.fold(FqName.ROOT) { path, segment -> path.child(segment) }
            }
            else -> return null
        }
        return ImportPathPrefix(organizationName, fqName)
    }

    private fun LighterASTNode.hasImportPathExpression(): Boolean {
        tree.forEachChildren(this) { child ->
            if (child.tokenType == CjNodeTypes.REFERENCE_EXPRESSION ||
                child.tokenType == CjNodeTypes.DOT_QUALIFIED_EXPRESSION
            ) return true
        }
        return false
    }

    /** 与 PSI 的容器校验一致：列表错误影响整组，兄弟 item 内错误只影响该项。 */
    private fun LighterASTNode.hasImportContainerErrors(): Boolean {
        tree.forEachChildren(this) { child ->
            if (child.tokenType.isParsingErrorElementType()) return true
            if (child.tokenType != CjNodeTypes.IMPORT_ITEM && child.tokenType != CjNodeTypes.IMPORT_GROUP &&
                child.hasImportContainerErrors()
            ) return true
        }
        return false
    }

    /** 按标识符叶子构造路径；错误表达式不转换成前缀或源码文本路径。 */
    private fun LighterASTNode.importPathSegments(): List<Name>? {
        val segments = mutableListOf<Name>()
        tree.forEachChildren(this) { child ->
            when (child.tokenType) {
                CjTokens.IDENTIFIER -> segments += Name.identifier(sourceIdentifier(child.asText()))
                CjNodeTypes.REFERENCE_EXPRESSION,
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                    segments += child.importPathSegments() ?: return null
                }
                CjTokens.DOT, TokenType.WHITE_SPACE, CjTokens.NL -> Unit
                else -> if (!CjTokens.COMMENTS.contains(child.tokenType)) return null
            }
        }
        return segments.takeIf { it.isNotEmpty() }
    }

    /** 转换单个 import item。 */
    private fun convertImportItem(
        item: LighterASTNode,
        groupPrefix: ImportPathPrefix?,
        condition: CfirExpression?,
        visibility: org.cangnova.cangjie.descriptors.Visibility,
    ): CfirImport? {
        if (item.hasImportContainerErrors()) return null
        val itemPath = item.extractImportPath() ?: return null
        var isAllUnder = false
        var aliasName: Name? = null

        tree.forEachChildren(item) { child ->
            when (child.tokenType) {
                CjTokens.MUL -> isAllUnder = true
                CjNodeTypes.IMPORT_ALIAS -> {
                    val idNode = tree.findChildByType(child, CjTokens.IDENTIFIER)
                    aliasName = idNode?.let { Name.identifier(sourceIdentifier(it.asText())) }
                }
            }
        }

        val resolvedPath = groupPrefix.resolveImportPath(itemPath.fqName, itemPath.organizationName)
        return buildImport {
            source = item.toSource()
            importedFqName = resolvedPath.fqName
            organizationName = resolvedPath.organizationName
            this.isAllUnder = isAllUnder
            this.aliasName = aliasName
            this.condition = condition
            this.visibility = visibility
        }
    }

    /** 提取 import directive 前导 `@When[...]` 的 raw 条件。 */
    private fun LighterASTNode.whenConditionExpressionOrNull(): CfirExpression? {
        val annotation = findFirstDescendantByType(this, CjNodeTypes.ANNOTATION) ?: return null
        val name = annotationNameInfo(annotation)?.rawName ?: return null
        if (name != "When") return null
        val conditionNode = findFirstDescendantByType(annotation, CjNodeTypes.ANNOTATION_WHEN_CONDITION)
            ?: return null
        val expressionNode = expressionBuilder.findFirstExpression(conditionNode) ?: return null
        return expressionBuilder.convertExpression(expressionNode)
    }

    /** 构造文件顶层声明列表。 */
    private fun buildFileDeclarations(file: LighterASTNode): List<CfirDeclaration> {
        val declarations = mutableListOf<CfirDeclaration>()
        val pendingAnnotations = mutableListOf<LighterASTNode>()
        tree.forEachChildren(file) { child ->
            when (child.tokenType) {
                CjNodeTypes.FOREIGN -> {
                    check(file.tokenType == CjNodeTypes.CJ_FILE) {
                        "A foreign collection must be owned by the file declaration driver."
                    }
                    val foreignBlockAnnotations = pendingAnnotations.toList() +
                        (tree.findChildByType(child, CjStubElementTypes.ANNOTATIONS)?.let { list ->
                            tree.getChildrenByType(list, CjNodeTypes.ANNOTATION)
                        } ?: emptyList())
                    val foreignBody = tree.findChildByType(child, CjNodeTypes.FOREIGN_BODY)
                    if (foreignBody != null) {
                        buildFileDeclarations(foreignBody).forEach { declaration ->
                            if (foreignBlockAnnotations.isNotEmpty()) {
                                val inheritedAnnotations = foreignBlockAnnotations.mapNotNull { annotation ->
                                    val annotationName = annotationNameInfo(annotation) ?: return@mapNotNull null
                                    buildRawAnnotationCall(
                                        annotation = annotation,
                                        rawName = annotationName.rawName,
                                        carrier = declaration,
                                        annotationName = annotationName,
                                    )
                                }
                                declaration.replaceAnnotations(inheritedAnnotations + declaration.annotations)
                            }
                            declarations.add(declaration)
                        }
                    }
                    // These annotations belong to the foreign block, not to the
                    // declaration following it.
                    pendingAnnotations.clear()
                    return@forEachChildren
                }
                CjStubElementTypes.ANNOTATIONS -> {
                    tree.forEachChildren(child) { annotation ->
                        if (annotation.tokenType == CjNodeTypes.ANNOTATION || annotation.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
                            pendingAnnotations += annotation
                        }
                    }
                    return@forEachChildren
                }
                CjNodeTypes.ANNOTATION,
                    -> {
                    pendingAnnotations += child
                    return@forEachChildren
                }
                CjNodeTypes.MACRO_EXPRESSION -> {
                    val declaration = convertTopLevelMacroDeclaration(child)
                        ?: convertStandaloneMacroDeclaration(child)
                    if (pendingAnnotations.isNotEmpty()) {
                        collectMacroSurfacesFromAnnotations(
                            ownerNode = child,
                            modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList(), parsedModifiers = null),
                            ownerKind = MacroSurfaceOwnerKind.DECLARATION,
                            carrier = declaration,
                        )
                        pendingAnnotations.clear()
                    }
                    declarations.add(declaration)
                    return@forEachChildren
                }
            }
            if (LightTreeRawCfirExpressionBuilder.isDeclarationToken(child.tokenType)) {
                val declaration = convertDeclaration(child)
                if (pendingAnnotations.isNotEmpty()) {
                    collectMacroSurfacesFromAnnotations(
                        ownerNode = child,
                        modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList(), parsedModifiers = null),
                        ownerKind = MacroSurfaceOwnerKind.DECLARATION,
                        carrier = declaration,
                    )
                    pendingAnnotations.clear()
                }
                declarations.add(declaration)
            }
        }
        return declarations
    }

    /**
     * 文件级 `@Macro decl` 的 LightTree 形态是 MACRO_EXPRESSION，input
     * 内部才包含 carrier 声明。声明宏 surface 必须绑定这个 carrier，
     * 后续 stable splice 才能按对象身份替换最终 CFIR 声明。
     */
    private fun convertTopLevelMacroDeclaration(
        node: LighterASTNode,
    ): CfirDeclaration? {
        val chain = resolveTopLevelMacroDeclarationChain(node) ?: return null
        val (declarationNode, macroExpressions) = chain
        // primary 身份只能由 parser 的真实作用域产生，FUNC 不能按名称升级为构造器。
        return convertDeclaration(declarationNode, macroExpressions)
    }

    /**
     * 将声明列表中的独立宏调用保留为真实 CFIR 声明节点，并以节点身份建立 splice 槽。
     * 该节点只存在于 construction 阶段，展开结果会替换其所在的声明列表位置。
     */
    private fun convertStandaloneMacroDeclaration(node: LighterASTNode): CfirDeclaration {
        val surfaceId = MacroSurfaceIdGenerator.next()
        val declaration = buildSourceDeclaration(CfirMacroExpansionDeclarationSymbol()) { symbol ->
            buildMacroExpansionDeclaration {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
            }
        }
        val rawSyntax = node.asText()
        val headScan = checkNotNull(macroExpressionHeadSyntax(node)) {
            "A declaration macro call must retain its invocation head syntax."
        }
        val annotationName = checkNotNull(macroExpressionAnnotationNameInfo(node)) {
            "A declaration macro call must retain its invocation name."
        }
        val attrNode = tree.findChildByType(node, CjNodeTypes.MACRO_ATTR)
        val macroAttributeText = macroExpressionAttributeText(rawSyntax, headScan, attrNode)
        val macroAttributeSource = macroExpressionAttributeSource(node, headScan, attrNode)
        val surfaceSource = node.toSource().sliceMacroExpressionSource(
            MacroExpressionTextRange(headScan.annotationStartOffset, rawSyntax.length),
        )
        val inputNode = tree.findChildByType(node, CjNodeTypes.MACRO_INPUT)
        val qualifiedName = macroSurfaceQualifiedName(annotationName)
        val isCompileTimeVisible = tree.findChildByType(node, CjTokens.ATEXCL) != null
        val hasParenthesizedInput = headScan.hasParenthesizedInput
        collectedMacroSurfaces += MacroSurfaceDecl(
            surfaceId = surfaceId,
            qualifiedName = qualifiedName,
            isQualifiedName = annotationName.segmentTexts.size > 1,
            kind = if (isCompileTimeVisible) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
            parsingFacts = checkNotNull(macroParsingFacts(node)) {
                "A declaration macro surface must retain facts from the parser root that created it."
            },
            hasParenthesis = hasParenthesizedInput,
            attrTokens = tokenizeSurfacePayload(
                payload = macroAttributeText,
                baseOffset = macroAttributeSource?.startOffset ?: 0,
            ),
            inputTokens = tokenizeSurfacePayload(inputNode),
            sourceRange = MacroSurfaceSourceRange(
                source = surfaceSource,
                startOffset = surfaceSource.startOffset,
                endOffset = surfaceSource.endOffset,
            ),
            scopeContext = macroSurfaceScopeContext(),
            modifiers = emptyList(),
            carriedAnnotations = emptyList(),
            capturedRawSyntax = rawSyntax.substring(headScan.annotationStartOffset),
            containerContext = macroSurfaceContainerContext(node),
            replaceHandle = CfirReplaceHandle(
                handleId = surfaceId,
                ownerSnapshot = context.snapshotMacroOwner(),
                carrier = declaration,
            ),
        )
        return declaration
    }

    /**
     * LightTree 顶层 annotation 包裹会形成多层 MACRO_EXPRESSION 链。
     * 这里必须按链恢复最终 carrier 声明，并让每层 wrapper 都把 annotation-site
     * 附着到同一份 CFIR 声明对象上。
     */
    private fun resolveTopLevelMacroDeclarationChain(root: LighterASTNode): Pair<LighterASTNode, List<LighterASTNode>>? {
        val macroExpressions = mutableListOf<LighterASTNode>()
        var current: LighterASTNode? = root

        while (current != null && current.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
            macroExpressions += current
            val inputNode = tree.findChildByType(current, CjNodeTypes.MACRO_INPUT) ?: return null
            findDirectMacroExpressionChild(inputNode)?.let { macroExpression ->
                current = macroExpression
                return@let
            } ?: run {
                findDirectDeclarationChild(inputNode)?.let { declarationNode ->
                    return declarationNode to macroExpressions.toList()
                }
                current = null
            }
        }

        return null
    }

    /**
     * 将顶层 [CjNodeTypes.MACRO_EXPRESSION] 恢复成 CFIR annotation 与 macro surface。
     *
     * LightTree 中声明宏的 wrapper 与真实 carrier 声明分离，本方法负责把 wrapper 的语法快照、
     * annotation slot 与稳定替换句柄统一挂到 [carrier] 上，保证后续宏展开可以按同一个声明对象 splice。
     */
    private fun applyTopLevelMacroExpression(
        node: LighterASTNode,
        declarationNode: LighterASTNode,
        carrier: CfirDeclaration,
    ) {
        val inputNode = tree.findChildByType(node, CjNodeTypes.MACRO_INPUT) ?: return
        val rawWrapperText = node.asText()
        val headScan = macroExpressionHeadSyntax(node) ?: return
        val surfaceSource = node.toSource().sliceMacroExpressionSource(
            MacroExpressionTextRange(headScan.annotationStartOffset, rawWrapperText.length),
        )
        val annotationName = macroExpressionAnnotationNameInfo(node) ?: return
        val rawName = annotationName.rawName
        val surfaceId = MacroSurfaceIdGenerator.next()
        val attrNode = tree.findChildByType(node, CjNodeTypes.MACRO_ATTR)
        val macroAttributeText = macroExpressionAttributeText(rawWrapperText, headScan, attrNode)
        val macroAttributeSource = macroExpressionAttributeSource(node, headScan, attrNode)
        val annotationSource = macroExpressionAnnotationSource(node, annotationName, macroAttributeSource)
        val rawAnnotationSyntax = source.subSequence(annotationSource.startOffset, annotationSource.endOffset).toString()
        val isCompileTimeVisible = tree.findChildByType(node, CjTokens.ATEXCL) != null
        val hasParenthesizedInput = headScan.hasParenthesizedInput
        val containingSymbol = when (carrier) {
            is CfirValueParameter -> carrier.containingDeclarationSymbol
            else -> carrier.symbol
        }
        val annotationCall = buildRawAnnotationCall(
            annotation = node,
            rawName = rawName,
            containingSymbol = containingSymbol,
            sourceOverride = annotationSource,
            typeRefOverride = buildAnnotationTypeRef(annotationName),
            calleeReferenceSourceOverride = annotationName.calleeReferenceSource,
            argumentListSourceOverride = macroAttributeSource,
            macroAttributeOverride = attrNode,
            macroAttributeTextOverride = macroAttributeText,
            macroAttributeStartOffsetOverride = macroAttributeSource?.startOffset,
        )
        val annotationIndex = carrier.annotations.size
        carrier.replaceAnnotations(carrier.annotations + annotationCall)
        val annotationCarrier = annotationMetadata.record(
            CfirAnnotationSlotSnapshot(
                owner = carrier,
                annotationIndex = annotationIndex,
                originalAnnotation = annotationCall,
                rawSyntax = rawAnnotationSyntax,
                forcedCustom = isCompileTimeVisible,
                isCompileTimeVisible = isCompileTimeVisible,
                annotationSource = annotationSource,
                qualifiedName = macroSurfaceQualifiedName(annotationName),
                argumentText = macroAttributeText,
                tokens = tokenizeMacroExpressionAnnotationSyntax(
                    rawAnnotationSyntax,
                    annotationSource.startOffset,
                    annotationSource,
                ),
                callSite = MacroCallSite.DECLARATION,
            )
        )
        collectedMacroSurfaces += MacroSurfaceDecl(
            surfaceId = surfaceId,
            qualifiedName = macroSurfaceQualifiedName(annotationName),
            isQualifiedName = annotationName.segmentTexts.size > 1,
            kind = if (isCompileTimeVisible) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
            parsingFacts = macroParsingFacts(node),
            hasParenthesis = hasParenthesizedInput,
            attrTokens = tokenizeSurfacePayload(
                payload = macroAttributeText,
                baseOffset = macroAttributeSource?.startOffset ?: 0,
            ),
            inputTokens = macroExpressionDeclarationInputTokens(
                inputNode = inputNode,
                annotationSource = annotationSource,
                declarationNode = declarationNode,
                hasParenthesizedInput = hasParenthesizedInput,
            ),
            sourceRange = MacroSurfaceSourceRange(
                source = surfaceSource,
                startOffset = surfaceSource.startOffset,
                endOffset = surfaceSource.endOffset,
            ),
            scopeContext = macroSurfaceScopeContext(),
            modifiers = emptyList(),
            carriedAnnotations = emptyList(),
            capturedRawSyntax = rawWrapperText.substring(headScan.annotationStartOffset),
            containerContext = macroSurfaceContainerContext(declarationNode),
            replaceHandle = CfirReplaceHandle(
                handleId = surfaceId,
                ownerSnapshot = context.snapshotMacroOwner(),
                carrier = carrier,
                annotationCarrier = annotationCarrier,
            ),
        )
    }

    /** 从同一解析根的直接 invocation token 读取已冻结 facts，不从 CFIR owner 反推。 */
    private fun macroParsingFacts(node: LighterASTNode): MacroParsingFacts? {
        val invocationToken = tree.findChildByType(node, CjTokens.AT)
            ?: tree.findChildByType(node, CjTokens.ATEXCL)
            ?: error("A macro surface must contain its direct invocation token.")
        return macroFacts[invocationToken.startOffset]
    }

    /**
     * 捕获 declaration macro 的真实 input token。
     *
     * 官方 `@Outer @Inner decl` 是外层 macro 的声明输入包装内层 wrapper；LightTree 的
     * `MACRO_INPUT` 在无括号声明宏上可能只保留空占位，因此这里必须用 wrapper annotation
     * 头结束到最终 carrier 声明结束的源码切片恢复输入。
     */
    private fun macroExpressionDeclarationInputTokens(
        inputNode: LighterASTNode,
        annotationSource: CjSourceElement,
        declarationNode: LighterASTNode,
        hasParenthesizedInput: Boolean,
    ): List<MacroSurfaceToken> {
        if (hasParenthesizedInput) return tokenizeSurfacePayload(inputNode)
        return tokenizeSourceSlice(annotationSource.endOffset, declarationNode.endOffset)
    }

    /**
     * 收集 [node] 子树中 token 类型等于 [tokenType] 的所有后代节点。
     *
     * LightTree 没有 PSI 的 typed descendant API，这里集中封装递归遍历，避免各调用点重复处理树访问。
     */
    private fun findDescendantsByType(
        node: LighterASTNode,
        tokenType: com.intellij.psi.tree.IElementType,
    ): List<LighterASTNode> {
        val result = mutableListOf<LighterASTNode>()

        /** 递归访问当前节点及其子节点，并按目标 token 类型累积命中节点。 */
        fun visit(current: LighterASTNode) {
            if (current.tokenType == tokenType) {
                result += current
            }
            tree.forEachChildren(current, ::visit)
        }
        visit(node)
        return result
    }

    /**
     * 将 macro expression 还原出的 annotation 文本切分为 surface token。
     *
     * [baseOffset] 使用 wrapper 的源偏移，保证 token offset 与后续宏诊断、替换范围保持一致。
     */
    private fun tokenizeMacroExpressionAnnotationSyntax(
        rawAnnotationSyntax: String,
        baseOffset: Int,
        source: CjSourceElement,
    ): List<MacroSurfaceToken> =
        MacroPayloadTokenizer.tokenize(rawAnnotationSyntax, baseOffset).map { token ->
            MacroSurfaceToken(
                text = token.text,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
                kindName = token.kindName,
                sourceRange = MacroSurfaceSourceRange(source, token.startOffset, token.endOffset),
            )
        }

    /**
     * 在 [node] 子树中查找第一个声明节点。
     *
     * 该查询用于语法恢复场景，只返回 LightTree 声明 token，不会把表达式或 annotation 节点提升为声明。
     */
    private fun findFirstDeclarationDescendant(node: LighterASTNode): LighterASTNode? {
        if (LightTreeRawCfirExpressionBuilder.isDeclarationToken(node.tokenType)) return node
        tree.forEachChildren(node) { child ->
            findFirstDeclarationDescendant(child)?.let { return it }
        }
        return null
    }

    /**
     * 查找 [node] 的直接声明子节点。
     *
     * 顶层 macro declaration 链只允许 wrapper input 的直接 carrier 被绑定，避免跨层吞掉内层 wrapper。
     */
    private fun findDirectDeclarationChild(node: LighterASTNode): LighterASTNode? {
        var declarationNode: LighterASTNode? = null
        tree.forEachChildren(node) { child ->
            if (declarationNode == null && LightTreeRawCfirExpressionBuilder.isDeclarationToken(child.tokenType)) {
                declarationNode = child
            }
        }
        return declarationNode
    }

    /**
     * 查找 [node] 的直接 macro expression 子节点。
     *
     * 多层 annotation wrapper 解析依赖直接子节点关系，以保持 wrapper 顺序与源代码嵌套一致。
     */
    private fun findDirectMacroExpressionChild(node: LighterASTNode): LighterASTNode? {
        var macroExpression: LighterASTNode? = null
        tree.forEachChildren(node) { child ->
            if (macroExpression == null && child.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
                macroExpression = child
            }
        }
        return macroExpression
    }

    /**
     * 从 macro expression 中提取 annotation/macro 名称文本。
     *
     * 返回值保留限定名文本；空白或缺失名称返回 null，由上层跳过无效 surface。
     */
    private fun extractMacroExpressionNameText(node: LighterASTNode): String? {
        val reference = findMacroExpressionAnnotationNameNode(node) ?: return null
        return reference.asText().trim().takeIf { it.isNotEmpty() }
    }

    /**
     * 查找 macro expression 直接携带的名称节点。
     *
     * 支持限定名与普通引用两种 LightTree 形态，供兼容旧调用点使用。
     */
    private fun findMacroExpressionNameNode(node: LighterASTNode): LighterASTNode? {
        return tree.findChildByType(node, CjNodeTypes.DOT_QUALIFIED_EXPRESSION)
            ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
    }

    // ===== 通用提取辅助 =====

    /** 从声明节点提取名称 */
    private fun extractName(node: LighterASTNode): Name {
        val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
            ?: tree.findChildByType(node, CjNodeTypes.OPERATION_NAME)
            ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
        return if (nameNode != null) {
            if (nameNode.tokenType == CjNodeTypes.OPERATION_NAME) nameNode.asText().asOperatorName()
            else Name.identifier(sourceIdentifier(nameNode.asText()))
        } else {
            Name.special("<anonymous>")
        }
    }

    /**
     * 属性名必须来自 `prop` 后紧邻的声明名槽位。
     * 解析错误恢复后的后续标识符不能被当作属性名，否则会把无效声明推进到 CFIR resolve。
     */
    private fun extractPropertyName(node: LighterASTNode): Name {
        var afterPropKeyword = false
        var result: Name? = null

        tree.forEachChildren(node) { child ->
            if (result != null) return@forEachChildren
            when {
                child.tokenType == CjTokens.PROP_KEYWORD -> afterPropKeyword = true
                !afterPropKeyword -> Unit
                child.tokenType == CjTokens.IDENTIFIER -> result = Name.identifier(sourceIdentifier(child.asText()))
                child.tokenType == CjNodeTypes.OPERATION_NAME -> result = child.asText().asOperatorName()
                child.tokenType.isParsingErrorElementType() -> result = Name.special("<anonymous>")
            }
        }

        return result ?: Name.special("<anonymous>")
    }

    /**
     * 提取函数声明名称，并把操作符文本归一化为编译器约定名称。
     *
     * [valueParametersCount] 用于区分一元/二元 `+`、`-`，`[]` 通过参数形态进一步区分 get/set。
     */
    private fun extractFunctionName(node: LighterASTNode, valueParametersCount: Int): Name {
        val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
            ?: tree.findChildByType(node, CjNodeTypes.OPERATION_NAME)
            ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
            ?: return Name.special("<anonymous>")

        return when (val rawName = nameNode.asText()) {
            "-" -> if (valueParametersCount == 0) OperatorNameConventions.UNARY_MINUS else OperatorNameConventions.MINUS
            "+" -> if (valueParametersCount == 0) OperatorNameConventions.UNARY_PLUS else OperatorNameConventions.PLUS
            "[]" -> if (isSubscriptSetOperator(node)) OperatorNameConventions.SET else OperatorNameConventions.GET
            else -> if (nameNode.tokenType == CjNodeTypes.OPERATION_NAME) rawName.asOperatorName()
                    else Name.identifier(sourceIdentifier(rawName))
        }
    }

    /**
     * 判断 `[]` 操作符函数是否为索引赋值（SET）形式。
     *
     * 根据仓颉语言规范，索引赋值形式的特征是：
     * 最后一个参数为命名参数 `value!`（含 EXCL 标记且名称为 "value"）。
     *
     * - 索引取值：`operator func [](index1: T1, index2: T2, ...): R`
     * - 索引赋值：`operator func [](index1: T1, ..., value!: TN): R`
     */
    private fun isSubscriptSetOperator(funcNode: LighterASTNode): Boolean {
        val paramList = tree.findChildByType(funcNode, CjNodeTypes.VALUE_PARAMETER_LIST) ?: return false
        val params = tree.getChildrenByType(paramList, CjNodeTypes.VALUE_PARAMETER)
        val lastParam = params.lastOrNull() ?: return false
        val hasExcl = tree.findChildByType(lastParam, CjTokens.EXCL) != null
        if (!hasExcl) return false
        val nameNode = tree.findChildByType(lastParam, CjTokens.IDENTIFIER) ?: return false
        return nameNode.asText() == "value"
    }

    /** 提取类型参数列表 */
    private fun extractTypeParameters(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): List<CfirTypeParameter> {
        val typeParamList = tree.findChildByType(node, CjNodeTypes.TYPE_PARAMETER_LIST) ?: return emptyList()
        val typeConstraintBounds = collectTypeConstraintBounds(node)
        return tree.getChildrenByType(typeParamList, CjNodeTypes.TYPE_PARAMETER).map { typeParameter ->
            convertTypeParameter(
                typeParameter,
                containingDeclarationSymbol,
                typeConstraintBounds[typeParameterName(typeParameter)].orEmpty(),
            )
        }
    }

    /** 提取函数类型参数列表 */
    private fun extractFunctionTypeParameters(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): List<CfirTypeParameter> {
        val typeParamList = tree.findChildByType(node, CjNodeTypes.TYPE_PARAMETER_LIST) ?: return emptyList()
        val typeConstraintBounds = collectTypeConstraintBounds(node)
        return tree.getChildrenByType(typeParamList, CjNodeTypes.TYPE_PARAMETER).map { typeParameter ->
            convertTypeParameter(
                typeParameter,
                containingDeclarationSymbol,
                typeConstraintBounds[typeParameterName(typeParameter)].orEmpty(),
            )
        }
    }

    /** 转换单个类型参数（仓颉文法只允许 where 子句承载上界约束，类型参数节点自身不携带 bound） */
    private fun convertTypeParameter(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        additionalBounds: List<CfirTypeRef> = emptyList(),
    ): CfirTypeParameter {
        val name = typeParameterName(node)

        return buildSourceDeclaration(CfirTypeParameterSymbol()) { symbol ->
            buildTypeParameter {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
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

    /**
     * 提取类型参数名称。
     *
     * 语法错误导致缺失标识符时返回稳定错误名称，保证 raw CFIR 仍能保留声明骨架。
     */
    private fun typeParameterName(node: LighterASTNode): Name {
        return tree.findChildByType(node, CjTokens.IDENTIFIER)?.let { Name.identifier(sourceIdentifier(it.asText())) }
            ?: Name.identifier("<error>")
    }

    /**
     * 收集 [ownerNode] where/type constraint 子句声明的类型参数上界。
     *
     * 返回值按类型参数名分组，后续构造 [CfirTypeParameter] 时会把这些约束追加为 bounds。
     */
    private fun collectTypeConstraintBounds(ownerNode: LighterASTNode): Map<Name, List<CfirTypeRef>> {
        val typeConstraintList = tree.findChildByType(ownerNode, CjNodeTypes.TYPE_CONSTRAINT_LIST) ?: return emptyMap()
        val boundsByParameter = linkedMapOf<Name, MutableList<CfirTypeRef>>()

        tree.getChildrenByType(typeConstraintList, CjNodeTypes.TYPE_CONSTRAINT).forEach { constraint ->
            val parameterNameNode = tree.findChildByType(constraint, CjNodeTypes.REFERENCE_EXPRESSION)
                ?: tree.findChildByType(constraint, CjTokens.IDENTIFIER)
                ?: return@forEach
            val parameterName = Name.identifier(sourceIdentifier(parameterNameNode.asText()))
            val boundRefs = tree.getChildrenByType(constraint, CjNodeTypes.TYPE_REFERENCE).map(::convertTypeRef)
            if (boundRefs.isEmpty()) return@forEach

            boundsByParameter.getOrPut(parameterName) { mutableListOf() }.addAll(boundRefs)
        }

        return boundsByParameter
    }

    /** 提取超类型引用列表 */
    private fun collectTypeConstraintDiagnosticData(ownerNode: LighterASTNode): CfirTypeConstraintDiagnosticData? {
        val typeConstraints = tree.findChildByType(ownerNode, CjNodeTypes.TYPE_CONSTRAINT_LIST)
            ?.let { tree.getChildrenByType(it, CjNodeTypes.TYPE_CONSTRAINT) }
            .orEmpty()
            .mapNotNull { constraint ->
                val parameterNameNode = tree.findChildByType(constraint, CjNodeTypes.REFERENCE_EXPRESSION)
                    ?: tree.findChildByType(constraint, CjTokens.IDENTIFIER)
                    ?: return@mapNotNull null
                CfirTypeConstraintReference(
                    parameterName = Name.identifier(sourceIdentifier(parameterNameNode.asText())),
                    source = parameterNameNode.toSource(),
                    boundTypeRefs = tree.getChildrenByType(constraint, CjNodeTypes.TYPE_REFERENCE).map(::convertTypeRef),
                    constraintSource = constraint.toSource(),
                )
            }

        if (typeConstraints.isEmpty()) return null

        return CfirTypeConstraintDiagnosticData(
            typeConstraints = typeConstraints,
        )
    }

    /**
     * 汇总声明节点上仅供诊断使用的附加属性。
     *
     * 目前保留 type constraint 与函数体参数列表的源位置信息；没有任何附加数据时返回共享 EMPTY 实例。
     */
    private fun declarationAttributes(ownerNode: LighterASTNode): CfirDeclarationAttributes {
        var hasAttributes = false
        val attributes = CfirDeclarationAttributes()
        val keywordType = when (ownerNode.tokenType) {
            CjNodeTypes.CLASS -> CjTokens.CLASS_KEYWORD
            CjNodeTypes.STRUCT -> CjTokens.STRUCT_KEYWORD
            CjNodeTypes.INTERFACE -> CjTokens.INTERFACE_KEYWORD
            CjNodeTypes.ENUM -> CjTokens.ENUM_KEYWORD
            CjNodeTypes.EXTEND -> CjTokens.EXTEND_KEYWORD
            else -> null
        }
        if (keywordType != null) {
            val keyword = checkNotNull(tree.findChildByType(ownerNode, keywordType)) {
                "Source nominal declaration has no declaration keyword."
            }
            val facts = checkNotNull(nominalFacts[keyword.startOffset]) {
                "Source nominal declaration is missing its parser facts."
            }
            attributes.cjmpSourceNominalHasDefault = facts.hasCjmpCommonDefault(baseSession.languageVersionSettings)
            hasAttributes = true
        }
        if (tree.getChildrenByType(ownerNode, CjNodeTypes.TYPE_PARAMETER_LIST).isNotEmpty()) {
            attributes.hasGenericNode = true
            hasAttributes = true
        }

        collectTypeConstraintDiagnosticData(ownerNode)?.let { diagnosticData ->
            attributes.typeConstraintDiagnosticData = diagnosticData
            hasAttributes = true
        }

        collectFunctionBodyDiagnosticData(ownerNode)?.let { diagnosticData ->
            attributes.functionBodyDiagnosticData = diagnosticData
            hasAttributes = true
        }

        return if (hasAttributes) attributes else CfirDeclarationAttributes.EMPTY
    }

    /**
     * 收集函数体相关诊断需要的参数列表源信息。
     *
     * LightTree 错误恢复可能产生多个参数列表，保留这些 source 可让 checker 在 CFIR 阶段定位重复列表。
     */
    private fun collectFunctionBodyDiagnosticData(ownerNode: LighterASTNode): CfirFunctionBodyDiagnosticData? {
        val parameterLists = tree.getChildrenByType(ownerNode, CjNodeTypes.VALUE_PARAMETER_LIST)
            .map { parameterList ->
                CfirValueParameterListReference(
                    source = parameterList.toSource(),
                )
            }

        if (parameterLists.size <= 1) return null

        return CfirFunctionBodyDiagnosticData(
            valueParameterLists = parameterLists,
        )
    }

    /**
     * 提取 class-like 或 extend 声明的直接超类型引用。
     *
     * 只读取 [CjNodeTypes.SUPER_TYPE_LIST] 下的 [CjNodeTypes.SUPER_TYPE_ENTRY]，避免误收 body 内部类型。
     */
    private fun extractSuperTypeRefs(node: LighterASTNode): List<CfirTypeRef> {
        val superTypeList = tree.findChildByType(node, CjNodeTypes.SUPER_TYPE_LIST) ?: return emptyList()
        val superTypeEntries = tree.getChildrenByType(superTypeList, CjNodeTypes.SUPER_TYPE_ENTRY)
        return superTypeEntries.mapNotNull { entry ->
            val typeRef = tree.findChildByType(entry, CjNodeTypes.TYPE_REFERENCE)
            typeRef?.let { convertTypeRef(it) }
        }
    }

    /** 按源码顺序收集直接及宏包装成员，枚举构造项也通过声明入口 lowering 一次。 */
    private fun extractClassMembers(
        node: LighterASTNode,
    ): List<CfirDeclaration> {
        // 查找 CLASS_BODY / INTERFACE_BODY / ENUM_BODY 等
        val bodyNode = tree.findChildByType(node, CjNodeTypes.CLASS_BODY)
            ?: tree.findChildByType(node, CjNodeTypes.INTERFACE_BODY)
            ?: tree.findChildByType(node, CjNodeTypes.ENUM_BODY)
            ?: return emptyList()

        val declarations = mutableListOf<CfirDeclaration>()
        val pendingAnnotations = mutableListOf<LighterASTNode>()
        tree.forEachChildren(bodyNode) { child ->
            val tt = child.tokenType
            when (tt) {
                CjStubElementTypes.ANNOTATIONS -> {
                    tree.forEachChildren(child) { annotation ->
                        if (annotation.tokenType == CjNodeTypes.ANNOTATION || annotation.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
                            pendingAnnotations += annotation
                        }
                    }
                    return@forEachChildren
                }
                CjNodeTypes.ANNOTATION,
                    -> {
                    pendingAnnotations += child
                    return@forEachChildren
                }
                CjNodeTypes.MACRO_EXPRESSION -> {
                    val declaration = convertTopLevelMacroDeclaration(child)
                        ?: convertStandaloneMacroDeclaration(child)
                    if (pendingAnnotations.isNotEmpty()) {
                        collectMacroSurfacesFromAnnotations(
                            ownerNode = child,
                            modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList(), parsedModifiers = null),
                            ownerKind = MacroSurfaceOwnerKind.DECLARATION,
                            carrier = declaration,
                        )
                        pendingAnnotations.clear()
                    }
                    declarations.add(declaration)
                    return@forEachChildren
                }
            }
            if (LightTreeRawCfirExpressionBuilder.isDeclarationToken(tt)) {
                val declaration = convertDeclaration(child)
                if (pendingAnnotations.isNotEmpty()) {
                    collectMacroSurfacesFromAnnotations(
                        ownerNode = child,
                        modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList(), parsedModifiers = null),
                        ownerKind = MacroSurfaceOwnerKind.DECLARATION,
                        carrier = declaration,
                    )
                    pendingAnnotations.clear()
                }
                declarations.add(declaration)
            }
        }
        return declarations
    }

    /**
     * LightTree 路径与 PSI 路径保持同一语言约束：
     * 非顶层 class-like 不再继续构造 `ClassId`，而是直接转为 invalid declaration。
     */
    private fun buildInvalidClassLikeDeclaration(
        source: org.cangnova.cangjie.source.AbstractCjSourceElement,
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
                reason = "Cangjie only supports top-level $kind declarations, but found illegal non-top-level declaration: $name"
            }
        }
    }

    /** 只检查原始函数块的直接语句；嵌套空块本身也是语句，不能展开后再判空。 */
    private fun hasEmptyFunctionBlock(node: LighterASTNode): Boolean {
        val block = tree.findChildByType(node, CjNodeTypes.BLOCK) ?: return false
        tree.forEachChildren(block) { child ->
            if (LightTreeRawCfirExpressionBuilder.isDeclarationToken(child.tokenType) ||
                LightTreeRawCfirExpressionBuilder.isExpressionToken(child.tokenType)
            ) return false
        }
        return true
    }

    /** 提取声明自身的类型引用，避免把参数、约束或 body 内部的类型误当成声明返回类型。 */
    private fun extractReturnTypeRef(node: LighterASTNode): CfirTypeRef {
        val typeRef = when (node.tokenType) {
            CjNodeTypes.FUNC,
            CjNodeTypes.MAIN_FUNC,
            CjNodeTypes.MACRO,
                -> findFunctionLikeReturnTypeRef(node)
            else -> tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)
        }
        return convertTypeRef(typeRef)
    }

    /**
     * 查找函数形声明冒号后的直接返回类型引用。
     *
     * 该方法只扫描声明直接子节点，避免把参数类型、泛型约束或函数体内部类型当作返回类型。
     */
    private fun findFunctionLikeReturnTypeRef(node: LighterASTNode): LighterASTNode? {
        var isReturnType = false
        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjTokens.COLON -> isReturnType = true
                CjNodeTypes.TYPE_REFERENCE -> if (isReturnType) return child
            }
        }
        return null
    }

    /** 提取值参数列表 */
    private fun extractValueParameters(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        signature: MacroParameterSignature,
    ): List<CfirValueParameter> {
        return extractValueParameterNodes(node)
            .map { convertValueParameter(it, containingDeclarationSymbol, signature) }
    }

    /**
     * 提取声明直接参数列表中的值参数节点。
     *
     * 参数列表数量与投影都来自当前声明的所有直接列表；重复列表的全部 owner 按源码顺序保留。
     */
    private fun extractValueParameterNodes(node: LighterASTNode): List<LighterASTNode> {
        val lists = tree.getChildrenByType(node, CjNodeTypes.VALUE_PARAMETER_LIST)
        val parameters = lists.flatMap { tree.getChildrenByType(it, CjNodeTypes.VALUE_PARAMETER) }
        val projection = parameterProjection(node, lists.size, parameters)
        return parameters.filterIndexed { index, _ -> projection.retains(index) }
    }

    /** flag 与参数删除共用官方完整声明投影，多列表不会仅由首个省略号置位。 */
    private fun hasVariableLengthArgument(node: LighterASTNode): Boolean {
        val lists = tree.getChildrenByType(node, CjNodeTypes.VALUE_PARAMETER_LIST)
        val parameters = lists.flatMap { tree.getChildrenByType(it, CjNodeTypes.VALUE_PARAMETER) }
        return parameterProjection(node, lists.size, parameters).hasVariableLengthArgument
    }

    /** 实际文法节点确定声明种类，FOREIGN 继续读取同树 parser 返回属性。 */
    private fun parameterProjection(
        node: LighterASTNode,
        parameterListCount: Int,
        parameters: List<LighterASTNode>,
    ): CjVariadicParameterRules.Result {
        val kind = when (node.tokenType) {
            CjNodeTypes.FUNC -> CjmpModifierDeclarationKind.FUNCTION
            CjNodeTypes.PRIMARY_CONSTRUCTOR -> CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR
            CjNodeTypes.SECONDARY_CONSTRUCTOR -> CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR
            CjNodeTypes.MACRO -> CjmpModifierDeclarationKind.MACRO
            CjNodeTypes.MAIN_FUNC -> CjmpModifierDeclarationKind.MAIN
            CjNodeTypes.FINALIZER -> CjmpModifierDeclarationKind.FINALIZER
            CjNodeTypes.PROPERTY_ACCESSOR -> CjmpModifierDeclarationKind.OTHER
            else -> error("Value parameters require an actual callable grammar owner: ${node.tokenType}")
        }
        return CjVariadicParameterRules.select(kind, parameterListCount, modifierListFor(node).isForeign,
            parameters.map(::isVariableLengthParameter))
    }

    /** 只读取实际参数的直接省略号词元，不把默认值或注解文本当成变长参数。 */
    private fun isVariableLengthParameter(node: LighterASTNode): Boolean = tree.findChildByType(node, CjTokens.ELLIPSIS) != null

    /**
     * 统计声明签名中的值参数数量。
     *
     * 主要服务操作符名称归一化，尤其是一元/二元 `+`、`-` 的区分。
     */
    private fun countValueParameters(node: LighterASTNode): Int {
        return extractValueParameterNodes(node).size
    }

    /** 有函数体时保留惰性块，无体声明则保持 null。 */
    private fun extractBody(node: LighterASTNode): CfirBlock? {
        val blockNode = tree.findChildByType(node, CjNodeTypes.BLOCK) ?: return null
        return if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) buildLazyBlock()
        else expressionBuilder.convertBlock(blockNode)
    }

    /**
     * 提取属性体中显式声明的 getter/setter 节点。
     *
     * 无属性体或无 accessor 时返回空列表，调用方据此判断默认、抽象和隐式 accessor 语义。
     */
    private fun extractPropertyAccessorNodes(node: LighterASTNode): List<LighterASTNode> {
        val propertyBody = tree.findChildByType(node, CjNodeTypes.PROPERTY_BODY) ?: return emptyList()
        return tree.getChildrenByType(propertyBody, CjNodeTypes.PROPERTY_ACCESSOR)
    }

    /**
     * 判断属性 accessor 节点是否为 getter。
     *
     * LightTree accessor 没有独立强类型模型，当前以 `get` 关键字存在性区分 getter 与 setter。
     */
    private fun isGetterAccessor(node: LighterASTNode): Boolean {
        return tree.findChildByType(node, CjTokens.GET_KEYWORD) != null
    }

    /** 存在初值时保留惰性节点，BODY_RESOLVE 再构建表达式。 */
    private fun buildVariableInitializer(node: LighterASTNode): CfirExpression? {
        // 查找 = 之后的表达式
        var afterEq = false
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjTokens.EQ) {
                afterEq = true
            } else if (afterEq && LightTreeRawCfirExpressionBuilder.isExpressionToken(child.tokenType)) {
                return if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                    buildLazyExpression { source = child.toSource() }
                } else {
                    expressionBuilder.convertExpression(child)
                }
            }
        }
        return null
    }

    /** 类型引用转换 */
    private fun convertTypeRef(typeRefNode: LighterASTNode?): CfirTypeRef {
        return convertTypeReference(typeRefNode, tree, source) { it.toSourceElement() }
    }

    /** 判断是否有 var 关键字 */
    private fun hasVarKeyword(node: LighterASTNode): Boolean {
        return tree.childrenThroughErrors(node).any { it.tokenType == CjTokens.VAR_KEYWORD }
    }

    /** 判断变量声明关键字是否为 const。 */
    private fun hasConstKeyword(node: LighterASTNode): Boolean {
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjTokens.CONST_KEYWORD) return true
        }
        return false
    }

    /**
     * 将源码中的 `const` 变量声明关键字同步到声明状态。
     *
     * 仅 [CfirDeclarationStatusImpl] 支持原地标记；其他实现保持不变以尊重状态对象边界。
     */
    private fun CfirDeclarationStatus.withConstDeclarationKeyword(hasConstKeyword: Boolean): CfirDeclarationStatus {
        if (hasConstKeyword && this is CfirDeclarationStatusImpl) {
            isConst = true
        }
        return this
    }

    /** 查找模式子节点 */
    private fun findPatternChild(node: LighterASTNode): LighterASTNode? {
        tree.forEachChildren(node) { child ->
            if (LightTreeRawCfirExpressionBuilder.isPatternToken(child.tokenType)) return child
            // 与 PSI 相同，仅穿透 parser 错误包装，保留原始根模式，不选择任意嵌套 binding。
            if (child.tokenType.isParsingErrorElementType()) findPatternChild(child)?.let { return it }
        }
        return null
    }

    /**
     * 为模式绑定中的单个命名变量构造 CFIR 变量声明。
     *
     * 该声明使用 fake source 指回模式绑定槽位，并复制外层变量状态，保证模式分解出的变量可被后续 scope/resolve 独立处理。
     */
    internal fun createPatternBindingVariable(
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

    /**
     * 判断接口成员函数是否应按 default 实现处理。
     *
     * 官方 `ParserImpl::SetDefaultFunc`（`ParseDecl.cpp:874-898`）的规则是
     * `scopeKind == INTERFACE_BODY && !FOREIGN && !ABSTRACT` —— **DEFAULT 是 ABSTRACT 的补集**。
     * 因此这里与隐式 abstract 推断**共用同一个结论**（[isImplicitAbstractClassLikeFunction]），
     * 而不是再独立判一次 `hasSyntaxBody`：在 `.cj` 下两者等价，到 `.cj.d` 会分叉 ——
     * 护栏 1 抑制了无体函数的隐式 abstract 后，官方会给这种函数置 DEFAULT。
     */
    private fun isDefaultInterfaceFunction(
        node: LighterASTNode,
        modifiers: LightTreeModifierList,
        isImplicitAbstract: Boolean,
    ): Boolean =
        isInInterfaceMemberContext() &&
                !modifiers.isForeign &&
                !modifiers.isAbstract &&
                !isImplicitAbstract

    /**
     * 判断接口属性是否应标记为 default 实现。
     *
     * 与函数同理：`DEFAULT := !ABSTRACT`（官方 PROP_DECL 分支见 `ABSTRACT` 即 return）。
     * 用"有效 abstract"而非"是否存在 accessor body"，可同时消除 PSI / LightTree 两条路径
     * 在"接口属性带 `{}` 但 accessor 无体"上的既有分歧。
     */
    private fun isDefaultInterfaceProperty(
        modifiers: LightTreeModifierList,
        isImplicitAbstract: Boolean,
    ): Boolean =
        isInInterfaceMemberContext() &&
                !modifiers.isAbstract &&
                !isImplicitAbstract

    /**
     * 判断接口属性 accessor 是否应标记为 default。
     *
     * accessor 级别的 default 只依赖该 accessor 是否有语法 body，而不继承属性整体的判断结果。
     */
    private fun isDefaultInterfaceAccessor(node: LighterASTNode, modifiers: LightTreeModifierList): Boolean =
        isInInterfaceMemberContext() &&
                !modifiers.isAbstract &&
                hasSyntaxBody(node)

    /**
     * 判断当前声明转换是否处在接口成员上下文。
     *
     * 本地声明永远不按接口成员处理，容器符号必须是 [CfirInterfaceSymbol]。
     */
    private fun isInInterfaceMemberContext(): Boolean =
        !context.inLocalContext && context.containingClassSymbolIfAny is CfirInterfaceSymbol

    /** LightTree 只提取语法事实；implicit abstract 的语言规则由共享 raw-CFIR resolver 决定。 */
    private fun isImplicitAbstractClassLikeFunction(
        node: LighterASTNode,
        modifiers: LightTreeModifierList,
    ): Boolean = isImplicitAbstractClassLikeMember(
        memberKind = CfirImplicitAbstractMemberKind.FUNCTION,
        modifiers = modifiers,
        hasBody = hasSyntaxBody(node),
        hasExplicitAccessors = false,
    )

    /**
     * 当前 class 体的修饰符（供 CJMP 隐式 abstract 例外判定；interface/struct/enum 体为 null）。
     */
    private var enclosingClassModifiers: LightTreeModifierList? = null

    /** 在 class 成员提取期间记录外层 class 修饰符。 */
    private inline fun <T> withEnclosingClassModifiers(modifiers: LightTreeModifierList, block: () -> T): T {
        val previous = enclosingClassModifiers
        enclosingClassModifiers = modifiers
        try {
            return block()
        } finally {
            enclosingClassModifiers = previous
        }
    }

    /** 将当前 LightTree 节点的语法事实投影到共享隐式 abstract 规则。 */
    private fun isImplicitAbstractClassLikeMember(
        memberKind: CfirImplicitAbstractMemberKind,
        modifiers: LightTreeModifierList,
        hasBody: Boolean,
        hasExplicitAccessors: Boolean,
    ): Boolean {
        val containerKind = if (context.inLocalContext) {
            CfirImplicitAbstractContainerKind.OTHER
        } else {
            when (context.containingClassSymbolIfAny) {
                is CfirClassSymbol -> CfirImplicitAbstractContainerKind.CLASS
                is CfirInterfaceSymbol -> CfirImplicitAbstractContainerKind.INTERFACE
                else -> CfirImplicitAbstractContainerKind.OTHER
            }
        }
        val enclosing = enclosingClassModifiers
        return CfirImplicitAbstractStatusResolver.isImplicitAbstract(
            CfirImplicitAbstractMemberFacts(
                memberKind = memberKind,
                containerKind = containerKind,
                sourceKind = sourceKind,
                isForeign = modifiers.isForeign,
                hasIntrinsicAnnotation = memberKind == CfirImplicitAbstractMemberKind.FUNCTION &&
                        modifiers.hasBuiltinAnnotation(BuiltInAnnotationKind.INTRINSIC),
                isCommon = modifiers.cjmpDeclarationRole().isCommon,
                isExplicitlyAbstract = modifiers.isAbstract,
                containerIsCommon = enclosing?.cjmpDeclarationRole()?.isCommon == true,
                containerIsSpecific = enclosing?.cjmpDeclarationRole()?.isSpecific == true,
                containerIsAbstract = enclosing?.isAbstract == true,
                hasBody = hasBody,
                hasExplicitAccessors = hasExplicitAccessors,
            ),
        )
    }

    /**
     * 查询 LightTree modifier 中的语言内置注解身份。
     *
     * intrinsic 的无体函数不能进入隐式 abstract/default 推断；身份来自 annotation
     * 节点名称和 common registry，而不是从整段源码文本猜测。
     */
    private fun LightTreeModifierList.hasBuiltinAnnotation(
        kind: BuiltInAnnotationKind,
    ): Boolean = annotations.any { annotation ->
        val rawName = annotationNameInfo(annotation)?.rawName ?: return@any false
        // Keep the complete source spelling as the lookup key.  A qualified
        // annotation may have the same short name as a builtin but is a user
        // annotation until type resolution proves otherwise.
        BuiltInAnnotationRegistry.findLanguageBuiltIn(rawName)?.kind == kind
    }

    /**
     * 判断 class/interface 成员属性是否应由无体语法隐式标记为 abstract。
     *
     * foreign 属性不走隐式 abstract；存在属性体或显式 accessor 时由更具体的 accessor/default 逻辑处理。
     */
    private fun isImplicitAbstractClassLikeProperty(
        node: LighterASTNode,
        modifiers: LightTreeModifierList,
        accessors: List<LighterASTNode>,
    ): Boolean = isImplicitAbstractClassLikeMember(
        memberKind = CfirImplicitAbstractMemberKind.PROPERTY,
        modifiers = modifiers,
        hasBody = hasPropertyBody(node),
        hasExplicitAccessors = accessors.isNotEmpty(),
    )

    /**
     * 判断节点是否直接拥有语法函数体块。
     *
     * 该检查只看 LightTree 结构，不读取已经构造出的 CFIR body。
     */
    private fun hasSyntaxBody(node: LighterASTNode): Boolean =
        tree.findChildByType(node, CjNodeTypes.BLOCK) != null

    /**
     * 判断属性节点是否直接拥有属性体。
     *
     * 属性体存在意味着 getter/setter 语义需要由属性体进一步拆分，而不是简单按无体成员处理。
     */
    private fun hasPropertyBody(node: LighterASTNode): Boolean =
        tree.findChildByType(node, CjNodeTypes.PROPERTY_BODY) != null

    /**
     * 统一执行 source declaration builder 并返回构造出的声明。
     *
     * 该入口保留 symbol 与 declaration 的泛型关联，供所有 LightTree raw CFIR 声明构造路径共享。
     */
    private inline fun <D : CfirDeclaration, S : CfirBasedSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)

        return declaration
    }
}
