package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import PackageFormat.ExprKind
import PackageFormat.ReferenceInfo
import PackageFormat.VarInfo
import PackageFormat.ConstValue
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.common.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.declarations.utils.hasSerializedInitializer
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirPrimitiveDeclarationFactory
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.*
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.test.*

/** 官方源导出 initializer 的树与常量事实分别校验；session 不注册任何 PSI symbol provider。 */
@OptIn(CfirSymbolProviderInternals::class, ExperimentalUnsignedTypes::class)
class CjoConstInitializerAstTest {
    @Test
    fun sdkConstantsPreservePowerOperatorsAndTypedOperands() = fixture { f ->
        for ((packageName, name) in listOf("std.core" to "NS_PER_US", "std.unittest" to "MAX_BATCH_SIZE")) {
            val context = f.context(packageName)
            val index = context.header.topLevelCallableNameToIndices.getValue(name).single()
            val root = assertIs<CfirPatternVariable>(context.createDeclDeserializer().deserializeDecl(index))
            val operation = assertIs<CfirFunctionCall>(root.initializer)
            assertEquals(OperatorNameConventions.EXPONENTIATION, assertIs<CfirNamedReference>(operation.calleeReference).name)
            assertEquals(CfirFunctionCallOrigin.Operator, operation.origin)
            assertIs<CfirLiteralExpression>(operation.explicitReceiver)
            assertIs<CfirLiteralExpression>(operation.argumentList.arguments.single())
            assertEquals(ConePrimitiveType.INT64, operation.explicitReceiver!!.coneTypeOrNull)
            assertEquals(ConePrimitiveType.UINT64, operation.argumentList.arguments.single().coneTypeOrNull)
            val wire = context.pkg.allDecls(index)!!.info(VarInfo()) as VarInfo
            assertEquals(ConstValue.NONE, wire.valueType)
            assertEquals(ExprKind.BinaryExpr, context.pkg.allExprs(wire.initializer.toInt() - 1)!!.kind)
            assertEquals(root.returnTypeRef.coneType, operation.coneTypeOrNull)
            val leaf = root.pattern.bindingVariables().single()
            assertTrue(leaf.status.isConst)
            assertTrue(leaf.hasSerializedInitializer)
            assertSame(root.initializer, leaf.initializer)
            assertFalse(leaf.evaluatedInitializer is CfirEvaluatorResult.Evaluated)
        }
    }

    @Test
    fun sdkStringEmptyKeepsTheActualConstructorCallAndRecord() = fixture { f ->
        val context = f.context("std.core")
        val index = context.header.fullIdReferenceKeyToIndex.getValue("_CNat6String5emptyE")
        val reader = context.createDeclDeserializer()
        val field = assertIs<CfirFieldVariable>(reader.deserializeDecl(index))
        val call = assertIs<CfirFunctionCall>(field.initializer)
        val reference = assertIs<CfirResolvedNamedReference>(call.calleeReference)
        val wire = context.pkg.allDecls(index)!!.info(VarInfo()) as VarInfo
        val expr = context.pkg.allExprs(wire.initializer.toInt() - 1)!!
        assertEquals(ExprKind.CallExpr, expr.kind)
        val callee = context.pkg.allExprs(expr.operands(0).toInt() - 1)!!
        val target = (callee.info(ReferenceInfo()) as ReferenceInfo).target!!
        assertSame(reader.deserializeSymbol(target.index.toInt() - 1), reference.resolvedSymbol)
        val constructor = assertIs<CfirConstructor>(reference.resolvedSymbol.cfir)
        assertTrue(constructor.status.isConst)
        assertNull(constructor.body, "读取 call 必须保留 initializer，而不展开 callee body")
        assertTrue(call.argumentList.arguments.isEmpty())
        assertEquals(field.returnTypeRef.coneType, call.coneTypeOrNull)
        assertEquals(ConstValue.NONE, wire.valueType)
        assertFalse(field.evaluatedInitializer is CfirEvaluatorResult.Evaluated)
    }

    @Test
    fun sdkBlockAndEmptyArrayPreserveTheirDistinctWireShapes() = fixture { f ->
        val context = f.context("std.core")
        fun root(name: String): CfirPatternVariable = assertIs(context.createDeclDeserializer().deserializeDecl(
            context.header.topLevelCallableNameToIndices.getValue(name).single()))
        val blockRoot = root("IS_SIMD_SUPPORTED")
        val block = when (val expression = blockRoot.initializer) {
            is CfirBlock -> expression
            is CfirUnsafeExpression -> assertIs<CfirBlock>(expression.body)
            else -> error("官方 Block initializer 必须保留真实 block")
        }
        assertIs<CfirFunctionCall>(block.statements.single())
        val arrayRoot = root("EMPTY_UINT8_RAW_ARRAY")
        val construction = assertIs<CfirFunctionCall>(arrayRoot.initializer)
        assertEquals(CfirFunctionCallOrigin.CompilerCoreIntrinsic, construction.origin)
        assertTrue(construction.argumentList.arguments.isEmpty())
        assertEquals(arrayRoot.returnTypeRef.coneType, construction.coneTypeOrNull)
        assertFalse(arrayRoot.initializer is CfirArrayLiteral)
    }

