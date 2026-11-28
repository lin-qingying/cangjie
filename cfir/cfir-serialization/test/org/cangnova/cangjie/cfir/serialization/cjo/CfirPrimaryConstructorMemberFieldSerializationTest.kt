@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.Package
import PackageFormat.ParamInfo
import PackageFormat.TypeKind
import PackageFormat.VarInfo
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.bindPrimaryConstructorParameterField
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.correspondingField
import org.cangnova.cangjie.cfir.correspondingValueParameter
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.source.importedSourceGraphProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/**
 * 官方 FuncParam 与 VarDecl 分别保存 isMemberParam，不能用参数到字段的 raw 关联替代二进制事实。
 * raw builder 的字段生成由 raw 模块测试覆盖；本文件独立验证 CJO 读写和已解析 CFIR 的 producer。
 */
class CfirPrimaryConstructorMemberFieldSerializationTest :
    CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @TempDir
    lateinit var consumerRoot: Path

    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    /** 同名参数和字段的两个位独立读取，既不互相覆盖，也不按名字恢复不存在的引用边。 */
    @Test
    fun readerPreservesParameterAndFieldFlagsIndependently() {
        for (parameterIsMember in listOf(false, true)) {
            for (fieldIsMember in listOf(false, true)) {
                val metadata = memberFlagMetadata(parameterIsMember, fieldIsMember)
                val pkg = encode(metadata)
                assertEquals(parameterIsMember, assertIs<ParamInfo>(pkg.allDecls(2)!!.info(ParamInfo())).isMemberParam)
                assertEquals(fieldIsMember, assertIs<VarInfo>(pkg.allDecls(3)!!.info(VarInfo())).isMemberParam)

                val owner = deserializeClass(pkg, consumerRoot.resolve("$parameterIsMember-$fieldIsMember"))
                val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
                val parameter = constructor.valueParameters.single()
                val field = owner.declarations.filterIsInstance<CfirFieldVariable>().single()
                assertTrue(constructor.isPrimary)
                assertEquals(parameterIsMember, parameter.isMemberParameter)
                assertEquals(fieldIsMember, field.isMemberParameter)
                assertNull(parameter.correspondingField)
                assertNull(field.correspondingValueParameter)
                assertTrue(owner.declarations.none { it is CfirProperty || it is CfirPropertyAccessor })
            }
        }
    }

    /** let/var 成员写为 VarDecl，普通参数保持非成员；有无 raw 关联都必须写出独立成员位。 */
    @Test
    fun producerRoundTripsMemberFieldsAndParametersWithoutAccessors() {
        for (withRawLinks in listOf(false, true)) {
            val file = sourceFile(withRawLinks)
            val owner = file.declarations.filterIsInstance<CfirClass>().single()
            val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
            val parameters = constructor.valueParameters
            val fields = owner.declarations.filterIsInstance<CfirFieldVariable>()
            assertEquals(listOf(false, true, true), parameters.map { it.isMemberParameter })
            assertEquals(listOf(false, true), fields.map { it.isVar })
            if (withRawLinks) {
                fields.forEachIndexed { index, field ->
                    assertSame(field, parameters[index + 1].correspondingField)
                    assertSame(parameters[index + 1], field.correspondingValueParameter)
                }
            } else {
                assertTrue(parameters.all { it.correspondingField == null })
                assertTrue(fields.all { it.correspondingValueParameter == null })
            }

            val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file))
            assertEquals(7, metadata.declarations.size)
            assertEquals(2, metadata.declarations.count { it.kind == DeclKind.VarDecl })
            assertEquals(1, metadata.declarations.count { it.kind == DeclKind.FuncDecl })
            assertEquals(0, metadata.declarations.count { it.kind == DeclKind.PropDecl })
            val pkg = encode(metadata)
            val serializedParameters = metadata.declarations.withIndex().filter { it.value.kind == DeclKind.FuncParam }
            assertEquals(listOf(false, true, true), serializedParameters.map {
                assertIs<ParamInfo>(pkg.allDecls(it.index)!!.info(ParamInfo())).isMemberParam
            })
            val serializedFields = metadata.declarations.withIndex().filter { it.value.kind == DeclKind.VarDecl }
            assertEquals(listOf(true, true), serializedFields.map {
                assertIs<VarInfo>(pkg.allDecls(it.index)!!.info(VarInfo())).isMemberParam
            })
            assertEquals(listOf(false, true), serializedFields.map {
                assertIs<VarInfo>(pkg.allDecls(it.index)!!.info(VarInfo())).isVar
            })

            val restoredOwner = deserializeClass(pkg, consumerRoot.resolve(withRawLinks.toString()))
            // 父类型的固定标签必须借用同一 consumer provider 的真实外包 Object，不能按 ClassId 补造。
            val restoredSession = restoredOwner.moduleData.session
            val consumer = assertIs<CfirDeserializedSymbolProvider>(
                assertNotNull(restoredSession.importedSourceGraphProvider, "Library factory must install its CJO provider"),
            )
            val objectSymbol = assertIs<CfirClassSymbol>(consumer.getClassLikeSymbolByClassId(StdlibClassIds.Object))
            val objectType = assertIs<ConeClassLikeType>(restoredOwner.superTypeRefs.single().coneType)
            assertSame(objectSymbol, assertIs<ConeClassLikeLookupTagWithFixedSymbol>(objectType.lookupTag).symbol)
            assertTrue(objectSymbol.isBound)
            assertSame(restoredSession, objectSymbol.cfir.moduleData.session)
            val objectOrigin = kotlin.test.assertNotNull(objectSymbol.cfir.cjoDeclarationOrigin)
            assertNotEquals(kotlin.test.assertNotNull(restoredOwner.cjoDeclarationOrigin).packagePath, objectOrigin.packagePath)
            assertSame(objectSymbol, consumer.getClassLikeSymbolByClassId(StdlibClassIds.Object))
            val restoredConstructor = restoredOwner.declarations.filterIsInstance<CfirConstructor>().single()
            val restoredFields = restoredOwner.declarations.filterIsInstance<CfirFieldVariable>()
            assertTrue(restoredConstructor.isPrimary)
            assertEquals(listOf(false, true, true), restoredConstructor.valueParameters.map { it.isMemberParameter })
            assertEquals(listOf(true, true), restoredFields.map { it.isMemberParameter })
            assertEquals(listOf(false, true), restoredFields.map { it.isVar })
            assertEquals(3, restoredOwner.declarations.size, "Only the primary constructor and two fields may be restored")
            assertTrue(restoredOwner.declarations.none { it is CfirProperty || it is CfirPropertyAccessor })
        }
    }

    private fun memberFlagMetadata(parameterIsMember: Boolean, fieldIsMember: Boolean): CjoPackageMetadata =
        CjoPackageMetadata(
            fullPackageName = "memberfields",
            moduleName = "memberfields",
            declarations = listOf(
                CjoPackageDeclaration("Box", kind = DeclKind.ClassDecl, type = 2u, info = CjoClassInfo(body = listOf(2u, 4u))),
                CjoPackageDeclaration("init", kind = DeclKind.FuncDecl, type = 3u, isTopLevel = false,
                    attributes = flags(Attribute.CONSTRUCTOR, Attribute.PRIMARY_CONSTRUCTOR),
                    info = CjoFunctionInfo(body = CjoFunctionBodyInfo(
                        parameterLists = listOf(listOf(3u)),
                        desugaredParameterLists = listOf(listOf(0u)),
                        returnType = 2u,
                    ))),
                CjoPackageDeclaration("value", kind = DeclKind.FuncParam, type = 1u, isTopLevel = false,
                    info = CjoParameterInfo(isNamed = false, isMember = parameterIsMember)),
                CjoPackageDeclaration("value", kind = DeclKind.VarDecl, type = 1u, isTopLevel = false,
                    info = CjoVariableInfo(isVar = false, isMemberParam = fieldIsMember)),
            ),
            types = listOf(
                CjoTypeMetadata(TypeKind.Int64),
                CjoTypeMetadata(TypeKind.Class, semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 1u)),
                CjoTypeMetadata(TypeKind.Func, typeArguments = listOf(1u),
                    semanticInfo = CjoFunctionTypeInfoMetadata(returnType = 2u, isC = false, hasVariableLenArg = false)),
            ),
        )

    private fun sourceFile(withRawLinks: Boolean): CfirFile {
        val module = Module(CfirSession.Kind.Source)
        val psiFile = createPsiFile("memberfields", """
            package memberfields
            public class Box {
                public Box(ordinary: Int64, public let fixed: Int64, public var mutable: Int64) {}
            }
        """.trimIndent()) as CjFile
        val psiClass = psiFile.declarations.filterIsInstance<CjClass>().single()
        val psiConstructor = requireNotNull(psiClass.primaryConstructor)
        val classId = ClassId.topLevel(FqName("memberfields.Box"))
        val classSymbol = CfirClassSymbol(classId)
        val classType = ConeClassLikeType(classSymbol.toLookupTag())
        val constructorSymbol = CfirConstructorSymbol(CallableId(classId, Name.identifier("init")))
        val parameters = psiConstructor.valueParameters.map { psi ->
            buildValueParameter {
                source = psi.toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                isNamed = false
                name = Name.identifier(requireNotNull(psi.name))
                symbol = CfirValueParameterSymbol(CallableId(classId, name))
                containingDeclarationSymbol = constructorSymbol
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            }
        }
        val constructor = buildPrimaryConstructor {
            source = psiConstructor.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = CfirDeclarationStatusImpl()
            symbol = constructorSymbol
            returnTypeRef = classType.toCfirResolvedTypeRef(null)
            valueParameters += parameters
        }
        val fields = parameters.drop(1).mapIndexed { index, parameter ->
            val field = buildFieldVariable {
                source = parameter.source
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                dispatchReceiverType = classType
                status = CfirDeclarationStatusImpl()
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
                name = parameter.name
                symbol = CfirFieldVariableSymbol(CallableId(classId, name))
                isVar = index == 1
            }
            if (withRawLinks) {
                bindPrimaryConstructorParameterField(parameter, field)
            } else {
                parameter.isMemberParameter = true
                field.isMemberParameter = true
            }
            field
        }
        val owner = buildClass {
            source = psiClass.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.IMPORTS
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            scopeProvider = CfirCangJieScopeProvider()
            status = CfirDeclarationStatusImpl()
            name = Name.identifier("Box")
            symbol = classSymbol
            declarations += constructor
            declarations += fields
        }
        val file = buildFile {
            source = psiFile.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            symbol = CfirFileSymbol()
            name = "memberfields.cj"
            packageDirective = buildPackageDirective { packageFqName = FqName("memberfields"); isMacroPackage = false }
            declarations += owner
        }
        registerCjoSourceFixture(file)
        registerCjoSourceFixtureDependencies(module.session, module.session.dependenciesSymbolProvider)
        resolveCjoSourceFixtureStatuses(module.session, file.declarations, file)
        return file
    }

    private fun encode(metadata: CjoPackageMetadata): Package =
        Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(metadata)))

    /** 正式库工厂安装跨包 declarationResolver；临时根只保存本次被测包，SDK 仍来自原环境。 */
    private fun deserializeClass(pkg: Package, caseRoot: Path): CfirClass {
        Files.createDirectories(caseRoot)
        // 直接保存刚完成 wire 断言的正式 writer 字节；不重建表、不重映射或删除外包父边。
        val buffer = pkg.getByteBuffer().duplicate()
        buffer.position(0)
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val packageName = kotlin.test.assertNotNull(pkg.fullPkgName)
        Files.write(caseRoot.resolve(packageName + ".cjo"), bytes)
        val manager = CjoManager(
            CjoSearchPath(additionalLibrarySearchPaths = listOf(caseRoot.toFile())),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
        )
        val module = Module(CfirSession.Kind.Library, manager)
        val consumer = assertIs<CfirDeserializedSymbolProvider>(
            assertNotNull(module.session.importedSourceGraphProvider, "Library factory must install its CJO provider"),
        )
        val classId = ClassId.topLevel(FqName(packageName + ".Box"))
        val symbol = assertIs<CfirClassSymbol>(consumer.getClassLikeSymbolByClassId(classId))
        val owner = symbol.cfir
        assertSame(module, owner.moduleData)
        assertSame(module.session, owner.moduleData.session)
        assertNotNull(owner.cjoDeclarationOrigin)
        assertSame(symbol, consumer.getClassLikeSymbolByClassId(classId))
        return owner
    }

    private fun flags(vararg attributes: Attribute): List<ULong> =
        MutableList(attributes.maxOf { it.ordinal } / 64 + 1) { 0uL }.apply {
            for (attribute in attributes) {
                this[attribute.ordinal / 64] = this[attribute.ordinal / 64] or (1uL shl (attribute.ordinal % 64))
            }
        }

    private class Module(
        kind: CfirSession.Kind,
        cjoManager: CjoManager = CjoManager(
            CjoSearchPath(), CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT),
        ),
    ) : CfirModuleData() {
        override val name = Name.identifier("memberfields-test")
        private val libraryModule = CfirBinaryDependenciesModuleData(Name.identifier("memberfields-dependencies"))
        override val dependencies: List<CfirModuleData> = if (kind == CfirSession.Kind.Source) listOf(libraryModule) else emptyList()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "memberfields-test"
        override val session: CfirSession
            get() = checkNotNull(boundSession) { "Member-field fixture session is not assembled" }

        init {
            // 正常工厂安装原 Source 的 type resolver/supertype provider，库头仍由真实 CJO 装配。
            val factory = CfirDefaultSessionFactory()
            val context = CfirDefaultSessionFactory.Context(
                cjoManager = cjoManager,
                exportSettings = CfirExportAnalysisSettings(),
            )
            val shared = factory.createSharedLibrarySession(name, emptyList(), LanguageVersionSettingsImpl.DEFAULT, context)
            when (kind) {
                CfirSession.Kind.Source -> {
                    factory.createLibrarySession(shared, SingleModuleDataProvider(libraryModule), emptyList(), LanguageVersionSettingsImpl.DEFAULT, context)
                    factory.createSourceSession(this, emptyList(), CompilerConfiguration.createForCfirFrontend(), context)
                }
                CfirSession.Kind.Library -> factory.createLibrarySession(
                    shared, SingleModuleDataProvider(this), emptyList(), LanguageVersionSettingsImpl.DEFAULT, context,
                )
            }
        }
    }
}
