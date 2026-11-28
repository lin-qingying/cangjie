@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class, org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.buildResolvedStatus
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.originalForSubstitutionOverride
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFakeOverrideGenerator
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.serialization.provider.CfirExtendProviderComposer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * 用现有 SDK fixture 验证库声明在签名复制前已具有完整 resolved status。
 *
 * runTest 复用现有空 consumer fixture 只创建 headless 项目；不新增或修改任何 .cj 期望。
 * metadata 读取保持 SIGNATURE_ONLY，实际 substitution 和签名复制继续经过正式 status guard。
 */
class CjoBinaryDeclarationStatusTest : AbstractAnalysisApiBasedTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun actualSdkParentStatusSurvivesSubstitutionAndSecondCopy() = withSdk { mainFile, sdk ->
        val context = requireNotNull(sdk.cjoSymbolProvider.getOwnPackageContext("std.binary", CjoDeserializationMode.SIGNATURE_ONLY))
        val wire = CfirDeclDeserializer(context, CfirTypeDeserializer(context))
        // 官方 fixture：raw7 InterfaceDecl，body=[raw9, raw11]；raw11 返回其 raw8 GenericParam。
        val wireParent = wire.deserializeDecl(6) as CfirInterface
        assertEquals("LittleEndianOrder", wireParent.name.asString())
        val wireRead = wireParent.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "readLittleEndian" }
        assertEquals(Visibilities.Public, wireRead.status.visibility)
        assertEquals(Modality.ABSTRACT, wireRead.status.modality)
        assertTrue(wireRead.status.isStatic && wireRead.status.isAbstract)
        assertFalse(wireRead.status.isDefault)

        val parent = requireNotNull(sdk.symbolProvider.getClassLikeSymbolByClassId(
            ClassId(FqName("std.binary"), Name.identifier("LittleEndianOrder")),
        )).cfir as CfirInterface
        assertNotNull(parent.source, "必须检查 STUBS named provider 的声明")
        assertTrue(parent.status is CfirResolvedDeclarationStatus)
        assertEquals(facts(wireParent.status), facts(parent.status))
        val read = parent.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "readLittleEndian" }
        assertEquals(CfirResolvePhase.ANALYZED_DEPENDENCIES, read.resolvePhase)
        assertTrue(read.status is CfirResolvedDeclarationStatus, "不能等到 fake override guard 才修状态")
        assertEquals(facts(wireRead.status), facts(read.status))
        assertSame(sdk.moduleData, parent.moduleData)
        assertSame(sdk.moduleData, read.moduleData)
        assertSame(parent.symbol, read.symbol.getContainingClassSymbol())
        assertSame(parent.symbol, parent.typeParameters.single().containingDeclarationSymbol)
        assertSame(parent.typeParameters.single().symbol,
            (read.returnTypeRef.coneTypeOrNull as ConeTypeParameterTypeImpl).lookupTag.typeParameterSymbol)
        assertSame(read.symbol, read.valueParameters.single().containingDeclarationSymbol)
        assertTrue(read.valueParameters.single().status is CfirResolvedDeclarationStatus)
        val statusBeforeLazy = read.status
        read.symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
        assertSame(statusBeforeLazy, read.status, "已发布的依赖不能靠 lazy STATUS 重新补齐状态")

        val useSiteModule = CangJieProjectStructureProvider.getInstance(mainFile.project).getModule(mainFile, null)
        val consumer = LLCfirSessionCache.getInstance(mainFile.project).getSession(useSiteModule)
        assertSame(parent.symbol, consumer.symbolProvider.getClassLikeSymbolByClassId(parent.symbol.classId))
        val uint64 = ConePrimitiveType(PrimitiveTypeKind.UINT64)
        val ownerType = ConeClassLikeType(parent.symbol.toLookupTag(), listOf(uint64), isInterface = true)
        val scope = CfirClassSubstitutionScope(consumer, SingleFunctionScope(read.symbol), ownerType)
        fun query(): CfirNamedFunctionSymbol {
            val result = mutableListOf<CfirNamedFunctionSymbol>()
            scope.processFunctionsByName(read.name) { result.add(it) }
            return result.single()
        }
        val firstSymbol = query()
        assertSame(firstSymbol, query(), "substitution cache 保持同一声明身份")
        assertNotSame(read.symbol, firstSymbol)
        val first = firstSymbol.cfir
        assertTrue(first.status is CfirResolvedDeclarationStatus)
        assertEquals(facts(wireRead.status, isOverride = true), facts(first.status))
        assertSame(read, first.originalForSubstitutionOverride)
        assertSame(consumer.moduleData, first.moduleData)
        assertEquals(uint64, first.returnTypeRef.coneTypeOrNull)
        assertSame(firstSymbol, first.valueParameters.single().containingDeclarationSymbol)
        assertNull(first.dispatchReceiverType)

        val secondSymbol = CfirNamedFunctionSymbol(firstSymbol.callableId)
        val second = CfirFakeOverrideGenerator.createCopyForCfirFunction(
            secondSymbol, first, consumer, CfirDeclarationOrigin.SubstitutionOverride.CallSite, null,
            first.valueParameters.map { it.returnTypeRef }, first.typeParameters, first.returnTypeRef, null,
        )
        assertTrue(second.status is CfirResolvedDeclarationStatus)
        assertEquals(facts(first.status), facts(second.status))
        assertNotSame(first.status, second.status)
        assertSame(first, second.originalForSubstitutionOverride)
        assertSame(secondSymbol, second.valueParameters.single().containingDeclarationSymbol)
        assertEquals(uint64, second.returnTypeRef.coneTypeOrNull)
    }

    @Test
    fun actualSdkPropertyAndAccessorsKeepTheirOwnWireStatuses() = withSdk { _, sdk ->
        val binaryProvider = CfirExtendProviderComposer.fromSymbolProviders(listOf(sdk.cjoSymbolProvider))
        val binaryExtend = binaryProvider.getExtendsForClass(StdlibClassIds.Array).first {
            it.declarations.filterIsInstance<CfirProperty>().any { property -> property.name.asString() == "size" }
        }
        val binarySize = binaryExtend.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "size" }
        val materializedExtend = requireNotNull(sdk.extendProviderOrNull).getExtendsForClass(StdlibClassIds.Array).first {
            it.declarations.filterIsInstance<CfirProperty>().any { property -> property.name.asString() == "size" }
        }
        assertNotNull(materializedExtend.source)
        assertEquals(facts(binaryExtend.status), facts(materializedExtend.status))
        val size = materializedExtend.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "size" }
        assertTrue(size.status is CfirResolvedDeclarationStatus)
        assertEquals(facts(binarySize.status), facts(size.status))
        val getter = requireNotNull(size.getter)
        val wireGetter = requireNotNull(binarySize.getter)
        assertTrue(getter.status is CfirResolvedDeclarationStatus)
        assertEquals(facts(wireGetter.status), facts(getter.status), "getter 必须保留 PropInfo 引用的自身状态")
        assertSame(size.symbol, getter.propertySymbol)
        assertSame(sdk.moduleData, getter.moduleData)
        if (binarySize.setter == null) {
            assertNull(size.setter)
        } else {
            val setter = requireNotNull(size.setter)
            assertTrue(setter.status is CfirResolvedDeclarationStatus)
            assertEquals(facts(binarySize.setter!!.status), facts(setter.status))
            assertSame(size.symbol, setter.propertySymbol)
            assertSame(setter.symbol, setter.valueParameters.single().containingDeclarationSymbol)
        }
    }

    @Test
    fun actualSdkAbstractPropertyRestoresItsAccessorWithoutASyntaxBody() = withSdk { _, sdk ->
        val context = requireNotNull(sdk.cjoSymbolProvider.getOwnPackageContext("std.core", CjoDeserializationMode.SIGNATURE_ONLY))
        val collectionId = ClassId(FqName("std.core"), Name.identifier("Collection"))
        val classIndex = context.header.topLevelClassifierNameToIndices.getValue("Collection").single()
        val binary = CfirDeclDeserializer(context, CfirTypeDeserializer(context)).deserializeDecl(classIndex) as CfirInterface
        val binaryProperty = binary.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "size" }
        val binaryGetter = requireNotNull(binaryProperty.getter)
        assertTrue(binaryProperty.status.isAbstract && binaryGetter.status.isAbstract)
        val actual = requireNotNull(sdk.symbolProvider.getClassLikeSymbolByClassId(collectionId)).cfir as CfirInterface
        val property = actual.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "size" }
        val psi = (property.source as org.cangnova.cangjie.source.CjRealPsiSourceElement).psi as org.cangnova.cangjie.psi.CjProperty
        assertNull(psi.getter, "合法 abstract prop 的语法投影没有 getter body")
        assertNull(psi.setter)
        val getter = requireNotNull(property.getter)
        assertEquals(facts(binaryGetter.status), facts(getter.status))
        assertTrue(getter.status is CfirResolvedDeclarationStatus)
        assertNull(getter.body)
        assertEquals(binaryGetter.returnTypeRef.coneTypeOrNull, getter.returnTypeRef.coneTypeOrNull)
        assertSame(property.symbol, getter.propertySymbol)
        assertSame(sdk.moduleData, getter.moduleData)
        assertSame(actual.symbol, getter.symbol.getContainingClassSymbol())
        assertEquals(org.cangnova.cangjie.source.CjFakeSourceElementKind.DefaultAccessor, getter.source!!.kind)
        assertNull(property.setter, "metadata 没有 setter 时不能合成第二个 accessor")
    }

    @Test
    fun missingMetadataHeaderFailsWithoutPublishingADefaultStatus() = withSdk { mainFile, _ ->
        val source = CjPsiFactory(mainFile.project).createFile("public func source(): Unit {}")
        val root = CjFileStubBuilder().buildStubTree(source)
        val function = root.childrenStubs.filterIsInstance<CangJieNamedFunctionStubImpl>().single().psi
        val failure = assertThrows(IllegalStateException::class.java) { function.buildResolvedStatus() }
        assertTrue(failure.message.orEmpty().contains("no CJO status"))
    }

    @Test
    fun actualSdkSetterParameterKeepsItsWireNameAndOwner() = withSdk { _, sdk ->
        val context = requireNotNull(sdk.cjoSymbolProvider.getOwnPackageContext("std.core", CjoDeserializationMode.SIGNATURE_ONLY))
        val index = context.header.topLevelClassifierNameToIndices.getValue("Thread").single()
        val binary = CfirDeclDeserializer(context, CfirTypeDeserializer(context)).deserializeDecl(index) as CfirClass
        val wireProperty = binary.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "name" }
        val wireSetter = requireNotNull(wireProperty.setter)
        val wireParameter = wireSetter.valueParameters.single()
        assertEquals("newName", wireParameter.name.asString())
        val actual = requireNotNull(sdk.symbolProvider.getClassLikeSymbolByClassId(binary.symbol.classId)).cfir as CfirClass
        val property = actual.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "name" }
        val setter = requireNotNull(property.setter)
        val parameter = setter.valueParameters.single()
        assertEquals(wireParameter.name, parameter.name)
        assertEquals(wireParameter.isNamed, parameter.isNamed)
        assertEquals(facts(wireParameter.status), facts(parameter.status))
        assertEquals(property.returnTypeRef.coneTypeOrNull, parameter.returnTypeRef.coneTypeOrNull)
        assertSame(setter.symbol, parameter.containingDeclarationSymbol)
        assertSame(sdk.moduleData, parameter.moduleData)
        assertEquals(wireSetter.annotations.size, setter.annotations.size)
        assertEquals(wireParameter.annotations.size, parameter.annotations.size)
        setter.annotations.filterIsInstance<org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall>().forEach {
            assertSame(setter.symbol, it.containingDeclarationSymbol)
        }
        parameter.annotations.filterIsInstance<org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall>().forEach {
            assertSame(parameter.symbol, it.containingDeclarationSymbol)
        }
    }

    private fun withSdk(block: (CjFile, org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession) -> Unit) {
        runTest("analysis/low-level-api-cfir/testData/diagnostics/cjoImportedModuleIdentity.cj") { services ->
            val mainFile = requireNotNull(findMainFileAndModule(services).mainFile)
            val project = mainFile.project
            assertEquals(CaDeserializedDeclarationsOrigin.STUBS, CaPlatformSettings.getInstance(project).deserializedDeclarationsOrigin)
            assertFixture(project, "std.binary")
            assertFixture(project, "std.core")
            val platform = CangJieProjectStructureProvider.getInstance(project).getModule(mainFile, null).targetPlatform
            val sdk = LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(platform)
            block(mainFile, sdk)
        }
    }

    /** 明确选择 testResources 的实际文件，并证明 named provider 所属 SDK roots 覆盖同一文件。 */
    private fun assertFixture(project: Project, packageName: String) {
        val path = Path.of("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative/std/$packageName.cjo")
            .toAbsolutePath().normalize()
        val virtual = requireNotNull(StandardFileSystems.local().refreshAndFindFileByPath(path.toString().replace('\\', '/')))
        assertTrue(BuiltinsVirtualFileProvider.getInstance().isInBuiltinRoot(virtual, project))
        assertArrayEquals(Files.readAllBytes(path), virtual.contentsToByteArray())
        val psi = requireNotNull(PsiManager.getInstance(project).findFile(virtual)) as CjFile
        assertTrue(psi.isCompiled)
    }

    /** 与实际 CJO decoder 对比全部字段；isOverride 是签名副本唯一预期状态变化。 */
    private fun facts(status: CfirDeclarationStatus, isOverride: Boolean = status.isOverride): List<Any?> = listOf(
        status.visibility, status.modality, status.visibilityExplicitness, status.modalityExplicitness,
        status.bodyPresence, status.isDeclarationFile, status.isVisibilityExplicit, status.isModalityExplicit,
        status.isAbstractExplicit, isOverride, status.isOperator, status.isStatic, status.isConst, status.isMut,
        status.isUnsafe, status.isForeign, status.isC, status.isCommon, status.isFromCommonPart, status.isSpecific,
        status.isCommonWithDefault, status.isRedef, status.isDefault, status.isAbstract, status.isOpen, status.isSealed,
    )

    private class SingleFunctionScope(private val symbol: CfirNamedFunctionSymbol) : CfirTypeScope() {
        override fun getCallableNames(): Set<Name> = setOf(symbol.name)
        override fun getClassifierNames(): Set<Name> = emptySet()
        override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
            if (name == symbol.name) processor(symbol)
        }
        override fun processDirectOverriddenFunctionsWithBaseScope(functionSymbol: CfirNamedFunctionSymbol,
            processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction): ProcessorAction = ProcessorAction.NEXT
        override fun processDirectOverriddenPropertiesWithBaseScope(propertySymbol: CfirPropertySymbol,
            processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction): ProcessorAction = ProcessorAction.NEXT
        override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? = null
    }
}