    @Test
    fun callableLookupDoesNotMaterializeUnrelatedClassifiersOrEnums() = fixture { f ->
        val context = f.context("wire.constexpr")
        assertTrue(f.provider.getTopLevelCallableSymbols(FqName("wire.constexpr"), Name.identifier("missing")).isEmpty())
        assertTrue(f.provider.getTopLevelCallableSymbols(FqName("wire.constexpr"), Name.identifier("Cell")).isEmpty())
        assertTrue(context.declCache.isEmpty())
        val symbol = f.provider.getTopLevelCallableSymbols(FqName("wire.constexpr"), Name.identifier("scale")).single()
        val scaleIndex = context.header.topLevelCallableNameToIndices.getValue("scale").single()
        assertEquals(setOf(scaleIndex), context.declCache.keys)
        assertSame(symbol, checkNotNull(context.headerRecordOrNull(scaleIndex)).wireSymbol)
        val ready = f.provider.getTopLevelCallableSymbols(FqName("wire.constexpr"), Name.identifier("Ready")).single()
        assertIs<CfirEnumConstructorSymbol>(ready)
        val constructorIndex = context.header.promotedEnumConstructorNameToIndices.getValue("Ready").single()
        assertSame(ready, checkNotNull(context.headerRecordOrNull(constructorIndex)).wireSymbol)
        assertFalse(context.declCache.containsKey(context.header.topLevelClassifierNameToIndices.getValue("Cell").single()))
        assertFalse(context.declCache.containsKey(context.header.topLevelClassifierNameToIndices.getValue("Arena").single()))
        assertTrue(Name.identifier("Ready") in f.provider.symbolNamesProvider
            .getTopLevelCallableNamesInPackage(FqName("wire.constexpr"))!!)
    }

    @Test
    fun constCallsMembersAndBranchesRetainSourceAstAndWireLeafIdentity() = fixture { f ->
        val context = f.context("wire.constexpr")
        fun root(name: String): CfirPatternVariable = assertIs(context.createDeclDeserializer().deserializeDecl(
            context.header.topLevelCallableNameToIndices.getValue(name).single()))
        val built = root("built")
        val constructorCall = assertIs<CfirFunctionCall>(built.initializer)
        assertIs<CfirConstructor>(assertIs<CfirResolvedNamedReference>(constructorCall.calleeReference).resolvedSymbol.cfir)
        val nestedCall = assertIs<CfirFunctionCall>(constructorCall.argumentList.arguments.single())
        val function = assertIs<CfirNamedFunction>(assertIs<CfirResolvedNamedReference>(nestedCall.calleeReference).resolvedSymbol.cfir)
        assertEquals("add", function.name.asString())
        assertNull(function.body)
        assertEquals(2, nestedCall.argumentList.arguments.size)
        val scale = root("scale").pattern.bindingVariables().single().symbol
        assertSame(scale, assertIs<CfirResolvedNamedReference>(
            assertIs<CfirNamedAccessExpression>(nestedCall.argumentList.arguments.first()).calleeReference).resolvedSymbol)
        val member = assertIs<CfirNamedAccessExpression>(root("memberValue").initializer)
        assertIs<CfirFieldVariable>(assertIs<CfirResolvedNamedReference>(member.calleeReference).resolvedSymbol.cfir)
        assertSame(built.pattern.bindingVariables().single().symbol, assertIs<CfirResolvedNamedReference>(
            assertIs<CfirNamedAccessExpression>(member.explicitReceiver).calleeReference).resolvedSymbol)
        assertIs<CfirComparisonExpression>(root("comparison").initializer)
        val conditional = assertIs<CfirIfExpression>(root("chosen").initializer)
        assertIs<CfirFunctionCall>(conditional.thenBranch.statements.single())
        assertIs<CfirLiteralExpression>(assertIs<CfirBlock>(conditional.elseBranch).statements.single())
        assertIs<CfirTupleLiteral>(root("tupleValue").initializer)
        assertIs<CfirArrayLiteral>(root("zeroArray").initializer)
        assertEquals("-7", assertIs<CfirLiteralExpression>(root("negative").initializer).value)
        val opposite = assertIs<CfirFunctionCall>(root("opposite").initializer)
        assertEquals(OperatorNameConventions.UNARY_MINUS, assertIs<CfirNamedReference>(opposite.calleeReference).name)
        assertTrue(opposite.argumentList.arguments.isEmpty())
        val flipped = assertIs<CfirFunctionCall>(root("flipped").initializer)
        assertEquals(OperatorNameConventions.NOT, assertIs<CfirNamedReference>(flipped.calleeReference).name)
        assertIs<CfirFunctionCall>(root("shifted").initializer)
        val selected = assertIs<CfirFunctionCall>(root("selected").initializer)
        assertIs<CfirEnumConstructorSymbol>(assertIs<CfirResolvedNamedReference>(selected.calleeReference).resolvedSymbol)
        assertSame(built.initializer, built.pattern.bindingVariables().single().initializer)
    }

