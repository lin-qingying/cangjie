package org.cangjie.cfir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.AstLoadingFilter
import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirRealSourceElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangjie.cfir.declarations.builder.*
import org.cangjie.cfir.expressions.*
import org.cangjie.cfir.expressions.builder.*
import org.cangjie.cfir.patterns.*
import org.cangjie.cfir.patterns.builder.*
import org.cangjie.cfir.references.CfirNamedReference
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.symbols.*
import org.cangjie.cfir.types.*
import org.cangjie.cfir.types.builder.buildTupleTypeRef
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*

/**
 * PSI 鈫?Raw CFIR 鏋勫缓鍣紙瀵归綈 Kotlin 鐨?PsiRawFirBuilder锛夈€? *
 * 閬嶅巻 PSI 璇硶鏍戯紝鐢熸垚 Raw CFIR 涓棿琛ㄧず銆? *
 * 鍦?RAW_CFIR 闃舵锛? * - 鎵€鏈夌被鍨嬪紩鐢ㄤ负 CfirUserTypeRef锛堟湭瑙ｆ瀽锛? * - 鎵€鏈夌鍙峰紩鐢ㄤ负 CfirNamedReference锛堟湭缁戝畾锛? * - 涓嶅仛绫诲瀷鎺ㄦ柇銆侀噸杞借В鏋愶紙閭ｆ槸 CFIR_RESOLVE 鐨勫伐浣滐級
 */
