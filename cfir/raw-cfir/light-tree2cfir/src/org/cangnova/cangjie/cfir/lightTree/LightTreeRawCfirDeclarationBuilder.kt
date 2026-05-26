package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.builder.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildInoutArgumentExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationReplaceCarrier
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.resolve.providers.macro.IfAvailableSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallSite
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceIdGenerator
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceScopeContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * LightTree → Raw CFIR 声明构建器（对齐 PsiRawCfirBuilder 的声明转换部分）。
 *
 * 继承 [AbstractRawCfirBuilder]，实现三个模板方法，
 * 通过 `when(node.tokenType)` 手动分发代替 PSI Visitor 模式。
 */
class LightTreeRawCfirDeclarationBuilder(
    session: CfirSession,
    internal val baseScopeProvider: CfirScopeProvider,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
    context: Context<LighterASTNode> = Context(),
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

    private fun LightTreeModifierList.toDeclarationStatusForCurrentContext(
        defaultVisibility: Visibility? = null,
    ): CfirDeclarationStatus {
        val inInterfaceContext = !context.inLocalContext && containerSymbolIfAny is CfirInterfaceSymbol
        return toDeclarationStatus(context.inLocalContext, inInterfaceContext, defaultVisibility)
    }

    private enum class MacroSurfaceOwnerKind {
        DECLARATION,
        PARAMETER,
    }

    // ===== AbstractRawCfirBuilder 抽象方法实现 =====

    // ===== 表达式构建器（延迟初始化，解决循环依赖） =====

    val expressionBuilder: LightTreeRawCfirExpressionBuilder by lazy {
        LightTreeRawCfirExpressionBuilder(baseSession, tree, source, context, this)
    }

    // ===== Public API =====

    override fun buildFile(file: LighterASTNode): CfirFile {
        error("Use buildCfirFile(lightTreeRoot, sourceFile, linesMapping) for LightTree file conversion")
    }

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
        val rawName = extractAnnotationNameText(annotation) ?: return null
        return withPackageContext(packageFqName) {
            buildRawAnnotationCall(
                annotation = annotation,
                rawName = rawName,
                containingSymbol = containingSymbol,
                sourceOverride = sourceOverride,
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
        val rawName = extractMacroExpressionNameText(macroExpression) ?: return null
        return withPackageContext(packageFqName) {
            buildRawAnnotationCall(
                annotation = macroExpression,
                rawName = rawName,
                containingSymbol = containingSymbol,
                sourceOverride = sourceOverride,
                argumentListSourceOverride = argumentListSourceOverride,
            )
        }
    }

    // ===== 声明转换入口 =====

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
                        val declarations = extractClassMembers(node).toMutableList().also { declarations ->
                            if (declarations.none { it is CfirConstructor }) {
                                declarations.add(0, buildImplicitPrimaryConstructor(node))
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
                        extractTypeParameters(node, symbol) to extractClassMembers(node).toMutableList()
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
                        val declarations = extractClassMembers(node).toMutableList().also { declarations ->
                            if (declarations.none { it is CfirConstructor }) {
                                declarations.add(0, buildImplicitPrimaryConstructor(node))
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
                        val declarations = extractClassMembers(node).toMutableList().also { declarations ->
                            if (declarations.none { it is CfirConstructor }) {
                                declarations.add(0, buildImplicitPrimaryConstructor(node))
                            }
                            val enumBody = tree.findChildByType(node, CjNodeTypes.ENUM_BODY)
                            if (enumBody != null) {
                                val enumCtors = tree.getChildrenByType(enumBody, CjNodeTypes.ENUM_CONSTRUCTOR)
                                    .map { convertEnumConstructor(it, typeParameters) }
                                declarations.addAll(0, enumCtors)
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
                }
            }
        }
    }

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
                status = modifiers.toDeclarationStatusForCurrentContext()
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.name = name
                this.valueParameters.addAll(valueParams)
                this.body = body
                isMut = modifiers.isMut
            }
        }.also { bindFunctionTarget(functionTarget, it) }
    }

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

    private fun convertFinalizer(node: LighterASTNode): CfirFinalizer {
        val modifiers = LightTreeModifierList.from(tree, node)
        val functionSymbol = CfirFinalizerSymbol(callableIdFor(SpecialNames.END_INIT))
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
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = modifiers.toDeclarationStatusForCurrentContext()
                returnTypeRef = buildImplicitTypeRef()
                this.valueParameters.addAll(valueParams)
                this.body = body
            }
        }.also { bindFunctionTarget(functionTarget, it) }
    }

    // ===== 属性/字段/变量 =====

    private fun convertProperty(node: LighterASTNode): CfirProperty {
        val name = extractName(node)
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
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = modifiers.toDeclarationStatusForCurrentContext()
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
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = context.inLocalContext
                dispatchReceiverType = currentDispatchReceiverType()
                status = modifiers.toDeclarationStatusForCurrentContext()
                returnTypeRef = explicitReturnTypeRef
                    ?: if (isGetter) propertyTypeRef else baseSession.builtinTypes.unitType.toCfirResolvedTypeRef(source)
                this.propertySymbol = propertySymbol
                this.isGetter = isGetter
                this.valueParameters.addAll(valueParameters)
                this.body = body
            }
        }.also { bindFunctionTarget(functionTarget, it) }
    }

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
                this.returnTypeRef = typeRef
                this.name = name
                this.initializer = initializer
                this.isVar = isVar
            }
        }
    }

    private fun convertPatternVariable(node: LighterASTNode): CfirPatternVariable {
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeRef = extractReturnTypeRef(node)
        val patternNode = findPatternChild(node)
        val status = modifiers.toDeclarationStatusForCurrentContext()
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

    private fun convertConstructor(node: LighterASTNode, isPrimary: Boolean): CfirConstructor {
        val modifiers = LightTreeModifierList.from(tree, node)
        val constructorSymbol = CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))
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
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = modifiers.toDeclarationStatusForCurrentContext()
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
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = modifiers.toDeclarationStatusForCurrentContext()
                    returnTypeRef = buildImplicitTypeRef()
                    this.valueParameters.addAll(valueParams)
                    this.body = body
                }
            }
        }.also { bindFunctionTarget(functionTarget, it) }
    }

    // ===== 类型别名 =====

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

    private fun convertEnumConstructor(
        node: LighterASTNode,
        ownerTypeParameters: List<CfirTypeParameter> = emptyList(),
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
                typeParameters.addAll(ownerTypeParameters)
                returnTypeRef = buildImplicitTypeRef()
                this.valueParameters.addAll(valueParameters)
                name = enumName
            }
        }
    }

    // ===== 值参数 =====

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
            val rawName = extractAnnotationNameText(annotation) ?: return@forEach
            val qualifiedName = macroSurfaceQualifiedName(rawName)
            val annotationCall = buildRawAnnotationCall(annotation, rawName, carrier)
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

    private fun buildRawAnnotationCall(
        annotation: LighterASTNode,
        rawName: String,
        carrier: CfirDeclaration,
    ): CfirAnnotationCall {
        val containingSymbol = when (carrier) {
            is CfirValueParameter -> carrier.containingDeclarationSymbol
            else -> carrier.symbol
        }
        return buildRawAnnotationCall(annotation, rawName, containingSymbol)
    }

    private fun buildRawAnnotationCall(
        annotation: LighterASTNode,
        rawName: String,
        containingSymbol: CfirBasedSymbol<*>,
        sourceOverride: CjSourceElement? = null,
        argumentListSourceOverride: CjSourceElement? = null,
    ): CfirAnnotationCall {
        val valueArgumentList = findFirstDescendantByType(annotation, CjNodeTypes.VALUE_ARGUMENT_LIST)
        val arguments = convertAnnotationArguments(annotation, valueArgumentList)
        return buildAnnotationCall {
            source = sourceOverride ?: annotation.toSource()
            typeRef = buildAnnotationTypeRef(rawName, annotation)
            this.arguments.addAll(arguments)
            argumentList = buildArgumentList {
                source = argumentListSourceOverride ?: valueArgumentList?.toSource()
                this.arguments.addAll(arguments)
            }
            calleeReference = buildNamedReference(Name.identifier(rawName.substringAfterLast('.')), annotation.toSource())
            containingDeclarationSymbol = containingSymbol
        }
    }

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

    private fun findFirstExpressionIn(node: LighterASTNode): LighterASTNode? {
        if (LightTreeRawCfirExpressionBuilder.isExpressionToken(node.tokenType)) return node
        tree.forEachChildren(node) { child ->
            findFirstExpressionIn(child)?.let { return it }
        }
        return null
    }

    private fun buildAnnotationTypeRef(rawName: String, annotation: LighterASTNode): CfirTypeRef {
        val parts = rawName.split('.').filter(String::isNotBlank)
        if (parts.isEmpty()) return buildImplicitTypeRef()
        return buildUserTypeRef {
            source = annotation.toSource()
            qualifier += parts.map { part ->
                buildQualifierPart {
                    source = annotation.toSource()
                    name = Name.identifier(part)
                }
            }
        }
    }

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

    private data class MacroSurfaceCommon(
        val surfaceId: Long,
        val qualifiedName: FqName?,
        val kind: MacroSurface.Kind,
        val hasParenthesis: Boolean,
        val attrTokens: List<MacroSurfaceToken>,
        val inputTokens: List<MacroSurfaceToken>,
        val sourceRange: MacroSurfaceSourceRange,
        val scopeContext: MacroSurfaceScopeContext,
        val modifiers: List<String>,
        val carriedAnnotations: List<String>,
        val capturedRawSyntax: String,
        val containerContext: MacroSurfaceContainerContext,
        val replaceHandle: CfirReplaceHandle,
    )

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

    private fun extractAnnotationNameText(annotation: LighterASTNode): String? {
        val nameNode = findAnnotationNameNode(annotation) ?: return null
        return nameNode.asText().trim().takeIf { it.isNotEmpty() }
    }

    private fun findAnnotationNameNode(annotation: LighterASTNode): LighterASTNode? {
        var result: LighterASTNode? = null

        fun visit(node: LighterASTNode) {
            when (node.tokenType) {
                CjNodeTypes.VALUE_ARGUMENT_LIST,
                CjNodeTypes.MACRO_INPUT,
                CjNodeTypes.MACRO_ATTR,
                -> return
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
                CjNodeTypes.REFERENCE_EXPRESSION,
                -> result = node
            }
            tree.forEachChildren(node, ::visit)
        }

        visit(annotation)
        return result
    }

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

    private fun macroSurfaceScopeContext(): MacroSurfaceScopeContext {
        val classFqName = (containerSymbolIfAny as? CfirClassLikeSymbol<*>)?.classId?.asSingleFqName()
        val functionName = (containerSymbolIfAny as? CfirCallableSymbol<*>)?.name
        return MacroSurfaceScopeContext(
            packageFqName = packageFqName,
            enclosingClassFqName = classFqName,
            enclosingFunctionName = functionName,
        )
    }

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

    private fun hasAncestor(node: LighterASTNode, tokenType: com.intellij.psi.tree.IElementType): Boolean {
        var current = node.getParent()
        while (current != null) {
            if (current.tokenType == tokenType) return true
            current = current.getParent()
        }
        return false
    }

    private fun commaListPosition(node: LighterASTNode): Int? {
        val parent = node.getParent()?.takeIf { it.tokenType == CjNodeTypes.VALUE_PARAMETER_LIST } ?: return null
        return tree.getChildrenByType(parent, CjNodeTypes.VALUE_PARAMETER).indexOf(node).takeIf { it >= 0 }
    }

    // ===== 文件级构建辅助 =====

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

    private fun containsChildByType(node: LighterASTNode, type: com.intellij.psi.tree.IElementType): Boolean {
        tree.forEachChildren(node) { child ->
            if (child.tokenType == type || containsChildByType(child, type)) return true
        }
        return false
    }

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

    private fun applyTopLevelMacroExpression(
        node: LighterASTNode,
        carrier: CfirDeclaration,
    ) {
        val inputNode = tree.findChildByType(node, CjNodeTypes.MACRO_INPUT) ?: return
        val rawName = extractMacroExpressionNameText(node) ?: return
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
            val restoredReturnType = findFunctionReturnTypeRefInMacroExpression(macroExpression)
                ?.let(::convertTypeRef)
            restoredReturnType?.let(carrier::replaceReturnTypeRef)
        }
    }

    private fun findFunctionReturnTypeRefInMacroExpression(macroExpression: LighterASTNode): LighterASTNode? {
        val parameterList = findFirstDescendantByType(macroExpression, CjNodeTypes.VALUE_PARAMETER_LIST) ?: return null
        val block = findFirstDescendantByType(macroExpression, CjNodeTypes.BLOCK)
        val afterParameters = tree.getEndOffset(parameterList)
        val beforeBody = block?.let(tree::getStartOffset) ?: macroExpression.endOffset
        return findDescendantsByType(macroExpression, CjNodeTypes.TYPE_REFERENCE)
            .firstOrNull { typeRef ->
                tree.getStartOffset(typeRef) >= afterParameters && tree.getEndOffset(typeRef) <= beforeBody
            }
    }

    private fun findDescendantsByType(
        node: LighterASTNode,
        tokenType: com.intellij.psi.tree.IElementType,
    ): List<LighterASTNode> {
        val result = mutableListOf<LighterASTNode>()
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

    private fun findFirstDeclarationDescendant(node: LighterASTNode): LighterASTNode? {
        if (LightTreeRawCfirExpressionBuilder.isDeclarationToken(node.tokenType)) return node
        tree.forEachChildren(node) { child ->
            findFirstDeclarationDescendant(child)?.let { return it }
        }
        return null
    }

    private fun findDirectDeclarationChild(node: LighterASTNode): LighterASTNode? {
        var declarationNode: LighterASTNode? = null
        tree.forEachChildren(node) { child ->
            if (declarationNode == null && LightTreeRawCfirExpressionBuilder.isDeclarationToken(child.tokenType)) {
                declarationNode = child
            }
        }
        return declarationNode
    }

    private fun findDirectMacroExpressionChild(node: LighterASTNode): LighterASTNode? {
        var macroExpression: LighterASTNode? = null
        tree.forEachChildren(node) { child ->
            if (macroExpression == null && child.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
                macroExpression = child
            }
        }
        return macroExpression
    }

    private fun extractMacroExpressionNameText(node: LighterASTNode): String? {
        val reference = tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION) ?: return null
        return reference.asText().trim().takeIf { it.isNotEmpty() }
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

    /** 转换单个类型参数 */
    private fun convertTypeParameter(node: LighterASTNode): CfirTypeParameter {
        val name = tree.findChildByType(node, CjTokens.IDENTIFIER)?.let { Name.identifier(it.asText()) }
            ?: Name.identifier("<error>")

        // 提取 bounds（extends bounds）
        val bounds = mutableListOf<CfirTypeRef>()
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjNodeTypes.TYPE_REFERENCE) {
                bounds.add(convertTypeRef(child))
            }
        }

        return buildSourceDeclaration(CfirTypeParameterSymbol()) { symbol ->
            buildTypeParameter {
                resolvePhase = CfirResolvePhase.RAW_CFIR
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                this.name = name
                this.bounds.addAll(bounds)
            }
        }
    }

    private fun convertTypeParameter(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        additionalBounds: List<CfirTypeRef> = emptyList(),
    ): CfirTypeParameter {
        val name = typeParameterName(node)
        val bounds = extractInlineTypeParameterBounds(node) + additionalBounds

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
                this.bounds.addAll(bounds)
            }
        }
    }

    private fun typeParameterName(node: LighterASTNode): Name {
        return tree.findChildByType(node, CjTokens.IDENTIFIER)?.let { Name.identifier(it.asText()) }
            ?: Name.identifier("<error>")
    }

    private fun extractInlineTypeParameterBounds(node: LighterASTNode): List<CfirTypeRef> {
        val bounds = mutableListOf<CfirTypeRef>()
        tree.forEachChildren(node) { child ->
            if (child.tokenType == CjNodeTypes.TYPE_REFERENCE) {
                bounds += convertTypeRef(child)
            }
        }
        return bounds
    }

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
                )
            }

        if (typeConstraints.isEmpty()) return null

        return CfirTypeConstraintDiagnosticData(
            typeConstraints = typeConstraints,
        )
    }

    private fun declarationAttributes(ownerNode: LighterASTNode): CfirDeclarationAttributes {
        val diagnosticData = collectTypeConstraintDiagnosticData(ownerNode) ?: return CfirDeclarationAttributes.EMPTY
        return CfirDeclarationAttributes().apply {
            typeConstraintDiagnosticData = diagnosticData
        }
    }

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
            -> findFunctionReturnTypeRefInMacroExpression(node)
                ?: tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)
            else -> tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)
        }
        return convertTypeRef(typeRef)
    }

    /** 提取值参数列表 */
    private fun extractValueParameters(
        node: LighterASTNode,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        requiresExplicitType: Boolean = true,
    ): List<CfirValueParameter> {
        val paramList = tree.findChildByType(node, CjNodeTypes.VALUE_PARAMETER_LIST) ?: return emptyList()
        return tree.getChildrenByType(paramList, CjNodeTypes.VALUE_PARAMETER)
            .map { convertValueParameter(it, containingDeclarationSymbol, requiresExplicitType) }
    }

    private fun countValueParameters(node: LighterASTNode): Int {
        val paramList = tree.findChildByType(node, CjNodeTypes.VALUE_PARAMETER_LIST) ?: return 0
        return tree.getChildrenByType(paramList, CjNodeTypes.VALUE_PARAMETER).size
    }

    /** 提取函数体块 */
    private fun extractBody(node: LighterASTNode): CfirBlock? {
        val blockNode = tree.findChildByType(node, CjNodeTypes.BLOCK) ?: return null
        return expressionBuilder.convertBlock(blockNode)
    }

    private fun extractPropertyAccessorNodes(node: LighterASTNode): List<LighterASTNode> {
        val propertyBody = tree.findChildByType(node, CjNodeTypes.PROPERTY_BODY) ?: return emptyList()
        return tree.getChildrenByType(propertyBody, CjNodeTypes.PROPERTY_ACCESSOR)
    }

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

    /** 查找模式子节点 */
    private fun findPatternChild(node: LighterASTNode): LighterASTNode? {
        tree.forEachChildren(node) { child ->
            if (LightTreeRawCfirExpressionBuilder.isPatternToken(child.tokenType)) return child
        }
        return null
    }

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
            copied.isAbstract = status.isAbstract
            copied.isOpen = status.isOpen
            copied.isSealed = status.isSealed
        }
    }

    private inline fun <D : CfirDeclaration, S : CfirBasedSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)

        return declaration
    }
}