    @Test
    fun genericNonConstFieldKeepsItsLambdaAndActualParameterOwner() = fixture { f ->
        val context = f.context("wire.constexpr")
        val index = context.header.topLevelClassifierNameToIndices.getValue("Arena").single()
        val arena = assertIs<CfirClass>(context.createDeclDeserializer().deserializeDecl(index))
        val counts = arena.declarations.filterIsInstance<CfirFieldVariable>().single()
        assertFalse(counts.status.isConst)
        assertTrue(counts.hasSerializedInitializer)
        val construction = assertIs<CfirFunctionCall>(counts.initializer)
        val lambda = assertIs<CfirAnonymousFunctionExpression>(construction.argumentList.arguments.last()).anonymousFunction
        assertTrue(lambda.isLambda)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, lambda.resolvePhase)
        assertSame(lambda.symbol, lambda.valueParameters.single().containingDeclarationSymbol)
        val returned = assertIs<CfirReturnExpression>(lambda.body!!.statements.single())
        assertSame(lambda, returned.target.labeledElement)
        assertActualLambdaTypeConversion(context, returned.result)
    }

    @Test
    fun signatureViewKeepsFactsWithoutForcingFullSourceAst() = fixture { f ->
        val full = f.context("wire.constexpr")
        val signature = full.withMode(CjoDeserializationMode.SIGNATURE_ONLY)
        val index = full.header.topLevelCallableNameToIndices.getValue("built").single()
        val fullRoot = assertIs<CfirPatternVariable>(full.createDeclDeserializer().deserializeDecl(index))
        val signatureRoot = assertIs<CfirPatternVariable>(signature.createDeclDeserializer().deserializeDecl(index))
        assertIs<CfirFunctionCall>(fullRoot.initializer)
        assertNull(signatureRoot.initializer)
        assertTrue(signatureRoot.pattern.bindingVariables().single().hasSerializedInitializer)
        assertNotSame(fullRoot.pattern.bindingVariables().single().symbol, signatureRoot.pattern.bindingVariables().single().symbol)
        assertEquals(CjoDeserializationMode.SIGNATURE_ONLY, signature.mode)
    }

    @Test
    fun malformedWireRetainsFailureInsteadOfDroppingTheInitializer() = fixture { f ->
        val context = f.context("wire.constexpr")
        val index = context.header.topLevelCallableNameToIndices.getValue("built").single()
        val wire = context.pkg.allDecls(index)!!.info(VarInfo()) as VarInfo
        val call = context.pkg.allExprs(wire.initializer.toInt() - 1)!!
        assertTrue(call.mutateInfoType(PackageFormat.ExprInfo.LitConstInfo))
        val reader = context.createDeclDeserializer()
        val firstFailure = assertFailsWith<IllegalStateException> { reader.deserializeDecl(index) }
        val failed = assertIs<CjoDeclarationCacheEntry.Failed>(context.declarationEntries[index])
        assertSame(firstFailure, failed.failure.cause)
        assertEquals(context.header.fullPkgName, failed.failure.packageName)
        assertEquals(index, failed.failure.declarationIndex)
        assertEquals(context.mode, failed.failure.mode)
        assertNull(context.declCache[index])
        assertSame(failed.failure, assertFailsWith<CjoDeclarationDeserializationException> {
            reader.deserializeSymbol(index)
        })
        assertSame(failed.failure, assertFailsWith<CjoDeclarationDeserializationException> {
            reader.deserializeDecl(index)
        })
        assertSame(failed, context.declarationEntries[index])
        assertSame(firstFailure, failed.failure.cause)
        assertNull(context.declCache[index])
        val signatures = context.withMode(CjoDeserializationMode.SIGNATURE_ONLY)
        val signatureRoot = assertIs<CfirPatternVariable>(signatures.createDeclDeserializer().deserializeDecl(index))
        assertNull(signatureRoot.initializer)
        assertTrue(signatureRoot.pattern.bindingVariables().single().hasSerializedInitializer)
    }

    @Test
    fun coldLambdaParameterAndFieldFirstOrdersKeepOneCanonicalFunction() {
        for (fieldFirst in listOf(false, true)) fixture { f ->
            val context = f.context("wire.constexpr")
            val expressionIndex = lambdaIndex(context)
            val info = context.pkg.allExprs(expressionIndex)!!.info(PackageFormat.LambdaInfo()) as PackageFormat.LambdaInfo
            val parameterIndex = info.funcBody.paramLists(0)!!.params(0).toInt() - 1
            val fieldIndex = (0 until context.pkg.allDeclsLength).single {
                context.pkg.allDecls(it)!!.identifier == "counts"
            }
            val reader = context.createDeclDeserializer()
            if (fieldFirst) assertIs<CfirFieldVariable>(reader.deserializeDecl(fieldIndex))
            val parameter = assertIs<CfirValueParameter>(reader.deserializeSymbol(parameterIndex)!!.cfir)
            val field = assertIs<CfirFieldVariable>(reader.deserializeDecl(fieldIndex))
            val call = assertIs<CfirFunctionCall>(field.initializer)
            val function = assertIs<CfirAnonymousFunctionExpression>(call.argumentList.arguments.last()).anonymousFunction
            assertSame(function.symbol, parameter.containingDeclarationSymbol)
            assertSame(parameter, function.valueParameters.single())
            assertSame(parameter.symbol, checkNotNull(context.headerRecordOrNull(parameterIndex)).wireSymbol)
            val returned = assertIs<CfirReturnExpression>(function.body!!.statements.single())
            assertSame(function, returned.target.labeledElement)
            assertActualLambdaTypeConversion(context, returned.result)
            assertSame(function, assertIs<CjoAnonymousFunctionCacheEntry.Ready>(
                context.anonymousFunctionEntries[expressionIndex]).function)
        }
    }

    @Test
    fun signatureOnlyColdLambdaParameterNeverReadsMalformedBody() = fixture { f ->
        val context = f.context("wire.constexpr").withMode(CjoDeserializationMode.SIGNATURE_ONLY)
        val expressionIndex = lambdaIndex(context)
        val info = context.pkg.allExprs(expressionIndex)!!.info(PackageFormat.LambdaInfo()) as PackageFormat.LambdaInfo
        val parameterIndex = info.funcBody.paramLists(0)!!.params(0).toInt() - 1
        assertTrue(info.funcBody.mutateBody(UInt.MAX_VALUE))
        val reader = context.createDeclDeserializer()
        val parameter = assertIs<CfirValueParameter>(reader.deserializeSymbol(parameterIndex)!!.cfir)
        val function = assertIs<CfirAnonymousFunction>(parameter.containingDeclarationSymbol.cfir)
        assertNull(function.body)
        assertSame(parameter, function.valueParameters.single())
        val fieldIndex = (0 until context.pkg.allDeclsLength).single { context.pkg.allDecls(it)!!.identifier == "counts" }
        val field = assertIs<CfirFieldVariable>(reader.deserializeDecl(fieldIndex))
        assertNull(field.initializer)
        assertTrue(field.hasSerializedInitializer)
        assertIs<CjoAnonymousFunctionCacheEntry.Ready>(context.anonymousFunctionEntries[expressionIndex])
        assertFalse(context.withMode(CjoDeserializationMode.FULL).anonymousFunctionEntries.containsKey(expressionIndex))
    }

    @Test
    fun cyclicLambdaBodyFailsWithoutPublishingParameterOrFunction() = fixture { f ->
        val context = f.context("wire.constexpr")
        val expressionIndex = lambdaIndex(context)
        val info = context.pkg.allExprs(expressionIndex)!!.info(PackageFormat.LambdaInfo()) as PackageFormat.LambdaInfo
        val parameterIndex = info.funcBody.paramLists(0)!!.params(0).toInt() - 1
        val body = context.pkg.allExprs(info.funcBody.body.toInt() - 1)!!
        assertEquals(ExprKind.Block, body.kind)
        assertTrue(body.mutateOperands(0, (expressionIndex + 1).toUInt()))
        val reader = context.createDeclDeserializer()
        assertFailsWith<IllegalStateException> { reader.deserializeSymbol(parameterIndex) }
        val failed = assertIs<CjoDeclarationCacheEntry.Failed>(context.declarationEntries[parameterIndex])
        assertNull(context.declCache[parameterIndex])
        assertIs<CjoAnonymousFunctionCacheEntry.Failed>(context.anonymousFunctionEntries[expressionIndex])
        assertSame(failed.failure, assertFailsWith<CjoDeclarationDeserializationException> {
            reader.deserializeSymbol(parameterIndex)
        })
    }

    /** 官方 raw71 是 TypeConvExpr；验证完整 operand/type，不要求不存在的 callee FullId。 */
    private fun assertActualLambdaTypeConversion(context: CfirDeserializationContext, expression: CfirExpression) {
        val lambda = context.pkg.allExprs(lambdaIndex(context))!!.info(PackageFormat.LambdaInfo()) as PackageFormat.LambdaInfo
        val block = context.pkg.allExprs(lambda.funcBody.body.toInt() - 1)!!
        val returned = context.pkg.allExprs(block.operands(0).toInt() - 1)!!
        assertEquals(ExprKind.ReturnExpr, returned.kind)
        val wire = context.pkg.allExprs(returned.operands(0).toInt() - 1)!!
        assertEquals(ExprKind.TypeConvExpr, wire.kind)
        assertEquals(PackageFormat.ExprInfo.NONE, wire.infoType)
        assertEquals(1, wire.operandsLength)
        val operand = context.pkg.allExprs(wire.operands(0).toInt() - 1)!!
        assertEquals(ExprKind.LitConstExpr, operand.kind)
        val literal = operand.info(PackageFormat.LitConstInfo()) as PackageFormat.LitConstInfo
        val conversion = assertIs<CfirTypeConversion>(expression)
        val targetType = context.createTypeDeserializer().deserializeTypeFromField(wire.type)
        val operandType = context.createTypeDeserializer().deserializeTypeFromField(operand.type)
        assertSame(ConePrimitiveType.UINT8, targetType)
        assertSame(ConePrimitiveType.INT64, operandType)
        assertSame(targetType, conversion.coneTypeOrNull)
        assertSame(targetType, conversion.targetTypeRef.coneType)
        val argument = assertIs<CfirLiteralExpression>(conversion.argument)
        assertSame(operandType, argument.coneTypeOrNull)
        assertEquals(literal.strValue, argument.value)
        assertEquals("0", argument.value)
    }

    private fun lambdaIndex(context: CfirDeserializationContext): Int =
        (0 until context.pkg.allExprsLength).single { context.pkg.allExprs(it)!!.kind == ExprKind.LambdaExpr }

    @Test
    fun unaryPlusUsesItsUnaryOperatorIdentity() = fixture { f ->
        val context = f.context("wire.constexpr")
        val index = context.header.topLevelCallableNameToIndices.getValue("opposite").single()
        val variable = context.pkg.allDecls(index)!!.info(VarInfo()) as VarInfo
        val expression = context.pkg.allExprs(variable.initializer.toInt() - 1)!!
        assertEquals(ExprKind.UnaryExpr, expression.kind)
        val operator = expression.info(PackageFormat.UnaryInfo()) as PackageFormat.UnaryInfo
        assertTrue(operator.mutateOp(PackageFormat.OperatorKind.Add))
        val root = assertIs<CfirPatternVariable>(context.createDeclDeserializer().deserializeDecl(index))
        val unary = assertIs<CfirFunctionCall>(root.initializer)
        assertEquals(OperatorNameConventions.UNARY_PLUS, assertIs<CfirNamedReference>(unary.calleeReference).name)
        assertTrue(unary.argumentList.arguments.isEmpty())
        val scaleIndex = context.header.topLevelCallableNameToIndices.getValue("scale").single()
        assertSame(checkNotNull(context.headerRecordOrNull(scaleIndex)).wireSymbol, assertIs<CfirResolvedNamedReference>(
            assertIs<CfirNamedAccessExpression>(unary.explicitReceiver).calleeReference).resolvedSymbol)
    }

    @Test
    fun sdkSignatureDefaultParameterFactDoesNotFabricateAUnitLiteral() = fixture { f ->
        val context = f.context("std.core").withMode(CjoDeserializationMode.SIGNATURE_ONLY)
        val bit = org.cangnova.cangjie.metadata.model.Attribute.HAS_INITIAL.ordinal
        val parameterIndex = (0 until context.pkg.allDeclsLength).first {
            val declaration = context.pkg.allDecls(it)!!
            declaration.kind == PackageFormat.DeclKind.FuncParam && declaration.identifier == "flush" &&
                bit / 64 < declaration.attributesLength &&
                (declaration.attributes(bit / 64) and (1uL shl (bit % 64))) != 0uL
        }
        val parameter = assertIs<CfirValueParameter>(context.createDeclDeserializer().deserializeSymbol(parameterIndex)!!.cfir)
        assertEquals(ConePrimitiveType.BOOLEAN, parameter.returnTypeRef.coneType)
        val defaultFact = assertIs<org.cangnova.cangjie.cfir.expressions.impl.CfirExpressionStub>(parameter.defaultValue)
        assertEquals(parameter.returnTypeRef.coneType, defaultFact.coneTypeOrNull)
    }

    @Test
    fun primitiveCallLayoutUsesCanonicalBuiltinOwnerAndFormalConversion() = fixture { f ->
        val destinations = listOf(
            PackageFormat.TypeKind.Int8 to PrimitiveTypeKind.INT8,
            PackageFormat.TypeKind.UInt8 to PrimitiveTypeKind.UINT8,
            PackageFormat.TypeKind.Int64 to PrimitiveTypeKind.INT64,
            PackageFormat.TypeKind.UInt64 to PrimitiveTypeKind.UINT64,
            PackageFormat.TypeKind.Float16 to PrimitiveTypeKind.FLOAT16,
            PackageFormat.TypeKind.Float64 to PrimitiveTypeKind.FLOAT64,
            PackageFormat.TypeKind.Rune to PrimitiveTypeKind.RUNE,
        )
        val core = f.context("std.core")
        for ((wireKind, kind) in destinations) {
            // 合法 primitive conversion 的 schema call 形状；不声称这是 cjc 1.1.3 的最终脱糖输出。
            val context = primitiveCallContext(f, wireKind)
            val root = assertIs<CfirPatternVariable>(context.createDeclDeserializer().deserializeDecl(0))
            val conversion = assertIs<CfirTypeConversion>(root.initializer)
            assertEquals(kind, assertIs<ConePrimitiveType>(conversion.targetTypeRef.coneType).kind)
            assertSame(conversion.targetTypeRef.coneType, conversion.coneTypeOrNull)
            assertSame(ConePrimitiveType.INT64, conversion.argument.coneTypeOrNull)
            assertEquals("0", assertIs<CfirLiteralExpression>(conversion.argument).value)
            assertSame(root.initializer, root.pattern.bindingVariables().single().initializer)
            val primitive = context.lookupPrimitiveTypeSymbol(kind)
            assertSame(core.moduleData, primitive.cfir.moduleData)
            assertSame(primitive, CfirBuiltinSymbolProvider(core.moduleData.session, core.moduleData).getClassLikeSymbolByClassId(kind.classId))
            assertSame(primitive, CfirBuiltinSymbolProvider(core.moduleData.session, core.moduleData).getClassLikeSymbolByClassId(kind.classId))
            assertSame(primitive, core.lookupPrimitiveTypeSymbol(kind))
            assertSame(CfirPrimitiveDeclarationFactory.getInstance(core.moduleData.session, core.moduleData),
                CfirPrimitiveDeclarationFactory.getInstance(core.moduleData.session, core.moduleData))
        }
    }

    @Test
    fun primitiveCallRejectsMissingTypeAndOmittedValueInsteadOfInventingTargets() {
        for (missingType in listOf(false, true)) fixture { f ->
            val context = primitiveCallContext(f, PackageFormat.TypeKind.UInt8, defaultArgument = !missingType)
            if (missingType) assertTrue(context.pkg.allExprs(0)!!.mutateType(0u))
            val first = assertFailsWith<IllegalStateException> { context.createDeclDeserializer().deserializeDecl(0) }
            val failure = assertIs<CjoDeclarationCacheEntry.Failed>(context.declarationEntries[0])
            assertSame(first, failure.failure.cause)
            assertNull(context.declCache[0])
            assertSame(failure.failure, assertFailsWith<CjoDeclarationDeserializationException> {
                context.createDeclDeserializer().deserializeSymbol(0)
            })
        }
    }

    @Test
    fun primitiveQualifierUsesTheActualSdkModuleAcrossManagers() = fixture { f ->
        val sdkModule = Module()
        val sdkManager = CjoManager(
            CjoSearchPath.forRoots(emptyList(), listOf(f.sdk.toFile())),
            declarationMaterialization = f.manager.declarationMaterialization,
        )
        val sdkProvider = CfirDeserializedSymbolProvider(sdkModule.session, sdkManager, CfirCangJieScopeProvider(), sdkModule,
            moduleDataProvider = cjoFixtureModuleDataProvider(sdkModule, setOf(f.sdk)))
        val consumer = primitiveCallContext(f, PackageFormat.TypeKind.UInt8, packageProvider = sdkProvider)
        assertIs<CfirTypeConversion>(assertIs<CfirPatternVariable>(
            consumer.createDeclDeserializer().deserializeDecl(0)).initializer)
        val symbol = consumer.lookupPrimitiveTypeSymbol(PrimitiveTypeKind.UINT8)
        assertSame(sdkModule, symbol.cfir.moduleData)
        assertNotSame(f.module, symbol.cfir.moduleData)
        assertSame(symbol, CfirBuiltinSymbolProvider(sdkModule.session, sdkModule).getClassLikeSymbolByClassId(PrimitiveTypeKind.UINT8.classId))
        assertSame(symbol, sdkProvider.getOwnPackageContext("std.core", CjoDeserializationMode.FULL)!!
            .lookupPrimitiveTypeSymbol(PrimitiveTypeKind.UINT8))
    }

    @Test
    fun primitiveCallRejectsAConversionToBool() = fixture { f ->
        val context = primitiveCallContext(f, PackageFormat.TypeKind.Bool)
        assertFailsWith<IllegalStateException> { context.createDeclDeserializer().deserializeDecl(0) }
        assertIs<CjoDeclarationCacheEntry.Failed>(context.declarationEntries[0])
        assertNull(context.declCache[0])
    }

    /** 构造两个 type、真实 Expr kind/union 和全局 VarInfo 的只读协议片段；目标 std.core 使用现有 SDK。 */
    private fun primitiveCallContext(
        f: Fixture, targetKind: UShort, defaultArgument: Boolean = false,
        packageProvider: org.cangnova.cangjie.cfir.serialization.provider.CjoPackageContextProvider = f.provider,
    ): CfirDeserializationContext {
        val b = FlatBufferBuilder()
        val destination = PackageFormat.SemaTy.createSemaTy(b, targetKind, 0, PackageFormat.SemaTyInfo.NONE, 0)
        val argumentType = PackageFormat.SemaTy.createSemaTy(b, PackageFormat.TypeKind.Int64, 0, PackageFormat.SemaTyInfo.NONE, 0)
        val types = PackageFormat.Package.createAllTypesVector(b, intArrayOf(destination, argumentType))
        PackageFormat.Expr.startExpr(b)
        PackageFormat.Expr.addKind(b, ExprKind.PrimitiveTypeExpr)
        PackageFormat.Expr.addType(b, 1u)
        val callee = PackageFormat.Expr.endExpr(b)
        val literalInfo = PackageFormat.LitConstInfo.createLitConstInfo(
            b, b.createString("0"), PackageFormat.LitConstKind.Integer, 0u)
        PackageFormat.Expr.startExpr(b)
        PackageFormat.Expr.addKind(b, ExprKind.LitConstExpr)
        PackageFormat.Expr.addType(b, 2u)
        PackageFormat.Expr.addInfoType(b, PackageFormat.ExprInfo.LitConstInfo)
        PackageFormat.Expr.addInfo(b, literalInfo)
        val literal = PackageFormat.Expr.endExpr(b)
        val argumentOperands = PackageFormat.Expr.createOperandsVector(b,
            if (defaultArgument) uintArrayOf() else uintArrayOf(2u))
        val argumentInfo = PackageFormat.FuncArgInfo.createFuncArgInfo(b, false, defaultArgument)
        PackageFormat.Expr.startExpr(b)
        PackageFormat.Expr.addType(b, 2u)
        PackageFormat.Expr.addOperands(b, argumentOperands)
        PackageFormat.Expr.addInfoType(b, PackageFormat.ExprInfo.FuncArgInfo)
        PackageFormat.Expr.addInfo(b, argumentInfo)
        val argument = PackageFormat.Expr.endExpr(b)
        val callOperands = PackageFormat.Expr.createOperandsVector(b, uintArrayOf(1u, 3u))
        val callInfo = PackageFormat.CallInfo.createCallInfo(b, false, PackageFormat.CallKind.CallBuiltinFunction)
        PackageFormat.Expr.startExpr(b)
        PackageFormat.Expr.addKind(b, ExprKind.CallExpr)
        PackageFormat.Expr.addType(b, 1u)
        PackageFormat.Expr.addOperands(b, callOperands)
        PackageFormat.Expr.addInfoType(b, PackageFormat.ExprInfo.CallInfo)
        PackageFormat.Expr.addInfo(b, callInfo)
        val call = PackageFormat.Expr.endExpr(b)
        val expressions = PackageFormat.Package.createAllExprsVector(b, intArrayOf(callee, literal, argument, call))
        val variableInfo = VarInfo.createVarInfo(b, false, true, false, 4u, ConstValue.NONE, 0)
        val name = b.createString("value")
        PackageFormat.Decl.startDecl(b)
        PackageFormat.Decl.addKind(b, PackageFormat.DeclKind.VarDecl)
        PackageFormat.Decl.addIsTopLevel(b, true)
        PackageFormat.Decl.addIdentifier(b, name)
        PackageFormat.Decl.addType(b, 1u)
        PackageFormat.Decl.addInfoType(b, PackageFormat.DeclInfo.VarInfo)
        PackageFormat.Decl.addInfo(b, variableInfo)
        val declaration = PackageFormat.Decl.endDecl(b)
        val declarations = PackageFormat.Package.createAllDeclsVector(b, intArrayOf(declaration))
        val packageName = b.createString("wire.primitive.protocol." + targetKind)
        PackageFormat.Package.startPackage(b)
        PackageFormat.Package.addFullPkgName(b, packageName)
        PackageFormat.Package.addAllTypes(b, types)
        PackageFormat.Package.addAllExprs(b, expressions)
        PackageFormat.Package.addAllDecls(b, declarations)
        b.finish(PackageFormat.Package.endPackage(b))
        val pkg = PackageFormat.Package.getRootAsPackage(b.dataBuffer())
        // 外部 SDK route 属于初始 producer，context 不能在既有 binding 上替换 router authority。
        val producer = if (packageProvider === f.provider) f.provider else CfirDeserializedSymbolProvider(
            f.module.session, CjoManager(CjoSearchPath { null }, declarationMaterialization = f.manager.declarationMaterialization),
            f.provider.deserializationBinding.scopeProvider, f.module,
            moduleDataProvider = cjoFixtureModuleDataProvider(f.module, emptySet()),
            dependencyCjoManager = f.manager,
            importedPackageContextProvider = packageProvider,
        )
        return CfirDeserializationContext(pkg, CjoPackageHeader.fromPackage(pkg), f.module, f.manager,
            f.manager.deserializedSymbolLookups.forBinding(producer.deserializationBinding, f.module, CjoDeserializationMode.FULL),
            packageContextProvider = producer).canonical()
    }

    /** 复现真实 holder/旧 FullId 模块仅 bindSession 的合法装配，不用注册组件掩盖 owner 错误。 */
    @Test
    fun primitiveFactoryAndFacadeShareTheActualOwnerWithoutModuleComponent() {
        for (facadeFirst in listOf(false, true)) fixture { f ->
            val core = f.context("std.core")
            assertNull(core.moduleData.session.nullableModuleData)
            val facade = CfirBuiltinSymbolProvider(core.moduleData.session, core.moduleData)
            val before = if (facadeFirst) facade.getClassLikeSymbolByClassId(PrimitiveTypeKind.UINT8.classId) else null
            val context = primitiveCallContext(f, PackageFormat.TypeKind.UInt8)
            assertIs<CfirTypeConversion>(assertIs<CfirPatternVariable>(
                context.createDeclDeserializer().deserializeDecl(0)).initializer)
            val primitive = context.lookupPrimitiveTypeSymbol(PrimitiveTypeKind.UINT8)
            assertSame(core.moduleData, primitive.cfir.moduleData)
            assertSame(primitive, before ?: facade.getClassLikeSymbolByClassId(PrimitiveTypeKind.UINT8.classId))
            assertNull(core.moduleData.session.nullableModuleData)
        }
    }

    /** 普通库会话可绑定多个模块；注册的 regular module 不能替换真实 SDK snapshot 的 module。 */
    @Test
    fun primitiveOwnerIgnoresAnUnrelatedRegisteredLibraryModule() = fixture { f ->
        val core = f.context("std.core")
        val regular = CfirBinaryDependenciesModuleData(Name.identifier("regular-dependencies"))
            .also { it.bindSession(core.moduleData.session) }
        core.moduleData.session.register(CfirModuleData::class, regular)
        val context = primitiveCallContext(f, PackageFormat.TypeKind.UInt8)
        assertIs<CfirTypeConversion>(assertIs<CfirPatternVariable>(
            context.createDeclDeserializer().deserializeDecl(0)).initializer)
        val symbol = context.lookupPrimitiveTypeSymbol(PrimitiveTypeKind.UINT8)
        assertNotSame(regular, symbol.cfir.moduleData)
        assertSame(core.moduleData, symbol.cfir.moduleData)
        assertSame(symbol, CfirBuiltinSymbolProvider(core.moduleData.session, core.moduleData)
            .getClassLikeSymbolByClassId(PrimitiveTypeKind.UINT8.classId))
        assertFailsWith<IllegalStateException> {
            CfirPrimitiveDeclarationFactory.getInstance(core.moduleData.session, regular)
        }
    }

    /** 并发 facade 与 metadata 消费者必须从同一 session factory 取得唯一 primitive symbol。 */
    @Test
    fun concurrentPrimitiveFacadesAndFactoryPublishOneSymbol() = fixture { f ->
        val core = f.context("std.core")
        val start = java.util.concurrent.CountDownLatch(1)
        val pool = java.util.concurrent.Executors.newFixedThreadPool(8)
        try {
            val futures = (0 until 8).map { index ->
                pool.submit<CfirPrimitiveTypeSymbol> {
                    check(start.await(10, java.util.concurrent.TimeUnit.SECONDS))
                    if (index % 2 == 0) {
                        core.lookupPrimitiveTypeSymbol(PrimitiveTypeKind.UINT8)
                    } else {
                        assertIs<CfirPrimitiveTypeSymbol>(CfirBuiltinSymbolProvider(core.moduleData.session, core.moduleData)
                            .getClassLikeSymbolByClassId(PrimitiveTypeKind.UINT8.classId))
                    }
                }
            }
            start.countDown()
            val symbols = futures.map { it.get(30, java.util.concurrent.TimeUnit.SECONDS) }
            for (symbol in symbols) {
                assertSame(symbols.first(), symbol)
                assertSame(core.moduleData, symbol.cfir.moduleData)
            }
        } finally {
            start.countDown()
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS))
        }
    }

    /** primitive 本身没有 CJO initializer，跨 mode 共用合成身份；两种 wire context 仍独立。 */
    @Test
    fun primitiveIdentitySharesOwnerAcrossModesWithoutSharingWireContexts() = fixture { f ->
        val full = primitiveCallContext(f, PackageFormat.TypeKind.UInt8)
        val signature = full.withMode(CjoDeserializationMode.SIGNATURE_ONLY)
        val root = assertIs<CfirPatternVariable>(signature.createDeclDeserializer().deserializeDecl(0))
        assertNull(root.initializer)
        assertTrue(root.pattern.bindingVariables().single().hasSerializedInitializer)
        assertIs<CfirTypeConversion>(assertIs<CfirPatternVariable>(
            full.createDeclDeserializer().deserializeDecl(0)).initializer)
        assertSame(full.lookupPrimitiveTypeSymbol(PrimitiveTypeKind.UINT8),
            signature.lookupPrimitiveTypeSymbol(PrimitiveTypeKind.UINT8))
        assertNotSame(full.importedContext("std.core"), signature.importedContext("std.core"))
    }

    private fun fixture(action: (Fixture) -> Unit) {
        val directory = Files.createTempDirectory("cjo-constexpr-ast-")
        try {
            val encoded = javaClass.classLoader.getResourceAsStream("cjo-wire/wire.constexpr.cjo.b64")!!
                .use { it.reader(Charsets.UTF_8).readText().trim() }
            Files.write(directory.resolve("wire.constexpr.cjo"), Base64.getDecoder().decode(encoded.filterNot(Char::isWhitespace)))
            action(Fixture(directory))
        } finally { directory.toFile().deleteRecursively() }
    }

    private class Fixture(directory: Path) {
        val module = Module()
        val repository = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
        val sdk = repository.resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative")
        val manager = CjoManager(CjoSearchPath.forRoots(emptyList(), listOf(directory.toFile(), sdk.toFile())))
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(directory, sdk)))
        fun context(name: String) = assertNotNull(provider.getOwnPackageContext(name, CjoDeserializationMode.FULL))
    }

    private class Module : CfirModuleData() {
        override val name = Name.identifier("cjo-constexpr-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjo-constexpr-test"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }
}
