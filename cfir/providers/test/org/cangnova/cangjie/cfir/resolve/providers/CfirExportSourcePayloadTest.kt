package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentStatus
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentView
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentViewEntry
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationResolveState
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirConstantValueExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExhaustivenessStatus
import org.cangnova.cangjie.cfir.expressions.buildArgumentListForErrorCall
import org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildMatchBranch
import org.cangnova.cangjie.cfir.expressions.builder.buildMatchExpression
import org.cangnova.cangjie.cfir.patterns.builder.buildConstPattern
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

/** 仅验证共享捕获工厂发布的产品合同，不推导诊断、SOURCE wire 标签或常量源语法。 */
@OptIn(CfirImplementationDetail::class)
class CfirExportSourcePayloadTest {
    @Test
    fun resolvedMappingValueRebindingIsVisibleWithUnchangedKeysAndOriginalList() {
        val fixture = Fixture()
        val function = fixture.function()
        val first = fixture.parameter(function.symbol, "first")
        val second = fixture.parameter(function.symbol, "second")
        val context = fixture.parameter(function.symbol, "context")
        function.replaceValueParameters(listOf(first, second, context))
        val argument = fixture.literal(11L)
        val contextArgument = fixture.literal(12L)
        val original = fixture.arguments(contextArgument, argument)
        val valueMapping = linkedMapOf<CfirExpression, CfirValueParameter>(argument to first)
        val allMapping = linkedMapOf<CfirExpression, CfirValueParameter>(contextArgument to context, argument to first)
        val resolved = buildResolvedArgumentList(original, valueMapping, allMapping)
        val before = resolved.payload<CfirExportSourcePayload.ResolvedArguments>()
        assertEquals(before, resolved.captureExportSourcePayload())
        assertSame(original, before.original)
        assertEquals(listOf(argument), before.arguments)
        assertSame(first, before.valueBindings.single().parameter)
        assertEquals(listOf(contextArgument, argument), before.allBindings.map { it.argument })
        assertEquals(listOf(context, first), before.allBindings.map { it.parameter })

        // 只改 value；两个映射的 key、顺序、来源及表达式类型全部保持不变。
        valueMapping[argument] = second
        allMapping[argument] = second
        val after = resolved.payload<CfirExportSourcePayload.ResolvedArguments>()
        assertSame(original, resolved.originalArgumentList)
        assertEquals(before.arguments, after.arguments)
        assertEquals(before.allBindings.map { it.argument }, after.allBindings.map { it.argument })
        assertSame(first, before.valueBindings.single().parameter)
        assertSame(first, before.allBindings.last().parameter)
        assertSame(second, after.valueBindings.single().parameter)
        assertSame(second, after.allBindings.last().parameter)
        assertNotEquals(before, after)
        assertReadOnly(before.arguments)
        assertReadOnly(before.valueBindings)
        assertReadOnly(before.allBindings)

        // 错误调用也保留全部原实参，而非仅保留成功绑定的 keys。
        val errorList = buildArgumentListForErrorCall(original, linkedMapOf(argument to first, contextArgument to null))
        val errorBefore = errorList.payload<CfirExportSourcePayload.ResolvedArguments>()
        errorList.mapping[argument] = second
        val errorAfter = errorList.payload<CfirExportSourcePayload.ResolvedArguments>()
        assertEquals(listOf(argument, contextArgument), errorBefore.arguments)
        assertEquals(errorBefore.arguments, errorAfter.arguments)
        assertSame(first, errorBefore.valueBindings.single().parameter)
        assertSame(second, errorAfter.valueBindings.single().parameter)
        assertEquals(errorBefore.valueBindings, errorBefore.allBindings)
        assertNotEquals(errorBefore, errorAfter)
    }

