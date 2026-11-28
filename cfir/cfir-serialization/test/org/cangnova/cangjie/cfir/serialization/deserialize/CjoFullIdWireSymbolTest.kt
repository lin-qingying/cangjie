package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.test.cjoFixtureModuleDataProvider
import org.cangnova.cangjie.test.bindCjoFixture
import PackageFormat.ReferenceInfo
import PackageFormat.PropInfo
import PackageFormat.GenericTyInfo
import PackageFormat.FullId
import PackageFormat.SemaTy
import PackageFormat.SemaTyInfo
import PackageFormat.TypeKind
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.hasSerializedInitializer
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CjoPackageContextProvider
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** 使用官方 cjc 1.1.3 wire；目标身份以实际 FullId/record 断言，session 不注册 PSI symbol provider。 */
@OptIn(CfirSymbolProviderInternals::class)
class CjoFullIdWireSymbolTest {
    @Test
    fun currentConstRefAndNamedProviderShareTheWireLeaf() = fixture { f ->
        val context = f.context("wire.current")
        val reader = context.createDeclDeserializer()
        val root = assertIs<CfirPatternVariable>(reader.deserializeDecl(1))
        val reference = assertIs<CfirNamedAccessExpression>(root.initializer)
        val symbol = assertIs<CfirPatternBindingSymbol>(reader.deserializeSymbol(0))
        assertSame(symbol, assertIs<CfirResolvedNamedReference>(reference.calleeReference).resolvedSymbol)
        assertSame(symbol, f.provider.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single())
        assertSame(context.declCache.getValue(0).wireSymbol, symbol)
        assertSame(root, root.pattern.bindingVariables().single().patternTreeOwner)
        assertSame(root.initializer, root.pattern.bindingVariables().single().initializer)
        assertEquals(context.createTypeDeserializer().deserializeTypeFromField(context.pkg.allExprs(1)!!.type), reference.coneTypeOrNull)
    }

