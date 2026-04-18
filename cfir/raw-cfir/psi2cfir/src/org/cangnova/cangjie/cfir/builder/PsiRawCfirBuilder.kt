package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.AstLoadingFilter
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjPsiSourceFileLinesMapping
import org.cangnova.cangjie.source.CjRealPsiSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildSuperReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjNodeTypes.BOOLEAN_CONSTANT
import org.cangnova.cangjie.psi.CjNodeTypes.FLOAT_CONSTANT
import org.cangnova.cangjie.psi.CjNodeTypes.INTEGER_CONSTANT
import org.cangnova.cangjie.psi.CjNodeTypes.RUNE_CONSTANT
import org.cangnova.cangjie.psi.CjNodeTypes.UNIT_CONSTANT

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

    override fun buildExpression(expression: PsiElement): CfirExpression {
        val cjExpression = expression as? CjExpression
            ?: error("Expected CjExpression but was ${expression::class.qualifiedName}")
        return converter.convertExpression(cjExpression)
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
                    declarations.addAll(file.declarations.map { buildDeclaration(it) })
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

        fun convertDeclaration(psi: CjDeclaration): CfirDeclaration = when (psi) {
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
                                declarations.add(0, buildImplicitPrimaryConstructor(psi))
                            }
                            typeParameters to declarations
                        }
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
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
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                withFunctionTarget(functionTarget) {
                    psi.bodyBlockExpression?.let { convertBlock(it) }
                }
            }

            return buildSourceDeclaration(CfirNamedFunctionSymbol(callableIdFor(name))) { symbol ->
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
            val getter = psi.getter?.let { accessor ->
                convertPropertyAccessor(
                    psi = accessor,
                    accessorName = Name.special("<get-${name.asString()}>"),
                    defaultReturnTypeRef = typeRef,
                )
            }
            val setter = psi.setter?.let { accessor ->
                convertPropertyAccessor(
                    psi = accessor,
                    accessorName = Name.special("<set-${name.asString()}>"),
                    defaultReturnTypeRef = buildImplicitTypeRef(),
                )
            }

            return buildSourceDeclaration(CfirPropertySymbol(callableIdFor(name))) { symbol ->
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
            defaultReturnTypeRef: CfirTypeRef,
        ): CfirNamedFunction {
            val valueParams = psi.valueParameters.map { parameter -> convertValueParameter(parameter) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                withFunctionTarget(functionTarget) {
                    psi.bodyExpression?.let(::toBlock)
                }
            }

            return buildSourceDeclaration(CfirNamedFunctionSymbol(callableIdFor(accessorName))) { symbol ->
                buildNamedFunction {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = psi.returnTypeReference?.let(::convertTypeRef) ?: defaultReturnTypeRef
                    this.name = accessorName
                    valueParameters.addAll(valueParams)
                    this.body = body
                    isMut = false
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
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                withFunctionTarget(functionTarget) {
                    psi.bodyBlockExpression?.let { convertBlock(it) }
                }
            }

            return buildSourceDeclaration(CfirMainFunctionSymbol(callableIdFor(Name.identifier("main")))) { symbol ->
                buildMainFunction {
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
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }.also { bindFunctionTarget(functionTarget, it) }
        }

        fun convertMacroDeclaration(psi: CjMacroDeclaration): CfirMacroDeclaration {
            val name = psi.nameAsSafeName
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                withFunctionTarget(functionTarget) {
                    psi.bodyBlockExpression?.let { convertBlock(it) }
                }
            }

            return buildSourceDeclaration(CfirMacroDeclarationSymbol(callableIdFor(name))) { symbol ->
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
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                withFunctionTarget(functionTarget) {
                    psi.bodyBlockExpression?.let { convertBlock(it) }
                }
            }

            return buildSourceDeclaration(CfirFinalizerSymbol(callableIdFor(SpecialNames.END_INIT))) { symbol ->
                buildFinalizer {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    dispatchReceiverType = currentDispatchReceiverType()
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = buildImplicitTypeRef()
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
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = false)
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                withFunctionTarget(functionTarget) {
                    psi.bodyBlockExpression?.let { convertBlock(it) }
                }
            }

            return buildSourceDeclaration(CfirConstructorSymbol(callableIdFor(SpecialNames.INIT))) { symbol ->
                if (isPrimary) {
                    buildPrimaryConstructor {
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        source = psi.toCjPsiSourceElement()
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData

                        attributes = CfirDeclarationAttributes.EMPTY
                        isLocal = context.inLocalContext
                        dispatchReceiverType = currentDispatchReceiverType()
                        status = convertDeclarationStatus(psi)
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

                        attributes = CfirDeclarationAttributes.EMPTY
                        isLocal = context.inLocalContext
                        dispatchReceiverType = currentDispatchReceiverType()
                        status = convertDeclarationStatus(psi)
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
            val valueParameters = valueTypeRefs.mapIndexed { index, valueTypeRef ->
                buildEnumConstructorValueParameter(
                    source = valueTypeRef.source ?: psi.toCjPsiSourceElement(),
                    name = enumConstructorPayloadParameterName(index),
                    returnTypeRef = valueTypeRef,
                )
            }
            return buildSourceDeclaration(CfirEnumConstructorSymbol(callableIdFor(enumConstructorName))) { symbol ->
                buildEnumConstructor {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = context.inLocalContext
                    status = CfirDeclarationStatusImpl.DEFAULT
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

        fun convertValueParameter(psi: CjParameter): CfirValueParameter {
            return buildSourceDeclaration(CfirValueParameterSymbol(callableIdFor(psi.nameAsSafeName))) { symbol ->
                buildValueParameter {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = psi.toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    isNamed = psi.isNamed
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    name = psi.nameAsSafeName
                    defaultValue = psi.defaultValue?.let { convertExpression(it) }
                }
            }
        }

        private fun convertTypeParameter(
            psi: CjTypeParameter,
            containingDeclarationSymbol: CfirBasedSymbol<*>,
            additionalBounds: List<CfirTypeRef> = emptyList(),
        ): CfirTypeParameter {
            val name = Name.identifier(psi.name ?: "<error>")
            val bounds = buildList {
                addAll(psi.extendsBounds.map(::convertTypeRef))
                addAll(additionalBounds)
            }

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
                    this.bounds.addAll(bounds)
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

        fun convertBlock(psi: CjBlockExpression): CfirBlock {
            if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                return buildBlock {
                    source = psi.toCjPsiSourceElement()
                }
            }
            val statements = withLocalContext {
                psi.statements.map { stmt ->
                    when (stmt) {
                        is CjPatternVariable -> convertPatternVariable(stmt)
                        is CjNamedFunction -> convertFunction(stmt)
                        is CjDeclaration -> convertDeclaration(stmt)
                        else -> convertExpression(stmt)
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
                BOOLEAN_CONSTANT -> CfirLiteralKind.BOOLEAN to true
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
            return buildRangeExpression {
                source = psi.toCjPsiSourceElement()
                this.start = start
                this.end = end
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
            val lambdaArgs = psi.lambdaArguments.mapNotNull { it.getLambdaExpression()?.let(::convertLambda) }

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
                }
            }

            // valueArguments 已经包含了 lambdaArguments，不需要再单独处理
            val allArgs = psi.valueArguments.mapNotNull(::convertCallArgument)
            val typeArgs = extractCallTypeArguments(psi, callee)

            val (receiver, reference) = resolveCalleeReference(callee)

            return buildFunctionCall {
                source = psi.toCjPsiSourceElement()
                calleeReference = reference
                argumentList = buildArgumentList {
                    arguments.addAll(allArgs)
                }
                explicitReceiver = receiver
                typeArguments.addAll(typeArgs)
                origin = callOriginFor(callee)
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

            if (!argument.isNamed()) return convertedExpression

            return buildBlock {
                source = argument.asElement().toCjPsiSourceElement()
                statements.add(convertedExpression)
            }
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
                    val recv = convertExpression(callee.receiverExpression)
                    val selector = callee.selectorExpression
                    val ref = if (selector is CjSimpleNameExpression) {
                        buildNamedReference(selector.referencedNameAsName, selector.toCjPsiSourceElement())
                    } else {
                        buildNamedReference(Name.identifier("<error>"), selector?.toCjPsiSourceElement())
                    }
                    recv to ref
                }

                else -> null to buildNamedReference(
                    Name.identifier(callee?.text ?: "<error>"),
                    callee?.toCjPsiSourceElement()
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
                    selector.lambdaArguments.mapNotNull { it.getLambdaExpression()?.let { l -> convertLambda(l) } }

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
            val loopParam = psi.loopParameter
            val loopVarName = loopParam?.nameAsSafeName ?: Name.special("<anonymous>")
            val loopTypeRef = if (loopParam != null) convertTypeRef(loopParam.typeReference) else buildImplicitTypeRef()
            val loopStatus = cloneDeclarationStatus(CfirDeclarationStatusImpl.DEFAULT)
            val variable = buildSourceDeclaration(CfirPatternVariableSymbol(callableIdFor(loopVarName))) { symbol ->
                buildPatternVariable {
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    source = (loopParam ?: psi).toCjPsiSourceElement()
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData

                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    status = loopStatus
                    returnTypeRef = loopTypeRef
                    pattern = buildBindingPattern {
                        source = (loopParam ?: psi).toCjPsiSourceElement()
                        name = loopVarName
                        typeRef = loopTypeRef.takeUnless { it is CfirImplicitTypeRef }
                        bindingVariable = createPatternBindingVariable(
                            source = (loopParam ?: psi).toCjPsiSourceElement(),
                            name = loopVarName,
                            status = loopStatus,
                            isLocal = true,
                            isVar = false,
                            returnTypeRef = loopTypeRef,
                        )
                    }
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

        private fun convertTry(psi: CjTryExpression): CfirTryExpression {
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
                val parameter =
                    buildSourceDeclaration(CfirValueParameterSymbol(callableIdFor(catchParamName))) { symbol ->
                        buildValueParameter {
                            resolvePhase = CfirResolvePhase.RAW_CFIR
                            source = (catchParam ?: clause).toCjPsiSourceElement()
                            this.symbol = symbol
                            origin = CfirDeclarationOrigin.Source
                            moduleData = baseModuleData

                            attributes = CfirDeclarationAttributes.EMPTY
                            isLocal = false
                            isNamed = false
                            status = CfirDeclarationStatusImpl.DEFAULT
                            returnTypeRef =
                                if (catchParam != null) convertTypeRef(catchParam.typeReference) else buildImplicitTypeRef()
                            name = catchParamName
                        }
                    }
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
                this.tryBlock = tryBlock
                this.handlers.addAll(handlers)
                this.catches.addAll(catches)
                this.finallyBlock = finallyBlock
            }
        }

        // ---- Lambda ----

        private fun convertLambda(psi: CjLambdaExpression): CfirAnonymousFunctionExpression {
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val hasExplicitParameterList = psi.valueParameters.isNotEmpty()
            val functionTarget = CfirFunctionTarget(labelName = null, isLambda = true)
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                withFunctionTarget(functionTarget) {
                    psi.bodyExpression?.let { convertBlock(it) }
                }
            }

            val anonymousFunction = buildSourceDeclaration(CfirAnonymousFunctionSymbol()) { symbol ->
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
            return buildMacroExpression {
                source = psi.toCjPsiSourceElement()
                name = psi.shortName
                inputText = psi.input?.text
                attrText = psi.attr?.text
            }
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

        private fun toBlock(psi: CjExpression): CfirBlock {
            if (psi is CjBlockExpression) return convertBlock(psi)
            return buildBlock {
                source = psi.toCjPsiSourceElement()
                statements.add(convertExpression(psi))
            }
        }

        private fun convertTypeRef(psi: CjTypeReference?): CfirTypeRef {
            return psi.toFirOrImplicitTypeRef { it.toSourceElement() }
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
                )
            }

            if (typeConstraints.isEmpty()) return null

            return CfirTypeConstraintDiagnosticData(
                typeConstraints = typeConstraints,
            )
        }

        private fun declarationAttributes(owner: CjElement?): CfirDeclarationAttributes {
            val typeParameterOwner = owner as? CjTypeParameterListOwner ?: return CfirDeclarationAttributes.EMPTY
            val diagnosticData =
                collectTypeConstraintDiagnosticData(typeParameterOwner) ?: return CfirDeclarationAttributes.EMPTY
            return CfirDeclarationAttributes().apply {
                typeConstraintDiagnosticData = diagnosticData
            }
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
            psi: CjNamedFunction,
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
            return typeStatement.body?.declarations?.map { convertDeclaration(it) } ?: emptyList()
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

        private fun convertDeclarationStatus(
            owner: CjModifierListOwner,
            declaration: CjDeclaration
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
            val defaultVisibility = when {
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
                    else -> defaultVisibility
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