    @Test
    fun appliedReferenceRetainsFunctionValueRoleEvenWhenTheFunctionReturnsAFunction() {
        val fixture = Fixture()
        val resultType = ConeFunctionType(emptyList(), ConePrimitiveType.INT64)
        val function = fixture.function(resultType, CfirDeclarationOrigin.Library)
        val parameter = fixture.parameter(function.symbol, "input", origin = CfirDeclarationOrigin.Library)
        function.replaceValueParameters(listOf(parameter))
        val parameterTypes = mutableListOf<ConeCangJieType>(ConePrimitiveType.INT64)
        val source = TestSource("applied")
        val asValue = CfirResolvedAppliedCallableReference(source, function.name, function.symbol, resultType, parameterTypes, true,
            org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Absent, resultType)
        val asCall = CfirResolvedAppliedCallableReference(source, function.name, function.symbol, resultType, parameterTypes, false,
            org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Absent, resultType)
        val valuePayload = asValue.payload<CfirExportSourcePayload.AppliedReference>()
        val callPayload = asCall.payload<CfirExportSourcePayload.AppliedReference>()
        assertTrue(valuePayload.isFunctionValue)
        assertFalse(callPayload.isFunctionValue)
        assertTrue(valuePayload.isBound)
        assertSame(function.symbol, valuePayload.symbol)
        assertSame(function, valuePayload.target)
        assertSame(resultType, valuePayload.returnType)
        assertEquals(valuePayload.name, callPayload.name)
        assertSame(valuePayload.symbol, callPayload.symbol)
        assertEquals(valuePayload.isBound, callPayload.isBound)
        assertSame(valuePayload.target, callPayload.target)
        assertSame(valuePayload.returnType, callPayload.returnType)
        assertEquals(valuePayload.parameterTypes, callPayload.parameterTypes)
        assertNotEquals(valuePayload, callPayload)

        parameterTypes[0] = ConePrimitiveType.BOOLEAN
        assertEquals(listOf(ConePrimitiveType.INT64), valuePayload.parameterTypes)
        assertEquals(listOf(ConePrimitiveType.INT64), callPayload.parameterTypes)
        assertNotEquals(valuePayload, asValue.captureExportSourcePayload())
        assertReadOnly(valuePayload.parameterTypes)
    }

