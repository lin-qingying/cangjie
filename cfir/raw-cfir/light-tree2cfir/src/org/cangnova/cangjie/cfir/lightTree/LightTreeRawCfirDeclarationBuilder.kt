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

import com.intellij.lang.LighterASTNode
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.builder.AbstractRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.Context
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.correspondingProperty
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
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.CjNodeTypes
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
) : AbstractLightTreeRawCfirBuilder(session, tree, source, context) {

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
        val inInterfaceContext = !context.inLocalContext && containerSymbolIfAny is CfirInterfaceSymbol
        return toDeclarationStatus(
            context.inLocalContext,
            inInterfaceContext,
            defaultVisibility,
            isDefault,
            isImplicitAbstract,
        )
    }

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

    /** LightTree 文件构建必须使用带 sourceFile/linesMapping 的专用入口。 */
    override fun buildFile(file: LighterASTNode): CfirFile {
        error("Use buildCfirFile(lightTreeRoot, sourceFile, linesMapping) for LightTree file conversion")
    }

    /** 构建 LightTree 文件根节点对应的 [CfirFile]。 */
    fun buildCfirFile(
        file: LighterASTNode,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): CfirFile {
        // 解析包名
        val packageNode = tree.findChildByType(file, CjNodeTypes.PACKAGE_DIRECTIVE)
        val packageFqName = packageNode?.let { extractPackageFqName(it) } ?: FqName.ROOT

        return withPackageContext(packageFqName) {
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

                    packageDirective = buildPackageDirectiveNode(packageNode, packageFqName)
                    imports.addAll(buildImportsFromFile(file))
                    declarations.addAll(buildFileDeclarations(file))
                }
            }
        }
    }

    /** 从 LightTree 声明节点构建 raw CFIR 声明。 */
    override fun buildDeclaration(declaration: LighterASTNode): CfirDeclaration =
        convertDeclaration(declaration)

    /**
     * Macro fragment reparse 入口。
     *
     * LightTree fragment 通过 wrapper parse 获得目标 node 后，必须显式继承
     * 原 macro 位点包上下文，避免临时 wrapper 文件污染 fragment symbol。
     */
    fun buildDeclarationInPackage(declaration: LighterASTNode, packageFqName: FqName): CfirDeclaration {
        return withPackageContext(packageFqName) {
            convertDeclaration(declaration)
        }
    }

    /** 从 LightTree 表达式节点构建 raw CFIR 表达式。 */
    override fun buildExpression(expression: LighterASTNode): CfirExpression =
        expressionBuilder.convertExpression(expression)

    /**
     * Macro expression fragment reparse 入口。
     */
    fun buildExpressionInPackage(expression: LighterASTNode, packageFqName: FqName): CfirExpression {
        return withPackageContext(packageFqName) {
            expressionBuilder.convertExpression(expression)
        }
    }

    /**
     * Macro parameter fragment reparse 入口。
     *
     * 参数 fragment 必须复用原宿主 callable symbol；不能把 wrapper 函数的
     * containing symbol 带入最终 CFIR。
     */
    fun buildValueParameterInPackage(
        parameter: LighterASTNode,
        containingSymbol: CfirBasedSymbol<*>,
        packageFqName: FqName,
    ): CfirValueParameter {
        return withPackageContext(packageFqName) {
            convertValueParameter(parameter, containingSymbol)
        }
    }

    /**
     * Macro custom annotation fragment reparse 入口。
     *
     * 直接从 annotation light-tree node 构造 [CfirAnnotationCall]，不通过
     * 临时参数 fragment 间接提取 annotation，避免 annotation slot 语法被参数语法改写。
     */
    fun buildAnnotationCallInPackage(
        annotation: LighterASTNode,
        containingSymbol: CfirBasedSymbol<*>,
        packageFqName: FqName,
        sourceOverride: CjSourceElement? = null,
        argumentListSourceOverride: CjSourceElement? = null,
    ): CfirAnnotationCall? {
        val annotationName = annotationNameInfo(annotation) ?: return null
        val sourceOffsetDelta = sourceOffsetDelta(sourceOverride, annotation)
        return withPackageContext(packageFqName) {
            buildRawAnnotationCall(
                annotation = annotation,
                rawName = annotationName.rawName,
                containingSymbol = containingSymbol,
                sourceOverride = sourceOverride,
                typeRefOverride = buildAnnotationTypeRef(annotationName, sourceOffsetDelta),
                calleeReferenceSourceOverride = annotationName.calleeReferenceSource.shiftedBy(sourceOffsetDelta),
                argumentListSourceOverride = argumentListSourceOverride,
            )
        }
    }

    /**
     * LightTree parser 会把 declaration 前的 `@Anno` 解析为 MACRO_EXPRESSION。
     * custom annotation reparse 需要把该表示重新落回 annotation payload。
     */
    fun buildMacroExpressionAnnotationCallInPackage(
        macroExpression: LighterASTNode,
        containingSymbol: CfirBasedSymbol<*>,
        packageFqName: FqName,
        sourceOverride: CjSourceElement? = null,
        argumentListSourceOverride: CjSourceElement? = null,
    ): CfirAnnotationCall? {
        val annotationName = macroExpressionAnnotationNameInfo(macroExpression) ?: return null
        val sourceOffsetDelta = sourceOffsetDelta(sourceOverride, macroExpression)
        return withPackageContext(packageFqName) {
            buildRawAnnotationCall(
                annotation = macroExpression,
                rawName = annotationName.rawName,
                containingSymbol = containingSymbol,
                sourceOverride = sourceOverride,
                typeRefOverride = buildAnnotationTypeRef(annotationName, sourceOffsetDelta),
                calleeReferenceSourceOverride = annotationName.calleeReferenceSource.shiftedBy(sourceOffsetDelta),
                argumentListSourceOverride = argumentListSourceOverride,
            )
        }
    }

    // ===== 声明转换入口 =====

    /** 按 LightTree 声明节点 token type 分派到具体声明转换函数。 */
    fun convertDeclaration(node: LighterASTNode): CfirDeclaration {
        val modifiers = LightTreeModifierList.from(tree, node)

        val declaration = when (node.tokenType) {
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
        collectMacroSurfacesFromAnnotations(node, modifiers, MacroSurfaceOwnerKind.DECLARATION, declaration)
        return declaration
    }

    // ===== 类/接口/结构体/枚举 =====

    /** 转换 class、interface、struct、enum 等 class-like 声明。 */
    private fun convertClass(node: LighterASTNode, classKind: CfirClassKind): CfirDeclaration {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val superTypes = extractSuperTypeRefs(node)

        if (!canDeclareTopLevelClassLike()) {
            return buildInvalidClassLikeDeclaration(
                source = node.toSource(),
                kind = classKind.name.lowercase(),
                name = name,
            )
        }

        val classId = topLevelClassId(name)
        return when (classKind) {
            CfirClassKind.CLASS -> buildSourceDeclaration(CfirClassSymbol(classId)) { symbol ->
                buildClass {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    val (typeParams, classDeclarations) = withContainerSymbol(symbol) {
                        val typeParameters = extractTypeParameters(node, symbol)
                        val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
                            extractClassMembers(node).toMutableList().also { declarations ->
                                addPrimaryConstructorParameterProperties(node, declarations)
                                if (declarations.none { it is CfirConstructor }) {
                                    declarations.add(0, buildImplicitPrimaryConstructor(node))
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
                        val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
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
                        val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
                            extractClassMembers(node).toMutableList().also { declarations ->
                                addPrimaryConstructorParameterProperties(node, declarations)
                                if (declarations.none { it is CfirConstructor }) {
                                    declarations.add(0, buildImplicitPrimaryConstructor(node))
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
                        val declarations = withDispatchReceiverType(symbol.rawDispatchReceiverType(typeParameters)) {
                            extractClassMembers(node).toMutableList().also { declarations ->
                                addPrimaryConstructorParameterProperties(node, declarations)
                                if (declarations.none { it is CfirConstructor }) {
                                    declarations.add(0, buildImplicitPrimaryConstructor(node))
                                }
                                val enumBody = tree.findChildByType(node, CjNodeTypes.ENUM_BODY)
                                if (enumBody != null) {
                                    val enumCtors = tree.getChildrenByType(enumBody, CjNodeTypes.ENUM_CONSTRUCTOR)
                                        .map { convertEnumConstructor(it) }
                                    declarations.addAll(0, enumCtors)
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
                    this.isRefEnum = false
                    val enumBody = tree.findChildByType(node, CjNodeTypes.ENUM_BODY)
                    this.isNonExhaustive = enumBody != null && tree.findChildByType(enumBody, CjTokens.ELLIPSIS) != null
                }
            }
        }
    }

    /**
     * 主构造 `let/const/var` 参数同时声明同名成员。
     *
     * LightTree 路径必须与 PSI raw builder 保持同一声明树形状：先生成主构造参数，
     * 再为带声明关键字的参数生成对应属性并记录 [CfirValueParameter.correspondingProperty]。
     */
    private fun addPrimaryConstructorParameterProperties(
        ownerNode: LighterASTNode,
        declarations: MutableList<CfirDeclaration>,
    ) {
        val primaryConstructorNode = findPrimaryConstructorNode(ownerNode) ?: return
        val primaryConstructorIndex = declarations.indexOfFirst { it is CfirConstructor && it.isPrimary }
        if (primaryConstructorIndex < 0) return

        val cfirPrimaryConstructor = declarations[primaryConstructorIndex] as CfirConstructor
        val generatedProperties = extractValueParameterNodes(primaryConstructorNode)
            .zip(cfirPrimaryConstructor.valueParameters)
            .mapNotNull { (parameterNode, valueParameter) ->
                if (!hasLetOrVarKeyword(parameterNode)) return@mapNotNull null
                convertPrimaryConstructorParameterProperty(parameterNode, valueParameter)
            }

        if (generatedProperties.isNotEmpty()) {
            declarations.addAll(primaryConstructorIndex + 1, generatedProperties)
        }
    }

    /** 为带 `let/var` 的主构造参数创建对应成员属性。 */
    private fun convertPrimaryConstructorParameterProperty(
        node: LighterASTNode,
        valueParameter: CfirValueParameter,
    ): CfirProperty {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val propertySource = node.toSource().fakeElement(CjFakeSourceElementKind.PropertyFromParameter)
        val propertySymbol = CfirPropertySymbol(callableIdFor(name))
        val propertyStatus = cloneDeclarationStatus(
            modifiers.toDeclarationStatusForCurrentContext()
                .withConstDeclarationKeyword(hasConstKeyword(node))
        ).also { status ->
            status.isMut = hasVarKeyword(node)
        }
        val defaultAccessorSource = propertySource.fakeElement(CjFakeSourceElementKind.DefaultAccessor)
        val propertyTypeRef = valueParameter.returnTypeRef.copyWithNewSource(defaultAccessorSource)
        val getter = buildPrimaryConstructorParameterPropertyAccessor(
            source = defaultAccessorSource,
            accessorName = Name.special("<get-${name.asString()}>"),
            propertyTypeRef = propertyTypeRef,
            propertySymbol = propertySymbol,
            propertyStatus = propertyStatus,
            isGetter = true,
        )
        val setter = if (hasVarKeyword(node)) {
            buildPrimaryConstructorParameterPropertyAccessor(
                source = defaultAccessorSource,
                accessorName = Name.special("<set-${name.asString()}>"),
                propertyTypeRef = propertyTypeRef,
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
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = propertyStatus
                returnTypeRef = valueParameter.returnTypeRef.copyWithNewSource(propertySource)
                this.name = name
                this.getter = getter
                this.setter = setter
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

    /** 从 class-like 节点中查找主构造节点。 */
    private fun findPrimaryConstructorNode(ownerNode: LighterASTNode): LighterASTNode? {
        val bodyNode = tree.findChildByType(ownerNode, CjNodeTypes.CLASS_BODY)
            ?: tree.findChildByType(ownerNode, CjNodeTypes.INTERFACE_BODY)
            ?: tree.findChildByType(ownerNode, CjNodeTypes.ENUM_BODY)
            ?: return null
        return tree.findChildByType(bodyNode, CjNodeTypes.PRIMARY_CONSTRUCTOR)
    }

    /** 为没有显式构造函数的 class-like 声明构造隐式主构造。 */
    private fun buildImplicitPrimaryConstructor(ownerNode: LighterASTNode): CfirConstructor {
        return buildSourceDeclaration(CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))) { symbol ->
            buildPrimaryConstructor {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = ownerNode.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                returnTypeRef = buildImplicitTypeRef()
                body = null
            }
        }
    }

    // ===== Extend =====

    /** 转换 extend 声明，保留扩展目标类型、约束与成员声明。 */
    private fun convertExtend(node: LighterASTNode): CfirExtend {
        val modifiers = LightTreeModifierList.from(tree, node)

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
        val members = extractClassMembers(node)

        return buildSourceDeclaration(CfirExtendSymbol()) { symbol ->
            buildExtend {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                val typeParams = extractTypeParameters(node, symbol)
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
    }

    // ===== 函数 =====

    /** 转换普通命名函数声明。 */
    private fun convertFunction(node: LighterASTNode): CfirFunction {
        val name = extractFunctionName(node, countValueParameters(node))
        val functionSymbol = CfirNamedFunctionSymbol(callableIdFor(name))
        val valueParams = extractValueParameters(node, functionSymbol)
        val modifiers = LightTreeModifierList.from(tree, node)
        val returnTypeRef = extractReturnTypeRef(node)
        val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else withContainerSymbol(functionSymbol) {
            withFunctionTarget(functionTarget) { extractBody(node) }
        }

        return buildSourceDeclaration(functionSymbol) { symbol ->
            buildNamedFunction {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                val typeParams = extractFunctionTypeParameters(node, symbol)
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = declarationAttributes(node)
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = modifiers.toDeclarationStatusForCurrentContext(
                    isDefault = isDefaultInterfaceFunction(node, modifiers),
                    isImplicitAbstract = isImplicitAbstractClassLikeFunction(node, modifiers),
                )
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.name = name
                this.valueParameters.addAll(valueParams)
                this.body = body
                isMut = modifiers.isMut
            }
        }.also { bindFunctionTarget(functionTarget, it) }
    }

    /** 转换仓颉入口 main 函数声明。 */
    private fun convertMainFunction(node: LighterASTNode): CfirMainFunction {
        val modifiers = LightTreeModifierList.from(tree, node)
        val functionSymbol = CfirMainFunctionSymbol(callableIdFor(Name.identifier("main")))
        val valueParams = extractValueParameters(node, functionSymbol)
        val returnTypeRef = extractReturnTypeRef(node)
        val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else withContainerSymbol(functionSymbol) {
            withFunctionTarget(functionTarget) { extractBody(node) }
        }

        return buildSourceDeclaration(functionSymbol) { symbol ->
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

    /** 转换 macro declaration。 */
    private fun convertMacroDeclaration(node: LighterASTNode): CfirMacroDeclaration {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val functionSymbol = CfirMacroDeclarationSymbol(callableIdFor(name))
        val valueParams = extractValueParameters(node, functionSymbol)
        val returnTypeRef = extractReturnTypeRef(node)
        val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else withContainerSymbol(functionSymbol) {
            withFunctionTarget(functionTarget) { extractBody(node) }
        }

        return buildSourceDeclaration(functionSymbol) { symbol ->
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

    /** 转换 finalizer 声明。 */
    private fun convertFinalizer(node: LighterASTNode): CfirFinalizer {
        val modifiers = LightTreeModifierList.from(tree, node)
        val functionSymbol = CfirFinalizerSymbol(callableIdFor(SpecialNames.END_INIT))
        val typeParams = extractFunctionTypeParameters(node, functionSymbol)
        val valueParams = extractValueParameters(node, functionSymbol)
        val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else withContainerSymbol(functionSymbol) {
            withFunctionTarget(functionTarget) { extractBody(node) }
        }

        return buildSourceDeclaration(functionSymbol) { symbol ->
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
                returnTypeRef = baseSession.builtinTypes.unitType.toCfirResolvedTypeRef(source)
                this.valueParameters.addAll(valueParams)
                this.body = body
            }
        }.also { bindFunctionTarget(functionTarget, it) }
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

        val modifiers = LightTreeModifierList.from(tree, node)
        val typeRef = extractReturnTypeRef(node)
        val accessors = extractPropertyAccessorNodes(node)
        val propertySymbol = CfirPropertySymbol(callableIdFor(name))
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

        return buildSourceDeclaration(propertySymbol) { symbol ->
            buildProperty {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = declarationAttributes(node)
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = modifiers.toDeclarationStatusForCurrentContext(
                    isDefault = isDefaultInterfaceProperty(node, modifiers, accessors),
                    isImplicitAbstract = isImplicitAbstractClassLikeProperty(node, modifiers, accessors),
                )
                this.returnTypeRef = typeRef
                this.name = name
                this.getter = getter
                this.setter = setter
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
        val modifiers = LightTreeModifierList.from(tree, node)
        val accessorSymbol = CfirPropertyAccessorSymbol()
        val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else withContainerSymbol(accessorSymbol) {
            withFunctionTarget(functionTarget) { extractBody(node) }
        }
        val isGetter = isGetterAccessor(node)
        val explicitReturnTypeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)?.let(::convertTypeRef)
        val valueParameters = extractValueParameters(
            node,
            accessorSymbol,
            requiresExplicitType = isGetter,
        )
        if (!isGetter) {
            valueParameters.firstOrNull()
                ?.takeIf { it.returnTypeRef is CfirImplicitTypeRef }
                ?.replaceReturnTypeRef(propertyTypeRef)
        }
        val source = node.toSource()

        return buildSourceDeclaration(accessorSymbol) { symbol ->
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
                    ?: if (isGetter) propertyTypeRef else baseSession.builtinTypes.unitType.toCfirResolvedTypeRef(source)
                this.propertySymbol = propertySymbol
                this.isGetter = isGetter
                this.valueParameters.addAll(valueParameters)
                this.body = body
            }
        }.also { bindFunctionTarget(functionTarget, it) }
    }

    /** 转换字段变量声明。 */
    private fun convertFieldVariable(node: LighterASTNode): CfirFieldVariable {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeRef = extractReturnTypeRef(node)
        val initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractInitializer(node)
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
                this.name = name
                this.initializer = initializer
                this.isVar = isVar
            }
        }
    }

    /** 转换 pattern variable 声明。 */
    private fun convertPatternVariable(node: LighterASTNode): CfirPatternVariable {
        val modifiers = LightTreeModifierList.from(tree, node)
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
        val initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractInitializer(node)
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
                this.pattern = pattern
                this.initializer = initializer
                this.isVar = isVar
            }
        }
    }

    // ===== 构造器 =====

    /** 转换主构造或次构造函数声明。 */
    private fun convertConstructor(node: LighterASTNode, isPrimary: Boolean): CfirConstructor {
        val modifiers = LightTreeModifierList.from(tree, node)
        val constructorSymbol = CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))
        val typeParams = extractFunctionTypeParameters(node, constructorSymbol)
        val valueParams = extractValueParameters(node, constructorSymbol, requiresExplicitType = true)
        val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else withContainerSymbol(constructorSymbol) {
            withFunctionTarget(functionTarget) { extractBody(node) }
        }

        return buildSourceDeclaration(constructorSymbol) { symbol ->
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
                    this.body = body
                }
            }
        }.also { bindFunctionTarget(functionTarget, it) }
    }

    // ===== 类型别名 =====

    /** 转换 typealias 声明；非法嵌套时显式构造 invalid declaration。 */
    private fun convertTypeAlias(node: LighterASTNode): CfirDeclaration {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
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
                attributes = CfirDeclarationAttributes.EMPTY
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
            Name.identifier(nameNode.asText())
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
        return buildSourceDeclaration(CfirEnumConstructorSymbol(callableIdFor(enumName))) { symbol ->
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
                returnTypeRef = buildImplicitTypeRef()
                this.valueParameters.addAll(valueParameters)
                name = enumName
            }
        }
    }

    // ===== 值参数 =====

    /** 转换函数、构造函数、宏或 lambda 的值参数。 */
    fun convertValueParameter(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        requiresExplicitType: Boolean = true,
    ): CfirValueParameter {
        val modifiers = LightTreeModifierList.from(tree, node)

        val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
        val paramName = if (nameNode != null) Name.identifier(nameNode.asText()) else Name.special("<error>")
        val typeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)

        val isNamed = tree.findChildByType(node, CjTokens.EXCL) != null
        val parameterSource = node.toSource()

        // 默认值是最后一个表达式子节点
        var defaultExpr: CfirExpression? = null
        tree.forEachChildren(node) { child ->
            if (LightTreeRawCfirExpressionBuilder.isExpressionToken(child.tokenType)) {
                defaultExpr = expressionBuilder.convertExpression(child)
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
                status = CfirDeclarationStatusImpl.DEFAULT
                returnTypeRef = when {
                    typeRef != null -> convertTypeRef(typeRef)
                    requiresExplicitType -> createNoTypeForParameterTypeRef(parameterSource)
                    else -> buildImplicitTypeRef()
                }
                name = paramName
                defaultValue = defaultExpr
                this.containingDeclarationSymbol = containingDeclarationSymbol
            }
        }
        collectMacroSurfacesFromAnnotations(node, modifiers, MacroSurfaceOwnerKind.PARAMETER, parameter)
        return parameter
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
        val metadataRegistry = baseSession.ensureAnnotationMetadataRegistry()

        val carriedAnnotations = modifiers.annotations.map { it.asText() }
        modifiers.annotations.forEach { annotation ->
            val annotationName = annotationNameInfo(annotation) ?: return@forEach
            val rawName = annotationName.rawName
            val qualifiedName = macroSurfaceQualifiedName(rawName)
            val annotationCall = buildRawAnnotationCall(
                annotation = annotation,
                rawName = rawName,
                carrier = carrier,
                annotationName = annotationName,
            )
            val annotationIndex = carrier.annotations.size
            carrier.replaceAnnotations(carrier.annotations + annotationCall)
            val valueArgumentList = findFirstDescendantByType(annotation, CjNodeTypes.VALUE_ARGUMENT_LIST)
            val snapshot = CfirAnnotationSlotSnapshot(
                owner = carrier,
                annotationIndex = annotationIndex,
                originalAnnotation = annotationCall,
                rawSyntax = annotation.asText(),
                forcedCustom = annotation.asText().trimStart().startsWith("@!"),
                qualifiedName = qualifiedName,
                argumentText = valueArgumentList?.asText(),
                tokens = tokenizeFullAnnotation(annotation),
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
    ): CfirAnnotationCall {
        val valueArgumentList = findFirstDescendantByType(annotation, CjNodeTypes.VALUE_ARGUMENT_LIST)
        val arguments = convertAnnotationArguments(annotation, valueArgumentList)
        return buildAnnotationCall {
            source = sourceOverride ?: annotation.toSource()
            typeRef = typeRefOverride ?: buildAnnotationTypeRef(rawName, annotation)
            this.arguments.addAll(arguments)
            argumentList = buildArgumentList {
                source = argumentListSourceOverride ?: valueArgumentList?.toSource()
                this.arguments.addAll(arguments)
            }
            calleeReference = buildNamedReference(
                Name.identifier(rawName.substringAfterLast('.')),
                calleeReferenceSourceOverride ?: annotationCalleeReferenceSource(annotation),
            )
            containingDeclarationSymbol = containingSymbol
        }
    }

    /** 转换 annotation 实参列表。 */
    private fun convertAnnotationArguments(
        annotation: LighterASTNode,
        valueArgumentList: LighterASTNode?,
    ): List<CfirExpression> {
        val valueArguments = valueArgumentList
            ?.let { tree.getChildrenByType(it, CjNodeTypes.VALUE_ARGUMENT) }
            .orEmpty()
            .mapNotNull(::convertAnnotationArgument)
        if (valueArguments.isNotEmpty()) return valueArguments

        val callingConvention = findFirstDescendantByType(annotation, CjNodeTypes.ANNOTATION_CALLING_CONV)
        if (callingConvention != null) {
            return listOf(buildLiteralExpression {
                source = callingConvention.toSource()
                kind = CfirLiteralKind.STRING
                value = callingConvention.asText()
            })
        }

        return emptyList()
    }

    /** 转换单个 annotation 实参。 */
    private fun convertAnnotationArgument(valueArgumentNode: LighterASTNode): CfirExpression? {
        val expressionNode = findFirstExpressionIn(valueArgumentNode) ?: return null
        val convertedExpression = expressionBuilder.convertExpression(expressionNode)
        val wrapped = if (tree.findChildByType(valueArgumentNode, CjTokens.INOUT_KEYWORD) != null) {
            buildInoutArgumentExpression {
                source = expressionNode.toSource()
                expression = convertedExpression
            }
        } else {
            convertedExpression
        }
        if (tree.findChildByType(valueArgumentNode, CjNodeTypes.VALUE_ARGUMENT_NAME) == null) return wrapped
        return buildBlock {
            source = valueArgumentNode.toSource()
            statements.add(wrapped)
        }
    }

    /** 在节点子树中查找第一个表达式节点。 */
    private fun findFirstExpressionIn(node: LighterASTNode): LighterASTNode? {
        if (LightTreeRawCfirExpressionBuilder.isExpressionToken(node.tokenType)) return node
        tree.forEachChildren(node) { child ->
            findFirstExpressionIn(child)?.let { return it }
        }
        return null
    }

    /** 根据原始 annotation 名称构造 user type ref。 */
    private fun buildAnnotationTypeRef(rawName: String, annotation: LighterASTNode): CfirTypeRef {
        val parts = rawName.split('.').filter(String::isNotBlank)
        if (parts.isEmpty()) return buildImplicitTypeRef()
        val annotationName = annotationNameInfo(annotation)
        val nameSource = annotationName?.nameSource ?: annotation.toSource()
        val segmentSources = annotationName?.segmentSources.orEmpty()
        return buildUserTypeRef {
            source = nameSource
            qualifier += parts.mapIndexed { index, part ->
                buildQualifierPart {
                    source = segmentSources.getOrNull(index) ?: nameSource
                    name = Name.identifier(part)
                }
            }
        }
    }

    /** 根据 annotation 名称信息构造可重定位的 user type ref。 */
    private fun buildAnnotationTypeRef(annotationName: AnnotationNameInfo, sourceOffsetDelta: Int = 0): CfirTypeRef {
        val parts = annotationName.rawName.split('.').filter(String::isNotBlank)
        if (parts.isEmpty()) return buildImplicitTypeRef()
        val nameSource = annotationName.nameSource.shiftedBy(sourceOffsetDelta)
        val segmentSources = annotationName.segmentSources.map { it.shiftedBy(sourceOffsetDelta) }
        return buildUserTypeRef {
            source = nameSource
            qualifier += parts.mapIndexed { index, part ->
                buildQualifierPart {
                    source = segmentSources.getOrNull(index) ?: nameSource
                    name = Name.identifier(part)
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
        val rawName = extractAnnotationNameText(annotation) ?: return null
        val shortName = rawName.substringAfterLast('.')
        val qualifiedName = macroSurfaceQualifiedName(rawName)
        val surfaceId = MacroSurfaceIdGenerator.next()
        val attrNode = findFirstDescendantByType(annotation, CjNodeTypes.MACRO_ATTR)
        val inputNode = findFirstDescendantByType(annotation, CjNodeTypes.MACRO_INPUT)
            ?: findFirstDescendantByType(annotation, CjNodeTypes.VALUE_ARGUMENT_LIST)
        val attrTokens = tokenizeSurfacePayload(attrNode)
        val inputTokens = tokenizeSurfacePayload(inputNode)
        val isForced = annotation.asText().trimStart().startsWith("@!")
        val common = MacroSurfaceCommon(
            surfaceId = surfaceId,
            qualifiedName = qualifiedName,
            kind = if (isForced) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
            hasParenthesis = inputNode != null,
            attrTokens = attrTokens,
            inputTokens = inputTokens,
            sourceRange = MacroSurfaceSourceRange(
                source = annotation.toSource(),
                startOffset = annotation.startOffset,
                endOffset = annotation.endOffset,
            ),
            scopeContext = macroSurfaceScopeContext(),
            modifiers = modifiers,
            carriedAnnotations = carriedAnnotations,
            capturedRawSyntax = annotation.asText(),
            containerContext = macroSurfaceContainerContext(ownerNode),
            replaceHandle = CfirReplaceHandle(
                handleId = surfaceId,
                carrier = carrier,
                annotationCarrier = annotationCarrier,
            ),
        )

        if (shortName == "IfAvailable") {
            return IfAvailableSurface(
                surfaceId = common.surfaceId,
                qualifiedName = common.qualifiedName,
                kind = common.kind,
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
                branchTokens = inputTokens,
            )
        }

        return when (ownerKind) {
            MacroSurfaceOwnerKind.DECLARATION -> MacroSurfaceDecl(
                surfaceId = common.surfaceId,
                qualifiedName = common.qualifiedName,
                kind = common.kind,
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
                kind = common.kind,
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
        return MacroPayloadTokenizer.tokenize(
            payload = node?.asText(),
            baseOffset = node?.startOffset ?: 0,
        ).map { token ->
            MacroSurfaceToken(
                text = token.text,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
                kindName = token.kindName,
            )
        }
    }

    /** 对完整 annotation 文本执行 lexer tokenization。 */
    private fun tokenizeFullAnnotation(node: LighterASTNode): List<MacroSurfaceToken> {
        return MacroPayloadTokenizer.tokenize(
            payload = node.asText(),
            baseOffset = node.startOffset,
        ).map { token ->
            MacroSurfaceToken(
                text = token.text,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
                kindName = token.kindName,
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
        return AnnotationNameInfo(
            rawName = rawName,
            nameSource = nameNode.toSource(),
            segmentSources = collectAnnotationNameSegmentSources(nameNode),
        )
    }

    /** 提取 macro-expression annotation 包装的名称信息。 */
    private fun macroExpressionAnnotationNameInfo(node: LighterASTNode): AnnotationNameInfo? {
        val nameNode = findMacroExpressionAnnotationNameNode(node) ?: return null
        val rawName = nameNode.asText().trim().takeIf { it.isNotEmpty() } ?: return null
        return AnnotationNameInfo(
            rawName = rawName,
            nameSource = nameNode.toSource(),
            segmentSources = collectAnnotationNameSegmentSources(nameNode),
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
        val sources = mutableListOf<CjSourceElement>()

        /** 深度遍历名称节点并收集 reference expression source。 */
        fun visit(node: LighterASTNode) {
            if (node.tokenType == CjNodeTypes.REFERENCE_EXPRESSION) {
                sources += node.toSource()
                return
            }
            node.forEachChildren(::visit)
        }

        visit(nameNode)
        return sources
    }

    /** 查找第一个 token type 为 [tokenType] 的后代节点。 */
    private fun findFirstDescendantByType(
        node: LighterASTNode,
        tokenType: com.intellij.psi.tree.IElementType,
    ): LighterASTNode? {
        if (node.tokenType == tokenType) return node
        tree.forEachChildren(node) { child ->
            findFirstDescendantByType(child, tokenType)?.let { return it }
        }
        return null
    }

    /** 将 surface 原始名称提升为 FQN。 */
    private fun macroSurfaceQualifiedName(rawName: String): FqName {
        val normalizedName = rawName.trim()
        if (normalizedName.contains('.')) return FqName(normalizedName)

        val name = Name.identifier(normalizedName)
        return if (packageFqName.isRoot) {
            FqName.topLevel(name)
        } else {
            packageFqName.child(name)
        }
    }

    /** 构造 macro surface 的 scope context。 */
    private fun macroSurfaceScopeContext(): MacroSurfaceScopeContext {
        val classFqName = (containerSymbolIfAny as? CfirClassLikeSymbol<*>)?.classId?.asSingleFqName()
        val functionName = (containerSymbolIfAny as? CfirCallableSymbol<*>)?.name
        return MacroSurfaceScopeContext(
            packageFqName = packageFqName,
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
                CjNodeTypes.CLASS_BODY -> return when (containerSymbolIfAny) {
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
        return if (text.isNotEmpty()) FqName(text) else FqName.ROOT
    }

    /** 构造 CFIR package directive。 */
    private fun buildPackageDirectiveNode(
        packageNode: LighterASTNode?,
        fqName: FqName,
    ): CfirPackageDirective {
        return buildPackageDirective {
            source = packageNode?.toSource()
            packageFqName = fqName
            isMacroPackage = packageNode?.let { containsChildByType(it, CjTokens.MACRO_KEYWORD) } == true
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
                    if (directive.tokenType == CjNodeTypes.IMPORT_DIRECTIVE) {
                        tree.forEachChildren(directive) { item ->
                            if (item.tokenType == CjNodeTypes.IMPORT_ITEM) {
                                convertImportItem(item)?.let { imports.add(it) }
                            }
                        }
                    }
                }
            }
        }
        return imports
    }

    /** 转换单个 import item。 */
    private fun convertImportItem(item: LighterASTNode): CfirImport? {
        // 提取导入的 FQN（从 DOT_QUALIFIED_EXPRESSION 或 REFERENCE_EXPRESSION）
        var fqNameText: String? = null
        var isAllUnder = false
        var aliasName: Name? = null

        tree.forEachChildren(item) { child ->
            when (child.tokenType) {
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                CjNodeTypes.REFERENCE_EXPRESSION -> fqNameText = child.asText()
                CjTokens.MUL -> isAllUnder = true
                CjNodeTypes.IMPORT_ALIAS -> {
                    val idNode = tree.findChildByType(child, CjTokens.IDENTIFIER)
                    aliasName = idNode?.let { Name.identifier(it.asText()) }
                }
            }
        }

        val fqName = fqNameText?.let { normalizeImportFqName(FqName(it)) } ?: return null
        return buildImport {
            source = item.toSource()
            importedFqName = fqName
            this.isAllUnder = isAllUnder
            this.aliasName = aliasName
        }
    }

    /** 规范化 LightTree 中可能重复包前缀的 import FQN。 */
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

    /** 构造文件顶层声明列表。 */
    private fun buildFileDeclarations(file: LighterASTNode): List<CfirDeclaration> {
        val declarations = mutableListOf<CfirDeclaration>()
        val pendingAnnotations = mutableListOf<LighterASTNode>()
        tree.forEachChildren(file) { child ->
            when (child.tokenType) {
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
                    if (child.isTopLevelMacroDeclaration()) {
                        val declaration = convertTopLevelMacroDeclaration(child) ?: return@forEachChildren
                        if (pendingAnnotations.isNotEmpty()) {
                            collectMacroSurfacesFromAnnotations(
                                ownerNode = child,
                                modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList()),
                                ownerKind = MacroSurfaceOwnerKind.DECLARATION,
                                carrier = declaration,
                            )
                            pendingAnnotations.clear()
                        }
                        declarations.add(declaration)
                    } else {
                        pendingAnnotations += child
                    }
                    return@forEachChildren
                }
            }
            if (LightTreeRawCfirExpressionBuilder.isDeclarationToken(child.tokenType)) {
                val declaration = convertDeclaration(child)
                if (pendingAnnotations.isNotEmpty()) {
                    collectMacroSurfacesFromAnnotations(
                        ownerNode = child,
                        modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList()),
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

    /** 判断当前 macro expression 是否包裹顶层声明。 */
    private fun LighterASTNode.isTopLevelMacroDeclaration(): Boolean {
        return resolveTopLevelMacroDeclarationChain(this) != null
    }

    /**
     * 文件级 `@Macro decl` 的 LightTree 形态是 MACRO_EXPRESSION，input
     * 内部才包含 carrier 声明。声明宏 surface 必须绑定这个 carrier，
     * 后续 stable splice 才能按对象身份替换最终 CFIR 声明。
     */
    private fun convertTopLevelMacroDeclaration(node: LighterASTNode): CfirDeclaration? {
        val chain = resolveTopLevelMacroDeclarationChain(node) ?: return null
        val (declarationNode, macroExpressions) = chain
        val carrier = convertDeclaration(declarationNode)
        macroExpressions.forEach { macroExpression ->
            repairMacroExpressionCarrierShape(macroExpression, carrier)
            applyTopLevelMacroExpression(macroExpression, carrier)
        }
        return carrier
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
            findDirectDeclarationChild(inputNode)?.let { declarationNode ->
                return declarationNode to macroExpressions.toList()
            }
            current = findDirectMacroExpressionChild(inputNode)
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
        carrier: CfirDeclaration,
    ) {
        val inputNode = tree.findChildByType(node, CjNodeTypes.MACRO_INPUT) ?: return
        val annotationName = macroExpressionAnnotationNameInfo(node) ?: return
        val rawName = annotationName.rawName
        val surfaceId = MacroSurfaceIdGenerator.next()
        val attrNode = tree.findChildByType(node, CjNodeTypes.MACRO_ATTR)
        val rawAnnotationSyntax = macroExpressionAnnotationSyntax(node, rawName, attrNode)
        val containingSymbol = when (carrier) {
            is CfirValueParameter -> carrier.containingDeclarationSymbol
            else -> carrier.symbol
        }
        val annotationCall = buildRawAnnotationCall(
            annotation = node,
            rawName = rawName,
            containingSymbol = containingSymbol,
            sourceOverride = node.toSource(),
            typeRefOverride = buildAnnotationTypeRef(annotationName),
            calleeReferenceSourceOverride = annotationName.calleeReferenceSource,
            argumentListSourceOverride = attrNode?.toSource(),
        )
        val annotationIndex = carrier.annotations.size
        carrier.replaceAnnotations(carrier.annotations + annotationCall)
        val annotationCarrier = baseSession.ensureAnnotationMetadataRegistry().record(
            CfirAnnotationSlotSnapshot(
                owner = carrier,
                annotationIndex = annotationIndex,
                originalAnnotation = annotationCall,
                rawSyntax = rawAnnotationSyntax,
                forcedCustom = node.asText().trimStart().startsWith("@!"),
                qualifiedName = macroSurfaceQualifiedName(rawName),
                argumentText = attrNode?.asText(),
                tokens = tokenizeMacroExpressionAnnotationSyntax(rawAnnotationSyntax, node.startOffset),
                callSite = MacroCallSite.DECLARATION,
            )
        )
        collectedMacroSurfaces += MacroSurfaceDecl(
            surfaceId = surfaceId,
            qualifiedName = macroSurfaceQualifiedName(rawName),
            kind = if (node.asText().trimStart().startsWith("@!")) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
            hasParenthesis = inputNode.asText().trimStart().startsWith("("),
            attrTokens = tokenizeSurfacePayload(attrNode),
            inputTokens = tokenizeSurfacePayload(inputNode),
            sourceRange = MacroSurfaceSourceRange(
                source = node.toSource(),
                startOffset = node.startOffset,
                endOffset = node.endOffset,
            ),
            scopeContext = macroSurfaceScopeContext(),
            modifiers = emptyList(),
            carriedAnnotations = emptyList(),
            capturedRawSyntax = node.asText(),
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
     * `@Anno func f(): T` 在 LightTree 中以 MACRO_EXPRESSION 包住声明。
     * 其中 carrier FUNC 子树可能不直接包含返回类型节点，返回类型保留在 wrapper 上；
     * raw CFIR 必须把这个声明形状恢复到 carrier，后续 checker/resolve 才能只读 CFIR。
     */
    private fun repairMacroExpressionCarrierShape(
        macroExpression: LighterASTNode,
        carrier: CfirDeclaration,
    ) {
        if (carrier is CfirFunction && carrier.returnTypeRef is CfirImplicitTypeRef) {
            val restoredReturnType = (
                    findFunctionLikeReturnTypeRef(macroExpression)
                        ?: findMacroExpressionWrapperReturnTypeRef(macroExpression)
                    )?.let(::convertTypeRef)
            restoredReturnType?.let(carrier::replaceReturnTypeRef)
        }
    }

    /**
     * 在 macro wrapper 层查找函数返回类型引用。
     *
     * LightTree 对 `@Anno func f(): T` 的拆分可能让返回类型停留在 MACRO_EXPRESSION wrapper 上；
     * 这里只接受参数列表之后、函数体之前且不属于 where 约束的 [CjNodeTypes.TYPE_REFERENCE]。
     */
    private fun findMacroExpressionWrapperReturnTypeRef(macroExpression: LighterASTNode): LighterASTNode? {
        val parameterList = findFirstDescendantByType(macroExpression, CjNodeTypes.VALUE_PARAMETER_LIST) ?: return null
        val block = findFirstDescendantByType(macroExpression, CjNodeTypes.BLOCK)
        val afterParameters = tree.getEndOffset(parameterList)
        val beforeBody = block?.let(tree::getStartOffset) ?: macroExpression.endOffset
        return findDescendantsByType(macroExpression, CjNodeTypes.TYPE_REFERENCE)
            .firstOrNull { typeRef ->
                tree.getStartOffset(typeRef) >= afterParameters &&
                        tree.getEndOffset(typeRef) <= beforeBody &&
                        !isInsideTypeConstraintList(typeRef, macroExpression)
            }
    }

    /**
     * 判断 [node] 是否位于 [root] 的 type constraint list 内部。
     *
     * 返回类型恢复需要排除 where 子句中的类型引用，否则泛型上界会被误认为函数返回类型。
     */
    private fun isInsideTypeConstraintList(node: LighterASTNode, root: LighterASTNode): Boolean {
        val nodeStart = tree.getStartOffset(node)
        val nodeEnd = tree.getEndOffset(node)
        var result = false

        /** 深度遍历 [root]，沿途携带当前节点是否已经进入 type constraint list。 */
        fun visit(current: LighterASTNode, insideConstraint: Boolean) {
            if (tree.getStartOffset(current) == nodeStart && tree.getEndOffset(current) == nodeEnd) {
                result = insideConstraint
                return
            }
            if (result) return
            val nextInsideConstraint = insideConstraint || current.tokenType == CjNodeTypes.TYPE_CONSTRAINT_LIST
            tree.forEachChildren(current) { child ->
                visit(child, nextInsideConstraint)
            }
        }
        visit(root, insideConstraint = false)
        return result
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
     * LightTree 的 MACRO_EXPRESSION 同时承载 declaration macro 与 annotation-site。
     * raw 阶段先恢复 annotation slot 文本，classification 再决定最终 splice 槽位。
     */
    private fun macroExpressionAnnotationSyntax(
        node: LighterASTNode,
        rawName: String,
        attrNode: LighterASTNode?,
    ): String {
        val prefix = if (node.asText().trimStart().startsWith("@!")) "@!" else "@"
        return prefix + rawName + attrNode?.asText().orEmpty()
    }

    /**
     * 将 macro expression 还原出的 annotation 文本切分为 surface token。
     *
     * [baseOffset] 使用 wrapper 的源偏移，保证 token offset 与后续宏诊断、替换范围保持一致。
     */
    private fun tokenizeMacroExpressionAnnotationSyntax(
        rawAnnotationSyntax: String,
        baseOffset: Int,
    ): List<MacroSurfaceToken> =
        MacroPayloadTokenizer.tokenize(rawAnnotationSyntax, baseOffset).map { token ->
            MacroSurfaceToken(
                text = token.text,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
                kindName = token.kindName,
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
            nameNode.asText().asOperatorName()
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
                child.tokenType == CjTokens.IDENTIFIER -> result = Name.identifier(child.asText())
                child.tokenType == CjNodeTypes.OPERATION_NAME -> result = child.asText().asOperatorName()
                child.tokenType == TokenType.ERROR_ELEMENT -> result = Name.special("<anonymous>")
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
            else -> rawName.asOperatorName()
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
        return tree.findChildByType(node, CjTokens.IDENTIFIER)?.let { Name.identifier(it.asText()) }
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
            val parameterName = Name.identifier(parameterNameNode.asText())
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
                    parameterName = Name.identifier(parameterNameNode.asText()),
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

    /** 提取类成员声明（排除 ENUM_CONSTRUCTOR，枚举构造器在 convertClass 中单独处理） */
    private fun extractClassMembers(node: LighterASTNode): List<CfirDeclaration> {
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
                    if (child.isTopLevelMacroDeclaration()) {
                        val declaration = convertTopLevelMacroDeclaration(child) ?: return@forEachChildren
                        if (pendingAnnotations.isNotEmpty()) {
                            collectMacroSurfacesFromAnnotations(
                                ownerNode = child,
                                modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList()),
                                ownerKind = MacroSurfaceOwnerKind.DECLARATION,
                                carrier = declaration,
                            )
                            pendingAnnotations.clear()
                        }
                        declarations.add(declaration)
                    } else {
                        pendingAnnotations += child
                    }
                    return@forEachChildren
                }
            }
            // 排除 ENUM_CONSTRUCTOR（由 convertClass 单独处理）
            if (tt != CjNodeTypes.ENUM_CONSTRUCTOR && LightTreeRawCfirExpressionBuilder.isDeclarationToken(tt)) {
                val declaration = convertDeclaration(child)
                if (pendingAnnotations.isNotEmpty()) {
                    collectMacroSurfacesFromAnnotations(
                        ownerNode = child,
                        modifiers = LightTreeModifierList(tree, modifierListNode = null, annotations = pendingAnnotations.toList()),
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
        requiresExplicitType: Boolean = true,
    ): List<CfirValueParameter> {
        return extractValueParameterNodes(node)
            .map { convertValueParameter(it, containingDeclarationSymbol, requiresExplicitType) }
    }

    /**
     * 提取声明直接参数列表中的值参数节点。
     *
     * LightTree 路径只使用第一个 [CjNodeTypes.VALUE_PARAMETER_LIST] 作为签名参数列表，重复列表另由诊断属性记录。
     */
    private fun extractValueParameterNodes(node: LighterASTNode): List<LighterASTNode> {
        val paramList = tree.findChildByType(node, CjNodeTypes.VALUE_PARAMETER_LIST) ?: return emptyList()
        return tree.getChildrenByType(paramList, CjNodeTypes.VALUE_PARAMETER)
    }

    /**
     * 统计声明签名中的值参数数量。
     *
     * 主要服务操作符名称归一化，尤其是一元/二元 `+`、`-` 的区分。
     */
    private fun countValueParameters(node: LighterASTNode): Int {
        return extractValueParameterNodes(node).size
    }

    /** 提取函数体块 */
    private fun extractBody(node: LighterASTNode): CfirBlock? {
        val blockNode = tree.findChildByType(node, CjNodeTypes.BLOCK) ?: return null
        return expressionBuilder.convertBlock(blockNode)
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

    /** 提取初始化器表达式 */
    private fun extractInitializer(node: LighterASTNode): CfirExpression? {
        // 查找 = 之后的表达式
        var afterEq = false
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjTokens.EQ) {
                afterEq = true
            } else if (afterEq && LightTreeRawCfirExpressionBuilder.isExpressionToken(child.tokenType)) {
                return expressionBuilder.convertExpression(child)
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
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjTokens.VAR_KEYWORD) return true
        }
        return false
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

    /** 判断主构造参数是否声明为成员参数。 */
    private fun hasLetOrVarKeyword(node: LighterASTNode): Boolean {
        tree.forEachChildren(node) { child ->
            when (child.tokenType) {
                CjTokens.LET_KEYWORD, CjTokens.CONST_KEYWORD, CjTokens.VAR_KEYWORD -> return true
            }
        }
        return false
    }

    /** 查找模式子节点 */
    private fun findPatternChild(node: LighterASTNode): LighterASTNode? {
        tree.forEachChildren(node) { child ->
            if (LightTreeRawCfirExpressionBuilder.isPatternToken(child.tokenType)) return child
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
     * 深复制声明状态到可变 [CfirDeclarationStatusImpl]。
     *
     * 模式绑定变量需要继承外层 `let/var/const`、可见性与修饰符语义，但不能共享同一个可变 status 实例。
     */
    internal fun cloneDeclarationStatus(status: CfirDeclarationStatus): CfirDeclarationStatusImpl {
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
            copied.isDefault = status.isDefault
            copied.isAbstract = status.isAbstract
            copied.isOpen = status.isOpen
            copied.isSealed = status.isSealed
        }
    }

    /**
     * 判断接口成员函数是否应按 default 实现处理。
     *
     * 非 foreign、非 abstract 且源码含函数体时才标记 default，避免 lazy body 模式影响语义判断。
     */
    private fun isDefaultInterfaceFunction(node: LighterASTNode, modifiers: LightTreeModifierList): Boolean =
        isInInterfaceMemberContext() &&
                !modifiers.isForeign &&
                !modifiers.isAbstract &&
                hasSyntaxBody(node)

    /**
     * 判断接口属性是否因为显式 accessor body 而拥有 default 实现。
     *
     * 仅属性 accessor 自身存在 body 时成立，普通无体接口属性仍交给抽象语义处理。
     */
    private fun isDefaultInterfaceProperty(
        node: LighterASTNode,
        modifiers: LightTreeModifierList,
        accessors: List<LighterASTNode> = extractPropertyAccessorNodes(node),
    ): Boolean =
        isInInterfaceMemberContext() &&
                !modifiers.isAbstract &&
                accessors.any(::hasSyntaxBody)

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
        !context.inLocalContext && containerSymbolIfAny is CfirInterfaceSymbol

    /**
     * 官方 parser 在 class/interface 体内把无 body 函数、无属性体且无 getter/setter 的属性标记为 abstract。
     * LightTree 路径必须基于源码语法判断，不能受 lazy body 构建模式影响。
     */
    private fun isImplicitAbstractClassLikeFunction(
        node: LighterASTNode,
        modifiers: LightTreeModifierList,
    ): Boolean =
        isInClassOrInterfaceMemberContext() &&
                !modifiers.isForeign &&
                !hasSyntaxBody(node)

    /**
     * 判断 class/interface 成员属性是否应由无体语法隐式标记为 abstract。
     *
     * foreign 属性不走隐式 abstract；存在属性体或显式 accessor 时由更具体的 accessor/default 逻辑处理。
     */
    private fun isImplicitAbstractClassLikeProperty(
        node: LighterASTNode,
        modifiers: LightTreeModifierList,
        accessors: List<LighterASTNode>,
    ): Boolean =
        isInClassOrInterfaceMemberContext() &&
                !modifiers.isForeign &&
                !hasPropertyBody(node) &&
                accessors.isEmpty()

    /**
     * 判断当前声明转换是否处在 class 或 interface 成员上下文。
     *
     * 用于限制隐式 abstract 只作用于类型成员，避免污染局部声明。
     */
    private fun isInClassOrInterfaceMemberContext(): Boolean =
        !context.inLocalContext && (containerSymbolIfAny is CfirClassSymbol || containerSymbolIfAny is CfirInterfaceSymbol)

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
