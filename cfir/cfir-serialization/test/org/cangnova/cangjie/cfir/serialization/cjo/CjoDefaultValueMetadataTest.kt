@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.test.bindCjoFixture
import PackageFormat.DeclKind
import PackageFormat.FuncInfo
import PackageFormat.Package
import PackageFormat.ParamInfo
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpPreparedGroup
import org.cangnova.cangjie.cfir.session.CfirCjmpClassifierDomain
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjTuplePattern
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import java.nio.ByteBuffer
import kotlin.test.*
import kotlin.test.assertNotNull as assertValueNotNull

/** 默认参数辅助函数的引用边、物理 owner 和初始标志必须分别往返，不能伪造成参数引用。 */
@OptIn(CfirImplementationDetail::class)
class CjoDefaultValueMetadataTest : CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun helperKeepsClassOwnerRegardlessOfMemberLoadOrder() {
        for (helperFirst in listOf(false, true)) {
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(classMetadata())))
            val context = context(pkg)
            // 首次请求 helper 或原函数；真实 owner 由正常 header 入口恢复，不伪造 class 成员槽。
            val firstLoaded = context.createDeclDeserializer().deserializeDecl(if (helperFirst) 3 else 1)
            val owner = assertIs<CfirClass>(context.createDeclDeserializer().deserializeDecl(0))
            val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "f" }
            val parameter = function.valueParameters.single()
            val metadata = assertValueNotNull(parameter.defaultValueMetadata)
            val helper = assertValueNotNull(metadata.function)
            assertSame(if (helperFirst) helper else function, firstLoaded)
            assertSame(helper, context.declCache[3])
            assertFalse(owner.declarations.any { it === helper })
            assertTrue(metadata.hasInitial)
            assertSame(helper, metadata.function)
            assertSame(function.symbol, parameter.containingDeclarationSymbol)
            assertSame(parameter, context.declCache[2])
            assertSame(helper, (context.declCache[2] as CfirValueParameter).defaultValueMetadata?.function)
            assertEquals(ClassId.topLevel(FqName("defaults.Box")), assertIs<ConeClassLikeType>(helper.dispatchReceiverType).classId)
        }
    }

    @Test
    fun constructorHelperKeepsStaticClassOwner() {
        val input = classMetadata()
        val declarations = input.declarations.toMutableList()
        declarations[1] = declarations[1].copy(identifier = "init", attributes = flags(Attribute.CONSTRUCTOR))
        declarations[3] = declarations[3].copy(attributes = flags(Attribute.STATIC))
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(input.copy(declarations = declarations))))
        val owner = assertIs<CfirClass>(context(pkg).createDeclDeserializer().deserializeDecl(0))
        val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
        val parameter = constructor.valueParameters.single()
        val helper = assertValueNotNull(parameter.defaultValueMetadata?.function)
        assertSame(constructor.symbol, parameter.containingDeclarationSymbol)
        assertFalse(owner.declarations.any { it === helper })
        assertTrue(helper.status.isStatic)
        assertNull(helper.dispatchReceiverType)
        assertEquals(ClassId.topLevel(FqName("defaults.Box")), helper.containingClassForStaticMemberAttr?.classId)
    }

    @Test
    fun initialFlagAndHelperReferenceRemainIndependent() {
        for (hasInitial in listOf(false, true)) {
            for (hasHelper in listOf(false, true)) {
                val input = classMetadata(hasInitial = hasInitial, hasHelper = hasHelper)
                val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(input)))
                val owner = assertIs<CfirClass>(context(pkg).createDeclDeserializer().deserializeDecl(0))
                val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "f" }
                val parameter = function.valueParameters.single()
                assertEquals(hasInitial, parameter.defaultValueMetadata?.hasInitial)
                assertEquals(hasHelper, parameter.defaultValueMetadata?.function != null)
                // HAS_INITIAL 与 helper 是存在合同；种子没有 defaultVal，不能制造 Unit 作为默认表达式。
                assertEquals(0u, assertIs<PackageFormat.ParamInfo>(assertValueNotNull(pkg.allDecls(2)).info(PackageFormat.ParamInfo())).defaultVal)
                assertNull(parameter.defaultValue)
                assertTrue(parameter.isNamed)
            }
        }
    }

    /** TOOL_ADD 是参数自己的 wire 属性，不能由被导入的 owner/package 推断。 */
    @Test
    fun toolAddedFlagRemainsOwnedByTheParameter() {
        val input = classMetadata(hasInitial = false, hasHelper = false)
        val declarations = input.declarations.toMutableList()
        declarations[2] = declarations[2].copy(attributes = flags(Attribute.TOOL_ADD))
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(input.copy(declarations = declarations))))
        val owner = assertIs<CfirClass>(context(pkg).createDeclDeserializer().deserializeDecl(0))
        val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "f" }
        assertTrue(function.valueParameters.single().exportFacts.isToolAdded)
        assertFalse(function.exportFacts.isToolAdded)
        assertFalse(owner.exportFacts.isToolAdded)
    }

    /** common CJO 的默认实现位在反序列化后仍是权威事实，不由当前成员列表重新推导。 */
    @Test
    fun commonDefaultAttributeRemainsAuthoritativeAfterDeserialization() {
        for (hasDefault in listOf(false, true)) {
            val input = classMetadata(hasInitial = false, hasHelper = false)
            val attributes = if (hasDefault) {
                flags(Attribute.COMMON, Attribute.FROM_COMMON_PART, Attribute.COMMON_WITH_DEFAULT)
            } else {
                flags(Attribute.COMMON, Attribute.FROM_COMMON_PART)
            }
            val declarations = input.declarations.toMutableList()
            declarations[0] = declarations[0].copy(attributes = attributes)
            declarations[1] = declarations[1].copy(attributes = attributes)
            val bytes = CjoPackageWriter.toByteArray(input.copy(
                schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
                declarations = declarations,
            ))
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
            val owner = assertIs<CfirClass>(context(pkg).createDeclDeserializer().deserializeDecl(0))
            val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "f" }

            assertTrue(owner.isFromCommonPart)
            assertEquals(hasDefault, owner.status.isCommonWithDefault)
            assertTrue(function.isFromCommonPart)
            assertEquals(hasDefault, function.status.isCommonWithDefault)
            assertNull(function.body, "CJO 不保存可用于重新推导默认实现的函数体")
            assertEquals(hasDefault, function.cjmpHasCommonDefault())
            assertEquals(hasDefault, owner.cjmpHasCommonDefault())
            assertEquals(hasDefault, function.hasCjmpCommonDefaultAttribute())
            assertEquals(hasDefault, owner.hasCjmpCommonDefaultAttribute())

            // 模拟后续声明图变化：已加载的二进制属性位不能随成员列表改变。
            @Suppress("UNCHECKED_CAST")
            (owner.declarations as MutableList<CfirDeclaration>).clear()
            assertEquals(hasDefault, owner.cjmpHasCommonDefault())
            assertEquals(hasDefault, owner.hasCjmpCommonDefaultAttribute())
        }
    }

    /** SPECIFIC 的 HasDefault 豁免不能赋予 COMMON_WITH_DEFAULT，实际 CJO 属性往返后仍分离。 */
    @Test
    fun specificHasDefaultDoesNotBecomeCommonDefaultAttribute() {
        val input = classMetadata(hasInitial = false, hasHelper = false)
        val declarations = input.declarations.toMutableList()
        declarations[1] = declarations[1].copy(attributes = flags(Attribute.SPECIFIC))
        val bytes = CjoPackageWriter.toByteArray(input.copy(
            schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
            declarations = declarations,
        ))
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val owner = assertIs<CfirClass>(context(pkg).createDeclDeserializer().deserializeDecl(0))
        val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "f" }
        assertTrue(function.status.isSpecific)
        assertFalse(function.status.isCommon)
        assertNull(function.body)
        assertTrue(function.cjmpHasCommonDefault())
        assertFalse(function.hasCjmpCommonDefaultAttribute())
    }

    /** 包级简单绑定复用模式容器初值，但 CJO 属性写在投影后的 VarDecl 上。 */
    @Test
    fun producerWritesCommonDefaultForSimpleSourceBindings() {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile(
            "defaults",
            "package defaults\ncommon let withDefault: Int64 = 1\ncommon let required: Int64",
        ) as CjFile
        val variables = psi.declarations.filterIsInstance<CjPatternVariable>()
        assertEquals(2, variables.size)
        val declarations = variables.mapIndexed { index, source ->
            val name = Name.identifier(if (index == 0) "withDefault" else "required")
            val status = CfirDeclarationStatusImpl().apply { isCommon = true }
            val binding = buildPatternBindingVariable {
                this.source = requireNotNull(source.pattern).toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                this.status = status
                symbol = CfirPatternBindingSymbol(CallableId(FqName("defaults"), name))
                this.name = name
                isVar = false
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            }
            buildPatternVariable {
                this.source = source.toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                this.status = status
                symbol = CfirPatternVariableSymbol(CallableId(FqName("defaults"), Name.special("<pattern-variable>")))
                isVar = false
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
                pattern = buildBindingPattern {
                    this.source = requireNotNull(source.pattern).toCjPsiSourceElement()
                    this.name = name
                    bindingVariable = binding
                }
                initializer = if (index == 0) buildLiteralExpression {
                    kind = CfirLiteralKind.INT
                    value = 1L
                    coneTypeOrNull = ConePrimitiveType.INT64
                } else null
            }
        }
        val file = sourceFile(module, declarations, psi)
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file), canonicalSources = listOf(CfirSourceGraphProducer.produce(listOf(file))))
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(metadata)))
        for ((name, expected) in listOf("withDefault" to true, "required" to false)) {
            val index = metadata.declarations.indexOfFirst { it.identifier == name }
            assertTrue(index >= 0, name)
            assertEquals(DeclKind.VarDecl, metadata.declarations[index].kind)
            val declaration = requireNotNull(pkg.allDecls(index))
            val originBit = Attribute.FROM_COMMON_PART.ordinal
            assertTrue(originBit / 64 < declaration.attributesLength &&
                declaration.attributes(originBit / 64) and (1uL shl (originBit % 64)) != 0uL)
            val ordinal = Attribute.COMMON_WITH_DEFAULT.ordinal
            val hasDefault = ordinal / 64 < declaration.attributesLength &&
                declaration.attributes(ordinal / 64) and (1uL shl (ordinal % 64)) != 0uL
            assertEquals(expected, hasDefault, name)
        }

        // 包级绑定的初值可能仅存于模式容器；序列化不能将惰性占位符当成缺失初值。
        declarations.first().replaceInitializer(buildLazyExpression())
        val unresolvedFile = sourceFile(module, declarations, psi)
        assertFailsWith<IllegalStateException> {
            CfirCjoPackageMetadataProducer.produce(listOf(unresolvedFile))
        }
    }

    /** 多绑定模式的根初值不能在投影成独立 VarDecl 时被遗漏。 */
    @Test
    fun producerRejectsLazyTuplePatternInitializer() {
        val module = Module(CfirSession.Kind.Source, CfirCjmpMode.COMMON)
        val psi = createPsiFile("defaults", "package defaults\ncommon let (first, second) = (1, 2)") as CjFile
        val source = psi.declarations.filterIsInstance<CjPatternVariable>().single()
        val tupleSource = assertIs<CjTuplePattern>(source.pattern)
        assertEquals(2, tupleSource.patterns.size)
        val status = CfirDeclarationStatusImpl().apply { isCommon = true }
        val patternElements = tupleSource.patterns.mapIndexed { index, element ->
            val name = Name.identifier(if (index == 0) "first" else "second")
            val binding = buildPatternBindingVariable {
                this.source = element.toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                this.status = status
                symbol = CfirPatternBindingSymbol(CallableId(FqName("defaults"), name))
                this.name = name
                isVar = false
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            }
            buildBindingPattern {
                this.source = element.toCjPsiSourceElement()
                this.name = name
                bindingVariable = binding
            }
        }
        val root = buildPatternVariable {
            this.source = source.toCjPsiSourceElement()
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            this.status = status
            symbol = CfirPatternVariableSymbol(CallableId(FqName("defaults"), Name.special("<pattern-variable>")))
            isVar = false
            returnTypeRef = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64)).toCfirResolvedTypeRef(null)
            pattern = buildTuplePattern {
                this.source = tupleSource.toCjPsiSourceElement()
                elements += patternElements
            }
            initializer = buildLazyExpression()
        }

        val bindings = root.pattern.bindingVariables().map { it.symbol }
        assertEquals(2, bindings.size)
        val file = sourceFile(module, listOf(root), psi)
        assertFailsWith<IllegalStateException> {
            CfirCjoPackageMetadataProducer.produce(listOf(file))
        }
        for (hidden in listOf(bindings.toSet(), setOf(bindings[0]), setOf(bindings[1]), emptySet())) {
            val family = CfirCjmpPreparedGroup(
                packageFqName = FqName("defaults"),
                roots = emptyList(),
                topLevelOrder = listOf(root.symbol),
                originalChildren = mapOf(root.symbol to bindings),
                commonDeclarations = listOf(root.symbol),
                containers = emptyList(),
                extensions = emptyList(),
                commonExtendOrder = emptyList(),
                originalDoNotExport = hidden,
                classifierDomain = CfirCjmpClassifierDomain.capture(root.moduleData.session, listOf(root), sourceFiles = listOf(file)),
            ).evaluate()
            assertEquals(hidden, family.doNotExport)
            // 完整 SOURCE 输入的 materialization 先验不能被导出选择或全部隐藏的 binding 绕过。
            assertFailsWith<IllegalStateException> {
                CfirCjoPackageMetadataProducer.produce(listOf(file), familySnapshot = family)
            }
        }
    }

    @Test
    fun rejectsDesugarReferenceToParameterInWriterAndReader() {
        val metadata = classMetadata()
        val malformedFunction = metadata.declarations[1].copy(info = functionInfo(listOf(3u), listOf(3u)))
        assertFailsWith<IllegalArgumentException> {
            CjoPackageWriter.toByteArray(metadata.copy(declarations = metadata.declarations.toMutableList().apply {
                set(1, malformedFunction)
            }))
        }
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(metadata)))
        val info = assertIs<FuncInfo>(pkg.allDecls(1)!!.info(FuncInfo()))
        assertTrue(info.funcBody!!.paramLists(0)!!.mutateDesugars(0, 3u))
        // writer 的 metadata 是调用参数；reader 接收被篡改 wire，由 header registry 校验数据不变量。
        val failure = assertFailsWith<IllegalStateException> {
            context(pkg).createDeclDeserializer().deserializeDecl(0)
        }
        assertEquals("A default-helper slot must reference its actual FuncDecl", failure.message)
    }

    @Test
    fun producerIndexesActualHelperAndPreservesTopLevelIdentity() {
        for (includeHelperFirst in listOf(false, true)) {
            val module = Module(CfirSession.Kind.Source)
            val psiFile = createPsiFile("defaults", "package defaults\nfunc default_p(): Int64 { 1 }\nfunc f(p!: Int64): Int64 { p }") as CjFile
            val functions = psiFile.declarations.filterIsInstance<CjNamedFunction>().associateBy { it.name }
            val helper = sourceFunction(module, assertValueNotNull(functions["default_p"]))
            val functionPsi = assertValueNotNull(functions["f"])
            val function = sourceFunction(module, functionPsi)
            val parameter = buildValueParameter {
                source = functionPsi.valueParameters.single().toCjPsiSourceElement()
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                isNamed = true
                name = Name.identifier("p")
                symbol = CfirValueParameterSymbol(CallableId(name))
                containingDeclarationSymbol = function.symbol
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            }
            parameter.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = true, function = helper)
            function.replaceValueParameters(listOf(parameter))
            val file = sourceFile(module, if (includeHelperFirst) listOf(helper, function) else listOf(function), psiFile)
            val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file))
            assertEquals(3, metadata.declarations.size)
            val functionMetadata = metadata.declarations.single { it.identifier == "f" }
            val body = assertIs<CjoFunctionInfo>(functionMetadata.info).body
            val helperIndex = body.desugaredParameterLists.single().single()
            assertNotEquals(body.parameterLists.single().single(), helperIndex)
            val helperMetadata = metadata.declarations[helperIndex.toInt() - 1]
            assertEquals("default_p", helperMetadata.identifier)
            assertEquals(DeclKind.FuncDecl, helperMetadata.kind)
            assertTrue(helperMetadata.isTopLevel)
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(CjoPackageWriter.toByteArray(metadata)))
            val loaded = assertIs<CfirNamedFunction>(context(pkg).createDeclDeserializer().deserializeDecl(
                metadata.declarations.indexOf(functionMetadata),
            ))
            val loadedParameter = loaded.valueParameters.single()
            assertTrue(loadedParameter.isNamed)
            assertTrue(assertValueNotNull(loadedParameter.defaultValueMetadata).hasInitial)
            assertEquals("default_p", loadedParameter.defaultValueMetadata?.function?.name?.asString())
        }
    }

    private fun classMetadata(hasInitial: Boolean = true, hasHelper: Boolean = true): CjoPackageMetadata =
        CjoPackageMetadata(
            fullPackageName = "defaults",
            moduleName = "defaults",
            declarations = listOf(
                CjoPackageDeclaration("Box", kind = DeclKind.ClassDecl, type = 2u,
                    // helper 由 p.desugar 拥有，词法 class 独立恢复；body 只保存真实物理成员 f。
                    info = CjoClassInfo(body = listOf(2u))),
                CjoPackageDeclaration("f", type = 3u, isTopLevel = false,
                    info = functionInfo(listOf(3u), listOf(if (hasHelper) 4u else 0u))),
                CjoPackageDeclaration("p", kind = DeclKind.FuncParam, type = 1u, isTopLevel = false,
                    attributes = if (hasInitial) flags(Attribute.HAS_INITIAL) else emptyList(),
                    info = CjoParameterInfo(isNamed = true, isMember = false)),
                CjoPackageDeclaration("default_p", type = 4u, isTopLevel = false, info = functionInfo()),
            ),
            types = listOf(
                CjoTypeMetadata(TypeKind.Int64),
                CjoTypeMetadata(TypeKind.Class, semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 1u)),
                // Decl.type 保存完整签名：f(Int64) -> Int64，default_p() -> Int64。
                CjoTypeMetadata(TypeKind.Func, typeArguments = listOf(1u),
                    semanticInfo = CjoFunctionTypeInfoMetadata(returnType = 1u, isC = false, hasVariableLenArg = false)),
                CjoTypeMetadata(TypeKind.Func,
                    semanticInfo = CjoFunctionTypeInfoMetadata(returnType = 1u, isC = false, hasVariableLenArg = false)),
            ),
        )

    private fun functionInfo(parameters: List<UInt> = emptyList(), helpers: List<UInt> = emptyList()) = CjoFunctionInfo(
        body = CjoFunctionBodyInfo(parameterLists = listOf(parameters), desugaredParameterLists = listOf(helpers), returnType = 1u),
    )

    private fun flags(vararg attributes: Attribute): List<ULong> {
        val lastWord = attributes.maxOfOrNull { it.ordinal / 64 } ?: return emptyList()
        return MutableList(lastWord + 1) { 0uL }.apply {
            for (attribute in attributes) {
                val word = attribute.ordinal / 64
                this[word] = this[word] or (1uL shl (attribute.ordinal % 64))
            }
        }
    }

    private fun context(pkg: Package): CfirDeserializationContext {
        val module = Module(CfirSession.Kind.Library)
        val manager = CjoManager(CjoSearchPath { null }, CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val binding = bindCjoFixture(module, manager, emptySet())
        return CfirDeserializationContext(
            pkg, CjoPackageHeader.fromPackage(pkg), module, manager,
            classSymbolLookup = manager.deserializedSymbolLookups.forBinding(binding, module, CjoDeserializationMode.FULL),
        )
    }

    private fun sourceFunction(module: Module, psi: CjNamedFunction): CfirNamedFunction = buildNamedFunction {
        source = psi.toCjPsiSourceElement()
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes()
        isLocal = false
        isMut = false
        status = CfirDeclarationStatusImpl()
        name = Name.identifier(requireNotNull(psi.name))
        symbol = CfirNamedFunctionSymbol(CallableId(FqName("defaults"), name))
        returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
    }

    private fun sourceFile(module: Module, members: List<CfirDeclaration>, psi: CjFile): CfirFile = buildFile {
        source = psi.toCjPsiSourceElement()
        moduleData = module
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes()
        symbol = CfirFileSymbol()
        name = "defaults.cj"
        packageDirective = buildPackageDirective { packageFqName = FqName("defaults"); isMacroPackage = false }
        resolveCjoSourceFixtureStatuses(module.session, members)
        declarations += members
    }.also(::registerCjoSourceFixture)

    private class Module(kind: CfirSession.Kind, cjmpMode: CfirCjmpMode? = null) : CfirModuleData() {
        override val name = Name.identifier("defaults-test")
        override val dependencies = emptyList<CfirModuleData>()
        override val refinementDependencies = emptyList<CfirModuleData>()
        override val allRefinementDependencies = emptyList<CfirModuleData>()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "defaults-test"
        override val session = object : CfirSession(kind) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
            it.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = cjmpMode))
            it.register(CfirExportAnalysisSettings::class, CfirExportAnalysisSettings())
        }
        init {
            bindSession(session)
            session.register(CfirModuleData::class, this)
        }
    }
}