    @Test
    fun nestedConstantFieldAndEveryElementVectorAreFrozenAgainstMutableAliases() {
        val fixture = Fixture()
        val one = fixture.constant(1L)
        val two = fixture.constant(2L)
        val field = fixture.field("stored", ConePrimitiveType.INT64)
        val fields = linkedMapOf<CfirCallableSymbol<*>, CfirConstantValue>(field to one)
        val objectValue = CfirConstantValue.ObjectValue(fixture.ownerType, fields, 101L,
            mapOf(field to ConePrimitiveType.INT64))
        val tupleElements = mutableListOf<CfirConstantValue>(one, one)
        val tuple = CfirConstantValue.Tuple(ConeTupleType(listOf(one.type, one.type)), tupleElements)
        val varrayElements = mutableListOf<CfirConstantValue>(one, one)
        val varray = CfirConstantValue.VArray(ConeVArrayType(one.type, 2L), varrayElements)
        val enumSymbol = CfirClassSymbol(ClassId(FqName("payload"), Name.identifier("Choice")))
        val constructor = CfirEnumConstructorSymbol(CallableId(enumSymbol.classId, Name.identifier("Value")))
        val enumArguments = mutableListOf<CfirConstantValue>(one)
        val enumValue = CfirConstantValue.EnumValue(ConeEnumType(enumSymbol.toLookupTag()), constructor, enumArguments)
        val arrayElements = mutableListOf<CfirConstantValue>(enumValue)
        val arrayType = ConeClassLikeType(
            CfirClassSymbol(ClassId(FqName("std.core"), Name.identifier("Array"))).toLookupTag(),
            listOf(enumValue.type),
        )
        val array = CfirConstantValue.ArrayValue(arrayType, arrayElements)
        val rootElements = mutableListOf<CfirConstantValue>(objectValue, tuple, varray, enumValue, array)
        val root = CfirConstantValue.Tuple(ConeTupleType(rootElements.map { it.type }), rootElements)
        val expression = CfirConstantValueExpression(TestSource("computed"), root)
        val before = expression.payload<CfirExportSourcePayload.Constant>()
        val graph = before.value
        val rootNode = graph.node<CfirExportConstantNode.Tuple>(graph.root)
        val objectNode = graph.node<CfirExportConstantNode.Object>(rootNode.elements[0])
        val tupleNode = graph.node<CfirExportConstantNode.Tuple>(rootNode.elements[1])
        val varrayNode = graph.node<CfirExportConstantNode.VArray>(rootNode.elements[2])
        val enumNode = graph.node<CfirExportConstantNode.Enum>(rootNode.elements[3])
        val arrayNode = graph.node<CfirExportConstantNode.Array>(rootNode.elements[4])
        val oneId = objectNode.fields.single().value
        assertEquals(before, expression.captureExportSourcePayload())
        assertEquals(7, graph.nodes.size)
        assertSame(root.type, rootNode.type)
        assertSame(objectValue.type, objectNode.type)
        assertSame(tuple.type, tupleNode.type)
        assertSame(varray.type, varrayNode.type)
        assertSame(enumValue.type, enumNode.type)
        assertSame(array.type, arrayNode.type)
        assertSame(one.type, graph.node<CfirExportConstantNode.Primitive>(oneId).type)
        assertSame(field, objectNode.fields.single().symbol)
        assertEquals(101L, objectNode.objectIdentity)
        assertSame(constructor, enumNode.constructor)
        assertEquals(CfirLiteralKind.INT, graph.node<CfirExportConstantNode.Primitive>(oneId).kind)
        assertEquals(BigInteger.ONE, graph.node<CfirExportConstantNode.Primitive>(oneId).value)

        // 每次独立修改一个真实 backing，恢复后同一工厂必须再次得到原产品。
        val mutations = listOf<Pair<() -> Unit, () -> Unit>>(
            { fields[field] = two } to { fields[field] = one },
            { tupleElements[0] = two } to { tupleElements[0] = one },
            { varrayElements[0] = two } to { varrayElements[0] = one },
            { enumArguments[0] = two } to { enumArguments[0] = one },
            { arrayElements.clear() } to { arrayElements += enumValue },
            { rootElements.reverse() } to { rootElements.reverse() },
        )
        for ((mutate, restore) in mutations) {
            mutate()
            assertEquals(oneId, objectNode.fields.single().value)
            assertEquals(listOf(oneId, oneId), tupleNode.elements)
            assertEquals(listOf(oneId, oneId), varrayNode.elements)
            assertEquals(listOf(oneId), enumNode.arguments)
            assertEquals(listOf(enumNode.identity), arrayNode.elements)
            assertEquals(listOf(objectNode.identity, tupleNode.identity, varrayNode.identity, enumNode.identity, arrayNode.identity), rootNode.elements)
            assertNotEquals(before, expression.captureExportSourcePayload())
            restore()
            assertEquals(before, expression.captureExportSourcePayload())
        }
        listOf(graph.nodes, rootNode.elements, objectNode.fields, tupleNode.elements, varrayNode.elements, enumNode.arguments, arrayNode.elements)
            .forEach(::assertReadOnly)
    }

