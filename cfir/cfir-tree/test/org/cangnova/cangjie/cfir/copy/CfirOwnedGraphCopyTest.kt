package org.cangnova.cangjie.cfir.copy

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.common.*
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.*
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.*
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import java.math.BigInteger
import java.util.Collections
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 验证身份、同域类型根、前向绑定、控制回边与 metadata 所有权，不以重新解析替代复制。 */
class CfirOwnedGraphCopyTest {
    private val typeRemapper = CfirTypeRefRemapper { type, _ -> type }
    private val attributes = object : CfirOwnedAttributeRemapper {
        override fun collectOwnedChildren(key: String, value: Any, visit: (CfirElement) -> Unit) {
            error("Test has no extension attribute: " + key)
        }
        override fun copyAttribute(key: String, value: Any, context: CfirOwnedGraphCopyContext): Any =
            error("Test has no extension attribute: " + key)
    }
    private val module = CfirSourceModuleData(
        name = Name.identifier("owned-copy-test"), dependencies = emptyList(), refinementDependencies = emptyList(),
        targetPlatform = CangJiePlatforms.defaultCangJiePlatform, platform = CfirPlatform.DEFAULT,
    )

    /** Native 保存后角色的结构合同，依据 materialization-native-authority.md §3；不作为 SOURCE 语言正例。 */
    @Test
    fun savedCallsShareOwnedBasesAndArgumentValuesAcrossRoots() {
        val valueType = ConePrimitiveType(PrimitiveTypeKind.INT32)
        val appliedType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val baseType = ConeFunctionType(listOf(appliedType, appliedType, appliedType, appliedType), appliedType)
        val freshValueType = ConePrimitiveType(PrimitiveTypeKind.INT32)
        val freshAppliedType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val freshBaseType = ConeFunctionType(List(4) { freshAppliedType }, freshAppliedType)
        val replacements = mapOf<ConeCangJieType, ConeCangJieType>(
            valueType to freshValueType, appliedType to freshAppliedType, baseType to freshBaseType,
        )
        val value = buildLiteralExpression { source = source("operand"); kind = CfirLiteralKind.INT; this.value = "7"; coneTypeOrNull = valueType }
        val unary = buildUnaryExpression {
            source = source("negation"); coneTypeOrNull = valueType; operand = value; operation = CfirSourceOperator.SUBTRACT
        }
        val binary = buildBinaryExpression {
            source = source("sum"); coneTypeOrNull = valueType; left = unary; right = value; operation = CfirSourceOperator.ADD
        }
        // FuncArg 的应用后类型是独立槽，不能从共享 value 的 Int32 推成 Int64 或反向覆盖 value。
        val argument = savedArgument(binary, appliedType)
        val inoutArgument = savedArgument(binary, appliedType, withInout = true)
        val defaultArgument = savedArgument(null, appliedType, isDefaultValue = true)
        val external = CfirNamedFunctionSymbol(CallableId(Name.identifier("saved-call-target")))
        val base = buildNamedAccessExpression {
            source = source("callee"); coneTypeOrNull = baseType
            calleeReference = buildResolvedNamedReference { name = Name.identifier("saved-call-target"); resolvedSymbol = external }
        }
        val first = buildCallExpression {
            source = source("first-call"); coneTypeOrNull = appliedType; this.base = base
            arguments += listOf(argument, inoutArgument, argument, defaultArgument)
            callKind = CfirSourceCallKind.DECLARED; hasSideEffect = true
        }
        val second = buildCallExpression {
            source = source("second-call"); coneTypeOrNull = appliedType; this.base = base
            arguments += listOf(defaultArgument, argument, inoutArgument, argument)
            callKind = CfirSourceCallKind.FUNCTION_POINTER; hasSideEffect = false
        }
        // 先只从 Call roots 收集，防止下面显式子根掩盖拥有边遍历遗漏。
        val callsOnly = deepCopyOwnedGraphs(listOf(first, second), CfirTypeRefRemapper { type, _ -> replacements.getValue(type) }, attributes)
        assertSame(callsOnly.getCopied(base), callsOnly.getCopied(first).base)
        assertSame(callsOnly.getCopied(first).base, callsOnly.getCopied(second).base)
        assertSame(callsOnly.getCopied(binary), callsOnly.getCopied(argument).value)
        assertSame(callsOnly.getCopied(binary), callsOnly.getCopied(inoutArgument).value)
        assertSame(callsOnly.getCopied(unary), callsOnly.getCopied(binary).left)
        assertSame(callsOnly.getCopied(value), callsOnly.getCopied(unary).operand)
        assertNull(callsOnly.getCopied(defaultArgument).value)
        val roots = listOf(argument, binary, second, value, first, base, first, defaultArgument, inoutArgument, unary)
        val result = deepCopyOwnedGraphs(roots, CfirTypeRefRemapper { type, _ -> replacements.getValue(type) }, attributes)
        assertEquals(roots.size, result.roots.size)
        roots.forEachIndexed { index, original ->
            assertSame(result.getCopied(original), result.roots[index])
            assertCopiedExpression(original, result, replacements.getValue(original.coneTypeOrNull!!))
        }
        val copiedFirst = result.getCopied(first)
        val copiedSecond = result.getCopied(second)
        val copiedArgument = result.getCopied(argument)
        val copiedInout = result.getCopied(inoutArgument)
        val copiedDefault = result.getCopied(defaultArgument)
        val copiedBinary = result.getCopied(binary)
        assertSame(result.getCopied(base), copiedFirst.base)
        assertSame(copiedFirst.base, copiedSecond.base)
        assertSame(external, (result.getCopied(base).calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        assertEquals(listOf(copiedArgument, copiedInout, copiedArgument, copiedDefault), copiedFirst.arguments)
        assertEquals(listOf(copiedDefault, copiedArgument, copiedInout, copiedArgument), copiedSecond.arguments)
        assertSame(copiedBinary, copiedArgument.value)
        assertSame(copiedBinary, copiedInout.value)
        assertSame(result.getCopied(unary), copiedBinary.left)
        assertSame(result.getCopied(value), copiedBinary.right)
        assertSame(copiedBinary.right, result.getCopied(unary).operand)
        assertEquals(CfirSourceOperator.SUBTRACT, result.getCopied(unary).operation)
        assertEquals(CfirSourceOperator.ADD, copiedBinary.operation)
        assertEquals(CfirSourceCallKind.DECLARED, copiedFirst.callKind)
        assertEquals(CfirSourceCallKind.FUNCTION_POINTER, copiedSecond.callKind)
        assertTrue(copiedFirst.hasSideEffect)
        assertFalse(copiedSecond.hasSideEffect)
        assertFalse(copiedArgument.isDefaultValue)
        assertFalse(copiedArgument.withInout)
        assertTrue(copiedInout.withInout)
        assertFalse(copiedInout.isDefaultValue)
        assertTrue(copiedDefault.isDefaultValue)
        assertFalse(copiedDefault.withInout)
        assertNull(copiedDefault.value)
        assertNotSame(first.arguments, copiedFirst.arguments)
        assertNotSame(second.arguments, copiedSecond.arguments)
        assertNotSame(copiedFirst.arguments, copiedSecond.arguments)
        val replacement = savedArgument(result.getCopied(value), freshAppliedType)
        copiedFirst.transformArguments(object : CfirTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E = element
            override fun transformFunctionArgument(functionArgument: CfirFunctionArgument, data: Unit): CfirStatement =
                if (functionArgument === copiedArgument) replacement else functionArgument
        }, Unit)
        assertEquals(listOf(replacement, copiedInout, replacement, copiedDefault), copiedFirst.arguments)
        assertEquals(listOf(copiedDefault, copiedArgument, copiedInout, copiedArgument), copiedSecond.arguments)
        assertEquals(listOf(argument, inoutArgument, argument, defaultArgument), first.arguments)
        assertSame(binary, argument.value)
        assertSame(valueType, binary.coneTypeOrNull)
        assertSame(appliedType, argument.coneTypeOrNull)
        assertNull(defaultArgument.value)
    }

    /** PrimitiveTypeExpr/WildcardExpr 保留表达式身份，既不变 TypeRef，也不变 wildcard pattern。 */
    @Test
    fun savedLeafExpressionsPreserveConcreteKindsAndPresentOrAbsentTypes() {
        val primitiveType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val primitive = buildPrimitiveTypeExpression { source = source("primitive-expression"); coneTypeOrNull = primitiveType }
        val wildcard = buildWildcardExpression { source = source("wildcard-expression"); coneTypeOrNull = primitiveType }
        val untypedWildcard = buildWildcardExpression()
        val argument = savedArgument(primitive, primitiveType)
        val result = deepCopyOwnedGraphs(listOf(wildcard, argument, primitive, untypedWildcard, wildcard), typeRemapper, attributes)
        assertCopiedExpression(primitive, result, primitiveType)
        assertCopiedExpression(wildcard, result, primitiveType)
        assertCopiedExpression(untypedWildcard, result, null)
        assertCopiedExpression(argument, result, primitiveType)
        assertSame(result.getCopied(primitive), result.getCopied(argument).value)
        assertSame(result.roots.first(), result.roots.last())
        assertNull(result.getCopied(untypedWildcard).source)
    }

    /** future d 真正拥有 initializer；跨 Array/Pointer/Spawn/根列表的 call 和 FuncArg 都只有一份 fresh 节点。 */
    @Test
    fun savedArrayPointerAndSpawnShareArgumentsAndFutureInitializerAcrossRoots() {
        val elementType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val futureType = ConeClassLikeType(ClassId.topLevel(FqName("SavedFuture")).toLookupTag())
        val arrayType = ConeVArrayType(futureType, 2)
        val pointerType = ConePointerType(futureType)
        val baseType = ConeFunctionType(listOf(elementType), futureType)
        val size = literal().also { it.replaceConeTypeOrNull(elementType) }
        val sizeArgument = savedArgument(size, elementType)
        val base = buildNamedAccessExpression {
            coneTypeOrNull = baseType
            calleeReference = buildResolvedNamedReference {
                name = Name.identifier("createFuture"); resolvedSymbol = CfirNamedFunctionSymbol(CallableId(name))
            }
        }
        val call = buildCallExpression {
            source = source("future-call"); coneTypeOrNull = futureType; this.base = base
            arguments += sizeArgument; callKind = CfirSourceCallKind.OBJECT_CREATION; hasSideEffect = true
        }
        val argument = savedArgument(call, futureType)
        val future = savedField("future", futureType, call)
        val array = buildArrayCreationExpression {
            source = source("value-array"); coneTypeOrNull = arrayType; arguments += listOf(argument, argument); isValueArray = true
        }
        val pointer = buildPointerExpression { source = source("pointer"); coneTypeOrNull = pointerType; this.argument = argument }
        val emptyPointer = buildPointerExpression { coneTypeOrNull = pointerType }
        val spawn = buildResolvedSpawnExpression {
            source = source("spawn"); coneTypeOrNull = futureType; this.argument = argument; this.future = future
        }
        val spawnWithoutArgument = buildResolvedSpawnExpression { coneTypeOrNull = futureType; this.future = future }
        // future 与 initializer 不显式列根也必须完整复制，不能靠共享 call 的另一个入口取得 future 所有权。
        val spawnOnly = deepCopyOwnedGraphs(listOf(spawn), typeRemapper, attributes)
        assertSame(spawnOnly.getCopied(future), spawnOnly.getCopied(spawn).future)
        assertSame(spawnOnly.getCopied(call), spawnOnly.getCopied(future).initializer)
        assertSame(spawnOnly.getCopied(call), spawnOnly.getCopied(argument).value)
        assertSame(spawnOnly.getCopied(future), spawnOnly.getCopiedSymbol(future.symbol).cfir)
        assertNotSame(future.symbol, spawnOnly.getCopied(future).symbol)
        val roots = listOf(array, pointer, spawn, call, argument, future, future.returnTypeRef, call, emptyPointer, spawnWithoutArgument)
        val result = deepCopyOwnedGraphs(roots, typeRemapper, attributes)
        roots.forEachIndexed { index, original -> assertSame(result.getCopied(original), result.roots[index]) }
        val copiedArray = assertCopiedExpression(array, result, arrayType)
        val copiedPointer = assertCopiedExpression(pointer, result, pointerType)
        val copiedSpawn = assertCopiedExpression(spawn, result, futureType)
        val copiedCall = assertCopiedExpression(call, result, futureType)
        val copiedArgument = assertCopiedExpression(argument, result, futureType)
        assertCopiedExpression(base, result, baseType)
        assertCopiedExpression(sizeArgument, result, elementType)
        assertCopiedExpression(size, result, elementType)
        assertCopiedExpression(emptyPointer, result, pointerType)
        assertCopiedExpression(spawnWithoutArgument, result, futureType)
        val copiedFuture = result.getCopied(future)
        assertEquals(future::class, copiedFuture::class)
        assertNotSame(future, copiedFuture)
        assertSame(future.source, copiedFuture.source)
        assertNotSame(future.symbol, copiedFuture.symbol)
        assertSame(result.getCopiedSymbol(future.symbol), copiedFuture.symbol)
        assertSame(copiedFuture, copiedFuture.symbol.cfir)
        assertSame(copiedFuture, copiedSpawn.future)
        assertSame(copiedFuture, result.getCopied(spawnWithoutArgument).future)
        assertSame(copiedCall, copiedFuture.initializer)
        assertSame(copiedCall, copiedArgument.value)
        assertSame(copiedArgument, copiedPointer.argument)
        assertSame(copiedArgument, copiedSpawn.argument)
        assertEquals(listOf(copiedArgument, copiedArgument), copiedArray.arguments)
        assertSame(result.getCopied(sizeArgument), copiedCall.arguments.single())
        assertSame(result.getCopied(size), result.getCopied(sizeArgument).value)
        assertSame(result.getCopied(base), copiedCall.base)
        assertSame(result.getCopied(future.returnTypeRef), copiedFuture.returnTypeRef)
        assertNotSame(future.returnTypeRef, copiedFuture.returnTypeRef)
        assertSame(futureType, (copiedFuture.returnTypeRef as CfirResolvedTypeRef).coneType)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, copiedFuture.returnTypeRef.typeUseOrigin)
        assertSame(futureType, (copiedArray.coneTypeOrNull as ConeVArrayType).elementType)
        assertSame(futureType, (copiedPointer.coneTypeOrNull as ConePointerType).pointeeType)
        assertTrue(copiedArray.isValueArray)
        assertNull(copiedArray.initializerSymbol)
        assertNull(result.getCopied(emptyPointer).argument)
        assertNull(result.getCopied(spawnWithoutArgument).argument)
        assertNotSame(array.arguments, copiedArray.arguments)
        assertNotSame(call.arguments, copiedCall.arguments)
        val replacement = savedArgument(null, futureType, isDefaultValue = true)
        copiedArray.transformArguments(object : CfirTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E = element
            override fun transformFunctionArgument(functionArgument: CfirFunctionArgument, data: Unit): CfirStatement = replacement
        }, Unit)
        assertEquals(listOf(replacement, replacement), copiedArray.arguments)
        assertSame(copiedArgument, copiedPointer.argument)
        assertSame(copiedArgument, copiedSpawn.argument)
        assertEquals(listOf(argument, argument), array.arguments)
        copiedFuture.replaceInitializer(result.getCopied(size))
        assertSame(copiedCall, copiedArgument.value)
        assertSame(call, future.initializer)
        assertSame(future, future.symbol.cfir)
        assertSame(call, argument.value)
    }

    /** initFunc 是借用边：只有独立列入 roots 的真实函数才取得所有权，前向符号随同域声明重绑。 */
    @Test
    fun savedArrayInitializerRebindsOwnedFunctionAndKeepsExternalFunctionBorrowed() {
        val type = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val arrayType = ConeClassLikeType(ClassId.topLevel(FqName("SavedArray")).toLookupTag())
        val initializerValue = literal().also { it.replaceConeTypeOrNull(type) }
        val initializerBody = buildBlock { coneTypeOrNull = type; statements += initializerValue }
        val initializer = function("arrayInitializer", CfirNamedFunctionSymbol(CallableId(Name.identifier("arrayInitializer"))), initializerBody)
        initializer.replaceReturnTypeRef(buildResolvedTypeRef { coneType = type })
        val externalBody = buildBlock { coneTypeOrNull = type; statements += literal().also { it.replaceConeTypeOrNull(type) } }
        val external = function("externalInitializer", CfirNamedFunctionSymbol(CallableId(Name.identifier("externalInitializer"))), externalBody)
        external.replaceReturnTypeRef(buildResolvedTypeRef { coneType = type })
        val argument = savedArgument(literal().also { it.replaceConeTypeOrNull(type) }, type)
        val array = buildArrayCreationExpression {
            source = source("array-owned-initializer"); coneTypeOrNull = arrayType
            arguments += argument; initializerSymbol = initializer.symbol; isValueArray = false
        }
        val externalArray = buildArrayCreationExpression {
            source = source("array-borrowed-initializer"); coneTypeOrNull = arrayType
            arguments += argument; initializerSymbol = external.symbol; isValueArray = false
        }
        // 即使借用的是尚未绑定的符号，Array 的构造/复制也不能成为它的 declaration owner。
        val symbolOnly = CfirNamedFunctionSymbol(CallableId(Name.identifier("borrowedInitializerSymbol")))
        val symbolOnlyArray = buildArrayCreationExpression { coneTypeOrNull = arrayType; initializerSymbol = symbolOnly; isValueArray = false }
        val emptyArray = buildArrayCreationExpression { coneTypeOrNull = arrayType; isValueArray = false }
        assertFalse(symbolOnly.isBound)
        val borrowed = deepCopyOwnedGraphs(listOf(array, externalArray, symbolOnlyArray, emptyArray), typeRemapper, attributes)
        assertSame(initializer.symbol, borrowed.getCopied(array).initializerSymbol)
        assertSame(external.symbol, borrowed.getCopied(externalArray).initializerSymbol)
        assertSame(initializer.symbol, borrowed.getCopiedSymbol(initializer.symbol))
        assertSame(external.symbol, borrowed.getCopiedSymbol(external.symbol))
        assertSame(initializer, borrowed.getCopied(array).initializerSymbol!!.cfir)
        assertSame(external, borrowed.getCopied(externalArray).initializerSymbol!!.cfir)
        assertSame(initializer, initializer.symbol.cfir)
        assertSame(external, external.symbol.cfir)
        assertSame(symbolOnly, borrowed.getCopied(symbolOnlyArray).initializerSymbol)
        assertSame(symbolOnly, borrowed.getCopiedSymbol(symbolOnly))
        assertFalse(symbolOnly.isBound)
        assertNull(borrowed.getCopied(emptyArray).initializerSymbol)
        assertTrue(borrowed.getCopied(emptyArray).arguments.isEmpty())
        listOf(initializer, initializerBody, initializerValue, external, externalBody).forEach { outside ->
            assertThrows(IllegalStateException::class.java) { borrowed.getCopied(outside) }
        }
        val result = deepCopyOwnedGraphs(listOf(array, externalArray, symbolOnlyArray, emptyArray, initializer), typeRemapper, attributes)
        val copiedArray = assertCopiedExpression(array, result, arrayType)
        val copiedExternalArray = assertCopiedExpression(externalArray, result, arrayType)
        val copiedInitializer = result.getCopied(initializer)
        assertNotSame(initializer, copiedInitializer)
        assertEquals(initializer::class, copiedInitializer::class)
        assertNotSame(initializer.symbol, copiedInitializer.symbol)
        assertSame(result.getCopiedSymbol(initializer.symbol), copiedInitializer.symbol)
        assertSame(copiedInitializer, copiedInitializer.symbol.cfir)
        assertSame(copiedInitializer.symbol, copiedArray.initializerSymbol)
        assertSame(external.symbol, copiedExternalArray.initializerSymbol)
        assertSame(external, copiedExternalArray.initializerSymbol!!.cfir)
        assertThrows(IllegalStateException::class.java) { result.getCopied(external) }
        assertThrows(IllegalStateException::class.java) { result.getCopied(externalBody) }
        assertSame(symbolOnly, assertCopiedExpression(symbolOnlyArray, result, arrayType).initializerSymbol)
        assertFalse(symbolOnly.isBound)
        assertNull(assertCopiedExpression(emptyArray, result, arrayType).initializerSymbol)
        assertTrue(result.getCopied(emptyArray).arguments.isEmpty())
        val copiedBody = assertCopiedExpression(initializerBody, result, type)
        assertSame(copiedBody, copiedInitializer.body)
        assertSame(assertCopiedExpression(initializerValue, result, type), copiedBody.statements.single())
        assertNotSame(initializerBody.statements, copiedBody.statements)
        assertSame(type, (copiedInitializer.returnTypeRef as CfirResolvedTypeRef).coneType)
        assertSame(result.getCopied(argument), copiedArray.arguments.single())
        assertSame(copiedArray.arguments.single(), copiedExternalArray.arguments.single())
        assertNotSame(copiedArray.arguments, copiedExternalArray.arguments)
        assertFalse(copiedArray.isValueArray)
        assertFalse(copiedExternalArray.isValueArray)
        assertSame(initializer.symbol, array.initializerSymbol)
        assertSame(external.symbol, externalArray.initializerSymbol)
        assertSame(initializer, initializer.symbol.cfir)
        assertSame(external, external.symbol.cfir)
        assertSame(initializerBody, initializer.body)
    }

    /** Native ForIn 拥有 iterable/guard/P/body；非 DIRECT iterable 是真实 lowered Block，不补 raw variable/condition。 */
    @Test
    fun savedForInCopiesPatternGuardAndNonEmptyBodyWithoutRawLoopRoles() {
        val elementType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
        val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
        val iterableType = ConeClassLikeType(ClassId.topLevel(FqName("SavedIterable")).toLookupTag())
        val bindingType = buildResolvedTypeRef { source = source("iteration-type"); typeUseOrigin = CfirTypeRefOrigin.SOURCE; coneType = elementType }
        val binding = buildPatternBindingVariable {
            source = source("iteration-binding"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true; status = CfirDeclarationStatusImpl(); returnTypeRef = bindingType
            symbol = CfirPatternBindingSymbol(CallableId(Name.identifier("item"))); name = Name.identifier("item")
            isVar = false; isTypeImplicit = false
        }
        val pattern = buildBindingPattern { source = source("iteration-pattern"); semanticTypeRef = bindingType; name = binding.name; bindingVariable = binding }
        val access = buildNamedAccessExpression {
            source = source("iteration-reference"); coneTypeOrNull = elementType
            calleeReference = buildResolvedNamedReference { name = binding.name; resolvedSymbol = binding.symbol }
        }
        val threshold = literal().also { it.replaceConeTypeOrNull(elementType) }
        val guard = buildBinaryExpression {
            source = source("iteration-guard"); coneTypeOrNull = boolType; left = access; right = threshold; operation = CfirSourceOperator.GREATER
        }
        val iterable = buildCallExpression {
            source = source("iterable"); coneTypeOrNull = iterableType
            base = buildNamedAccessExpression {
                coneTypeOrNull = ConeFunctionType(emptyList(), iterableType)
                calleeReference = buildResolvedNamedReference { name = Name.identifier("items"); resolvedSymbol = CfirNamedFunctionSymbol(CallableId(name)) }
            }
            callKind = CfirSourceCallKind.DECLARED; hasSideEffect = false
        }
        val loweredIterable = buildBlock { source = source("lowered-iterable"); coneTypeOrNull = iterableType; statements += iterable }
        val argument = savedArgument(access, elementType)
        val bodyCall = buildCallExpression {
            source = source("consume-item"); coneTypeOrNull = unitType
            base = buildNamedAccessExpression {
                coneTypeOrNull = ConeFunctionType(listOf(elementType), unitType)
                calleeReference = buildResolvedNamedReference { name = Name.identifier("consume"); resolvedSymbol = CfirNamedFunctionSymbol(CallableId(name)) }
            }
            arguments += argument; callKind = CfirSourceCallKind.DECLARED; hasSideEffect = true
        }
        val body = buildBlock { source = source("iteration-body"); coneTypeOrNull = unitType; statements += bodyCall }
        fun loop(kind: CfirSourceForInKind, savedGuard: CfirExpression?) = buildForInExpression {
            source = source("for-$kind"); coneTypeOrNull = unitType
            this.iterable = if (kind == CfirSourceForInKind.DIRECT) iterable else loweredIterable
            this.patternGuard = savedGuard; this.pattern = pattern; this.body = body; iterationKind = kind
        }
        val loops = CfirSourceForInKind.entries.map { loop(it, guard) }
        val unguarded = loop(CfirSourceForInKind.DIRECT, null)
        val loopOnly = deepCopyOwnedGraphs(listOf(loops.last()), typeRemapper, attributes)
        assertSame(loopOnly.getCopied(pattern), loopOnly.getCopied(loops.last()).pattern)
        assertSame(loopOnly.getCopied(body), loopOnly.getCopied(loops.last()).body)
        assertSame(loopOnly.getCopied(guard), loopOnly.getCopied(loops.last()).patternGuard)
        assertSame(loopOnly.getCopied(loweredIterable), loopOnly.getCopied(loops.last()).iterable)
        assertSame(loopOnly.getCopied(binding), loopOnly.getCopied(pattern).bindingVariable)
        assertSame(loopOnly.getCopied(binding).symbol,
            (loopOnly.getCopied(access).calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        assertSame(loopOnly.getCopied(bindingType), loopOnly.getCopied(binding).returnTypeRef)
        val roots = listOf(guard, body, bindingType) + loops + listOf(unguarded, iterable, pattern, access, binding)
        val result = deepCopyOwnedGraphs(roots, typeRemapper, attributes)
        roots.forEachIndexed { index, original -> assertSame(result.getCopied(original), result.roots[index]) }
        val copiedBody = assertCopiedExpression(body, result, unitType)
        val copiedGuard = assertCopiedExpression(guard, result, boolType)
        val copiedAccess = assertCopiedExpression(access, result, elementType)
        val copiedIterable = assertCopiedExpression(iterable, result, iterableType)
        val copiedLoweredIterable = assertCopiedExpression(loweredIterable, result, iterableType)
        val copiedPattern = result.getCopied(pattern)
        val copiedBinding = result.getCopied(binding)
        val copiedBindingType = result.getCopied(bindingType)
        loops.forEach { original ->
            val copied = assertCopiedExpression(original, result, unitType)
            assertFalse(CfirLoopExpression::class.java.isInstance(copied))
            assertEquals(original.iterationKind, copied.iterationKind)
            assertSame(if (original.iterationKind == CfirSourceForInKind.DIRECT) copiedIterable else copiedLoweredIterable, copied.iterable)
            assertSame(copiedGuard, copied.patternGuard)
            assertSame(copiedPattern, copied.pattern)
            assertSame(copiedBody, copied.body)
        }
        assertCopiedExpression(unguarded, result, unitType)
        assertNull(result.getCopied(unguarded).patternGuard)
        assertSame(copiedPattern, result.getCopied(unguarded).pattern)
        assertSame(copiedBody, result.getCopied(unguarded).body)
        assertNotSame(pattern, copiedPattern)
        assertEquals(pattern::class, copiedPattern::class)
        assertSame(pattern.source, copiedPattern.source)
        assertSame(copiedBinding, copiedPattern.bindingVariable)
        assertNotSame(binding.symbol, copiedBinding.symbol)
        assertSame(copiedBinding, copiedBinding.symbol.cfir)
        assertSame(copiedBinding.symbol, (copiedAccess.calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        assertSame(copiedBindingType, copiedPattern.semanticTypeRef)
        assertSame(copiedBindingType, copiedBinding.returnTypeRef)
        assertNotSame(bindingType, copiedBindingType)
        assertSame(bindingType.source, copiedBindingType.source)
        assertEquals(CfirTypeRefOrigin.SOURCE, copiedBindingType.typeUseOrigin)
        assertSame(elementType, copiedBindingType.coneType)
        assertSame(copiedAccess, copiedGuard.left)
        assertSame(assertCopiedExpression(threshold, result, elementType), copiedGuard.right)
        assertEquals(CfirSourceOperator.GREATER, copiedGuard.operation)
        assertSame(assertCopiedExpression(bodyCall, result, unitType), copiedBody.statements.single())
        assertSame(assertCopiedExpression(argument, result, elementType), result.getCopied(bodyCall).arguments.single())
        assertSame(copiedAccess, result.getCopied(argument).value)
        assertSame(copiedIterable, copiedLoweredIterable.statements.single())
        assertNotSame(body.statements, copiedBody.statements)
        assertNotSame(loweredIterable.statements, copiedLoweredIterable.statements)
        copiedBody.replaceStatements(listOf(copiedAccess))
        assertSame(bodyCall, body.statements.single())
        assertSame(copiedAccess, result.getCopied(loops.first()).body.statements.single())
        assertSame(binding.symbol, (access.calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        assertSame(bindingType, pattern.semanticTypeRef)
        assertNull(unguarded.patternGuard)
    }

    /** 原生 Jump 借用 For；只复制裸 jump 时保留域外目标，复制 For 时共用新目标。 */
    @Test
    fun forInJumpCopiesRebindOwnedTargetsAndKeepExternalLoopBorrowed() {
        val elementType = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
        val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
        for (isBreak in listOf(false, true)) {
            val target = CfirLoopTarget()
            val jump: CfirLoopJump = if (isBreak) buildBreakExpression {
                source = source("for-break"); coneTypeOrNull = ConePrimitiveType.NOTHING; this.target = target
            } else buildContinueExpression {
                source = source("for-continue"); coneTypeOrNull = ConePrimitiveType.NOTHING; this.target = target
            }
            val element = literal().also { it.replaceConeTypeOrNull(elementType) }
            val iterable = buildArrayLiteral {
                source = source("for-values"); coneTypeOrNull = ConeVArrayType(elementType, 2)
                elements += listOf(element, element)
            }
            val guard = buildLiteralExpression {
                source = source("for-where"); kind = CfirLiteralKind.BOOLEAN; value = true; coneTypeOrNull = boolType
            }
            val pattern = buildWildcardPattern {
                source = source("for-pattern"); semanticTypeRef = buildResolvedTypeRef { coneType = elementType }
            }
            val body = buildBlock {
                source = source("for-body"); coneTypeOrNull = ConePrimitiveType.NOTHING; statements += jump
            }
            val loop = buildForInExpression {
                source = source("for-owner"); coneTypeOrNull = unitType
                this.pattern = pattern; this.iterable = iterable; patternGuard = guard; this.body = body
                iterationKind = CfirSourceForInKind.DIRECT
            }
            target.bind(loop)

            val borrowed = deepCopyOwnedGraphs(listOf(jump), typeRemapper, attributes)
            assertSame(target, assertCopiedExpression(jump, borrowed, ConePrimitiveType.NOTHING).target)
            val borrowedOriginals = mutableListOf<CfirElement>()
            borrowed.forEachCopied { original, _ -> borrowedOriginals += original }
            assertFalse(borrowedOriginals.any { it === loop || it === body || it === iterable || it === pattern || it === guard })

            // 裸 jump 排在拥有根前，重复 roots 不得改变同域 loop/target 身份。
            val roots = listOf(jump, loop, body, jump, loop)
            val result = deepCopyOwnedGraphs(roots, typeRemapper, attributes)
            assertEquals(roots.size, result.roots.size)
            roots.forEachIndexed { index, original -> assertSame(result.getCopied(original), result.roots[index]) }
            val copiedLoop = assertCopiedExpression(loop, result, unitType)
            val copiedJump = assertCopiedExpression(jump, result, ConePrimitiveType.NOTHING)
            val copiedTarget = copiedJump.target
            assertTrue(copiedTarget is CfirLoopTarget)
            assertTrue(copiedTarget.isBound)
            assertNotSame(target, copiedTarget)
            assertSame(copiedLoop, copiedTarget.labeledElement)
            assertSame(copiedJump, result.getCopied(body).statements.single())
            assertSame(result.getCopied(body), copiedLoop.body)
            assertSame(result.getCopied(pattern), copiedLoop.pattern)
            assertSame(result.getCopied(iterable), copiedLoop.iterable)
            assertSame(result.getCopied(guard), copiedLoop.patternGuard)
            assertEquals(CfirSourceForInKind.DIRECT, copiedLoop.iterationKind)
            assertSame(result.getCopied(element), result.getCopied(iterable).elements[0])
            assertSame(result.getCopied(iterable).elements[0], result.getCopied(iterable).elements[1])
            assertNotSame(body.statements, copiedLoop.body.statements)
            assertNotSame(iterable.elements, result.getCopied(iterable).elements)
            copiedLoop.body.replaceStatements(emptyList())
            assertSame(jump, body.statements.single())
            assertSame(target, jump.target)
            assertSame(loop, target.labeledElement)
            assertSame(body, loop.body)
            assertSame(guard, loop.patternGuard)
            assertSame(element, iterable.elements[0])
            assertSame(element, iterable.elements[1])
        }
    }

    /** 十种新角色的非空 annotations 都是拥有列表；共享注解节点只复制一次，各 owner 的列表独立。 */
    @Test
    fun savedRoleAnnotationListsCopyOwnedChildrenAndIsolateMutations() {
        val type = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
        val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
        val futureType = ConeClassLikeType(ClassId.topLevel(FqName("AnnotatedFuture")).toLookupTag())
        val value = literal().also { it.replaceConeTypeOrNull(type) }
        val annotationId = ClassId.topLevel(FqName("CopyMarker"))
        val annotationType = ConeClassLikeType(annotationId.toLookupTag())
        val annotation = buildAnnotationCall {
            source = source("copy-marker"); coneTypeOrNull = annotationType; annotationClassId = annotationId
            typeRef = buildResolvedTypeRef { coneType = annotationType }
            argumentList = buildArgumentList { arguments += value }
            calleeReference = buildResolvedNamedReference { name = Name.identifier("CopyMarker"); resolvedSymbol = CfirClassSymbol(annotationId) }
            containingDeclarationSymbol = CfirNamedFunctionSymbol(CallableId(Name.identifier("annotation-owner")))
        }
        val primitive = buildPrimitiveTypeExpression { coneTypeOrNull = type }
        val wildcard = buildWildcardExpression { coneTypeOrNull = type }
        val argument = savedArgument(value, type)
        val call = buildCallExpression {
            coneTypeOrNull = futureType
            base = buildNamedAccessExpression {
                coneTypeOrNull = ConeFunctionType(listOf(type), futureType)
                calleeReference = buildResolvedNamedReference { name = Name.identifier("future"); resolvedSymbol = CfirNamedFunctionSymbol(CallableId(name)) }
            }
            arguments += argument; callKind = CfirSourceCallKind.DECLARED; hasSideEffect = true
        }
        val boolean = buildLiteralExpression { kind = CfirLiteralKind.BOOLEAN; this.value = "true"; coneTypeOrNull = boolType }
        val unary = buildUnaryExpression { coneTypeOrNull = boolType; operand = boolean; operation = CfirSourceOperator.NOT }
        val binary = buildBinaryExpression { coneTypeOrNull = boolType; left = unary; right = boolean; operation = CfirSourceOperator.LOGICAL_AND }
        val futureArgument = savedArgument(call, futureType)
        val array = buildArrayCreationExpression {
            coneTypeOrNull = ConeVArrayType(futureType, 2); arguments += listOf(futureArgument, futureArgument); isValueArray = true
        }
        val pointer = buildPointerExpression { coneTypeOrNull = ConePointerType(futureType); this.argument = futureArgument }
        val spawn = buildResolvedSpawnExpression { coneTypeOrNull = futureType; this.argument = futureArgument; future = savedField("annotated-future", futureType, call) }
        val loop = buildForInExpression {
            coneTypeOrNull = unitType; iterable = array; patternGuard = binary
            pattern = buildWildcardPattern {
                source = source("annotated-loop-pattern")
                semanticTypeRef = buildResolvedTypeRef { coneType = unitType }
            }
            body = buildBlock { coneTypeOrNull = unitType; statements += call }; iterationKind = CfirSourceForInKind.DIRECT
        }
        val nodes = listOf(primitive, wildcard, argument, call, unary, binary, array, pointer, spawn, loop)
        nodes.forEach { it.replaceAnnotations(listOf(annotation, annotation)) }
        val result = deepCopyOwnedGraphs(nodes, typeRemapper, attributes)
        val copiedAnnotation = assertCopiedExpression(annotation, result, annotationType)
        assertSame(result.getCopied(value), copiedAnnotation.argumentList.arguments.single())
        assertNotSame(annotation.argumentList.arguments, copiedAnnotation.argumentList.arguments)
        assertSame(result.getCopied(value), result.getCopied(argument).value)
        nodes.forEach { original ->
            val copied = assertCopiedExpression(original, result, original.coneTypeOrNull)
            assertEquals(listOf(copiedAnnotation, copiedAnnotation), copied.annotations)
        }
        assertEquals(CfirSourceOperator.NOT, result.getCopied(unary).operation)
        assertEquals(CfirSourceOperator.LOGICAL_AND, result.getCopied(binary).operation)
        assertSame(result.getCopied(unary), result.getCopied(binary).left)
        assertSame(result.getCopied(boolean), result.getCopied(unary).operand)
        assertSame(result.getCopied(boolean), result.getCopied(binary).right)
        val replacement = buildAnnotationCallCopy(copiedAnnotation) {}
        result.getCopied(primitive).transformAnnotations(object : CfirTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E = element
            override fun transformAnnotationCall(annotationCall: CfirAnnotationCall, data: Unit): CfirStatement = replacement
        }, Unit)
        assertEquals(listOf(replacement, replacement), result.getCopied(primitive).annotations)
        nodes.drop(1).forEach { assertEquals(listOf(copiedAnnotation, copiedAnnotation), result.getCopied(it).annotations) }
        nodes.forEach { assertEquals(listOf(annotation, annotation), it.annotations) }
    }

    /** C extras 无formal且保留共享/重复槽；grouped copy 将其中真实借用参数重绑为同域fresh符号。 */
    @Test
    fun cVariadicArgumentCopyKeepsAllSlotsAndRebindsSharedExtraReferences() {
        val parameter = valueParameter(CfirNamedFunctionSymbol(CallableId(Name.identifier("c-owner"))))
        val fixed = literal()
        val extra = buildNamedAccessExpression {
            calleeReference = buildResolvedNamedReference { name = parameter.name; resolvedSymbol = parameter.symbol }
        }
        val raw = buildArgumentList { arguments += fixed; arguments += extra; arguments += extra }
        val resolved = org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList(raw, linkedMapOf(fixed to parameter),
            cVariadicArguments = CfirCVariadicArguments(1, listOf(extra, extra)))
        val copy = deepCopyOwnedGraphs(listOf(parameter, resolved), typeRemapper, attributes)
        val copiedParameter = copy.getCopied(parameter)
        val copied = copy.getCopied(resolved)
        assertEquals(3, copied.arguments.size)
        assertEquals(1, copied.mapping.size)
        assertSame(copiedParameter, copied.mapping.values.single())
        assertEquals(1, copied.cVariadicArguments?.fixedParameterCount)
        assertEquals(2, copied.cVariadicArguments?.extraArguments?.size)
        assertSame(copied.arguments[1], copied.arguments[2])
        assertSame(copied.arguments[1], copied.cVariadicArguments?.extraArguments?.first())
        assertSame(copied.arguments[1], copied.originalArgumentList?.arguments?.get(1))
        assertNotSame(extra, copied.arguments[1])
        assertSame(copiedParameter.symbol, (copied.arguments[1] as CfirNamedAccessExpression).let {
            (it.calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol
        })
        assertSame(parameter.symbol, (extra.calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
    }

    /** allArgs、fixed/context mapping及extras共用一次转换；恢复同时还原所有视图的原身份。 */
    @Test
    fun cVariadicArgumentsTransformOnceAndRestoreTheCompleteRoleProduct() {
        val parameter = valueParameter(CfirNamedFunctionSymbol(CallableId(Name.identifier("c-state"))))
        val fixed = literal()
        val extra = literal()
        val originalCArguments = CfirCVariadicArguments<CfirExpression>(1, listOf(extra, extra))
        val resolved = org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList(
            buildArgumentList { arguments += fixed; arguments += extra; arguments += extra }, linkedMapOf(fixed to parameter),
            cVariadicArguments = originalCArguments)
        val state = CfirArgumentListMutableState.capture(resolved)
        val transformed = java.util.IdentityHashMap<CfirElement, Int>()
        resolved.transformArguments(object : org.cangnova.cangjie.cfir.visitors.CfirTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E = element
            override fun transformLiteralExpression(literalExpression: CfirLiteralExpression, data: Unit): CfirExpression {
                transformed[literalExpression] = (transformed[literalExpression] ?: 0) + 1
                return buildLiteralExpressionCopy(literalExpression) {}
            }
        }, Unit)
        assertEquals(1, transformed[fixed])
        assertEquals(1, transformed[extra])
        assertSame(resolved.arguments[1], resolved.arguments[2])
        assertSame(resolved.arguments[1], resolved.cVariadicArguments?.extraArguments?.first())
        assertNotSame(fixed, resolved.arguments.first())
        state.restore()
        assertSame(originalCArguments, resolved.cVariadicArguments)
        assertSame(fixed, resolved.arguments.first())
        assertSame(extra, resolved.arguments[1])
        assertSame(fixed, resolved.mapping.keys.single())
        assertSame(resolved.mapping, resolved.mappingIncludingContextArguments)
        val zeroExtras = org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList(
            buildArgumentList { arguments += fixed }, linkedMapOf(fixed to parameter), cVariadicArguments = CfirCVariadicArguments(1, emptyList()))
        assertNotNull(zeroExtras.cVariadicArguments)
        assertTrue(zeroExtras.cVariadicArguments!!.extraArguments.isEmpty())
    }

    @Test
    fun groupedRootsReuseSuppliedOwnedTypeParameterAndShareAliases() {
        val owner = CfirNamedFunctionSymbol(CallableId(Name.identifier("owner")))
        val helper = CfirNamedFunctionSymbol(CallableId(Name.identifier("helper")))
        val oldT = CfirTypeParameterSymbol()
        val freshT = CfirTypeParameterSymbol()
        val parameter = buildTypeParameter {
            source = source("T"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            containingDeclarationSymbol = owner; symbol = oldT; name = Name.identifier("T")
            bounds += buildUserTypeRef { source = source("Bound"); qualifier += buildQualifierPart { name = Name.identifier("Bound") } }
        }
        val bound = parameter.bounds.single()
        val expression = buildNamedAccessExpression {
            calleeReference = buildResolvedNamedReference { name = Name.identifier("T"); resolvedSymbol = oldT }
        }
        val result = deepCopyOwnedGraphs(
            listOf(parameter, bound, expression), typeRemapper, attributes, mapOf(owner to helper, oldT to freshT),
        )
        val copied = result.getCopied(parameter)
        assertSame(freshT, copied.symbol)
        assertSame(copied, freshT.cfir)
        assertSame(helper, copied.containingDeclarationSymbol)
        assertSame(result.getCopied(bound), copied.bounds.single())
        assertSame(freshT, (result.getCopied(expression).calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        assertNotSame(parameter, copied)
        assertNotSame(bound, copied.bounds.single())
        assertSame(freshT, result.getCopiedSymbol(oldT))
    }

    @Test
    fun functionReturnLoopAndPatternBindingsAreFreshAndExternalReferencesStayExternal() {
        val symbol = CfirNamedFunctionSymbol(CallableId(Name.identifier("function")))
        val target = CfirFunctionTarget(isLambda = false)
        val loopTarget = CfirLoopTarget()
        val binding = buildPatternBindingVariable {
            source = source("binding"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true; status = CfirDeclarationStatusImpl(); returnTypeRef = buildImplicitTypeRef()
            this.symbol = CfirPatternBindingSymbol(CallableId(Name.identifier("binding")))
            name = Name.identifier("binding"); isVar = false; isTypeImplicit = true
        }
        val external = CfirNamedFunctionSymbol(CallableId(Name.identifier("external")))
        val body = buildBlock {
            statements += buildLetPatternExpression {
                initializer = literal(); pattern = buildBindingPattern { name = binding.name; bindingVariable = binding }
            }
            statements += buildNamedAccessExpression {
                calleeReference = buildResolvedNamedReference { name = binding.name; resolvedSymbol = binding.symbol }
            }
            statements += buildFunctionCall {
                calleeReference = buildResolvedNamedReference { name = Name.identifier("external"); resolvedSymbol = external }
            }
        }
        val jump = buildBreakExpression { this.target = loopTarget }
        val loop = buildLoopExpression { condition = literal(); this.body = buildBlock { statements += jump }; isDoWhile = false }
        loopTarget.bind(loop)
        body.replaceStatements(body.statements + loop)
        val returnExpression = buildReturnExpression { this.target = target; result = literal() }
        body.replaceStatements(body.statements + returnExpression)
        val function = function("function", symbol, body)
        target.bind(function)
        val result = deepCopyOwnedGraphs(listOf(function), typeRemapper, attributes)
        val copied = result.getCopied(function)
        assertNotSame(function.symbol, copied.symbol)
        assertSame(copied, result.getCopied(returnExpression).target.labeledElement)
        assertSame(result.getCopied(loop), result.getCopied(jump).target.labeledElement)
        assertNotSame(target, result.getCopied(returnExpression).target)
        assertNotSame(loopTarget, result.getCopied(jump).target)
        assertNotSame(binding.symbol, result.getCopiedSymbol(binding.symbol))
        val access = result.getCopied(body.statements[1] as CfirNamedAccessExpression)
        assertSame(result.getCopiedSymbol(binding.symbol), (access.calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        val call = result.getCopied(body.statements[2] as CfirFunctionCall)
        assertSame(external, (call.calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        assertNull(copied.controlFlowGraphReference)
        assertNotSame(function.status, copied.status)
    }

    @Test
    fun helperOwnedByAttributeIsCopiedIncludingCycleBackToParameter() {
        val owner = CfirNamedFunctionSymbol(CallableId(Name.identifier("owner")))
        val parameter = valueParameter(owner)
        val helper = function("helper", CfirNamedFunctionSymbol(CallableId(Name.identifier("helper"))), buildBlock {})
        // metadata 回边不重复取得所有权；helper 内引用真实参数身份。
        helper.body!!.replaceStatements(helper.body!!.statements + buildNamedAccessExpression {
            calleeReference = buildResolvedNamedReference { name = parameter.name; resolvedSymbol = parameter.symbol }
        })
        parameter.defaultValueMetadata = CfirParameterDefaultValueMetadata(true, helper)
        parameter.defaultAssignmentSyntaxFacts = CfirDefaultAssignmentSyntaxFacts(false, false)
        val result = deepCopyOwnedGraphs(listOf(parameter), typeRemapper, attributes)
        val copied = result.getCopied(parameter)
        assertNotSame(parameter.attributes, copied.attributes)
        assertNotSame(helper, copied.defaultValueMetadata!!.function)
        assertSame(result.getCopied(helper), copied.defaultValueMetadata!!.function)
        assertSame(copied.symbol, ((result.getCopied(helper).body!!.statements.single() as CfirNamedAccessExpression)
            .calleeReference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
        assertEquals(parameter.defaultAssignmentSyntaxFacts, copied.defaultAssignmentSyntaxFacts)
    }

    @Test
    fun resolvedArgumentMappingKeepsUnmappedItemsAndOriginalListAliases() {
        val owner = CfirNamedFunctionSymbol(CallableId(Name.identifier("callee")))
        val parameter = valueParameter(owner)
        val argument = literal()
        val unmapped = literal()
        val raw = buildArgumentList { arguments += argument; arguments += unmapped }
        val resolved = CfirResolvedArgumentListForErrorCall(raw, linkedMapOf(argument to parameter, unmapped to null))
        val result = deepCopyOwnedGraphs(listOf(resolved, parameter), typeRemapper, attributes)
        val copied = result.getCopied(resolved)
        assertSame(result.getCopied(argument), copied.originalArgumentList!!.arguments[0])
        assertSame(result.getCopied(unmapped), copied.arguments[1])
        assertSame(result.getCopied(parameter), copied.mapping[result.getCopied(argument)])
        assertEquals(2, copied.rawMappingForOwnedCopy.size)
        assertNull(copied.rawMappingForOwnedCopy[result.getCopied(unmapped)])
        assertNotSame(resolved.rawMappingForOwnedCopy, copied.rawMappingForOwnedCopy)
    }

    @Test
    fun nestedLambdaOwnsItsReturnTargetAndRebindsCapturedParameter() {
        val owner = CfirNamedFunctionSymbol(CallableId(Name.identifier("owner")))
        val parameter = valueParameter(owner)
        val target = CfirFunctionTarget(isLambda = true)
        val returned = buildReturnExpression {
            this.target = target
            result = buildNamedAccessExpression {
                calleeReference = buildResolvedNamedReference { name = parameter.name; resolvedSymbol = parameter.symbol }
            }
        }
        val lambda = buildAnonymousFunction {
            source = source("lambda"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true; returnTypeRef = buildImplicitTypeRef(); typeRef = buildImplicitTypeRef()
            symbol = CfirAnonymousFunctionSymbol(); isLambda = true; hasExplicitParameterList = false
            body = buildBlock { statements += returned }
        }
        target.bind(lambda)
        val wrapper = buildAnonymousFunctionExpression { anonymousFunction = lambda; isTrailingLambda = false }
        val result = deepCopyOwnedGraphs(listOf(wrapper, parameter), typeRemapper, attributes)
        assertSame(result.getCopied(lambda), result.getCopied(wrapper).anonymousFunction)
        assertSame(result.getCopied(lambda), result.getCopied(returned).target.labeledElement)
        assertNotSame(lambda.symbol, result.getCopiedSymbol(lambda.symbol))
        val reference = (result.getCopied(returned).result as CfirNamedAccessExpression).calleeReference
        assertSame(result.getCopied(parameter).symbol,
            (reference as org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference).resolvedSymbol)
    }

    @Test
    fun explicitTypeRemapperCoversExpressionAndDelegatedResolvedTypeRoots() {
        val input = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val output = ConePrimitiveType(PrimitiveTypeKind.FLOAT64)
        val delegated = buildImplicitTypeRef()
        val type = buildResolvedTypeRef { coneType = input; delegatedTypeRef = delegated }
        val expression = buildLiteralExpression { kind = CfirLiteralKind.INT; value = "1"; coneTypeOrNull = input }
        val result = deepCopyOwnedGraphs(listOf(type, expression, delegated),
            CfirTypeRefRemapper { actual, _ -> assertSame(input, actual); output }, attributes)
        assertSame(output, result.getCopied(type).coneType)
        assertSame(output, result.getCopied(expression).coneTypeOrNull)
        assertSame(result.getCopied(delegated), result.getCopied(type).delegatedTypeRef)
        assertNotSame(delegated, result.getCopied(delegated))
    }

    @Test
    fun rawThisAndInaccessibleReceiversKeepKindsAndNullableBinding() {
        val reference = buildThisReference { isImplicit = false }
        val receiver = CfirThisReceiverExpression(source("this"), emptyList(), null, reference)
        val inaccessible = CfirInaccessibleReceiverExpression(
            source("this"), emptyList(), null, reference, InaccessibleReceiverKind.STATIC_MEMBER,
        )
        val result = deepCopyOwnedGraphs(listOf(receiver, inaccessible), typeRemapper, attributes)
        assertNotSame(receiver, result.getCopied(receiver))
        assertEquals(receiver::class, result.getCopied(receiver)::class)
        assertEquals(inaccessible::class, result.getCopied(inaccessible)::class)
        assertSame(result.getCopied(reference), result.getCopied(receiver).calleeReference)
        assertSame(result.getCopied(reference), result.getCopied(inaccessible).calleeReference)
        assertNull(result.getCopied(reference).boundSymbol)
        assertSame(inaccessible.kind, result.getCopied(inaccessible).kind)
    }

    @Test
    fun ownedTypeParameterRemapsEverySmartcastTypeSlot() {
        val owner = CfirNamedFunctionSymbol(CallableId(Name.identifier("identity")))
        val oldT = CfirTypeParameterSymbol()
        val freshT = CfirTypeParameterSymbol()
        val declaration = buildTypeParameter {
            source = source("T"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            containingDeclarationSymbol = owner; symbol = oldT; name = Name.identifier("T")
        }
        val oldType = ConeTypeParameterTypeImpl(oldT.toLookupTag())
        val parameter = valueParameter(owner)
        parameter.replaceReturnTypeRef(buildResolvedTypeRef { coneType = oldType })
        val access = buildNamedAccessExpression {
            coneTypeOrNull = oldType
            calleeReference = buildResolvedNamedReference { name = parameter.name; resolvedSymbol = parameter.symbol }
        }
        val cast = CfirSmartCastExpression(source("cast"), emptyList(), oldType, access,
            buildResolvedTypeRef { coneType = oldType }, listOf(oldType), listOf(oldType), CfirSmartcastStability.STABLE_VALUE)
        val remapper = CfirTypeRefRemapper { type, context ->
            val input = type as ConeTypeParameterType
            ConeTypeParameterTypeImpl(context.remapSymbol(input.lookupTag.typeParameterSymbol).toLookupTag(), input.attributes)
        }
        val result = deepCopyOwnedGraphs(listOf(cast, declaration, parameter), remapper, attributes, mapOf(oldT to freshT))
        val copied = result.getCopied(cast)
        val types = listOf(copied.coneTypeOrNull!!, copied.originalExpression.coneTypeOrNull!!,
            (copied.smartcastType as CfirResolvedTypeRef).coneType, (result.getCopied(parameter).returnTypeRef as CfirResolvedTypeRef).coneType) +
            copied.upperTypesFromSmartCast + copied.lowerTypesFromSmartCast
        assertEquals(6, types.size)
        types.forEach { assertSame(freshT, (it as ConeTypeParameterType).lookupTag.typeParameterSymbol) }
        assertSame(result.getCopied(declaration), freshT.cfir)
        assertSame(result.getCopied(access), copied.originalExpression)
        assertNotSame(cast.upperTypesFromSmartCast, copied.upperTypesFromSmartCast)
        assertNotSame(cast.lowerTypesFromSmartCast, copied.lowerTypesFromSmartCast)
        assertSame(oldT, oldType.lookupTag.typeParameterSymbol)
    }

    @Test
    fun memberParameterAssociationsBorrowWithoutExpandingTheOwnedDomain() {
        val owner = CfirConstructorSymbol(CallableId(Name.identifier("Owner")))
        val parameter = valueParameter(owner)
        val field = buildFieldVariable {
            source = source("field"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; status = CfirDeclarationStatusImpl(); returnTypeRef = buildImplicitTypeRef()
            symbol = CfirFieldVariableSymbol(CallableId(Name.identifier("field"))); name = Name.identifier("field"); isVar = false
        }
        buildPrimaryConstructor {
            source = source("Owner"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; status = CfirDeclarationStatusImpl(); returnTypeRef = buildImplicitTypeRef()
            symbol = owner; valueParameters += parameter
        }
        bindPrimaryConstructorParameterField(parameter, field)
        val parameterOnly = deepCopyOwnedGraphs(listOf(parameter), typeRemapper, attributes)
        assertSame(field, parameterOnly.getCopied(parameter).correspondingField)
        assertThrows(IllegalStateException::class.java) { parameterOnly.getCopied(field) }
        val fieldOnly = deepCopyOwnedGraphs(listOf(field), typeRemapper, attributes)
        assertSame(parameter, fieldOnly.getCopied(field).correspondingValueParameter)
        assertThrows(IllegalStateException::class.java) { fieldOnly.getCopied(parameter) }
        val grouped = deepCopyOwnedGraphs(listOf(field, parameter), typeRemapper, attributes)
        assertSame(grouped.getCopied(field), grouped.getCopied(parameter).correspondingField)
        assertSame(grouped.getCopied(parameter), grouped.getCopied(field).correspondingValueParameter)
        assertTrue(grouped.getCopied(parameter).isMemberParameter)
        assertTrue(grouped.getCopied(field).isMemberParameter)
        assertSame(field, parameter.correspondingField)
    }

    @Test
    fun constantGraphPreservesAliasesCyclesAndRemapsFieldAndConstructorSymbols() {
        val type = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val ownerId = ClassId.topLevel(FqName("Node"))
        val enumId = ClassId.topLevel(FqName("Result"))
        val field = CfirFieldVariableSymbol(CallableId(ownerId, Name.identifier("next")))
        val freshField = CfirFieldVariableSymbol(field.callableId)
        val constructor = CfirEnumConstructorSymbol(CallableId(enumId, Name.identifier("Some")))
        val freshConstructor = CfirEnumConstructorSymbol(constructor.callableId)
        val fields = linkedMapOf<CfirCallableSymbol<*>, CfirConstantValue>()
        val storageTypes = linkedMapOf<CfirCallableSymbol<*>, ConeCangJieType>()
        val value = CfirConstantValue.ObjectValue(ConeClassLikeType(ownerId.toLookupTag()), Collections.unmodifiableMap(fields), 17,
            Collections.unmodifiableMap(storageTypes))
        fields[field] = value
        storageTypes[field] = value.type
        val primitive = CfirConstantValue.Primitive(type, CfirLiteralKind.INT, BigInteger.valueOf(42))
        val enum = CfirConstantValue.EnumValue(ConeEnumType(enumId.toLookupTag()), constructor, listOf(value, primitive))
        val first = CfirConstantValueExpression(source("first"), value)
        val alias = CfirConstantValueExpression(source("alias"), value)
        val second = CfirConstantValueExpression(source("enum"), enum)
        val result = deepCopyOwnedGraphs(listOf(first, alias, second), typeRemapper, attributes,
            mapOf(field to freshField, constructor to freshConstructor))
        val copied = result.getCopied(first).constantValue as CfirConstantValue.ObjectValue
        val copiedEnum = result.getCopied(second).constantValue as CfirConstantValue.EnumValue
        assertNotSame(value, copied)
        assertNotSame(value.fields, copied.fields)
        assertSame(copied, copied.fields.getValue(freshField))
        assertSame(copied, result.getCopied(alias).constantValue)
        assertSame(copied, copiedEnum.arguments[0])
        assertSame(freshConstructor, copiedEnum.constructor)
        assertEquals(BigInteger.valueOf(42), (copiedEnum.arguments[1] as CfirConstantValue.Primitive).value)
        assertFalse(copied.fields.containsKey(field))
        assertEquals(17L, copied.identity)
        assertSame(value, fields.getValue(field))
    }

    @Test
    fun nonEmptyVArrayConstantKeepsStorageShapeAndRemapsElementTypes() {
        val type = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val primitive = CfirConstantValue.Primitive(type, CfirLiteralKind.INT, BigInteger.ONE)
        val arrayType = ConeVArrayType(type, 2)
        val value = CfirConstantValue.VArray(arrayType, listOf(primitive, primitive))
        val expression = CfirConstantValueExpression(source("array"), value)
        val result = deepCopyOwnedGraphs(listOf(expression), typeRemapper, attributes)
        val copied = result.getCopied(expression).constantValue as CfirConstantValue.VArray
        assertEquals(2, copied.elements.size)
        assertSame(copied.elements[0], copied.elements[1])
        assertNotSame(primitive, copied.elements[0])
        assertNotSame(value.elements, copied.elements)
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(expression), CfirTypeRefRemapper { _, _ -> type }, attributes)
        }
    }

    @Test
    fun symbolRebindingsRejectWrongKindsDuplicateOwnersAndBoundDestinations() {
        val first = function("first", CfirNamedFunctionSymbol(CallableId(Name.identifier("first"))), buildBlock {})
        val second = function("second", CfirNamedFunctionSymbol(CallableId(Name.identifier("second"))), buildBlock {})
        val fresh = CfirNamedFunctionSymbol(CallableId(Name.identifier("fresh")))
        assertThrows(IllegalArgumentException::class.java) {
            deepCopyOwnedGraphs(listOf(first), typeRemapper, attributes,
                mapOf(first.symbol to CfirFieldVariableSymbol(CallableId(Name.identifier("wrong")))))
        }
        assertThrows(IllegalArgumentException::class.java) {
            deepCopyOwnedGraphs(listOf(first, second), typeRemapper, attributes, mapOf(first.symbol to fresh, second.symbol to fresh))
        }
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(first), typeRemapper, attributes, mapOf(first.symbol to second.symbol))
        }
        assertFalse(fresh.isBound)
        assertSame(first, first.symbol.cfir)
        assertSame(second, second.symbol.cfir)
    }

    @Test
    fun explicitInternalTargetsAreDeferredAndBoundToCopiedOwners() {
        val oldTarget = CfirFunctionTarget(isLambda = false)
        val freshTarget = CfirFunctionTarget(isLambda = false)
        val returned = buildReturnExpression { target = oldTarget; result = literal() }
        val owner = function("owner", CfirNamedFunctionSymbol(CallableId(Name.identifier("owner"))), buildBlock { statements += returned })
        oldTarget.bind(owner)
        val result = deepCopyOwnedGraphs(listOf(returned, owner), typeRemapper, attributes,
            targetRebindings = mapOf(oldTarget to freshTarget))
        assertTrue(freshTarget.isBound)
        assertSame(result.getCopied(owner), freshTarget.labeledElement)
        assertSame(freshTarget, result.getCopied(returned).target)
        assertSame(owner, oldTarget.labeledElement)
        assertTrue(result.analysisOwners.any { it === result.getCopied(owner) })
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(returned, owner), typeRemapper, attributes,
                targetRebindings = mapOf(oldTarget to CfirFunctionTarget(isLambda = false).also { it.bind(owner) }))
        }
        assertThrows(IllegalArgumentException::class.java) {
            deepCopyOwnedGraphs(listOf(returned, owner), typeRemapper, attributes,
                targetRebindings = mapOf(oldTarget to CfirLoopTarget()))
        }
    }

    @Test
    fun immutableEmptySingletonsKeepConcreteIdentity() {
        val mapping = org.cangnova.cangjie.cfir.expressions.impl.CfirEmptyAnnotationArgumentMapping
        val ordinary = buildArgumentList { source = source("empty") }
        val result = deepCopyOwnedGraphs(listOf(CfirEmptyArgumentList, mapping, ordinary), typeRemapper, attributes)
        assertSame(CfirEmptyArgumentList, result.getCopied(CfirEmptyArgumentList))
        assertSame(mapping, result.getCopied(mapping))
        assertNotSame(ordinary, result.getCopied(ordinary))
        assertNull(result.getCopied(mapping).source)
    }

    @Test
    fun externalSymbolDestinationCannotLeakAnOwnedInputDeclaration() {
        val a = function("a", CfirNamedFunctionSymbol(CallableId(Name.identifier("a"))), buildBlock {})
        val b = function("b", CfirNamedFunctionSymbol(CallableId(Name.identifier("b"))), buildBlock {})
        val oldTarget = CfirFunctionTarget(isLambda = false).also { it.bind(a) }
        val destination = CfirFunctionTarget(isLambda = false).also { it.bind(b) }
        val returned = buildReturnExpression { target = oldTarget; result = literal() }
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(returned, b), typeRemapper, attributes,
                mapOf(a.symbol to b.symbol), mapOf(oldTarget to destination))
        }
        assertSame(a, oldTarget.labeledElement)
        assertSame(b, destination.labeledElement)
    }

    @Test
    fun detachedBodyRequiresExplicitTargetWhenItsExternalOwnerIsRebound() {
        val old = function("old", CfirNamedFunctionSymbol(CallableId(Name.identifier("old"))), buildBlock {})
        val helper = function("helper", CfirNamedFunctionSymbol(CallableId(Name.identifier("helper"))), buildBlock {})
        val target = CfirFunctionTarget(isLambda = false).also { it.bind(old) }
        val returned = buildReturnExpression { this.target = target; result = literal() }
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(returned), typeRemapper, attributes, mapOf(old.symbol to helper.symbol))
        }
        val destination = CfirFunctionTarget(isLambda = false).also { it.bind(helper) }
        val result = deepCopyOwnedGraphs(listOf(returned), typeRemapper, attributes,
            mapOf(old.symbol to helper.symbol), mapOf(target to destination))
        assertSame(helper, result.getCopied(returned).target.labeledElement)
        assertSame(old, target.labeledElement)
    }

    @Test
    fun explicitExternalLoopTargetUsesItsDeclaredLoopOwner() {
        fun loop() = buildLoopExpression { condition = literal(); body = buildBlock {}; isDoWhile = false }
        val old = loop()
        val fresh = loop()
        val target = CfirLoopTarget().also { it.bind(old) }
        val destination = CfirLoopTarget().also { it.bind(fresh) }
        val jump = buildBreakExpression { this.target = target }
        val result = deepCopyOwnedGraphs(listOf(jump), typeRemapper, attributes,
            targetRebindings = mapOf(target to destination))
        assertSame(destination, result.getCopied(jump).target)
        assertSame(fresh, result.getCopied(jump).target.labeledElement)
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(jump, fresh), typeRemapper, attributes,
                targetRebindings = mapOf(target to destination))
        }
    }

    @Test
    fun constantFieldRebindingCannotCollapseDistinctStorageSlots() {
        val type = ConePrimitiveType(PrimitiveTypeKind.INT64)
        val a = CfirFieldVariableSymbol(CallableId(Name.identifier("a")))
        val b = CfirFieldVariableSymbol(CallableId(Name.identifier("b")))
        val primitive = CfirConstantValue.Primitive(type, CfirLiteralKind.INT, BigInteger.ONE)
        val value = CfirConstantValue.ObjectValue(ConeClassLikeType(ClassId.topLevel(FqName("Storage")).toLookupTag()),
            linkedMapOf(a to primitive, b to primitive), 1, mapOf(a to type, b to type))
        val expression = CfirConstantValueExpression(source("storage"), value)
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(expression), typeRemapper, attributes, mapOf(a to b))
        }
        assertEquals(2, value.fields.size)
    }

    @Test
    fun borrowedConstructorRebindingMustKeepItsConcreteKind() {
        val primary = buildPrimaryConstructor {
            source = source("primary"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; status = CfirDeclarationStatusImpl(); returnTypeRef = buildImplicitTypeRef()
            symbol = CfirConstructorSymbol(CallableId(Name.identifier("primary")))
        }
        val secondary = buildConstructor {
            source = source("secondary"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; status = CfirDeclarationStatusImpl(); returnTypeRef = buildImplicitTypeRef()
            symbol = CfirConstructorSymbol(CallableId(Name.identifier("secondary")))
        }
        val expression = buildLiteralExpression {
            kind = CfirLiteralKind.INT; value = "1"; coneTypeOrNull = ConePrimitiveType(PrimitiveTypeKind.INT64)
        }
        assertThrows(IllegalStateException::class.java) {
            deepCopyOwnedGraphs(listOf(expression), CfirTypeRefRemapper { type, context ->
                context.remapDeclaration(primary); type
            }, attributes, mapOf(primary.symbol to secondary.symbol))
        }
    }

    /** 只创建正式 FuncArg 角色；default 的真实缺失用 null 表达，不添加错误表达式占位。 */
    private fun savedArgument(
        value: CfirExpression?, type: ConeCangJieType, isDefaultValue: Boolean = false, withInout: Boolean = false,
    ): CfirFunctionArgument = buildFunctionArgument {
        source = source("saved-argument"); coneTypeOrNull = type; this.value = value
        this.isDefaultValue = isDefaultValue; this.withInout = withInout
    }

    /** Spawn.future 是声明及其 initializer 的拥有边，沿用本测试既有声明 fixture。 */
    private fun savedField(name: String, type: ConeCangJieType, initializer: CfirExpression): CfirFieldVariable = buildFieldVariable {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true; status = CfirDeclarationStatusImpl(); isVar = false; isTypeImplicit = false
        returnTypeRef = buildResolvedTypeRef { coneType = type; typeUseOrigin = CfirTypeRefOrigin.COMPILER_ADDED }
        symbol = CfirFieldVariableSymbol(CallableId(Name.identifier(name))); this.name = Name.identifier(name); this.initializer = initializer
    }

    /** 对每个表达式检查 fresh 节点、具体 kind、原 source 身份及独立的应用后类型槽。 */
    private fun <E : CfirExpression> assertCopiedExpression(
        original: E, result: CfirOwnedGraphCopyResult, expectedType: ConeCangJieType?,
    ): E = result.getCopied(original).also { copied ->
        assertNotSame(original, copied)
        assertEquals(original::class, copied::class)
        assertSame(original.source, copied.source)
        assertSame(expectedType, copied.coneTypeOrNull)
        assertEquals(original.annotations.size, copied.annotations.size)
        if (original.annotations.isNotEmpty()) {
            assertNotSame(original.annotations, copied.annotations)
            original.annotations.forEachIndexed { index, annotation -> assertSame(result.getCopied(annotation), copied.annotations[index]) }
        }
    }

    private fun valueParameter(owner: CfirBasedSymbol<*>): CfirValueParameter = buildValueParameter {
        source = source("parameter"); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true; status = CfirDeclarationStatusImpl(); returnTypeRef = buildImplicitTypeRef()
        symbol = CfirValueParameterSymbol(CallableId(Name.identifier("parameter"))); containingDeclarationSymbol = owner
        name = Name.identifier("parameter"); isNamed = false; isTypeImplicit = true
    }

    private fun function(name: String, symbol: CfirNamedFunctionSymbol, body: CfirBlock): CfirNamedFunction = buildNamedFunction {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Source; this.attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true; status = CfirDeclarationStatusImpl(); returnTypeRef = buildImplicitTypeRef()
        this.symbol = symbol; this.name = Name.identifier(name); this.body = body; isMut = false
    }

    private fun literal(): CfirLiteralExpression = buildLiteralExpression { kind = CfirLiteralKind.INT; value = "1" }
    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
}
