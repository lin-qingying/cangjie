package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.CfirCodeFragmentBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirAnonymousFunctionBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirExtendBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirFieldVariableBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirFileBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirInterfaceBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirNamedFunctionBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirStructBuilder
import org.cangnova.cangjie.cfir.declarations.builder.CfirTypeAliasBuilder
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildExtend
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildInterface
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildStruct
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperationKind
import org.cangnova.cangjie.cfir.expressions.InaccessibleReceiverKind
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBinaryOp
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildComparisonExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildIfExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildRangeExpression
import org.cangnova.cangjie.cfir.expressions.buildInaccessibleReceiverExpression
import org.cangnova.cangjie.cfir.expressions.buildSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeConversion
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeOperator
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Cfir2ChirCoverageTest {
    @Test
    fun `lowers value if expression into CHIR phi`() {
        val condition = buildComparisonExpression {
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
            operation = CfirComparisonOp.LT
            left = intLiteral(1)
            right = intLiteral(2)
        }
        val function = functionReturningExpression(
            name = "choose",
            returnType = ConePrimitiveType.INT32,
            expression = buildIfExpression {
                coneTypeOrNull = ConePrimitiveType.INT32
                this.condition = condition
                thenBranch = blockOf(ConePrimitiveType.INT32, intLiteral(10))
                elseBranch = intLiteral(20)
            },
        )

        val chirFunction = convertSingleFunction(function)

        assertTrue(chirFunction.blocks.any { it.name == "then" })
        assertTrue(chirFunction.blocks.any { it.name == "else" })
        assertTrue(chirFunction.otherOperations().contains(Cfir2ChirOperation.PHI.canonicalName))
    }

    @Test
    fun `rejects non-unit if branch that falls through without value`() {
        val condition = buildComparisonExpression {
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
            operation = CfirComparisonOp.LT
            left = intLiteral(1)
            right = intLiteral(2)
        }
        val function = functionReturningExpression(
            name = "badIf",
            returnType = ConePrimitiveType.INT32,
            expression = buildIfExpression {
                coneTypeOrNull = ConePrimitiveType.INT32
                this.condition = condition
                thenBranch = blockOf(ConePrimitiveType.UNIT)
                elseBranch = intLiteral(20)
            },
        )

        assertThrows(Cfir2ChirConversionException::class.java) {
            convertSingleFunction(function)
        }
    }

    @Test
    fun `reuses already converted anonymous function without swallowing storage errors`() {
        val anonymous = buildAnonymousFunction {
            commonDeclaration()
            status = CfirDeclarationStatusImpl()
            returnTypeRef = typeRef(ConePrimitiveType.UNIT)
            body = blockOf(ConePrimitiveType.UNIT)
            symbol = CfirAnonymousFunctionSymbol()
            hasExplicitParameterList = false
            isLambda = true
            typeRef = typeRef(ConeFunctionType(emptyList(), ConePrimitiveType.UNIT, isClosureType = true))
        }
        val anonymousExpression = buildAnonymousFunctionExpression {
            anonymousFunction = anonymous
            isTrailingLambda = false
        }
        val function = functionWithBody(
            name = "useLambdaTwice",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = listOf(anonymousExpression, anonymousExpression),
        )

        val operations = convertSingleFunction(function).otherOperations()

        assertEquals(2, operations.count { it == Cfir2ChirOperation.CFIR_ANONYMOUS_FUNCTION.canonicalName })
    }

    @Test
    fun `preserves CFIR-only expression forms as cfir2chir operations`() {
        val function = functionWithBody(
            name = "sourceForms",
            returnType = ConePrimitiveType.UNIT,
            bodyType = ConePrimitiveType.UNIT,
            statements = listOf(
                buildArrayLiteral {
                    coneTypeOrNull = ConeVArrayType(ConePrimitiveType.INT32, size = 2)
                    elements += intLiteral(1)
                    elements += intLiteral(2)
                },
                buildTupleLiteral {
                    coneTypeOrNull = ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))
                    elements += intLiteral(1)
                    elements += boolLiteral(true)
                },
                buildRangeExpression {
                    coneTypeOrNull = ConeStructType(ConeClassLikeLookupTagImpl(classId("Range")))
                    start = intLiteral(1)
                    end = intLiteral(3)
                    isInclusive = true
                },
                buildTypeConversion {
                    coneTypeOrNull = ConePrimitiveType.INT64
                    argument = intLiteral(1)
                    targetTypeRef = typeRef(ConePrimitiveType.INT64)
                },
                buildTypeOperator {
                    coneTypeOrNull = ConePrimitiveType.BOOLEAN
                    operation = CfirTypeOperationKind.IS
                    argument = intLiteral(1)
                    typeRef = intTypeRef()
                },
                buildBinaryOp {
                    coneTypeOrNull = ConePrimitiveType.BOOLEAN
                    kind = CfirBinaryOpKind.AND
                    left = boolLiteral(true)
                    right = boolLiteral(false)
                },
                buildThisReceiverExpression {
                    coneTypeOrNull = ConeStructType(ConeClassLikeLookupTagImpl(classId("Self")))
                    calleeReference = buildThisReference {
                        boundSymbol = CfirStructSymbol(classId("Self"))
                        isImplicit = false
                    }
                },
                buildInaccessibleReceiverExpression {
                    coneTypeOrNull = ConeStructType(ConeClassLikeLookupTagImpl(classId("Self")))
                    calleeReference = buildThisReference {
                        boundSymbol = CfirStructSymbol(classId("Self"))
                        isImplicit = true
                    }
                    kind = InaccessibleReceiverKind.CLASS_HEADER
                },
                buildSmartCastExpression {
                    coneTypeOrNull = ConePrimitiveType.INT64
                    originalExpression = intLiteral(1)
                    smartcastType = typeRef(ConePrimitiveType.INT64)
                    upperTypesFromSmartCast = listOf(ConePrimitiveType.INT64)
                    lowerTypesFromSmartCast = listOf(ConePrimitiveType.INT32)
                    smartcastStability = CfirSmartcastStability.STABLE_VALUE
                },
            ),
        )

        val operations = convertSingleFunction(function).otherOperations().toSet()

        assertTrue(Cfir2ChirOperation.CFIR_ARRAY_LITERAL.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TUPLE_LITERAL.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_RANGE.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TYPE_CONVERSION.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_TYPE_OPERATOR.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_BINARY_OP.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_THIS_RECEIVER.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_INACCESSIBLE_RECEIVER.canonicalName in operations)
        assertTrue(Cfir2ChirOperation.CFIR_SMART_CAST.canonicalName in operations)
    }

    @Test
    fun `maps composite Cone types without depending on CFIR tree module from chir tree`() {
        val mapper = Cfir2ChirTypeMapper()

        assertEquals(
            ChirVArrayType(ChirResolvedTypeRef(ChirPrimitiveType.INT32), rank = 4),
            mapper.mapConeType(ConeVArrayType(ConePrimitiveType.INT32, size = 4)),
        )
        assertEquals(
            ChirTupleType(listOf(ChirResolvedTypeRef(ChirPrimitiveType.INT32), ChirResolvedTypeRef(ChirPrimitiveType.BOOL))),
            mapper.mapConeType(ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN))),
        )
        assertEquals(
            ChirPrimitiveType.INT64,
            mapper.mapConeType(ConeTypeAliasType(classId("AliasInt64"), expandedType = ConePrimitiveType.INT64)),
        )
        assertEquals(
            ChirNamedType("Any"),
            mapper.mapConeType(ConeAnyType),
        )
        assertThrows(Cfir2ChirConversionException::class.java) {
            mapper.mapConeType(ConePrimitiveType.IDEAL_INT)
        }
    }

    @Test
    fun `converts class-like declarations in cfir2chir module`() {
        val module = convertModule(
            buildStruct {
                commonClassLikeDeclaration("Point", CfirStructSymbol(classId("Point")))
                declarations += field("x", ConePrimitiveType.INT32, mutable = false)
            },
            buildInterface {
                commonClassLikeDeclaration("Drawable", CfirInterfaceSymbol(classId("Drawable")))
            },
            buildExtend {
                commonDeclaration()
                symbol = CfirExtendSymbol()
                status = CfirDeclarationStatusImpl()
                extendedTypeRef = typeRef(ConeStructType(ConeClassLikeLookupTagImpl(classId("Point"))))
                superTypeRefs += typeRef(ConeClassLikeType(ConeClassLikeLookupTagImpl(classId("Drawable")), isInterface = true))
            },
            buildTypeAlias {
                commonDeclaration()
                scopeProvider = UnusedScopeProvider
                symbol = CfirTypeAliasSymbol(classId("Meters"))
                status = CfirDeclarationStatusImpl()
                name = Name.identifier("Meters")
                expandedTypeRef = intTypeRef()
            },
            CfirPrimitiveTypeDeclaration(
                moduleData = TestModuleData,
                symbol = CfirPrimitiveTypeSymbol(classId("BuiltinInt32"), PrimitiveTypeKind.INT32),
                name = Name.identifier("BuiltinInt32"),
                kind = PrimitiveTypeKind.INT32,
                scopeProvider = UnusedScopeProvider,
            ),
            buildCodeFragment {
                commonDeclaration()
                symbol = CfirCodeFragmentSymbol()
                block = blockOf(ConePrimitiveType.INT32, intLiteral(42))
            },
        )

        val struct = module.declarations.filterIsInstance<ChirStructDeclaration>().single()
        val iface = module.declarations.filterIsInstance<ChirClassDeclaration>().single { it.name == "Drawable" }
        val extend = module.declarations.filterIsInstance<ChirExtendDeclaration>().single()
        val typeAlias = module.declarations.filterIsInstance<ChirTypeDeclaration>().single { it.name == "Meters" }
        val primitive = module.declarations.filterIsInstance<ChirTypeDeclaration>().single { it.name == "BuiltinInt32" }
        val codeFragment = module.declarations.filterIsInstance<ChirFunctionDeclaration>().single { it.name == "<code-fragment>" }

        assertEquals("Point", struct.name)
        assertEquals(listOf("x"), struct.fieldDeclarations.map { it.name })
        assertTrue(ChirStringAttribute("cfir.kind", "interface") in iface.attributes)
        assertEquals("sample/Point", extend.targetType.renderName)
        assertEquals(listOf("sample/Drawable"), extend.extendedTypes.map { it.renderName })
        assertEquals("Meters", typeAlias.name)
        assertTrue(typeAlias.attributes.any { it is ChirStringAttribute && it.key == "cfir.expandedType" && it.value == "int32" })
        assertTrue(primitive.attributes.any { it is ChirStringAttribute && it.key == "cfir.primitive.kind" && it.value == "INT32" })
        assertEquals("int32", codeFragment.returnType.renderName)
        assertTrue(ChirStringAttribute("cfir.kind", "codeFragment") in codeFragment.attributes)
    }

    private fun convertSingleFunction(function: CfirNamedFunction): ChirFunctionDeclaration =
        convertModule(function).declarations.filterIsInstance<ChirFunctionDeclaration>().single()

    private fun convertModule(vararg declarations: CfirDeclaration): ChirModule =
        DefaultCfir2ChirConverter().convert(listOf(fileWith(*declarations))).modules.single()

    private fun functionReturningExpression(
        name: String,
        returnType: ConePrimitiveType,
        expression: CfirExpression,
    ): CfirNamedFunction =
        functionWithBody(name, returnType, returnType, listOf(expression))

    private fun functionWithBody(
        name: String,
        returnType: ConePrimitiveType,
        bodyType: ConePrimitiveType,
        statements: List<CfirStatement>,
    ): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            commonNamedFunctionDeclaration(symbol)
            this.name = Name.identifier(name)
            returnTypeRef = typeRef(returnType)
            body = blockOf(bodyType, *statements.toTypedArray())
        }
    }

    private fun blockOf(type: ConePrimitiveType, vararg statements: CfirStatement) = buildBlock {
        coneTypeOrNull = type
        this.statements += statements
    }

    private fun field(name: String, type: ConePrimitiveType, mutable: Boolean): CfirFieldVariable {
        val variableName = Name.identifier(name)
        return buildFieldVariable {
            commonFieldVariableDeclaration(CfirFieldVariableSymbol(CallableId(PackageName, variableName)))
            this.name = variableName
            returnTypeRef = typeRef(type)
            initializer = null
            isVar = mutable
        }
    }

    private fun fileWith(vararg declarations: CfirDeclaration): CfirFile =
        buildFile {
            commonDeclaration()
            symbol = CfirFileSymbol()
            name = "coverage.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PackageName
                isMacroPackage = false
            }
            this.declarations += declarations
        }

    private fun CfirNamedFunctionBuilder.commonNamedFunctionDeclaration(symbol: CfirNamedFunctionSymbol) {
        commonDeclaration()
        isLocal = false
        dispatchReceiverType = null
        status = CfirDeclarationStatusImpl()
        this.symbol = symbol
        isMut = false
    }

    private fun CfirFieldVariableBuilder.commonFieldVariableDeclaration(symbol: CfirFieldVariableSymbol) {
        commonDeclaration()
        isLocal = false
        dispatchReceiverType = null
        status = CfirDeclarationStatusImpl()
        this.symbol = symbol
    }

    private fun CfirNamedFunctionBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun CfirFieldVariableBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun CfirFileBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun CfirExtendBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun CfirTypeAliasBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun CfirCodeFragmentBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun CfirAnonymousFunctionBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        dispatchReceiverType = null
    }

    private fun CfirStructBuilder.commonClassLikeDeclaration(
        name: String,
        symbol: CfirStructSymbol,
    ) {
        commonDeclaration()
        scopeProvider = UnusedScopeProvider
        status = CfirDeclarationStatusImpl()
        this.name = Name.identifier(name)
        this.symbol = symbol
    }

    private fun CfirInterfaceBuilder.commonClassLikeDeclaration(
        name: String,
        symbol: CfirInterfaceSymbol,
    ) {
        commonDeclaration()
        scopeProvider = UnusedScopeProvider
        status = CfirDeclarationStatusImpl()
        this.name = Name.identifier(name)
        this.symbol = symbol
    }

    private fun CfirStructBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun CfirInterfaceBuilder.commonDeclaration() {
        moduleData = TestModuleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
    }

    private fun intLiteral(value: Int) = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        this.value = value
        coneTypeOrNull = ConePrimitiveType.INT32
    }

    private fun boolLiteral(value: Boolean) = buildLiteralExpression {
        kind = CfirLiteralKind.BOOLEAN
        this.value = value
        coneTypeOrNull = ConePrimitiveType.BOOLEAN
    }

    private fun intTypeRef(): CfirTypeRef = typeRef(ConePrimitiveType.INT32)

    private fun typeRef(type: org.cangnova.cangjie.cfir.types.ConeCangJieType): CfirTypeRef =
        buildResolvedTypeRef {
            coneType = type
        }

    private fun classId(name: String): ClassId =
        ClassId(PackageName, Name.identifier(name))

    private fun ChirFunctionDeclaration.otherOperations(): List<String> =
        blocks.flatMap { block ->
            block.expressions.mapNotNull { expression ->
                (expression as? ChirOtherExpression)?.operation
            }
        }

    private object UnusedScopeProvider : CfirScopeProvider() {
        override fun getUseSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = error("scope provider must not be used by cfir2chir coverage tests")

        override fun getTypealiasConstructorScope(
            typeAlias: CfirTypeAlias,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirScope = error("scope provider must not be used by cfir2chir coverage tests")

        override fun getDeclarationSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = error("scope provider must not be used by cfir2chir coverage tests")
    }

    private object TestSession : CfirSession(Kind.Source)

    private object TestModuleData : CfirModuleData() {
        override val name: Name = Name.identifier("cfir2chir-coverage")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = targetPlatform.isCommon()
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "cfir2chir-coverage"
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }

    private companion object {
        val PackageName = FqName("sample")
    }
}