    @Test
    fun constantGraphKeepsDistinctObjectsReusesRepeatedReferencesAndClosesCycles() {
        val fixture = Fixture()
        val value = fixture.constant(7L)
        val field = fixture.field("value", value.type)
        val next = fixture.field("next", fixture.ownerType)
        val firstFields = linkedMapOf<CfirCallableSymbol<*>, CfirConstantValue>(field to value)
        val secondFields = linkedMapOf<CfirCallableSymbol<*>, CfirConstantValue>(field to value)
        val firstStorageTypes = linkedMapOf<CfirCallableSymbol<*>, ConeCangJieType>(field to value.type)
        val secondStorageTypes = linkedMapOf<CfirCallableSymbol<*>, ConeCangJieType>(field to value.type)
        val first = CfirConstantValue.ObjectValue(fixture.ownerType, firstFields, 201L, firstStorageTypes)
        val second = CfirConstantValue.ObjectValue(fixture.ownerType, secondFields, 202L, secondStorageTypes)
        val elements = mutableListOf<CfirConstantValue>(first, second, first)
        val expression = CfirConstantValueExpression(
            TestSource("shared-object-graph"), CfirConstantValue.Tuple(ConeTupleType(elements.map { it.type }), elements),
        )
        val before = expression.payload<CfirExportSourcePayload.Constant>()
        val root = before.value.node<CfirExportConstantNode.Tuple>(before.value.root)
        val firstNode = before.value.node<CfirExportConstantNode.Object>(root.elements[0])
        val secondNode = before.value.node<CfirExportConstantNode.Object>(root.elements[1])
        assertEquals(firstNode.fields, secondNode.fields)
        assertNotEquals(firstNode.identity, secondNode.identity)
        assertEquals(201L, firstNode.objectIdentity)
        assertEquals(202L, secondNode.objectIdentity)
        assertSame(root.elements[0], root.elements[2])
        assertEquals(4, before.value.nodes.size)

        // 环是值图回边，不沿 Object 的 equals/hashCode 递归，也不再次登记同一实例。
        firstFields[next] = second
        secondFields[next] = first
        firstStorageTypes[next] = fixture.ownerType
        secondStorageTypes[next] = fixture.ownerType
        val cyclic = assertDoesNotThrow<CfirExportSourcePayload.Constant> { expression.payload() }
        val cyclicRoot = cyclic.value.node<CfirExportConstantNode.Tuple>(cyclic.value.root)
        val cyclicFirst = cyclic.value.node<CfirExportConstantNode.Object>(cyclicRoot.elements[0])
        val cyclicSecond = cyclic.value.node<CfirExportConstantNode.Object>(cyclicRoot.elements[1])
        assertEquals(4, cyclic.value.nodes.size)
        assertEquals(cyclic.value.nodes.size, cyclic.value.nodes.map { it.identity }.toSet().size)
        assertSame(cyclicRoot.elements[0], cyclicRoot.elements[2])
        assertSame(cyclicSecond.identity, cyclicFirst.fields.single { it.symbol === next }.value)
        assertSame(cyclicFirst.identity, cyclicSecond.fields.single { it.symbol === next }.value)
        assertEquals(1, firstNode.fields.size)
        assertEquals(1, secondNode.fields.size)
        assertNotEquals(before, cyclic)
        assertDoesNotThrow { assertEquals(cyclic, expression.captureExportSourcePayload()) }
    }