    @Test
    fun currentDisplayTextDoesNotSelectTheTarget() = fixture { f ->
        val context = f.context("wire.current")
        val info = context.pkg.allExprs(1)!!.info(ReferenceInfo()) as ReferenceInfo
        val spelling = info.referenceAsByteBuffer
        "other".toByteArray(Charsets.UTF_8).forEachIndexed { i, b -> spelling.put(spelling.position() + i, b) }
        val root = assertIs<CfirPatternVariable>(context.createDeclDeserializer().deserializeDecl(1))
        val reference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(root.initializer).calleeReference)
        assertEquals("other", reference.name.asString())
        assertSame(context.declCache.getValue(0).wireSymbol, reference.resolvedSymbol)
    }

    @Test
    fun importedAliasUsesExportIdAndTheNamedProvidersRecord() = fixture { f ->
        val context = f.context("wire.imported")
        val root = assertIs<CfirPatternVariable>(context.createDeclDeserializer().deserializeDecl(0))
        val access = assertIs<CfirNamedAccessExpression>(root.initializer)
        val reference = assertIs<CfirResolvedNamedReference>(access.calleeReference)
        assertEquals("visibleAlias", reference.name.asString())
        assertEquals("first", assertIs<CfirPatternBindingSymbol>(reference.resolvedSymbol).name.asString())
        assertSame(f.context("wire.current").declCache.getValue(0).wireSymbol, reference.resolvedSymbol)
        assertSame(reference.resolvedSymbol, f.provider.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single())
        val wire = context.pkg.allExprs(0)!!.info(ReferenceInfo()) as ReferenceInfo
        assertEquals("_CN12wire.current5firstE", wire.target!!.decl)
        assertEquals(context.createTypeDeserializer().deserializeTypeFromField(context.pkg.allExprs(0)!!.type), access.coneTypeOrNull)
    }

    @Test
    fun modulesDoNotShareWireSymbolsEvenForTheSameSnapshot() = fixture { f ->
        val first = f.context("wire.current").createDeclDeserializer().deserializeSymbol(0)
        val otherModule = Module()
        val otherBinding = bindCjoFixture(otherModule, f.manager, setOf(f.directory, f.sdk))
        val snapshot = assertNotNull(f.manager.loadPackageSnapshot("wire.current"))
        val other = CfirDeserializationContext(snapshot.pkg, snapshot.header, otherModule, f.manager,
            f.manager.deserializedSymbolLookups.forBinding(otherBinding, otherModule, CjoDeserializationMode.FULL), snapshot.sourcePath)
        assertNotSame(first, other.createDeclDeserializer().deserializeSymbol(0))
        assertSame(otherModule, other.createDeclDeserializer().deserializeSymbol(0)!!.cfir.moduleData)
    }

    @Test
    fun enumColdMemberAndOwnerFirstOrdersUseTheSameCanonicalFamily() {
        for (ownerFirst in listOf(false, true)) fixture { f ->
            val context = f.context("wire.current")
            val reader = context.createDeclDeserializer()
            if (ownerFirst) assertIs<CfirEnum>(reader.deserializeDecl(3))
            val member = assertIs<CfirEnumConstructorSymbol>(reader.deserializeSymbol(4))
            val owner = assertIs<CfirEnum>(reader.deserializeDecl(3))
            assertSame(member, owner.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == "Ready" }.symbol)
            assertEquals(owner.symbol.classId, member.callableId.classId)
            val root = assertIs<CfirPatternVariable>(reader.deserializeDecl(2))
            val access = assertIs<CfirNamedAccessExpression>(root.initializer)
            assertSame(member, assertIs<CfirResolvedNamedReference>(access.calleeReference).resolvedSymbol)
            assertEquals(context.createTypeDeserializer().deserializeTypeFromField(context.pkg.allExprs(2)!!.type), access.coneTypeOrNull)
            assertSame(member, reader.deserializeSymbol(4))
        }
    }

    @Test
    fun enumDisplayCanNameOneMemberWhileFullIdTargetsAnother() = fixture { f ->
        val context = f.context("wire.current")
        val info = context.pkg.allExprs(2)!!.info(ReferenceInfo()) as ReferenceInfo
        assertTrue(info.target!!.mutateIndex(6u))
        val reader = context.createDeclDeserializer()
        val root = assertIs<CfirPatternVariable>(reader.deserializeDecl(2))
        val reference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(root.initializer).calleeReference)
        assertEquals("Ready", reference.name.asString())
        assertSame(reader.deserializeSymbol(5), reference.resolvedSymbol)
        assertNotSame(reader.deserializeSymbol(4), reference.resolvedSymbol)
    }

    @Test
    fun boundHeaderReentryDoesNotPublishAHalfCompletedDeclaration() = fixture { f ->
        val context = f.context("wire.current")
        val reader = context.createDeclDeserializer()
        var checked = false
        val symbol = context.materialization.read {
            val header = assertNotNull(reader.deserializeSymbolForReference(1))
            assertTrue(header.isBound)
            assertIs<CjoDeclarationCacheEntry.Header>(context.declarationEntries[1])
            assertEquals(CfirResolvePhase.TYPES, header.cfir.resolvePhase)
            context.materialization.postCompute {
                assertSame(header, reader.deserializeSymbolForReference(1))
                assertTrue(header.isBound)
                assertNull(context.declCache[1])
                assertFailsWith<IllegalStateException> { reader.deserializeDecl(1) }
                checked = true
            }
            header
        }
        assertTrue(checked)
        assertSame(symbol, reader.deserializeSymbol(1))
        assertEquals(CfirResolvePhase.BODY_RESOLVE, symbol.cfir.resolvePhase)
        assertIs<CjoDeclarationCacheEntry.Ready>(context.declarationEntries[1])
        assertIs<CfirNamedAccessExpression>(assertIs<CfirPatternVariable>(reader.deserializeDecl(1)).initializer)
    }

    @Test
    fun signaturesAndFullReadersAreIsolatedInBothOrdersAndAcrossImports() {
        for (signatureFirst in listOf(false, true)) fixture { f ->
            val full = f.context("wire.imported")
            val signatures = full.withMode(CjoDeserializationMode.SIGNATURE_ONLY)
            val order = if (signatureFirst) listOf(signatures, full) else listOf(full, signatures)
            order.forEach { assertIs<CfirPatternVariable>(it.createDeclDeserializer().deserializeDecl(0)) }
            val fullRecord = full.declCache.getValue(0)
            val signatureRecord = signatures.declCache.getValue(0)
            assertNotSame(fullRecord.wireSymbol, signatureRecord.wireSymbol)
            assertIs<CfirNamedAccessExpression>(assertIs<CfirPatternVariable>(fullRecord.declarationRoot).initializer)
            assertNull(assertIs<CfirPatternVariable>(signatureRecord.declarationRoot).initializer)
            assertTrue(assertIs<CfirPatternBindingSymbol>(signatureRecord.wireSymbol).cfir.hasSerializedInitializer)
            val targetFull = f.context("wire.current")
            val targetSignatures = targetFull.withMode(CjoDeserializationMode.SIGNATURE_ONLY)
            val stub = CfirDeclDeserializer.forCompiledFileStub(targetFull, targetFull.createTypeDeserializer())
            assertIs<CfirPatternVariable>(stub.deserializeDecl(0))
            assertNull(assertIs<CfirPatternVariable>(targetSignatures.declCache.getValue(0).declarationRoot).initializer)
            assertNotSame(targetFull.declCache.getValue(0).wireSymbol, targetSignatures.declCache.getValue(0).wireSymbol)
            assertSame(targetSignatures.declCache.getValue(0).wireSymbol, stub.deserializeSymbol(0))
        }
    }

    @Test
    fun concurrentColdReadersWaitForCompletionAndShareOneRecord() = fixture { f ->
        val context = f.context("wire.current")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        val callers = Executors.newFixedThreadPool(2)
        try {
            val first = callers.submit<org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> {
                context.materialization.read {
                    val header = assertNotNull(context.createDeclDeserializer().deserializeSymbolForReference(1))
                    context.materialization.postCompute {
                        entered.countDown()
                        assertTrue(release.await(10, TimeUnit.SECONDS))
                    }
                    header
                }
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            val second = callers.submit<org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> {
                secondStarted.countDown()
                assertNotNull(f.context("wire.current").createDeclDeserializer().deserializeSymbol(1))
            }
            assertTrue(secondStarted.await(10, TimeUnit.SECONDS))
            assertFailsWith<java.util.concurrent.TimeoutException> { second.get(100, TimeUnit.MILLISECONDS) }
            release.countDown()
            assertSame(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))
            assertIs<CfirNamedAccessExpression>(assertIs<CfirPatternVariable>(context.declCache.getValue(1).declarationRoot).initializer)
        } finally { release.countDown(); callers.shutdownNow() }
    }

    @Test
    fun invalidFullIdRetainsFailureAndDoesNotPoisonTheSignatureView() = fixture { f ->
        val context = f.context("wire.current")
        val info = context.pkg.allExprs(1)!!.info(ReferenceInfo()) as ReferenceInfo
        assertTrue(info.target!!.mutateIndex(999u))
        val reader = context.createDeclDeserializer()
        assertFailsWith<IllegalStateException> { reader.deserializeDecl(1) }
        val failed = assertIs<CjoDeclarationCacheEntry.Failed>(context.declarationEntries[1])
        assertSame(failed.failure, assertFailsWith<CjoDeclarationDeserializationException> { reader.deserializeSymbol(1) })
        assertNull(context.declCache[1])
        val signatures = context.withMode(CjoDeserializationMode.SIGNATURE_ONLY)
        assertNull(assertIs<CfirPatternVariable>(signatures.createDeclDeserializer().deserializeDecl(1)).initializer)
    }

    @Test
    fun officialAnnotationAndMemberFamiliesUseExactRecordsWithoutAPsiProvider() = fixture { f ->
        val context = f.context("wire.reentry")
        val reader = context.createDeclDeserializer()
        val holderIndex = context.header.topLevelClassifierNameToIndices.getValue("Holder").single()
        // 从 wire property getter index 冷读，不由函数名重造 accessor 身份。
        val propertyIndex = (0 until context.pkg.allDeclsLength).single { context.pkg.allDecls(it)!!.identifier == "value" }
        val propertyInfo = context.pkg.allDecls(propertyIndex)!!.info(PropInfo()) as PropInfo
        val getterIndex = propertyInfo.getters(0).toInt() - 1
        val getterSymbol = assertNotNull(reader.deserializeSymbol(getterIndex))
        val holder = assertIs<CfirClass>(reader.deserializeDecl(holderIndex))
        val property = holder.declarations.filterIsInstance<CfirProperty>().single()
        assertSame(getterSymbol, property.getter!!.symbol)
        assertSame(property.symbol, property.getter!!.propertySymbol)
        assertSame(property.symbol, reader.deserializeSymbol(propertyIndex))
        val fields = holder.declarations.filterIsInstance<CfirFieldVariable>()
        assertEquals(setOf("member", "shared"), fields.map { it.name.asString() }.toSet())
        assertTrue(fields.single { it.name.asString() == "shared" }.status.isStatic)
        assertTrue(fields.all { it.symbol.callableId.classId == holder.symbol.classId })
        val markIndex = context.header.topLevelClassifierNameToIndices.getValue("Mark").single()
        var reentered = false
        context.materialization.read {
            val markSymbol = assertNotNull(reader.deserializeSymbolForReference(markIndex))
            context.materialization.postCompute {
                assertSame(markSymbol, reader.deserializeSymbolForReference(markIndex))
                reentered = true
            }
        }
        assertTrue(reentered)
        val mark = assertIs<CfirClass>(reader.deserializeDecl(markIndex))
        val annotation = assertNotNull(mark.annotations.singleOrNull())
        val argument = assertIs<CfirNamedArgumentExpression>(annotation.arguments.single())
        val array = assertIs<CfirArrayLiteral>(argument.expression)
        val reference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(array.elements.single()).calleeReference)
        assertIs<CfirEnumConstructorSymbol>(reference.resolvedSymbol)
        assertSame(reference.resolvedSymbol, context.importedContext("std.core").createDeclDeserializer()
            .deserializeSymbol(context.importedContext("std.core").header.fullIdReferenceKeyToIndex.getValue("_CNat14AnnotationKind4TypeKE")))
    }

    @Test
    fun coldGenericTypeBuildsTheActualOwnerAndFBoundInEachMode() = fixture { f ->
        val symbols = CjoDeserializationMode.entries.map { mode ->
            val context = f.context("wire.generic").withMode(mode)
            val type = assertIs<ConeTypeParameterType>(context.createTypeDeserializer().deserializeType(3))
            val parameter = assertIs<CfirTypeParameterSymbol>(context.createDeclDeserializer().deserializeSymbol(3))
            val owner = assertIs<CfirClass>(context.createDeclDeserializer().deserializeDecl(2))
            assertSame(parameter, type.lookupTag.typeParameterSymbol)
            assertSame(owner.symbol, parameter.cfir.containingDeclarationSymbol)
            assertTrue(parameter.isBound && owner.symbol.isBound)
            val bound = assertIs<ConeClassLikeType>(parameter.cfir.bounds.single().coneType)
            assertEquals("Link", bound.classId.shortClassName.asString())
            assertSame(parameter, assertIs<ConeTypeParameterType>(bound.typeArguments.single().type).lookupTag.typeParameterSymbol)
            assertSame(context.declCache.getValue(3).wireSymbol, parameter)
            assertSame(type, context.createTypeDeserializer().deserializeType(3))
            parameter
        }
        assertNotSame(symbols[0], symbols[1])
    }

    @Test
    fun coldFunctionGenericSignatureUsesOneDeclaredTypeParameter() = fixture { f ->
        val context = f.context("wire.generic")
        val type = assertIs<ConeTypeParameterType>(context.createTypeDeserializer().deserializeType(8))
        val parameter = assertIs<CfirTypeParameterSymbol>(context.createDeclDeserializer().deserializeSymbol(8))
        val function = assertIs<CfirNamedFunction>(context.createDeclDeserializer().deserializeDecl(7))
        assertSame(function.symbol, parameter.cfir.containingDeclarationSymbol)
        assertSame(parameter, type.lookupTag.typeParameterSymbol)
        assertSame(parameter, assertIs<ConeTypeParameterType>(function.returnTypeRef.coneType).lookupTag.typeParameterSymbol)
        assertSame(parameter, assertIs<ConeTypeParameterType>(function.valueParameters.single().returnTypeRef.coneType).lookupTag.typeParameterSymbol)
    }

    @Test
    fun coldCompositeTypeAndItsSelfBoundConvergeOnTheSameParameter() = fixture { f ->
        val context = f.context("wire.generic")
        val type = assertIs<ConeClassLikeType>(context.createTypeDeserializer().deserializeType(0))
        val parameter = assertIs<CfirTypeParameterSymbol>(context.createDeclDeserializer().deserializeSymbol(1))
        assertSame(parameter, assertIs<ConeTypeParameterType>(type.typeArguments.single().type).lookupTag.typeParameterSymbol)
        assertSame(type, parameter.cfir.bounds.single().coneType)
    }

    @Test
    fun importedGenericFullIdColdReadUsesTheExportedParameterRecord() = fixture { f ->
        for (mode in CjoDeserializationMode.entries) {
            val context = importedGenericContext(f, "_CN12wire.generic4LinkIG_E_TG1T", mode)
            val type = assertIs<ConeTypeParameterType>(context.createTypeDeserializer().deserializeType(0))
            val target = f.context("wire.generic").withMode(mode)
            val parameter = assertIs<CfirTypeParameterSymbol>(target.createDeclDeserializer().deserializeSymbol(1))
            val owner = assertIs<CfirInterface>(target.createDeclDeserializer().deserializeDecl(0))
            assertSame(parameter, type.lookupTag.typeParameterSymbol)
            assertSame(owner.symbol, parameter.cfir.containingDeclarationSymbol)
            assertSame(target.declCache.getValue(1).wireSymbol, parameter)
            val selfBound = assertIs<ConeClassLikeType>(parameter.cfir.bounds.single().coneType)
            assertSame(parameter, assertIs<ConeTypeParameterType>(selfBound.typeArguments.single().type).lookupTag.typeParameterSymbol)
        }
    }

    @Test
    fun genericTypeCacheHitCannotEscapeAnActiveOwnerCompletion() = fixture { f ->
        val context = f.context("wire.generic")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        val callers = Executors.newFixedThreadPool(2)
        try {
            val first = callers.submit<CfirTypeParameterSymbol> {
                context.materialization.read {
                    val symbol = assertIs<CfirTypeParameterSymbol>(context.createDeclDeserializer().deserializeSymbolForReference(1))
                    assertNotNull(context.typeCache[1])
                    context.materialization.postCompute {
                        entered.countDown()
                        assertTrue(release.await(10, TimeUnit.SECONDS))
                    }
                    symbol
                }
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            val second = callers.submit<ConeTypeParameterType> {
                secondStarted.countDown()
                assertIs<ConeTypeParameterType>(context.createTypeDeserializer().deserializeType(1))
            }
            assertTrue(secondStarted.await(10, TimeUnit.SECONDS))
            assertFailsWith<java.util.concurrent.TimeoutException> { second.get(100, TimeUnit.MILLISECONDS) }
            release.countDown()
            assertSame(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS).lookupTag.typeParameterSymbol)
            assertIs<CjoDeclarationCacheEntry.Ready>(context.declarationEntries[1])
        } finally { release.countDown(); callers.shutdownNow() }
    }

    @Test
    fun genericTargetsRejectWrongFamiliesAndIdentifierAliases() = fixture { f ->
        val context = f.context("wire.generic")
        val info = context.pkg.allTypes(3)!!.info(GenericTyInfo()) as GenericTyInfo
        assertTrue(info.declPtr!!.mutateIndex(3u)) // Box，不是 GenericParamDecl。
        val type = assertIs<ConeErrorType>(context.createTypeDeserializer().deserializeType(3))
        assertTrue(type.diagnostic.reason.contains("not a GenericParamDecl"))
        assertTrue(context.declarationEntries.isEmpty())
        val imported = importedGenericContext(f, "T", CjoDeserializationMode.FULL)
        assertIs<ConeErrorType>(imported.createTypeDeserializer().deserializeType(0))
    }

    @Test
    fun distinctManagersRouteImportedAliasToTheNamedTargetInBothOrdersAndModes() {
        for (namedFirst in listOf(false, true)) fixture { f ->
            val split = split(f)
            for (mode in CjoDeserializationMode.entries) {
                val target = assertNotNull(split.target.getOwnPackageContext("wire.current", mode))
                val named = target.cjoManager.deserializedSymbolLookups.forBinding(target.binding, split.targetModule, mode)
                val before = if (namedFirst) named.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single() else null
                val consumer = assertNotNull(split.consumer.getOwnPackageContext("wire.imported", mode))
                val wire = consumer.pkg.allExprs(0)!!.info(ReferenceInfo()) as ReferenceInfo
                val symbol = consumer.fullIdResolver.resolveSymbol(wire.target!!)
                assertSame(target, consumer.importedContext("wire.current"))
                assertSame(split.targetModule, symbol.cfir.moduleData)
                assertNotSame(consumer.moduleData, symbol.cfir.moduleData)
                assertSame(target.declCache.getValue(0).wireSymbol, symbol)
                assertSame(symbol, before ?: named.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single())
                if (mode == CjoDeserializationMode.FULL) {
                    val root = assertIs<CfirPatternVariable>(consumer.createDeclDeserializer().deserializeDecl(0))
                    val reference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(root.initializer).calleeReference)
                    assertEquals("visibleAlias", reference.name.asString())
                    assertSame(symbol, reference.resolvedSymbol)
                }
                assertFalse(split.consumer.hasPackage(FqName("wire.current")))
                assertFalse(split.consumer.hasPackage(FqName("std.core")))
            }
        }
    }

    @Test
    fun separateSdkManagerSuppliesTheActualAnnotationConstructorRecord() = fixture { f ->
        val split = split(f)
        val consumer = assertNotNull(split.consumer.getOwnPackageContext("wire.reentry", CjoDeserializationMode.FULL))
        val markIndex = consumer.header.topLevelClassifierNameToIndices.getValue("Mark").single()
        val mark = assertIs<CfirClass>(consumer.createDeclDeserializer().deserializeDecl(markIndex))
        val array = assertIs<CfirArrayLiteral>(assertIs<CfirNamedArgumentExpression>(mark.annotations.single().arguments.single()).expression)
        val reference = assertIs<CfirResolvedNamedReference>(assertIs<CfirNamedAccessExpression>(array.elements.single()).calleeReference)
        val sdk = assertNotNull(split.sdk.getOwnPackageContext("std.core", CjoDeserializationMode.FULL))
        val enum = assertIs<CfirEnum>(split.sdk.getClassLikeSymbolByClassId(
            org.cangnova.cangjie.name.ClassId(FqName("std.core"), Name.identifier("AnnotationKind")))!!.cfir)
        val named = enum.declarations.filterIsInstance<CfirEnumConstructor>().single { it.name.asString() == "Type" }.symbol
        assertSame(named, reference.resolvedSymbol)
        assertSame(split.sdkModule, named.cfir.moduleData)
        assertSame(sdk, consumer.importedContext("std.core"))
        val index = sdk.header.fullIdReferenceKeyToIndex.getValue("_CNat14AnnotationKind4TypeKE")
        assertSame(sdk.declCache.getValue(index).wireSymbol, named)
    }

    @Test
    fun separateLibraryGenericRoutePreservesOwnerFBoundAndMode() = fixture { f ->
        val split = split(f)
        val symbols = CjoDeserializationMode.entries.map { mode ->
            val consumer = importedGenericContext(f, "_CN12wire.generic4LinkIG_E_TG1T", mode,
                split.consumer, split.consumerModule, split.consumerManager)
            val type = assertIs<ConeTypeParameterType>(consumer.createTypeDeserializer().deserializeType(0))
            val target = assertNotNull(split.target.getOwnPackageContext("wire.generic", mode))
            val parameter = assertIs<CfirTypeParameterSymbol>(target.createDeclDeserializer().deserializeSymbol(1))
            val owner = assertIs<CfirInterface>(target.createDeclDeserializer().deserializeDecl(0))
            assertSame(parameter, type.lookupTag.typeParameterSymbol)
            assertSame(owner.symbol, parameter.cfir.containingDeclarationSymbol)
            assertSame(split.targetModule, parameter.cfir.moduleData)
            assertSame(parameter, target.declCache.getValue(1).wireSymbol)
            assertSame(parameter, assertIs<ConeTypeParameterType>(
                assertIs<ConeClassLikeType>(parameter.cfir.bounds.single().coneType).typeArguments.single().type).lookupTag.typeParameterSymbol)
            assertSame(target, consumer.importedContext("wire.generic"))
            parameter
        }
        assertNotSame(symbols[0], symbols[1])
    }

    @Test
    fun importedModuleDataUsesTheTargetsExactSourcePath() = fixture { f ->
        val targetModule = Module(session = f.module.session)
        assertNotSame(f.module, targetModule)
        assertSame(f.module.session, targetModule.session)
        val ownerPath = f.directory.resolve("wire.current.cjo").toAbsolutePath().normalize()
        val mapping = MultipleModuleDataProvider(linkedMapOf(
            f.module to LibraryPathFilter.LibraryList(setOf(f.directory.resolve("wire.imported.cjo").toAbsolutePath().normalize())),
            targetModule to LibraryPathFilter.LibraryList(setOf(ownerPath))),
            f.module)
        val manager = CjoManager(CjoSearchPath.forRoots(emptyList(), listOf(f.directory.toFile())))
        val provider = CfirDeserializedSymbolProvider(f.module.session, manager, CfirCangJieScopeProvider(), f.module,
            moduleDataProvider = mapping)
        val consumer = assertNotNull(provider.getOwnPackageContext("wire.imported", CjoDeserializationMode.FULL))
        val wire = consumer.pkg.allExprs(0)!!.info(ReferenceInfo()) as ReferenceInfo
        val symbol = consumer.fullIdResolver.resolveSymbol(wire.target!!)
        assertSame(targetModule, symbol.cfir.moduleData)
        assertEquals(ownerPath, consumer.importedContext("wire.current").sourcePath)
        assertSame(symbol, provider.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single())
    }

    @Test
    fun missingTargetOwnerDoesNotUseTheConsumerDependencySnapshot() = fixture { f ->
        val provider = CfirDeserializedSymbolProvider(f.module.session, f.manager, CfirCangJieScopeProvider(), f.module,
            moduleDataProvider = MultipleModuleDataProvider(mapOf(
                f.module to LibraryPathFilter.LibraryList(setOf(f.directory.resolve("wire.imported.cjo").toAbsolutePath().normalize()))),
                f.module))
        val context = assertNotNull(provider.getOwnPackageContext("wire.imported", CjoDeserializationMode.FULL))
        assertFailsWith<IllegalStateException> { context.importedContext("wire.current") }
        assertFalse(context.declCache.containsKey(0))
    }

    /** 真实 CJO 来源位于登记目录内时，named 与 FullId 仍使用该目录所属的同一模块。 */
    @Test
    fun registeredDirectoryOwnsItsActualPackageFiles() = fixture { f ->
        val mapping = MultipleModuleDataProvider(mapOf(
            f.module to LibraryPathFilter.LibraryList(setOf(f.directory))), f.module)
        val provider = CfirDeserializedSymbolProvider(f.module.session, f.manager, CfirCangJieScopeProvider(), f.module,
            moduleDataProvider = mapping)
        val imported = assertNotNull(provider.getOwnPackageContext("wire.imported", CjoDeserializationMode.FULL))
        val target = imported.importedContext("wire.current")
        assertSame(f.module, imported.moduleData)
        assertSame(f.module, target.moduleData)
        assertEquals(f.directory.resolve("wire.current.cjo").toAbsolutePath().normalize(), target.sourcePath)
        assertEquals(setOf(f.directory.toAbsolutePath().normalize()), mapping.getModuleDataPaths(f.module))
    }

    /** 相对登记与绝对来源必须表示同一 root，不能要求来源与目录字符串相等。 */
    @Test
    fun relativeLibraryRootOwnsAbsoluteAndRelativeSources() = fixture { f ->
        val root = Path.of("registered-binary-root")
        val source = root.resolve("std").resolve("sample.cjo")
        val mapping = MultipleModuleDataProvider(mapOf(
            f.module to LibraryPathFilter.LibraryList(setOf(root))), f.module)
        assertSame(f.module, mapping.getModuleData(source))
        assertSame(f.module, mapping.getModuleData(source.toAbsolutePath().normalize()))
        assertEquals(setOf(root.toAbsolutePath().normalize()), mapping.getModuleDataPaths(f.module))
    }

    /** 相邻目录和未登记文件不属于这个 root；依赖列表不存在隐式 owner。 */
    @Test
    fun neighboringAndUnregisteredSourcesHaveNoOwner() = fixture { f ->
        val root = Path.of("registered-binary-root")
        val mapping = MultipleModuleDataProvider(mapOf(
            f.module to LibraryPathFilter.LibraryList(setOf(root))), f.module)
        assertEquals(null, mapping.getModuleData(Path.of("registered-binary-root-neighbor", "sample.cjo")))
        assertEquals(null, mapping.getModuleData(Path.of("other-binary-root", "sample.cjo")))
        assertEquals(null, mapping.getModuleData(null))
    }

    /** 精确 common-part 文件在普通库目录中时，它的独立 module 身份不能被插入顺序吞掉。 */
    @Test
    fun exactFileOwnerInsideLibraryRootIsIndependentOfRegistrationOrder() = fixture { f ->
        val target = Module()
        val file = f.directory.resolve("wire.current.cjo")
        val broad: Pair<CfirModuleData, LibraryPathFilter> = f.module to LibraryPathFilter.LibraryList(setOf(f.directory))
        val exact: Pair<CfirModuleData, LibraryPathFilter> = target to LibraryPathFilter.LibraryList(setOf(file))
        for (entries in listOf(linkedMapOf(broad, exact), linkedMapOf(exact, broad))) {
            val mapping = MultipleModuleDataProvider(entries, f.module)
            assertSame(target, mapping.getModuleData(file))
            assertSame(f.module, mapping.getModuleData(f.directory.resolve("wire.imported.cjo")))
        }
    }

    /** 同一个标准化 root 被两模块认领必须拒绝，不能靠首个条目或模块名字消歧。 */
    @Test
    fun competingCanonicalLibraryRootsRejectAmbiguousOwnership() = fixture { f ->
        val other = Module()
        val root = f.directory.toAbsolutePath().normalize()
        val mapping = MultipleModuleDataProvider(linkedMapOf(
            f.module to LibraryPathFilter.LibraryList(setOf(root)),
            other to LibraryPathFilter.LibraryList(setOf(root.resolve(".")))), f.module)
        assertFailsWith<IllegalStateException> { mapping.getModuleData(root.resolve("wire.current.cjo")) }
    }

    @Test
    fun crossModuleReentryAndColdNamedReadShareOneCompletionTransaction() = fixture { f ->
        val split = split(f)
        val consumer = assertNotNull(split.consumer.getOwnPackageContext("wire.imported", CjoDeserializationMode.FULL))
        val target = assertNotNull(split.target.getOwnPackageContext("wire.current", CjoDeserializationMode.FULL))
        assertSame(consumer.materialization, target.materialization)
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        val callers = Executors.newFixedThreadPool(2)
        try {
            val first = callers.submit<org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> {
                consumer.materialization.read {
                    val a = assertNotNull(consumer.createDeclDeserializer().deserializeSymbolForReference(0))
                    val b = target.materialization.read {
                        val result = assertNotNull(target.createDeclDeserializer().deserializeSymbolForReference(0))
                        assertSame(a, consumer.createDeclDeserializer().deserializeSymbolForReference(0))
                        assertNull(consumer.declCache[0])
                        assertNull(target.declCache[0])
                        result
                    }
                    consumer.materialization.postCompute {
                        entered.countDown()
                        assertTrue(release.await(10, TimeUnit.SECONDS))
                    }
                    b
                }
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            val second = callers.submit<org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>> {
                started.countDown()
                split.target.getTopLevelCallableSymbols(FqName("wire.current"), Name.identifier("first")).single()
            }
            assertTrue(started.await(10, TimeUnit.SECONDS))
            assertFailsWith<java.util.concurrent.TimeoutException> { second.get(100, TimeUnit.MILLISECONDS) }
            release.countDown()
            assertSame(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS))
            assertIs<CjoDeclarationCacheEntry.Ready>(consumer.declarationEntries[0])
            assertIs<CjoDeclarationCacheEntry.Ready>(target.declarationEntries[0])
        } finally { release.countDown(); callers.shutdownNow() }
    }

    @Test
    fun missingTypeVectorClearsTheRecursionGuardOnEveryRead() = fixture { f ->
        val builder = FlatBufferBuilder()
        val name = builder.createString("wire.no.types")
        PackageFormat.Package.startPackage(builder)
        PackageFormat.Package.addFullPkgName(builder, name)
        builder.finish(PackageFormat.Package.endPackage(builder))
        val pkg = PackageFormat.Package.getRootAsPackage(builder.dataBuffer())
        val context = CfirDeserializationContext(pkg, CjoPackageHeader.fromPackage(pkg), f.module, f.manager,
            f.manager.deserializedSymbolLookups.forBinding(f.provider.deserializationBinding, f.module, CjoDeserializationMode.FULL))
        val reader = context.createTypeDeserializer()
        repeat(2) {
            val error = assertIs<ConeErrorType>(reader.deserializeType(0))
            assertEquals("Cannot read type index 0", error.diagnostic.reason)
        }
        assertTrue(context.typeCache.isEmpty())
    }

    @Test
    fun throwingTypeVectorReadClearsTheRecursionGuardAndReleasesTheLock() = fixture { f ->
        val context = f.context("wire.current")
        val reader = context.createTypeDeserializer()
        val first = assertFailsWith<IndexOutOfBoundsException> { reader.deserializeType(1000000) }
        val second = assertFailsWith<IndexOutOfBoundsException> { reader.deserializeType(1000000) }
        assertEquals(first.message, second.message)
        assertTrue(context.typeCache.isEmpty())
        assertEquals(context.createTypeDeserializer().deserializeType(0), reader.deserializeType(0))
        assertFalse(context.materialization.lock.isLocked)
    }

    @Test
    fun missingMetadataRouteCannotUseTheConsumerDependencyManager() = fixture { f ->
        val split = split(f)
        val provider = CfirDeserializedSymbolProvider(split.consumerModule.session, split.consumerManager,
            CfirCangJieScopeProvider(), split.consumerModule,
            moduleDataProvider = cjoFixtureModuleDataProvider(split.consumerModule, setOf(f.directory.resolve("consumer"))),
            dependencyCjoManager = f.manager,
            importedPackageContextProvider = CjoPackageContextProvider { _, _ -> null })
        assertNull(provider.getPackageContext("wire.current", CjoDeserializationMode.FULL))
        assertNotNull(f.manager.loadPackageSnapshot("wire.current"))
    }

    @Test
    fun failedCrossModuleTransactionDoesNotLeaveATypeCacheHitToAFailedRecord() = fixture { f ->
        val split = split(f)
        val consumer = importedGenericContext(f, "_CN12wire.generic4LinkIG_E_TG1T", CjoDeserializationMode.FULL,
            split.consumer, split.consumerModule, split.consumerManager)
        val target = assertNotNull(split.target.getOwnPackageContext("wire.generic", CjoDeserializationMode.FULL))
        assertFailsWith<IllegalStateException> {
            consumer.materialization.read {
                assertIs<ConeTypeParameterType>(consumer.createTypeDeserializer().deserializeType(0))
                assertTrue(target.typeCache.isNotEmpty())
                consumer.materialization.postCompute { error("injected metadata completion failure") }
            }
        }
        assertTrue(consumer.typeCache.isEmpty())
        assertTrue(target.typeCache.isEmpty())
        assertIs<CjoDeclarationCacheEntry.Failed>(target.declarationEntries[1])
        assertFailsWith<CjoDeclarationDeserializationException> { consumer.createTypeDeserializer().deserializeType(0) }
    }

    /** 不改写官方 wire，将 consumer、目标库和 SDK 分别放在独立 manager/module 中。 */
    private fun split(f: Fixture): Split {
        fun own(name: String, packages: List<String>): Path {
            val directory = Files.createDirectory(f.directory.resolve(name))
            packages.forEach { Files.copy(f.directory.resolve("$it.cjo"), directory.resolve("$it.cjo")) }
            return directory
        }
        val consumerDirectory = own("consumer", listOf("wire.imported", "wire.reentry"))
        val targetDirectory = own("target", listOf("wire.current", "wire.generic"))
        val sdkDirectory = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
            .resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative")
        val transaction = f.manager.declarationMaterialization
        val consumerModule = Module()
        val targetModule = Module()
        val sdkModule = Module()
        fun manager(directory: Path) = CjoManager(CjoSearchPath.forRoots(emptyList(), listOf(directory.toFile())), transaction)
        val sdkManager = manager(sdkDirectory)
        val sdk = CfirDeserializedSymbolProvider(sdkModule.session, sdkManager, CfirCangJieScopeProvider(), sdkModule,
            moduleDataProvider = cjoFixtureModuleDataProvider(sdkModule, setOf(sdkDirectory)))
        val targetManager = manager(targetDirectory)
        val target = CfirDeserializedSymbolProvider(targetModule.session, targetManager, CfirCangJieScopeProvider(), targetModule,
            moduleDataProvider = cjoFixtureModuleDataProvider(targetModule, setOf(targetDirectory)),
            importedPackageContextProvider = sdk)
        val consumerManager = manager(consumerDirectory)
        val consumer = CfirDeserializedSymbolProvider(consumerModule.session, consumerManager, CfirCangJieScopeProvider(), consumerModule,
            moduleDataProvider = cjoFixtureModuleDataProvider(consumerModule, setOf(consumerDirectory)),
            importedPackageContextProvider = CjoPackageContextProvider { name, mode ->
                val contexts = listOf(target, sdk).mapNotNull { it.getOwnPackageContext(name, mode) }
                check(contexts.size <= 1)
                contexts.singleOrNull()
            })
        return Split(consumer, target, sdk, consumerModule, targetModule, sdkModule, consumerManager)
    }

    private data class Split(
        val consumer: CfirDeserializedSymbolProvider,
        val target: CfirDeserializedSymbolProvider,
        val sdk: CfirDeserializedSymbolProvider,
        val consumerModule: CfirModuleData,
        val targetModule: CfirModuleData,
        val sdkModule: CfirModuleData,
        val consumerManager: CjoManager,
    )

    /** imported Generic wire 的精确 export key 来自官方 fixture；这只构造类型池，不重写目标声明。 */
    private fun importedGenericContext(
        f: Fixture, exportKey: String, mode: CjoDeserializationMode,
        packageProvider: CjoPackageContextProvider = f.provider,
        module: CfirModuleData = f.module, manager: CjoManager = f.manager,
    ): CfirDeserializationContext {
        val builder = FlatBufferBuilder()
        val key = builder.createString(exportKey)
        val target = FullId.createFullId(builder, 0, key, 0u)
        val generic = GenericTyInfo.createGenericTyInfo(builder, target, 0)
        val type = SemaTy.createSemaTy(builder, TypeKind.Generic, 0, SemaTyInfo.GenericTyInfo, generic)
        val types = PackageFormat.Package.createAllTypesVector(builder, intArrayOf(type))
        val imports = PackageFormat.Package.createImportsVector(builder, intArrayOf(builder.createString("wire.generic")))
        val name = builder.createString("wire.generic.imported")
        PackageFormat.Package.startPackage(builder)
        PackageFormat.Package.addFullPkgName(builder, name)
        PackageFormat.Package.addAllTypes(builder, types)
        PackageFormat.Package.addImports(builder, imports)
        builder.finish(PackageFormat.Package.endPackage(builder))
        val pkg = PackageFormat.Package.getRootAsPackage(builder.dataBuffer())
        return CfirDeserializationContext(pkg, CjoPackageHeader.fromPackage(pkg), module, manager,
            manager.deserializedSymbolLookups.forBinding(
                (packageProvider as CfirDeserializedSymbolProvider).deserializationBinding, module, mode), mode = mode,
            packageContextProvider = packageProvider).canonical()
    }

    private fun fixture(action: (Fixture) -> Unit) {
        val directory = Files.createTempDirectory("cjo-fullid-wire-")
        try {
            for (packageName in listOf("wire.current", "wire.imported", "wire.reentry", "wire.generic")) {
                val bytes = javaClass.classLoader.getResourceAsStream("cjo-wire/$packageName.cjo.b64")!!
                    .use { Base64.getDecoder().decode(it.reader(Charsets.UTF_8).readText().trim()) }
                Files.write(directory.resolve("$packageName.cjo"), bytes)
            }
            action(Fixture(directory))
        } finally { directory.toFile().deleteRecursively() }
    }

    private class Fixture(val directory: Path) {
        val module = Module()
        private val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }
        val sdk = root.resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative")
        val manager = CjoManager(CjoSearchPath { key -> if (key == "CANGJIE_LIBRARY") "$directory${java.io.File.pathSeparator}$sdk" else null })
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module,
            moduleDataProvider = cjoFixtureModuleDataProvider(module, setOf(directory, sdk)))
        fun context(packageName: String): CfirDeserializationContext {
            val loaded = assertNotNull(manager.loadPackageSnapshot(packageName))
            return CfirDeserializationContext(loaded.pkg, loaded.header, module, manager,
                manager.deserializedSymbolLookups.forBinding(provider.deserializationBinding, module, CjoDeserializationMode.FULL), loaded.sourcePath).canonical()
        }
    }

    /** 同一 decoder 的多个路径 owner 共用真实 session；普通 Module() 仍各自创建库 session。 */
    private class Module(
        override val session: CfirSession = object : CfirSession(CfirSession.Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        },
    ) : CfirModuleData() {
        override val name = Name.identifier("cjo-fullid-wire-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "cjo-fullid-wire-test"
        init { bindSession(session) }
    }
}
