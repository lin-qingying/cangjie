package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.builder.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toCjLightSourceElement
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildTupleTypeRef
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * LightTree → Raw CFIR 声明构建器（对齐 PsiRawCfirBuilder 的声明转换部分）。
 *
 * 继承 [AbstractRawCfirBuilder]，实现三个模板方法，
 * 通过 `when(node.tokenType)` 手动分发代替 PSI Visitor 模式。
 */
class LightTreeRawCfirDeclarationBuilder(
    session: CfirSession,
    private val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val source: CharSequence,
    context: Context<LighterASTNode> = Context(),
    private val fileName: String = "",
    val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) : AbstractRawCfirBuilder<LighterASTNode>(session, context) {

    private fun callableIdFor(name: Name): CallableId {
        return if (context.inLocalContext) CallableId(name) else CallableId(packageFqName, name)
    }

    // ===== AbstractRawCfirBuilder 抽象方法实现 =====

    override fun LighterASTNode.toSourceElement(): AbstractCjSourceElement =
        toCjLightSourceElement(tree)

    override fun LighterASTNode.elementType(): IElementType = tokenType

    override fun LighterASTNode.asText(): String = getNodeText(this, source)

    private fun LighterASTNode.toSource(): CjSourceElement =
        toCjLightSourceElement(tree)

    // ===== 表达式构建器（延迟初始化，解决循环依赖） =====

    val expressionBuilder: LightTreeRawCfirExpressionBuilder by lazy {
        LightTreeRawCfirExpressionBuilder(baseSession, tree, source, context, this)
    }

    // ===== Public API =====

    override fun buildFile(file: LighterASTNode): CfirFile {
        // 解析包名
        val packageNode = tree.findChildByType(file, CjNodeTypes.PACKAGE_DIRECTIVE)
        val packageFqName = packageNode?.let { extractPackageFqName(it) } ?: FqName.ROOT

        return withPackageContext(packageFqName) {
            val symbol = CfirFileSymbol()
            buildSourceDeclaration(symbol) { fileSymbol ->
                buildFile {
                    source = file.toSource()
                    this.symbol = fileSymbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    name = fileName // LightTree 文件名由外部传入
                    packageDirective = buildPackageDirectiveNode(packageNode, packageFqName)
                    imports.addAll(buildImportsFromFile(file))
                    declarations.addAll(buildFileDeclarations(file))
                }
            }
        }
    }

    override fun buildDeclaration(declaration: LighterASTNode): CfirDeclaration =
        convertDeclaration(declaration)

    override fun buildExpression(expression: LighterASTNode): CfirExpression =
        expressionBuilder.convertExpression(expression)

    // ===== 声明转换入口 =====

    fun convertDeclaration(node: LighterASTNode): CfirDeclaration = when (node.tokenType) {
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
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                reason = "Unsupported declaration: ${node.tokenType}"
            }
        }
    }

    // ===== 类/接口/结构体/枚举 =====

    private fun convertClass(node: LighterASTNode, classKind: CfirClassKind): CfirDeclaration {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeParams = extractTypeParameters(node)
        val superTypes = extractSuperTypeRefs(node)
        val classDeclarations = extractClassMembers(node).toMutableList()

        if (classKind != CfirClassKind.INTERFACE && classDeclarations.none { it is CfirConstructor }) {
            classDeclarations.add(0, buildImplicitPrimaryConstructor(node))
        }

        // 枚举：将 ENUM_CONSTRUCTOR 放在声明列表前面
        if (classKind == CfirClassKind.ENUM) {
            val enumBody = tree.findChildByType(node, CjNodeTypes.ENUM_BODY)
            if (enumBody != null) {
                val enumCtors = tree.getChildrenByType(enumBody, CjNodeTypes.ENUM_CONSTRUCTOR)
                    .map { convertEnumConstructor(it, typeParams) }
                classDeclarations.addAll(0, enumCtors)
            }
        }

        return when (classKind) {
            CfirClassKind.CLASS -> buildSourceDeclaration(CfirClassSymbol()) { symbol ->
                buildClass {
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = modifiers.toDeclarationStatus(context.inLocalContext)
                    this.typeParameters.addAll(typeParams)
                    this.superTypeRefs.addAll(superTypes)
                    this.declarations.addAll(classDeclarations)
                    this.name = name
                }
            }
            CfirClassKind.INTERFACE -> buildSourceDeclaration(CfirInterfaceSymbol()) { symbol ->
                buildInterface {
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = modifiers.toDeclarationStatus(context.inLocalContext)
                    this.typeParameters.addAll(typeParams)
                    this.superTypeRefs.addAll(superTypes)
                    this.declarations.addAll(classDeclarations)
                    this.name = name
                }
            }
            CfirClassKind.STRUCT -> buildSourceDeclaration(CfirStructSymbol()) { symbol ->
                buildStruct {
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = modifiers.toDeclarationStatus(context.inLocalContext)
                    this.typeParameters.addAll(typeParams)
                    this.superTypeRefs.addAll(superTypes)
                    this.declarations.addAll(classDeclarations)
                    this.name = name
                }
            }
            CfirClassKind.ENUM -> buildSourceDeclaration(CfirEnumSymbol()) { symbol ->
                buildEnum {
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = modifiers.toDeclarationStatus(context.inLocalContext)
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
                source = ownerNode.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = CfirDeclarationStatusImpl.DEFAULT
                returnTypeRef = buildImplicitTypeRef()
                body = null
            }
        }
    }

    // ===== Extend =====

    private fun convertExtend(node: LighterASTNode): CfirExtend {
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeParams = extractTypeParameters(node)

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
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
                this.typeParameters.addAll(typeParams)
                this.extendedTypeRef = extendedType
                this.superTypeRefs.addAll(superTypes)
                this.declarations.addAll(members)
            }
        }
    }

    // ===== 函数 =====

    private fun convertFunction(node: LighterASTNode): CfirFunction {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeParams = extractFunctionTypeParameters(node)
        val returnTypeRef = extractReturnTypeRef(node)
        val valueParams = extractValueParameters(node)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractBody(node)

        return buildSourceDeclaration(CfirNamedFunctionSymbol(callableIdFor(name))) { symbol ->
            buildNamedFunction {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.name = name
                this.valueParameters.addAll(valueParams)
                this.body = body
                isMut = modifiers.isMut
            }
        }
    }

    private fun convertMainFunction(node: LighterASTNode): CfirMainFunction {
        val modifiers = LightTreeModifierList.from(tree, node)
        val valueParams = extractValueParameters(node)
        val returnTypeRef = extractReturnTypeRef(node)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractBody(node)

        return buildSourceDeclaration(CfirMainFunctionSymbol(callableIdFor(Name.identifier("main")))) { symbol ->
            buildMainFunction {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
                this.returnTypeRef = returnTypeRef
                this.valueParameters.addAll(valueParams)
                this.body = body
            }
        }
    }

    private fun convertMacroDeclaration(node: LighterASTNode): CfirMacroDeclaration {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val valueParams = extractValueParameters(node)
        val returnTypeRef = extractReturnTypeRef(node)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractBody(node)

        return buildSourceDeclaration(CfirMacroDeclarationSymbol(callableIdFor(name))) { symbol ->
            buildMacroDeclaration {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
                this.returnTypeRef = returnTypeRef
                this.name = name
                this.valueParameters.addAll(valueParams)
                this.body = body
            }
        }
    }

    private fun convertFinalizer(node: LighterASTNode): CfirFinalizer {
        val modifiers = LightTreeModifierList.from(tree, node)
        val valueParams = extractValueParameters(node)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractBody(node)

        return buildSourceDeclaration(CfirFinalizerSymbol(callableIdFor(SpecialNames.END_INIT))) { symbol ->
            buildFinalizer {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
                returnTypeRef = buildImplicitTypeRef()
                this.valueParameters.addAll(valueParams)
                this.body = body
            }
        }
    }

    // ===== 属性/字段/变量 =====

    private fun convertProperty(node: LighterASTNode): CfirProperty {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeRef = extractReturnTypeRef(node)

        return buildSourceDeclaration(CfirPropertySymbol(callableIdFor(name))) { symbol ->
            buildProperty {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
                this.returnTypeRef = typeRef
                this.name = name
            }
        }
    }

    private fun convertFieldVariable(node: LighterASTNode): CfirFieldVariable {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeRef = extractReturnTypeRef(node)
        val initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractInitializer(node)
        val isVar = hasVarKeyword(node)

        return buildSourceDeclaration(CfirFieldVariableSymbol(callableIdFor(name))) { symbol ->
            buildFieldVariable {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
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
        val pattern = patternNode?.let { expressionBuilder.convertPattern(it) }
            ?: org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern { source = node.toSource() }
        val initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractInitializer(node)
        val isVar = hasVarKeyword(node)

        return buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(Name.special("<pattern-variable>")))) { symbol ->
            buildPatternVariable {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
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
        val valueParams = extractValueParameters(node)
        val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) null else extractBody(node)

        return buildSourceDeclaration(CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))) { symbol ->
            if (isPrimary) {
                buildPrimaryConstructor {
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = modifiers.toDeclarationStatus(context.inLocalContext)
                    returnTypeRef = buildImplicitTypeRef()
                    this.valueParameters.addAll(valueParams)
                    this.body = body
                }
            } else {
                buildConstructor {
                    source = node.toSource()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = modifiers.toDeclarationStatus(context.inLocalContext)
                    returnTypeRef = buildImplicitTypeRef()
                    this.valueParameters.addAll(valueParams)
                    this.body = body
                }
            }
        }
    }

    // ===== 类型别名 =====

    private fun convertTypeAlias(node: LighterASTNode): CfirTypeAlias {
        val name = extractName(node)
        val modifiers = LightTreeModifierList.from(tree, node)
        val typeParams = extractTypeParameters(node)
        val expandedType = extractReturnTypeRef(node)

        return buildSourceDeclaration(CfirTypeAliasSymbol()) { symbol ->
            buildTypeAlias {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = modifiers.toDeclarationStatus(context.inLocalContext)
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
        val enumCtorTypeRef = when (valueTypeRefs.size) {
            0 -> buildImplicitTypeRef()
            1 -> valueTypeRefs.first()
            else -> buildTupleTypeRef {
                source = node.toSource()
                elementTypeRefs.addAll(valueTypeRefs)
            }
        }

        return buildSourceDeclaration(CfirEnumConstructorSymbol(callableIdFor(enumName))) { symbol ->
            buildEnumConstructor {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = CfirDeclarationStatusImpl.DEFAULT
                typeParameters.addAll(ownerTypeParameters)
                returnTypeRef = enumCtorTypeRef
                name = enumName
            }
        }
    }

    // ===== 值参数 =====

    fun convertValueParameter(node: LighterASTNode): CfirValueParameter {
        val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
        val paramName = if (nameNode != null) Name.identifier(nameNode.asText()) else Name.special("<error>")
        val typeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)
        val paramType = convertTypeRef(typeRef)

        // 默认值是最后一个表达式子节点
        var defaultExpr: CfirExpression? = null
        tree.forEachChildren(node) { child ->
            if (LightTreeRawCfirExpressionBuilder.isExpressionToken(child.tokenType)) {
                defaultExpr = expressionBuilder.convertExpression(child)
            }
        }

        return buildSourceDeclaration(CfirValueParameterSymbol(callableIdFor(paramName))) { symbol ->
            buildValueParameter {
                source = node.toSource()
                this.symbol = symbol
                origin = CfirDeclarationOrigin.Source
                moduleData = baseModuleData
                attributes = CfirDeclarationAttributes.EMPTY
                status = CfirDeclarationStatusImpl.DEFAULT
                returnTypeRef = paramType
                name = paramName
                defaultValue = defaultExpr
            }
        }
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
        }
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
        tree.forEachChildren(file) { child ->
            if (LightTreeRawCfirExpressionBuilder.isDeclarationToken(child.tokenType)) {
                declarations.add(convertDeclaration(child))
            }
        }
        return declarations
    }

    // ===== 通用提取辅助 =====

    /** 从声明节点提取名称 */
    private fun extractName(node: LighterASTNode): Name {
        val nameNode = tree.findChildByType(node, CjTokens.IDENTIFIER)
            ?: tree.findChildByType(node, CjNodeTypes.REFERENCE_EXPRESSION)
        return if (nameNode != null) {
            Name.identifier(nameNode.asText())
        } else {
            Name.special("<anonymous>")
        }
    }

    /** 提取类型参数列表 */
    private fun extractTypeParameters(node: LighterASTNode): List<CfirTypeParameter> {
        val typeParamList = tree.findChildByType(node, CjNodeTypes.TYPE_PARAMETER_LIST) ?: return emptyList()
        return tree.getChildrenByType(typeParamList, CjNodeTypes.TYPE_PARAMETER).map { convertTypeParameter(it) }
    }

    /** 提取函数类型参数列表 */
    private fun extractFunctionTypeParameters(node: LighterASTNode): List<CfirTypeParameter> =
        extractTypeParameters(node)

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

    /** 提取超类型引用列表 */
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
        tree.forEachChildren(bodyNode) { child ->
            val tt = child.tokenType
            // 排除 ENUM_CONSTRUCTOR（由 convertClass 单独处理）
            if (tt != CjNodeTypes.ENUM_CONSTRUCTOR && LightTreeRawCfirExpressionBuilder.isDeclarationToken(tt)) {
                declarations.add(convertDeclaration(child))
            }
        }
        return declarations
    }

    /** 提取返回类型引用 */
    private fun extractReturnTypeRef(node: LighterASTNode): CfirTypeRef {
        val typeRef = tree.findChildByType(node, CjNodeTypes.TYPE_REFERENCE)
        return convertTypeRef(typeRef)
    }

    /** 提取值参数列表 */
    private fun extractValueParameters(node: LighterASTNode): List<CfirValueParameter> {
        val paramList = tree.findChildByType(node, CjNodeTypes.VALUE_PARAMETER_LIST) ?: return emptyList()
        return tree.getChildrenByType(paramList, CjNodeTypes.VALUE_PARAMETER).map { convertValueParameter(it) }
    }

    /** 提取函数体块 */
    private fun extractBody(node: LighterASTNode): CfirBlock? {
        val blockNode = tree.findChildByType(node, CjNodeTypes.BLOCK) ?: return null
        return expressionBuilder.convertBlock(blockNode)
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
        return convertTypeReference(typeRefNode, tree, source) { it.toCjLightSourceElement(tree) }
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

    private inline fun <D : CfirDeclaration, S : CfirSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)
        symbol.bind(declaration)
        return declaration
    }
}