    @Test
    fun annotationEntryScalarsAndMatchMissingCasesAreFrozenWithoutCopyingBorrowedNodes() {
        val fixture = Fixture()
        val function = fixture.function()
        val argument = fixture.literal(21L)
        val parameter = fixture.parameter(function.symbol, "value", defaultValue = argument)
        function.replaceValueParameters(listOf(parameter))
        val entrySource = TestSource("annotation-entry")
        val entry = CfirAnnotationArgumentViewEntry(
            0, Name.identifier("value"), null, parameter, CfirAnnotationArgumentStatus.RESOLVED, true, argument, entrySource,
        )
        val entries = mutableListOf(entry)
        val annotation = fixture.annotation(function.symbol, fixture.arguments(), CfirAnnotationArgumentView(entries))
        val before = annotation.payload<CfirExportSourcePayload.Annotation>()
        val view = requireNotNull(before.argumentView)
        val frozenEntry = view.entries.single()
        assertNotSame(annotation.argumentView, view)
        assertSame(parameter, frozenEntry.parameter)
        assertNull(frozenEntry.argument)
        assertSame(argument, frozenEntry.constantExpression)
        val span = requireNotNull(frozenEntry.span)
        assertEquals(entrySource.startOffset, span.startOffset)
        assertEquals(entrySource.endOffset, span.endOffset)
        assertSame(entrySource.kind, span.kind)
        assertNull(span.text)

        // 只替换 view 的标量，所有 borrowed CFIR 身份和 source 均保留。
        entries[0] = entry.copy(sourceOrder = 3, explicitName = null, status = CfirAnnotationArgumentStatus.UNRESOLVED, isDefaultOrigin = false)
        val after = annotation.payload<CfirExportSourcePayload.Annotation>()
        assertEquals(0, frozenEntry.sourceOrder)
        assertEquals(Name.identifier("value"), frozenEntry.explicitName)
        assertEquals(CfirAnnotationArgumentStatus.RESOLVED, frozenEntry.status)
        assertTrue(frozenEntry.isDefaultOrigin)
        val changedEntry = requireNotNull(after.argumentView).entries.single()
        assertEquals(3, changedEntry.sourceOrder)
        assertNull(changedEntry.explicitName)
        assertEquals(CfirAnnotationArgumentStatus.UNRESOLVED, changedEntry.status)
        assertFalse(changedEntry.isDefaultOrigin)
        assertSame(frozenEntry.parameter, changedEntry.parameter)
        assertSame(frozenEntry.constantExpression, changedEntry.constantExpression)
        assertEquals(frozenEntry.span, changedEntry.span)
        assertSame(before.typeRef, after.typeRef)
        assertSame(before.callee, after.callee)
        assertSame(before.argumentList, after.argumentList)
        assertEquals(before.arguments, after.arguments)
        assertEquals(before.argumentMapping, after.argumentMapping)
        assertSame(function, before.owner)
        assertSame(before.owner, after.owner)
        assertNotEquals(before, after)
        assertReadOnly(view.entries)

        val missing = mutableListOf("false")
        val branch = buildMatchBranch {
            source = TestSource("match-branch")
            coneTypeOrNull = ConePrimitiveType.INT64
            pattern = buildConstPattern { expression = fixture.boolean(true) }
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT64
                statements += argument
            }
        }
        val match = buildMatchExpression {
            source = TestSource("match")
            coneTypeOrNull = ConePrimitiveType.INT64
            subject = fixture.boolean(true)
            branches += branch
            exhaustiveness = CfirMatchExhaustivenessStatus.NonExhaustive(missing, CfirMatchExhaustivenessStatus.Source.BodyResolve)
        }
        val matchBefore = match.payload<CfirExportSourcePayload.Match>()
        val frozenStatus = matchBefore.exhaustiveness as CfirMatchExhaustivenessStatus.NonExhaustive
        missing.clear()
        val matchAfter = match.payload<CfirExportSourcePayload.Match>()
        assertEquals(listOf("false"), frozenStatus.missingCaseTexts)
        assertEquals(CfirMatchExhaustivenessStatus.Source.BodyResolve, frozenStatus.source)
        assertEquals(emptyList<String>(), (matchAfter.exhaustiveness as CfirMatchExhaustivenessStatus.NonExhaustive).missingCaseTexts)
        assertSame(matchBefore.subject, matchAfter.subject)
        assertSame(branch, matchAfter.branches.single())
        assertNotEquals(matchBefore, matchAfter)
        assertReadOnly(frozenStatus.missingCaseTexts)
    }

    @Test
    fun libraryThisAndAnnotationPreserveNonNullUnboundSymbolsWithoutThrowingOrBinding() {
        val fixture = Fixture()
        val librarySymbol = CfirClassSymbol(ClassId(FqName("payload.library"), Name.identifier("Header")))
        val annotationOwner = CfirNamedFunctionSymbol(CallableId(FqName("payload.library"), Name.identifier("annotated")))
        val annotation = fixture.annotation(annotationOwner, fixture.arguments())
        val library = fixture.libraryClass(librarySymbol, annotation)
        val reference = buildThisReference {
            source = TestSource("library-this", "payload.cjo")
            boundSymbol = CfirClassSymbol(ClassId(FqName("payload.library"), Name.identifier("UnmaterializedOwner")))
            isImplicit = true
            diagnostic = null
        }
        val thisSymbol = requireNotNull(reference.boundSymbol)
        assertSame(CfirDeclarationOrigin.Library, library.origin)
        assertSame(annotation, library.annotations.single())
        assertFalse(thisSymbol.isBound)
        assertFalse(annotationOwner.isBound)
        val thisPayload = assertDoesNotThrow<CfirExportSourcePayload.ThisReference> { reference.payload() }
        val annotationPayload = assertDoesNotThrow<CfirExportSourcePayload.Annotation> { annotation.payload() }
        assertSame(thisSymbol, thisPayload.symbol)
        assertFalse(thisPayload.isBound)
        assertNull(thisPayload.declaration)
        assertTrue(thisPayload.isImplicit)
        assertNull(thisPayload.diagnostic)
        assertSame(annotationOwner, annotationPayload.ownerSymbol)
        assertFalse(annotationPayload.ownerIsBound)
        assertNull(annotationPayload.owner)
        assertFalse(thisSymbol.isBound)
        assertFalse(annotationOwner.isBound)
        assertEquals(thisPayload, reference.captureExportSourcePayload())
        assertEquals(annotationPayload, annotation.captureExportSourcePayload())

        val missingSymbolReference = buildThisReference {
            boundSymbol = null
            isImplicit = true
            diagnostic = null
        }
        assertNotEquals(thisPayload, missingSymbolReference.captureExportSourcePayload())
        val boundReference = buildThisReference {
            boundSymbol = librarySymbol
            isImplicit = false
            diagnostic = null
        }
        val boundPayload = boundReference.payload<CfirExportSourcePayload.ThisReference>()
        assertTrue(boundPayload.isBound)
        assertSame(library, boundPayload.declaration)
        assertSame(librarySymbol, boundPayload.symbol)
        val boundAnnotation = fixture.annotation(librarySymbol, fixture.arguments())
        val boundAnnotationPayload = boundAnnotation.payload<CfirExportSourcePayload.Annotation>()
        assertTrue(boundAnnotationPayload.ownerIsBound)
        assertSame(librarySymbol, boundAnnotationPayload.ownerSymbol)
        assertSame(library, boundAnnotationPayload.owner)
        assertFalse(thisSymbol.isBound)
        assertFalse(annotationOwner.isBound)
    }

    private inline fun <reified T : CfirExportSourcePayload> CfirElement.payload(): T = captureExportSourcePayload() as T

    private inline fun <reified T : CfirExportConstantNode> CfirExportConstantGraph.node(identity: CfirExportConstantIdentity): T =
        nodes.single { it.identity == identity } as T

    private fun assertReadOnly(list: List<*>) {
        assertThrows(UnsupportedOperationException::class.java) { (list as MutableList<*>).clear() }
    }

    /** 使用现有 builder 的完整字段；Library header 不参与 SOURCE 发布准入。 */
    private class Fixture {
        private val session = object : CfirSession(Kind.Source) {}
        private val moduleData: CfirModuleData = CfirSourceModuleData(
            Name.identifier("source-payload-test"), emptyList(), emptyList(),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also { it.bindSession(session) }
        private val ownerSymbol = CfirClassSymbol(ClassId(FqName("payload"), Name.identifier("Stored")))
        val ownerType = ConeClassLikeType(ownerSymbol.toLookupTag())
        private val owner = buildClass {
            source = TestSource("Stored")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            scopeProvider = CfirCangJieScopeProvider()
            status = resolvedStatus()
            symbol = ownerSymbol
            name = ownerSymbol.classId.shortClassName
        }

        fun function(
            resultType: ConeCangJieType = ConePrimitiveType.INT64,
            origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
        ): CfirNamedFunction = buildNamedFunction {
            source = TestSource("work")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            this.origin = origin
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            name = Name.identifier("work")
            symbol = CfirNamedFunctionSymbol(CallableId(FqName("payload"), name))
            returnTypeRef = resolvedType(resultType)
            isMut = false
            if (origin == CfirDeclarationOrigin.Source) {
                body = buildBlock {
                    coneTypeOrNull = resultType
                    statements += literal(0L)
                }
            }
        }

        fun parameter(
            owner: CfirBasedSymbol<*>,
            name: String,
            defaultValue: CfirExpression? = null,
            origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
        ): CfirValueParameter = buildValueParameter {
            source = TestSource("parameter:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            this.origin = origin
            attributes = CfirDeclarationAttributes()
            isLocal = false
            isNamed = true
            this.name = Name.identifier(name)
            symbol = CfirValueParameterSymbol(CallableId(this.name))
            containingDeclarationSymbol = owner
            returnTypeRef = resolvedType(ConePrimitiveType.INT64)
            this.defaultValue = defaultValue
        }

        fun field(name: String, type: ConeCangJieType): CfirFieldVariableSymbol {
            val field = buildFieldVariable {
                source = TestSource("field:$name")
                moduleData = this@Fixture.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = resolvedStatus()
                dispatchReceiverType = ownerType
                this.name = Name.identifier(name)
                symbol = CfirFieldVariableSymbol(CallableId(ownerSymbol.classId, this.name))
                returnTypeRef = resolvedType(type)
                isVar = true
            }
            (owner.declarations as MutableList<CfirDeclaration>).add(field)
            return field.symbol
        }

        fun arguments(vararg arguments: CfirExpression): CfirArgumentList = buildArgumentList {
            source = TestSource("arguments")
            this.arguments += arguments
        }

        fun annotation(
            owner: CfirBasedSymbol<*>,
            arguments: CfirArgumentList,
            view: CfirAnnotationArgumentView? = null,
        ): CfirAnnotationCall = buildAnnotationCall {
            source = TestSource("annotation", "payload.cjo")
            typeRef = resolvedType(ownerType)
            argumentList = arguments
            calleeReference = buildNamedReference { name = Name.identifier("Stored") }
            containingDeclarationSymbol = owner
            argumentView = view
            annotationResolveState = CfirAnnotationResolveState.UNRESOLVED
        }

        fun libraryClass(symbol: CfirClassSymbol, annotation: CfirAnnotationCall): CfirClass = buildClass {
            source = TestSource("library-header", "payload.cjo")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            scopeProvider = CfirCangJieScopeProvider()
            status = resolvedStatus()
            this.symbol = symbol
            name = symbol.classId.shortClassName
            annotations += annotation
        }

        fun literal(value: Long): CfirLiteralExpression = buildLiteralExpression {
            source = TestSource("literal:$value")
            coneTypeOrNull = ConePrimitiveType.INT64
            kind = CfirLiteralKind.INT
            this.value = value
        }

        fun boolean(value: Boolean): CfirLiteralExpression = buildLiteralExpression {
            source = TestSource("boolean:$value")
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
            kind = CfirLiteralKind.BOOLEAN
            this.value = value
        }

        fun constant(value: Long): CfirConstantValue.Primitive =
            CfirConstantValue.Primitive(ConePrimitiveType.INT64, CfirLiteralKind.INT, BigInteger.valueOf(value))

        private fun resolvedType(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }

        private fun resolvedStatus(): CfirResolvedDeclarationStatus = buildResolvedDeclarationStatus {
            visibility = Visibilities.Public
            isVisibilityExplicit = true
            isModalityExplicit = false
            isAbstractExplicit = false
            isOverride = false
            isOperator = false
            isStatic = false
            isConst = false
            isMut = false
            isUnsafe = false
            isForeign = false
            isC = false
            isCommon = false
            isSpecific = false
            isCommonWithDefault = false
            isRedef = false
            isDefault = false
            isAbstract = false
            isOpen = false
            isSealed = false
            modality = Modality.FINAL
        }
    }

    private class TestSource(identity: String, path: String? = null) : CjBinarySourceElement(identity, path, identity)
}