class PsiRawCfirBuilder(
    session: CfirSession,
    private val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) : AbstractRawCfirBuilder<PsiElement>(session) {

    // ===== AbstractRawCfirBuilder 鎶借薄鏂规硶瀹炵幇 =====

    override fun PsiElement.toSourceElement(): CfirSourceElement {
        val range = textRange
        val filePath = (containingFile as? CjFile)?.virtualFile?.path
        return CfirRealSourceElement(
            startOffset = range.startOffset,
            endOffset = range.endOffset,
            filePath = filePath,
        )
    }

    override fun PsiElement.elementType(): IElementType = node.elementType

    override fun PsiElement.asText(): String = text
    var mode:  BodyBuildingMode = bodyBuildingMode
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
     * 鏋勫缓 CfirFile锛堟枃浠剁骇鍏ュ彛鐐癸級銆?     */
    fun buildCfirFile(file: CjFile): CfirFile {
        return runOnStubs { file.accept(Visitor(), null) as CfirFile }
    }

    // ===== Visitor锛堝唴閮ㄧ被锛屽榻?Kotlin 鐨?PsiRawFirBuilder.Visitor锛?=====

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

    private inline fun <D : CfirDeclaration, S : CfirSymbol<D>> buildSourceDeclaration(
        symbol: S,
        builder: (S) -> D,
    ): D {
        val declaration = builder(symbol)
        symbol.bind(declaration)
        return declaration
    }

    private fun buildFile(file: CjFile): CfirFile {
        return withPackageContext(file.packageFqName) {
            val symbol = CfirFileSymbol()
            buildSourceDeclaration(symbol) { fileSymbol ->
                buildFile {
                    this.symbol = fileSymbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    name = file.name
                    packageDirective = buildPackageDirective(file.packageDirective)
                    imports.addAll(this@PsiRawCfirBuilder.buildImports(file))
                    declarations.addAll(file.declarations.map { buildDeclaration(it) })
                }
            }
        }
    }

    /**
     * Visitor 璐熻矗 PSI 鑺傜偣鍒?CFIR 鑺傜偣鐨勫垎鍙戣浆鎹€?     *
     * 瀵归綈 Kotlin 鐨?PsiRawFirBuilder.Visitor : KtVisitor<FirElement, FirElement?>
     */
    protected open inner class Visitor : CjVisitor<CfirElement, Unit?>() {
        override fun visitCjFile(file: CjFile, data: Unit?): CfirElement = buildFile(file)

        override fun visitDeclaration(dcl: CjDeclaration, data: Unit?): CfirElement = buildDeclaration(dcl)

        override fun visitExpression(expression: CjExpression, data: Unit?): CfirElement = buildExpression(expression)
    }

    protected open inner class Converter {

        // ===== 澹版槑杞崲 =====

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
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    reason = "Unsupported declaration: ${psi.javaClass.simpleName}"
                }
            }
        }

        private fun convertClass(psi: CjClassLikeDeclaration, classKind: CfirClassKind): CfirClass {
            val name = psi.nameAsSafeName
            val classDeclarations = convertClassMembers(psi).toMutableList()
            if (psi is CjEnum) {
                classDeclarations.addAll(0, psi.constructor.map { convertEnumConstructor(it) })
            }
            return buildSourceDeclaration(CfirClassSymbol()) { symbol ->
                buildClass {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(convertTypeParameters(psi))
                    superTypeRefs.addAll(convertSuperTypeRefs(psi))
                    declarations.addAll(classDeclarations)
                    this.name = name
                    this.classKind = classKind
                }
            }
        }

        private fun convertExtend(psi: CjExtend): CfirExtend {
            val extendedTypeRef = convertTypeRef(psi.receiverTypeReceiver)
            val superTypes = psi.superTypeListEntries.map { convertTypeRef(it.typeReference) }
            val members = psi.body?.declarations?.map { convertDeclaration(it) } ?: emptyList()

            return buildSourceDeclaration(CfirExtendSymbol()) { symbol ->
                buildExtend {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(convertTypeParameters(psi))
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
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyBlockExpression?.let { convertBlock(it) }
            }

            return buildSourceDeclaration(CfirFunctionSymbol()) { symbol ->
                buildFunction {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(convertFunctionTypeParameters(psi))
                    this.returnTypeRef = returnTypeRef
                    this.name = name
                    valueParameters.addAll(valueParams)
                    this.body = body
                    isMut = psi.isMut
                }
            }
        }

        private fun convertProperty(psi: CjProperty): CfirProperty {
            val name = psi.nameAsSafeName
            val typeRef = convertTypeRef(psi.typeReference)
            val initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.initializer?.let { convertExpression(it) }
            }

            return buildSourceDeclaration(CfirPropertySymbol()) { symbol ->
                buildProperty {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    this.returnTypeRef = typeRef
                    this.name = name
                    this.initializer = initializer
                    isVar = psi.isVar
                }
            }
        }

        private fun convertFieldVariable(psi: CjFieldVariable): CfirVariable {
            return buildSourceDeclaration(CfirVariableSymbol()) { symbol ->
                buildVariable {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    this.returnTypeRef = convertTypeRef(psi.typeReference)
                    name = psi.nameAsSafeName
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
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyBlockExpression?.let { convertBlock(it) }
            }

            return buildSourceDeclaration(CfirMainFunctionSymbol()) { symbol ->
                buildMainFunction {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }
        }

        fun convertMacroDeclaration(psi: CjMacroDeclaration): CfirMacroDeclaration {
            val name = psi.nameAsSafeName
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyBlockExpression?.let { convertBlock(it) }
            }

            return buildSourceDeclaration(CfirMacroDeclarationSymbol()) { symbol ->
                buildMacroDeclaration {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    this.name = name
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }
        }

        fun convertFinalizer(psi: CjFinalizer): CfirFinalizer {
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyBlockExpression?.let { convertBlock(it) }
            }

            return buildSourceDeclaration(CfirFinalizerSymbol()) { symbol ->
                buildFinalizer {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = buildImplicitTypeRef()
                    valueParameters.addAll(valueParams)
                    this.body = body
                }
            }
        }

        private fun convertPatternVariable(psi: CjPatternVariable): CfirPatternVariable {
            return buildSourceDeclaration(CfirPatternVariableSymbol()) { symbol ->
                buildPatternVariable {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    pattern = convertCasePattern(psi.pattern)
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
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyBlockExpression?.let { convertBlock(it) }
            }

            return buildSourceDeclaration(CfirConstructorSymbol()) { symbol ->
                buildConstructor {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    returnTypeRef = buildImplicitTypeRef()
                    valueParameters.addAll(valueParams)
                    this.body = body
                    this.isPrimary = isPrimary
                }
            }
        }

        private fun convertTypeAlias(psi: CjTypeAlias): CfirTypeAlias {
            val name = psi.nameAsSafeName
            val expandedType = convertTypeRef(psi.getTypeReference())

            return buildSourceDeclaration(CfirTypeAliasSymbol()) { symbol ->
                buildTypeAlias {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = convertDeclarationStatus(psi)
                    typeParameters.addAll(convertTypeAliasTypeParameters(psi))
                    this.name = name
                    expandedTypeRef = expandedType
                }
            }
        }

        private fun convertEnumConstructor(psi: CjEnumConstructor): CfirEnumConstructor {
            val enumConstructorName = psi.name?.let { Name.identifier(it) } ?: Name.special("<anonymous-enum-constructor>")
            val valueTypeRefs = psi.typeReferences.map { convertTypeRef(it) }
            val enumConstructorTypeRef = when (valueTypeRefs.size) {
                0 -> buildImplicitTypeRef()
                1 -> valueTypeRefs.first()
                else -> buildTupleTypeRef { elementTypeRefs.addAll(valueTypeRefs) }
            }
            return buildSourceDeclaration(CfirEnumConstructorSymbol()) { symbol ->
                buildEnumConstructor {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = enumConstructorTypeRef
                    name = enumConstructorName
                }
            }
        }

        // ===== 鍙傛暟杞崲 =====

        fun convertValueParameter(psi: CjParameter): CfirValueParameter {
            return buildSourceDeclaration(CfirValueParameterSymbol()) { symbol ->
                buildValueParameter {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    name = psi.nameAsSafeName
                    defaultValue = psi.defaultValue?.let { convertExpression(it) }
                }
            }
        }

        private fun convertTypeParameter(psi: CjTypeParameter): CfirTypeParameter {
            val name = Name.identifier(psi.name ?: "<error>")
            val bounds = psi.extendsBound?.let { listOf(convertTypeRef(it)) } ?: emptyList()

            return buildSourceDeclaration(CfirTypeParameterSymbol()) { symbol ->
                buildTypeParameter {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
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
            is CjCallExpression -> convertCall(psi)
            is CjDotQualifiedExpression -> convertDotQualified(psi)
            is CjSafeQualifiedExpression -> convertDotQualified(psi)
            is CjNameReferenceExpression -> convertNameReference(psi)
            is CjIfExpression -> convertIf(psi)
            is CjMatchExpression -> convertMatch(psi)
            is CjForExpression -> convertFor(psi)
            is CjWhileExpression -> convertWhile(psi)
            is CjDoWhileExpression -> convertDoWhile(psi)
            is CjReturnExpression -> convertReturn(psi)
            is CjBreakExpression -> buildJumpExpression { kind = CfirJumpKind.BREAK }
            is CjContinueExpression -> buildJumpExpression { kind = CfirJumpKind.CONTINUE }
            is CjThrowExpression -> convertThrow(psi)
            is CjTryExpression -> convertTry(psi)
            is CjLambdaExpression -> convertLambda(psi)
            is CjParenthesizedExpression -> psi.expression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Empty parenthesized expression")
            is CjArrayAccessExpression -> convertSubscript(psi)
            is CjCollectionLiteralExpression -> convertArrayLiteral(psi)
            is CjTupleExpression -> convertTupleLiteral(psi)
            is CjIsExpression -> convertTypeCheck(psi)
            is CjThisExpression -> buildQualifiedAccess {
                calleeReference = buildNamedReference(Name.special("<this>"))
            }
            is CjSuperExpression -> buildQualifiedAccess {
                calleeReference = buildNamedReference(Name.special("<super>"))
            }
            else -> buildErrorExpression(psi.toSourceElement(), "Unsupported expression: ${psi.javaClass.simpleName}")
        }

        fun convertBlock(psi: CjBlockExpression): CfirBlock {
            if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                return buildBlock { }
            }
            val statements = psi.statements.map { stmt ->
                when (stmt) {
                    is CjProperty -> convertLocalVariable(stmt)
                    is CjNamedFunction -> convertFunction(stmt)
                    is CjDeclaration -> convertDeclaration(stmt)
                    else -> convertExpression(stmt)
                }
            }
            return buildBlock {
                this.statements.addAll(statements)
            }
        }

        private fun convertLocalVariable(psi: CjProperty): CfirVariable {
            return buildSourceDeclaration(CfirVariableSymbol()) { symbol ->
                buildVariable {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = convertTypeRef(psi.typeReference)
                    name = psi.nameAsSafeName
                    initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                        null
                    } else {
                        psi.initializer?.let { convertExpression(it) }
                    }
                    isVar = psi.isVar
                }
            }
        }

        // ---- Literal ----

        private fun convertLiteral(psi: CjConstantExpression): CfirLiteralExpression {
            val text = psi.text
            val elementType = psi.node.elementType
            val (kind, value) = when (elementType) {
                CjTokens.INTEGER_LITERAL -> CfirLiteralKind.INT to text
                CjTokens.FLOAT_LITERAL -> CfirLiteralKind.FLOAT to text
                CjTokens.RUNE_LITERAL -> CfirLiteralKind.RUNE to text
                CjTokens.TRUE_KEYWORD -> CfirLiteralKind.BOOLEAN to true
                CjTokens.FALSE_KEYWORD -> CfirLiteralKind.BOOLEAN to false
                CjTokens.UNIT_LITERAL -> CfirLiteralKind.UNIT to null
                else -> CfirLiteralKind.STRING to text
            }
            return buildLiteralExpression {
                this.kind = kind
                this.value = value
            }
        }

        private fun convertStringTemplate(psi: CjStringTemplateExpression): CfirExpression {
            if (!psi.hasInterpolation()) {
                return buildLiteralExpression {
                    kind = CfirLiteralKind.STRING
                    value = psi.stringContent
                }
            }
            val parts = psi.entries.mapNotNull { entry ->
                when (entry) {
                    is CjStringTemplateEntryWithExpression ->
                        entry.expression?.let { convertExpression(it) }
                    else -> buildLiteralExpression {
                        kind = CfirLiteralKind.STRING
                        value = entry.text
                    }
                }
            }
            return buildStringInterpolation {
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
                        lValue = left
                        rValue = right
                    }
                }
                val opName = opToken.toCompoundAssignName()?.asString() ?: "<error>"
                return buildAssignment {
                    lValue = left
                    rValue = buildFunctionCall {
                        calleeReference = buildNamedReference(Name.identifier(opName))
                        explicitReceiver = left
                        arguments.add(right)
                    }
                }
            }

            // 閫昏緫/绌哄悎/绠￠亾
            opToken.toBinaryOpKind()?.let { kind ->
                return buildBinaryOp {
                    this.kind = kind
                    this.left = left
                    this.right = right
                }
            }

            // 姣旇緝
            opToken.toComparisonOp()?.let { op ->
                return buildComparisonExpression {
                    operation = op
                    this.left = left
                    this.right = right
                }
            }

            // 鍙噸杞借繍绠楃 鈫?鍑芥暟璋冪敤
            val operatorName = opToken.toBinaryName() ?: Name.identifier("<op:$opToken>")
            return buildFunctionCall {
                calleeReference = buildNamedReference(operatorName)
                explicitReceiver = left
                arguments.add(right)
            }
        }

        private fun convertRange(psi: CjRangeExpression): CfirRangeExpression {
            val start = psi.left?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing range start")
            val end = psi.right?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing range end")
            return buildRangeExpression {
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
                calleeReference = buildNamedReference(opName)
                explicitReceiver = base
            }
        }

        private fun convertPostfix(psi: CjPostfixExpression): CfirExpression {
            val base = psi.baseExpression?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing postfix operand")
            val opName = psi.operationToken.toPostfixUnaryName() ?: Name.identifier("<postfix>")
            return buildFunctionCall {
                calleeReference = buildNamedReference(opName)
                explicitReceiver = base
            }
        }

        // ---- Call & Access ----

        private fun convertCall(psi: CjCallExpression): CfirExpression {
            if (psi is CjSpawnExpression) {
                val lambda = psi.lambdaExpression
                val body = lambda?.bodyExpression?.let { convertBlock(it) } ?: buildBlock { }
                return buildSpawnExpression { this.body = body }
            }

            val callee = psi.calleeExpression
            val arguments = psi.valueArguments.mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e) } }
            val typeArgs = psi.typeArguments.map { convertTypeRef(it.typeReference) }
            val lambdaArgs = psi.lambdaArguments.mapNotNull { it.getLambdaExpression()?.let { l -> convertLambda(l) } }
            val allArgs = (arguments + lambdaArgs).toMutableList()

            val (receiver, reference) = resolveCalleeReference(callee)

            return buildFunctionCall {
                calleeReference = reference
                explicitReceiver = receiver
                this.arguments.addAll(allArgs)
                typeArguments.addAll(typeArgs)
            }
        }

        private fun resolveCalleeReference(callee: CjExpression?): Pair<CfirExpression?, CfirNamedReference> {
            return when (callee) {
                is CjNameReferenceExpression -> null to buildNamedReference(callee.referencedNameAsName)
                is CjDotQualifiedExpression -> {
                    val recv = convertExpression(callee.receiverExpression)
                    val selector = callee.selectorExpression
                    val ref = if (selector is CjSimpleNameExpression) {
                        buildNamedReference(selector.referencedNameAsName)
                    } else {
                        buildNamedReference(Name.identifier("<error>"))
                    }
                    recv to ref
                }
                else -> null to buildNamedReference(Name.identifier(callee?.text ?: "<error>"))
            }
        }

        private fun convertDotQualified(psi: CjQualifiedExpression): CfirExpression {
            val receiver = convertExpression(psi.receiverExpression)
            val selector = psi.selectorExpression
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing selector")

            if (selector is CjCallExpression) {
                val arguments = selector.valueArguments.mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e) } }
                val typeArgs = selector.typeArguments.map { convertTypeRef(it.typeReference) }
                val callee = selector.calleeExpression
                val ref = if (callee is CjSimpleNameExpression) {
                    buildNamedReference(callee.referencedNameAsName)
                } else {
                    buildNamedReference(Name.identifier(callee?.text ?: "<error>"))
                }
                val lambdaArgs = selector.lambdaArguments.mapNotNull { it.getLambdaExpression()?.let { l -> convertLambda(l) } }

                return buildFunctionCall {
                    calleeReference = ref
                    explicitReceiver = receiver
                    this.arguments.addAll(arguments + lambdaArgs)
                    typeArguments.addAll(typeArgs)
                }
            }

            if (selector is CjSimpleNameExpression) {
                return buildPropertyAccess {
                    calleeReference = buildNamedReference(selector.referencedNameAsName)
                    explicitReceiver = receiver
                }
            }

            return buildErrorExpression(psi.toSourceElement(), "Unsupported selector: ${selector.javaClass.simpleName}")
        }

        private fun convertNameReference(psi: CjNameReferenceExpression): CfirQualifiedAccess {
            return buildQualifiedAccess {
                calleeReference = buildNamedReference(psi.referencedNameAsName)
            }
        }

        // ---- Control Flow ----

        private fun convertIf(psi: CjIfExpression): CfirIfExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing if condition")
            val thenBranch = psi.then?.let { toBlock(it) } ?: buildBlock { }
            val elseBranch = psi.`else`?.let { convertExpression(it) }

            return buildIfExpression {
                this.condition = condition
                this.thenBranch = thenBranch
                this.elseBranch = elseBranch
            }
        }

        private fun convertMatch(psi: CjMatchExpression): CfirMatchExpression {
            val subject = psi.subjectExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing match subject")
            val branches = psi.entries.map { entry ->
                val pattern = if (entry.isElse) {
                    buildWildcardPattern()
                } else {
                    val conditions = entry.conditions
                    if (conditions.isEmpty()) {
                        buildWildcardPattern()
                    } else {
                        val expr = conditions.first().children.filterIsInstance<CjExpression>().firstOrNull()
                        if (expr != null) buildConstPattern { expression = convertExpression(expr) }
                        else buildWildcardPattern()
                    }
                }
                val guard = entry.patternGuard?.children?.filterIsInstance<CjExpression>()?.firstOrNull()?.let { convertExpression(it) }
                val body = entry.expression?.let { convertBlock(it) }
                    ?: entry.body?.let { convertBlock(it) }
                    ?: buildBlock { }

                buildMatchBranch {
                    this.pattern = pattern
                    this.guard = guard
                    this.body = body
                }
            }

            return buildMatchExpression {
                this.subject = subject
                this.branches.addAll(branches)
            }
        }

        // ---- Loops ----

        private fun convertFor(psi: CjForExpression): CfirForInExpression {
            val loopParam = psi.loopParameter
            val variable = buildSourceDeclaration(CfirVariableSymbol()) { symbol ->
                buildVariable {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = if (loopParam != null) convertTypeRef(loopParam.typeReference) else buildImplicitTypeRef()
                    name = loopParam?.nameAsSafeName ?: Name.special("<anonymous>")
                    isVar = false
                }
            }
            val iterable = psi.loopRange?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing for-in iterable")
            val body = psi.body?.let { toBlock(it) } ?: buildBlock { }

            return buildForInExpression {
                this.condition = buildLiteralExpression {
                    kind = CfirLiteralKind.BOOLEAN
                    value = true
                }
                this.isDoWhile = false
                this.variable = variable
                this.iterable = iterable
                this.body = body
            }
        }

        private fun convertWhile(psi: CjWhileExpression): CfirLoopExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing while condition")
            return buildLoopExpression {
                this.condition = condition
                this.body = psi.body?.let { toBlock(it) } ?: buildBlock { }
                isDoWhile = false
            }
        }

        private fun convertDoWhile(psi: CjDoWhileExpression): CfirLoopExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing do-while condition")
            return buildLoopExpression {
                this.condition = condition
                this.body = psi.body?.let { toBlock(it) } ?: buildBlock { }
                isDoWhile = true
            }
        }

        // ---- Jump & Exception ----

        private fun convertReturn(psi: CjReturnExpression): CfirReturnExpression {
            return buildReturnExpression {
                result = psi.returnedExpression?.let { convertExpression(it) }
            }
        }

        private fun convertThrow(psi: CjThrowExpression): CfirThrowExpression {
            val exception = psi.thrownExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing thrown expression")
            return buildThrowExpression {
                this.exception = exception
            }
        }

        private fun convertTry(psi: CjTryExpression): CfirTryExpression {
            val tryBlock = convertBlock(psi.tryBlock)
            val catches = psi.catchClauses.map { clause ->
                val catchParam = clause.catchParameter
                val parameter = buildSourceDeclaration(CfirValueParameterSymbol()) { symbol ->
                    buildValueParameter {
                        this.symbol = symbol
                        origin = CfirDeclarationOrigin.Source
                        moduleData = baseModuleData
                        resolvePhase = CfirResolvePhase.RAW_CFIR
                        attributes = CfirDeclarationAttributes.EMPTY
                        status = CfirDeclarationStatusImpl.DEFAULT
                        returnTypeRef = if (catchParam != null) convertTypeRef(catchParam.typeReference) else buildImplicitTypeRef()
                        name = catchParam?.name?.let { Name.identifier(it) } ?: Name.special("<error>")
                    }
                }
                val body = clause.catchBody?.let { if (it is CjBlockExpression) convertBlock(it) else buildBlock { } } ?: buildBlock { }
                buildCatch {
                    this.parameter = parameter
                    this.body = body
                }
            }
            val finallyBlock = psi.finallyBlock?.let { section ->
                val expr = section.finalExpression
                if (expr is CjBlockExpression) convertBlock(expr) else null
            }

            return buildTryExpression {
                this.tryBlock = tryBlock
                this.catches.addAll(catches)
                this.finallyBlock = finallyBlock
            }
        }

        // ---- Lambda ----

        private fun convertLambda(psi: CjLambdaExpression): CfirLambdaExpression {
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyExpression?.let { convertBlock(it) }
            }

            val anonymousFunction = buildSourceDeclaration(CfirFunctionSymbol()) { symbol ->
                buildFunction {
                    this.symbol = symbol
                    origin = CfirDeclarationOrigin.Source
                    moduleData = baseModuleData
                    resolvePhase = CfirResolvePhase.RAW_CFIR
                    attributes = CfirDeclarationAttributes.EMPTY
                    status = CfirDeclarationStatusImpl.DEFAULT
                    returnTypeRef = buildImplicitTypeRef()
                    name = Name.special("<anonymous>")
                    valueParameters.addAll(valueParams)
                    this.body = body
                    isMut = false
                }
            }
            return buildLambdaExpression {
                this.anonymousFunction = anonymousFunction
            }
        }

        // ---- Misc ----

        private fun convertSubscript(psi: CjArrayAccessExpression): CfirSubscriptExpression {
            val receiver = psi.arrayExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing subscript receiver")
            return buildSubscriptExpression {
                this.receiver = receiver
                indices.addAll(psi.indexExpressions.map { convertExpression(it) })
            }
        }

        private fun convertArrayLiteral(psi: CjCollectionLiteralExpression): CfirArrayLiteral {
            return buildArrayLiteral {
                elements.addAll(psi.innerExpressions.map { convertExpression(it) })
            }
        }

        private fun convertTupleLiteral(psi: CjTupleExpression): CfirTupleLiteral {
            return buildTupleLiteral {
                elements.addAll(psi.expressions.map { convertExpression(it) })
            }
        }

        private fun convertTypeCheck(psi: CjIsExpression): CfirTypeOperator {
            val argument = psi.leftHandSide?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing is-check operand")
            return buildTypeOperator {
                operation = CfirTypeOperationKind.IS
                this.argument = argument
                typeRef = convertTypeRef(psi.typeReference)
            }
        }

        private fun convertCasePattern(pattern: CjCasePatternElement?): CfirPattern {
            return when (pattern) {
                is CjBindingPattern -> buildBindingPattern {
                    name = pattern.nameAsSafeName
                }
                is CjTypePattern -> buildTypePattern {
                    typeRef = convertTypeRef(pattern.typeReference)
                    bindingName = pattern.nameAsName
                }
                is CjTuplePattern -> buildTuplePattern {
                    elements.addAll(pattern.patterns.map { convertCasePattern(it) })
                }
                is CjEnumPattern -> buildEnumPattern {
                    constructorReference = buildNamedReference(Name.special(pattern.expression?.text ?: "<enum-pattern>"))
                    arguments.addAll(pattern.patterns.map { convertCasePattern(it) })
                }
                is CjConstantPattern -> buildConstPattern {
                    expression = pattern.expression?.let { convertExpression(it) }
                        ?: buildErrorExpression(pattern.toSourceElement(), "Missing constant pattern expression")
                }
                is CjWildcardPattern -> buildWildcardPattern()
                else -> buildWildcardPattern()
            }
        }

        // ===== 杈呭姪鏂规硶 =====

        private fun toBlock(psi: CjExpression): CfirBlock {
            if (psi is CjBlockExpression) return convertBlock(psi)
            return buildBlock {
                statements.add(convertExpression(psi))
            }
        }

        private fun convertTypeRef(psi: CjTypeReference?): CfirTypeRef {
            return psi.toFirOrImplicitTypeRef { it.toSourceElement() }
        }

        private fun convertTypeParameters(psi: CjClassLikeDeclaration): List<CfirTypeParameter> {
            return (psi as? CjTypeParameterListOwner)?.typeParameters?.map { convertTypeParameter(it) } ?: emptyList()
        }

        private fun convertTypeParameters(psi: CjExtend): List<CfirTypeParameter> {
            return (psi as? CjTypeParameterListOwner)?.typeParameters?.map { convertTypeParameter(it) } ?: emptyList()
        }

        private fun convertTypeAliasTypeParameters(psi: CjTypeAlias): List<CfirTypeParameter> {
            return (psi as? CjTypeParameterListOwner)?.typeParameters?.map { convertTypeParameter(it) } ?: emptyList()
        }

        private fun convertFunctionTypeParameters(psi: CjNamedFunction): List<CfirTypeParameter> {
            return psi.typeParameters.map { convertTypeParameter(it) }
        }

        private fun convertSuperTypeRefs(psi: CjClassLikeDeclaration): List<CfirTypeRef> {
            val typeStatement = psi as? CjTypeStatement ?: return emptyList()
            return typeStatement.superTypeListEntries.map { convertTypeRef(it.typeReference) }
        }

        private fun convertClassMembers(psi: CjClassLikeDeclaration): List<CfirDeclaration> {
            val typeStatement = psi as? CjTypeStatement ?: return emptyList()
            return typeStatement.body?.declarations?.map { convertDeclaration(it) } ?: emptyList()
        }

        private fun convertDeclarationStatus(psi: CjDeclaration): CfirDeclarationStatus {
            val owner = psi as? CjModifierListOwner ?: return CfirDeclarationStatusImpl.DEFAULT
            return convertDeclarationStatus(owner)
        }

        private fun convertDeclarationStatus(owner: CjModifierListOwner): CfirDeclarationStatus {
            val modifiers = owner.modifierList ?: return CfirDeclarationStatusImpl.DEFAULT

            return buildDeclarationStatus(
                visibility = when {
                    modifiers.hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
                    modifiers.hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
                    modifiers.hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
                    modifiers.hasModifier(CjTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
                    else -> Visibilities.Public
                },
                isAbstract = modifiers.hasModifier(CjTokens.ABSTRACT_KEYWORD),
                isOpen = modifiers.hasModifier(CjTokens.OPEN_KEYWORD),
                isSealed = modifiers.hasModifier(CjTokens.SEALED_KEYWORD),
                isStatic = modifiers.hasModifier(CjTokens.STATIC_KEYWORD),
                isMut = modifiers.hasModifier(CjTokens.MUT_KEYWORD),
                isOverride = modifiers.hasModifier(CjTokens.OVERRIDE_KEYWORD),
                isOperator = modifiers.hasModifier(CjTokens.OPERATOR_KEYWORD),
                isUnsafe = modifiers.hasModifier(CjTokens.UNSAFE_KEYWORD),
                isForeign = modifiers.hasModifier(CjTokens.FOREIGN_KEYWORD),
            )
        }
    }

    // ===== 鏂囦欢绾ф瀯寤鸿緟鍔?=====

    private fun buildPackageDirective(psi: CjPackageDirective?): CfirPackageDirective {
        val fqName = psi?.fqName ?: FqName.ROOT
        return buildPackageDirective {
            packageFqName = fqName
        }
    }

    private fun buildImports(file: CjFile): List<CfirImport> {
        val importDirectives = file.importDirectives
        return importDirectives.flatMap { directive ->
            directive.importItems.mapNotNull { item ->
                val fqName = item.importedFqName ?: return@mapNotNull null
                buildImport {
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

    companion object {}
}



