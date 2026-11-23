package org.cangnova.cangjie.analysis.decompiled.psi.text

import org.cangnova.cangjie.test.bindCjoFixture
import PackageFormat.ClassInfo
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.Package as CjoPackage
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.analysis.decompiler.stub.CjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.analysis.tests.AbstractCfirAnalysisResolveTest
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirCjmpLoadDiagnosticsComponent
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.ModifierMaskUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals as checkAssertEquals
import org.junit.jupiter.api.Assertions.assertFalse as checkAssertFalse
import org.junit.jupiter.api.Assertions.assertTrue as checkAssertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream

/**
 * 官方 cjc 1.1.3 的声明状态经过 CJO loader、compiled stub 和源码重解析后保持一致。
 *
 * 下方三个 gzip/base64 数据块保存本次官方编译的原始 CJO；测试不调用 cjc、不依赖本机 SDK。
 * 验证命令（每条 exit=0，JSON 诊断输出为空）：
 * ordinary.cj --output-type=staticlib --diagnostic-format=json --output-dir ordinary-out
 * common.cj --experimental --output-type=chir --diagnostic-format=json
 * specific.cj cjmpmodifierroundtrip.chir --experimental
 *   --common-part-cjo=cjmpmodifierroundtrip.cjo --output-type=dylib
 *   -o cjmpmodifierroundtrip.dll --diagnostic-format=json
 *
 * 输入覆盖 public/private 主构造器、public/protected/internal init、public const init、
 * public const 主构造器、static init、~init、无参/有参 enum constructor 和三种 extend。
 * private 主构造器及 static init 未进入官方公开 CJO：两个专用用例只修改内存副本的属性位，
 * 保留官方构造器的 owner、签名及 wire 索引。static 的有效 PRIVATE 来自官方
 * ParseDecl.cpp::ParseConstructor；private static init 另经 cjc 验证为 parse_conflict_modifier。
 */
class CjoConstructorAndExtendModifierRoundTripTest : AbstractCfirAnalysisResolveTest() {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /** 主构造器的 PRIMARY_CONSTRUCTOR 形态与 const 独立于有效 PUBLIC。 */
    @Test
    fun primaryConstructorVisibilityAndConstSurviveOfficialCjo() {
        val roundTrip = roundTrip(ORDINARY)
        assertConstructor(roundTrip, "PrimaryPublic", ConstructorFacts(Visibilities.Public, isPrimary = true),
            setOf(CjTokens.PUBLIC_KEYWORD))
        assertConstructor(roundTrip, "ConstPrimary", ConstructorFacts(Visibilities.Public, isPrimary = true, isConst = true),
            setOf(CjTokens.PUBLIC_KEYWORD, CjTokens.CONST_KEYWORD))
    }

    /** 普通 init 分别保留三种访问级别，const init 保留 const token 和语义状态。 */
    @Test
    fun instanceConstructorVisibilityAndConstSurviveOfficialCjo() {
        val roundTrip = roundTrip(ORDINARY)
        listOf(
            Triple("SecondaryPublic", Visibilities.Public, CjTokens.PUBLIC_KEYWORD),
            Triple("SecondaryProtected", Visibilities.Protected, CjTokens.PROTECTED_KEYWORD),
            Triple("SecondaryInternal", Visibilities.Internal, CjTokens.INTERNAL_KEYWORD),
        ).forEach { (owner, visibility, token) ->
            assertConstructor(roundTrip, owner, ConstructorFacts(visibility), setOf(token))
        }
        assertConstructor(roundTrip, "ConstInit", ConstructorFacts(Visibilities.Public, isConst = true),
            setOf(CjTokens.PUBLIC_KEYWORD, CjTokens.CONST_KEYWORD))
    }

    /** 官方不导出 private primary；副本仅替换访问属性，声明签名仍来自官方 CJO。 */
    @Test
    fun privatePrimaryConstructorRetainsAccessWithoutChangingWireSignature() {
        val bytes = replaceConstructorAttributes(
            owner = "PrimaryPublic",
            removed = setOf(Attribute.PUBLIC),
            added = setOf(Attribute.PRIVATE),
        )
        assertConstructor(roundTrip(bytes, ORDINARY.packageName), "PrimaryPublic",
            ConstructorFacts(Visibilities.Private, isPrimary = true), setOf(CjTokens.PRIVATE_KEYWORD))
    }

    /** static init 不能印出隐式 PRIVATE，但重解析后的有效 visibility 仍应是 PRIVATE。 */
    @Test
    fun staticConstructorOmitsImplicitVisibilityButKeepsEffectivePrivate() {
        val bytes = replaceConstructorAttributes(
            owner = "StaticHost",
            removed = setOf(Attribute.PUBLIC),
            added = setOf(Attribute.PRIVATE, Attribute.STATIC),
        )
        assertConstructor(roundTrip(bytes, ORDINARY.packageName), "StaticHost",
            ConstructorFacts(Visibilities.Private, isStatic = true), setOf(CjTokens.STATIC_KEYWORD))
    }

    /** finalizer 与两种 enum wire 形态均有语义 visibility，但没有可物化的 modifier。 */
    @Test
    fun finalizerAndEnumEntriesRetainEmptyModifierLists() {
        val roundTrip = roundTrip(ORDINARY)
        val owner = roundTrip.loaded.filterIsInstance<CfirClass>().single { it.name.asString() == "StaticHost" }
        val finalizer = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "~init" }
        checkAssertEquals(Visibilities.Internal, finalizer.status.visibility)
        val finalizerStub = classStub(roundTrip.stub, "StaticHost").descendants()
            .single { it.stubType == CjStubElementTypes.FINALIZER }
        checkAssertEquals(emptySet<CjKeywordToken>(), modifiers(finalizerStub))
        val parsedFinalizer = PsiTreeUtil.findChildrenOfType(roundTrip.parsed.source, CjFinalizer::class.java).single()
        checkAssertTrue(ModifierMaskUtils.modifierKeywords.none(parsedFinalizer::hasModifier))
        val restoredOwner = roundTrip.parsed.cfir.declarations.filterIsInstance<CfirClass>()
            .single { it.name.asString() == "StaticHost" }
        checkAssertEquals(finalizer.status.visibility,
            restoredOwner.declarations.filterIsInstance<CfirFinalizer>().single().status.visibility)

        val enum = roundTrip.loaded.filterIsInstance<CfirEnum>().single { it.name.asString() == "Choice" }
        val entries = enum.declarations.filterIsInstance<CfirEnumConstructor>()
        checkAssertEquals(setOf("Empty", "Value"), entries.map { it.name.asString() }.toSet())
        entries.forEach { checkAssertEquals(Visibilities.Public, it.status.visibility) }
        val entryStubs = roundTrip.stub.descendants().filter { it.stubType == CjStubElementTypes.ENUM_CONSTRUCTOR }.toList()
        checkAssertEquals(2, entryStubs.size)
        entryStubs.forEach { checkAssertEquals(emptySet<CjKeywordToken>(), modifiers(it)) }
        val parsedEntries = PsiTreeUtil.findChildrenOfType(roundTrip.parsed.source, CjEnumConstructor::class.java)
        checkAssertEquals(setOf("Empty", "Value"), parsedEntries.map { it.name }.toSet())
        parsedEntries.forEach { entry -> checkAssertTrue(ModifierMaskUtils.modifierKeywords.none(entry::hasModifier)) }
        val restoredEnum = roundTrip.parsed.cfir.declarations.filterIsInstance<CfirEnum>().single()
        val restoredEntries = restoredEnum.declarations.filterIsInstance<CfirEnumConstructor>().associateBy { it.name.asString() }
        entries.forEach { entry ->
            val restored = restoredEntries.getValue(entry.name.asString())
            checkAssertEquals(entry.status.visibility, restored.status.visibility)
            checkAssertEquals(entry.valueParameters.size, restored.valueParameters.size)
        }
    }

    /** 普通 extend 的有效 PUBLIC 不能成为不合法的 public extend 前缀。 */
    @Test
    fun ordinaryExtendOmitsItsEffectivePublicModifier() {
        assertExtend(roundTrip(ORDINARY), isCommon = false, isSpecific = false, expectedModifiers = emptySet())
    }

    /** common part 必须保留 COMMON，不能把 FROM_COMMON_PART 当成另一个源码修饰符。 */
    @Test
    fun commonExtendPreservesCjmpModifiers() {
        assertExtend(roundTrip(COMMON, CfirCjmpMode.COMMON), isCommon = true, isSpecific = false,
            expectedModifiers = setOf(CjTokens.COMMON_KEYWORD))
    }

    /** specific 侧输入已通过官方 common CJO/CHIR 配对编译，保留 SPECIFIC 身份。 */
    @Test
    fun specificExtendPreservesCjmpModifiers() {
        assertExtend(roundTrip(SPECIFIC, CfirCjmpMode.SPECIFIC), isCommon = false, isSpecific = true,
            expectedModifiers = setOf(CjTokens.SPECIFIC_KEYWORD))
    }

    /** 217 版存根的 FOREIGN 位是 14；218 只在其后追加 CONST 位 15。 */
    @Test
    fun constAndForeignKeepDistinctStableBits() {
        val foreign = ModifierMaskUtils.computeMask({ false }, { it == CjTokens.FOREIGN_KEYWORD })
        val const = ModifierMaskUtils.computeMask({ false }, { it == CjTokens.CONST_KEYWORD })
        checkAssertEquals(1L shl 14, foreign)
        checkAssertEquals(1L shl 15, const)
        checkAssertFalse(ModifierMaskUtils.maskHasModifier(foreign, CjTokens.CONST_KEYWORD))
        checkAssertFalse(ModifierMaskUtils.maskHasModifier(const, CjTokens.FOREIGN_KEYWORD))
        checkAssertTrue(ModifierMaskUtils.maskHasModifier(foreign or const, CjTokens.FOREIGN_KEYWORD))
        checkAssertTrue(ModifierMaskUtils.maskHasModifier(foreign or const, CjTokens.CONST_KEYWORD))
        checkAssertTrue(ModifierMaskUtils.modifierKeywords.indexOf(CjTokens.FOREIGN_KEYWORD) <
            ModifierMaskUtils.modifierKeywords.indexOf(CjTokens.CONST_KEYWORD))
    }

    private fun assertConstructor(
        roundTrip: RoundTrip,
        owner: String,
        expected: ConstructorFacts,
        expectedModifiers: Set<CjKeywordToken>,
    ) {
        fun find(declarations: List<CfirDeclaration>): CfirConstructor =
            declarations.filterIsInstance<CfirClass>().single { it.name.asString() == owner }
                .declarations.filterIsInstance<CfirConstructor>().single { it.status.isStatic == expected.isStatic }
        val loaded = find(roundTrip.loaded)
        checkAssertEquals(CfirDeclarationOrigin.Library, loaded.origin)
        checkAssertTrue(loaded.status is CfirResolvedDeclarationStatus, owner)
        checkAssertEquals(expected, loaded.facts(), "官方/受控 CJO 的有效状态：$owner")
        val constructorType = if (expected.isPrimary) CjStubElementTypes.PRIMARY_CONSTRUCTOR else CjStubElementTypes.SECONDARY_CONSTRUCTOR
        val stub = classStub(roundTrip.stub, owner).descendants().single { it.stubType == constructorType }
        checkAssertEquals(expectedModifiers, modifiers(stub), "compiled modifier：$owner\n" + roundTrip.parsed.source.text)
        val restored = find(roundTrip.parsed.cfir.declarations)
        checkAssertEquals(CfirDeclarationOrigin.Source, restored.origin)
        checkAssertTrue(restored.status is CfirResolvedDeclarationStatus, owner)
        checkAssertEquals(expected, restored.facts(), "重解析的有效状态：$owner\n" + roundTrip.parsed.source.text)
        checkAssertEquals(loaded.valueParameters.size, restored.valueParameters.size)
    }

    private fun assertExtend(
        roundTrip: RoundTrip,
        isCommon: Boolean,
        isSpecific: Boolean,
        expectedModifiers: Set<CjKeywordToken>,
    ) {
        val loaded = roundTrip.loaded.filterIsInstance<CfirExtend>().single()
        checkAssertEquals(CfirDeclarationOrigin.Library, loaded.origin)
        checkAssertEquals(Visibilities.Public, loaded.status.visibility)
        checkAssertEquals(isCommon, loaded.status.isCommon)
        checkAssertEquals(isSpecific, loaded.status.isSpecific)
        val stub = roundTrip.stub.descendants().single { it.stubType == CjStubElementTypes.EXTEND }
        checkAssertEquals(expectedModifiers, modifiers(stub), roundTrip.parsed.source.text)
        val source = PsiTreeUtil.findChildrenOfType(roundTrip.parsed.source, CjExtend::class.java).single()
        checkAssertEquals(expectedModifiers, ModifierMaskUtils.modifierKeywords.filter(source::hasModifier).toSet())
        val restored = roundTrip.parsed.cfir.declarations.filterIsInstance<CfirExtend>().single()
        checkAssertEquals(CfirDeclarationOrigin.Source, restored.origin)
        checkAssertEquals(loaded.status.visibility, restored.status.visibility)
        checkAssertEquals(loaded.status.isCommon, restored.status.isCommon)
        checkAssertEquals(loaded.status.isSpecific, restored.status.isSpecific)
        checkAssertEquals(loaded.declarations.size, restored.declarations.size)
    }

    private fun roundTrip(fixture: OfficialCjo, mode: CfirCjmpMode = CfirCjmpMode.NONE): RoundTrip =
        roundTrip(fixture.bytes(), fixture.packageName, mode)

    private fun roundTrip(bytes: ByteArray, packageName: String, mode: CfirCjmpMode = CfirCjmpMode.NONE): RoundTrip {
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        checkAssertEquals(packageName, pkg.fullPkgName)
        val loadedPackage = LoadedCjoPackage(
            binaryFile = LightVirtualFile("$packageName.cjo"),
            packageFqName = FqName(packageName),
            pkg = pkg,
            header = CjoPackageHeader.fromPackage(pkg),
            cjoManager = CjoManager(CjoSearchPath.forRoots(emptyList())),
            isVersionSupported = true,
        )
        val session = strictSession(mode)
        val moduleData = session.moduleData
        bindCjoFixture(moduleData, loadedPackage.cjoManager, emptySet())
        val declarations = CjoDeclarationLoader.loadDeclarations(loadedPackage, moduleData)
        checkAssertTrue(declarations.isNotEmpty(), "不能由空 CJO 通过往返断言")
        val stub = CjoFileStubBuilder.buildFileStub(loadedPackage, moduleData)
        val text = buildDecompiledText(stub)
        val source = createCjFile("modifierRoundTrip", text)
        val parseErrors = PsiTreeUtil.findChildrenOfType(source, PsiErrorElement::class.java)
        checkAssertTrue(parseErrors.isEmpty(), "反编译声明必须可解析：$parseErrors\n$text")
        val sourceSession = strictSession(mode)
        val cfir = source.toCfirFile(sourceSession)
        val diagnostics = CfirDiagnosticCollector()
        resolveToPhase(cfir, sourceSession, targetPhase, diagnostics)
        checkAssertTrue(diagnostics.rawDiagnostics.none { it.severity.isError },
            "反编译声明解析不能产生 resolve error：" + diagnostics.rawDiagnostics.map { it.factoryName } + "\n" + text)
        return RoundTrip(declarations, stub, ParsedSource(source, cfir))
    }

    private fun strictSession(mode: CfirCjmpMode): CfirSession = createTestSession().also { session ->
        // 与正式工厂一致：每个会话独立装配配对存储、编译设置及加载诊断。
        session.register(CfirCjmpMappingStorage::class, CfirCjmpMappingStorage())
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = mode))
        session.register(CfirCjmpLoadDiagnosticsComponent::class, CfirCjmpLoadDiagnosticsComponent())
        session.register(CfirExceptionHandler::class, object : CfirExceptionHandler() {
            override fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing = throw throwable
            override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing = throw throwable
        })
    }

    /** 只修改副本中一个官方构造器的 attributes；不移动声明、签名或 owner 的 wire 索引。 */
    private fun replaceConstructorAttributes(owner: String, removed: Set<Attribute>, added: Set<Attribute>): ByteArray {
        val bytes = ORDINARY.bytes()
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        val host = (0 until pkg.allDeclsLength).map { requireNotNull(pkg.allDecls(it)) }
            .single { it.kind == DeclKind.ClassDecl && it.identifier == owner }
        val info = requireNotNull(host.info(ClassInfo()) as? ClassInfo)
        val constructor = (0 until info.bodyLength).map { position ->
            val wireIndex = info.body(position)
            require(wireIndex != 0u && wireIndex != UInt.MAX_VALUE)
            requireNotNull(pkg.allDecls(wireIndex.toInt() - 1))
        }.single { it.kind == DeclKind.FuncDecl && it.hasAttribute(Attribute.CONSTRUCTOR) }
        (removed + added).groupBy { it.ordinal / Long.SIZE_BITS }.forEach { (wordIndex, attributes) ->
            require(wordIndex < constructor.attributesLength)
            var word = constructor.attributes(wordIndex)
            attributes.forEach { attribute ->
                val bit = 1uL shl (attribute.ordinal % Long.SIZE_BITS)
                word = if (attribute in added) word or bit else word and bit.inv()
            }
            checkAssertTrue(constructor.mutateAttributes(wordIndex, word))
        }
        return bytes
    }

    private fun Decl.hasAttribute(attribute: Attribute): Boolean {
        val word = attribute.ordinal / Long.SIZE_BITS
        return word < attributesLength && attributes(word) and (1uL shl (attribute.ordinal % Long.SIZE_BITS)) != 0uL
    }

    private fun modifiers(stub: StubElement<*>): Set<CjKeywordToken> {
        val modifier = stub.childrenStubs.filterIsInstance<CangJieModifierListStubImpl>().single()
        return ModifierMaskUtils.modifierKeywords.filter(modifier::hasModifier).toSet()
    }

    private fun classStub(root: CangJieFileStubImpl, name: String): CangJieClassStubImpl =
        root.childrenStubs.filterIsInstance<CangJieClassStubImpl>().single { it.getName() == name }

    private fun StubElement<*>.descendants(): Sequence<StubElement<*>> = sequence {
        yield(this@descendants)
        childrenStubs.forEach { yieldAll(it.descendants()) }
    }

    private fun CfirConstructor.facts(): ConstructorFacts = ConstructorFacts(
        visibility = status.visibility,
        isPrimary = isPrimary,
        isStatic = status.isStatic,
        isConst = status.isConst,
    )

    private data class ConstructorFacts(
        val visibility: Visibility,
        val isPrimary: Boolean = false,
        val isStatic: Boolean = false,
        val isConst: Boolean = false,
    )

    private data class ParsedSource(val source: org.cangnova.cangjie.psi.CjFile, val cfir: CfirFile)
    private data class RoundTrip(val loaded: List<CfirDeclaration>, val stub: CangJieFileStubImpl, val parsed: ParsedSource)

    /** 校验原始官方 CJO 摘要，防止压缩数据被编辑后仍冒充官方验证输入。 */
    private data class OfficialCjo(val packageName: String, val sha256: String, val gzipBase64: String) {
        fun bytes(): ByteArray {
            val compressed = Base64.getMimeDecoder().decode(gzipBase64)
            val bytes = GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            checkAssertEquals(sha256, actual, packageName)
            return bytes
        }
    }

    companion object {
        private val ORDINARY = OfficialCjo(
            "modifierroundtrip",
            "75f624dba9a3f1c941e1ca96d6cf3b298a96daaf727fe62f6ddaa1561b26f5ab",
            """
            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
            """.trimIndent(),
        )
        private val COMMON = OfficialCjo(
            "cjmpmodifierroundtrip",
            "88b0456e3030018dbcbb097d31f8ece674a1f69ec8bb198710ec93667e930dbc",
            """
            H4sIAAAAAAAC/41WT2gUVxj/dnd2M4nbsCQrLGWl6bCihO260RDDIhKMq6FaDaIQqSVdZyZ14/5jdtYGjCCiRbCFtFjIYSlBcig0ePDkwZN48CDi
            QaEHDz147KGHXErTTH/vz2THyaTrg+99733zve993+9933uTJaLJz8+eyNA4KdRLcUpQklKUpiHK0H4SLUt5GsWcyAixeYgmwJcx+h50E2SDxkHT
            oClQHDQjudvYUgWkgpq2kdPrlsnkUdB6D9EEqI3J3QjRWkSsYfpP0MVjRL/CQCUs7Cyqgv8h7bblfAXrfwG/rjVK+tXSN6ZW0PT5aqNaN8pzZdOy
            6q2aYVvlhpbVys0vSrpV1wpzpUrTzGolXTebzdPmNbOCVY3W5UpZh5phNsyaYdb0stnUCl9+ldXg/Gc+6XXNjYip3IDSnFmyW5a7pGHVjZZuawXb
            apk3iHbDx0C/JB4juZHcIfCBvx1HkTEmOSL9/KRS8kz65Td/C5KlQQmJqVdvGBSW9hVJ7tlExFlxrRUaoFO0xM90ka5Qm87jvFN0mu4jF47D/i6s
            TXJ7bWE+usJZHxnYgJG/PUofOPe4Mt7jzm99e+zHXdk3v00L3RDzLx/uxNMj88cbY6/03yvz6r0GoFNQiIMmQHf/cZxXUEzh232MWVMC8vSnDcdZ
            FqI+wSP0Av2LAGwf/Hz74eB3+jN3fujeYu1l/50f/pK22h7fmF8pn79MNhogi8pxAoO34L8zORSG4TezcX5ju/9XIHPXurF8LWVqgO8MuyGFqYXD
            N3s78jXskZQuZXh+xHCSCnrXrxjsxfjclRxkNBKY2YeLCzZK5tPRqXrTPlKYGauWrKumdSKTqRDlsG528swOS2cmz1yc5euKctXUNeL7itn2ePIc
            kE48HF+5MZvnP3S/C3q9Wq3XMqdmi50dhmmVHgTUQnKrFlK8FhK8FlYkhqvo3oUFdcsfb8tHOrWw5qkFJt7ty5loQC1EPOPn+KhEhDATEVrLm46T
            lnf46mZwPTDVCr4lJMSCf0yP/t2SAWOVZ5sqc+4jgXHJHjt7ed7U7WLA3R/a8ncYGA7wF4VhukAGcJwGnim8J0tA9f37xV+XrDbefkBdqic/ORld
            3HtpXe675sGG+Tzow06R96Zf5tblAoSvwV+BNkBPgdEQ+J/OdhxVRKZIlFRpLyTfSPZNldh1qdMlb17HgccAeoZMXN7vzKzgSToHH1y/9+1cmzzX
            J8eOlGtl+ygqkr/8O9cHV5farBIFJmzarQ7j8Dbh8VfeL+Ekz6YYxyDWkfO3Iy7/GvrQb8p4/u/93NMtToZj9/iKblxswvzI8/NJcz9Cnnzwj/1v
            Eoszl7skLpKcPs9k/wGzIxi+eAkAAA==
            """.trimIndent(),
        )
        private val SPECIFIC = OfficialCjo(
            "cjmpmodifierroundtrip",
            "8f6e6ffbf0776eb6eaafb23d37036cc8cff40673d600ddbb3e9e974d67942181",
            """
            H4sIAAAAAAAC/61WTWwbRRR+a28cpzWVlboQVQbSlQURWlwH0hBZEYoILoGmPypFatVWkbO7Lpv6Z7VeF0tpLgghDhxSVKQcIhShHiqQQEJV1QMn
            lAMHDj3QWw+9IHHghHoowo355mfj7Wbd9MBIzzPz5s2b975536x1Ipr94OTRHE2QSkOUojRlaISyNEo5GiPRdEgBc6IHCpsr9DP66xh9AWlDPoZM
            QOYh70KSkBT1GtumSn3TM/NGw7WYfgDyEMoZyEaCaBVGt1Sxh3WbMEgNEt2JEV2NE6GjDpyZihg/GkIcQ8L/DezPYrCsOWXjcvmSpRU1Y6nm1Bqm
            XbEt12206qbn2o6ma3bzeNlwG1qxUq42LV0rG4bVbM5bV6wqdjmtxaptwMy0HKtuWnXDtppa8XzQs58Fd/ahZ2pFz23Bk11zGq4nrCt2lQfRqNUa
            9byxBNtF65Jd14rLWtWuY62gY7XaqkFVWNE1HNZnbeXiykWdHfr6jpi2Iyme50YVq+y1XL6ImeM2zJbhifBWGK4HIJG4yPsYz4/n30T/1aNuV5V3
            l+U38jxN4wazqIUJmpI3OwitaDHa2aJ0zFda3m+wPl6T9hm5xsSvk7ioG3rln15MGb5zHyJRUbE6j2qfXAs35X+OY4w2aJiO0Sqv8quo/3U6Q6ew
            Zx68WAMHRmgvdjIGrQvXAxu820MXIkD5KXv49J3q1KA///STd67t1e99z8Zzwl5hseVivVwGubcn8xsKYB5lx/g0AwMVMgVZ+7fb/RWGI4w/GLOm
            RvB2o9PtrgnVHtHH6bcITL/9+rMf9n9ubPrz1eUHP/5x5NxxNv5T+loLxMbeg4OheJluMkI34DvF4Hd0d6VBEXGz+M3Ozvjb0Pl7/VwcqUtG1Oqo
            ykxiMb8obsF3RoaS4zWRAJYqfv14EvCT4HNf8waT8UiGvVVqeyDvoYm5RtObLp6drJXdy5Z7NJerEuWxb2H2RJ+tZ2dPnFvg+0py19wV4ueK2ZN5
            FHj8vTx8DP2D/Tif6byPmo5lYNXIHVso9fwxDnxD79GXnANtMnGzp1D/aZoDL67TDNAKcUBhHLgf271ugk2P9+r/RqD+mfqFUJ0MRNR/PDDejAlF
            B/1oXFitb3W77C3wIDe3ojnATB2spSXEoj9Itx9v64BvkldYUtbZcwLfsjd5cnHJMrxSxPdPkfGOAaFh9GEsMxLL4HsS5uH9Z+Dh+3fPXLtX+fsI
            Gz+UZ94M4KLK7wKFdC9F6HwetpUeDzuQX4DPKPq/ujsxTCIvVSKUlP4U+R+BrSUlbk/h5Wq4nlNAZxi/DJeUfMuZW9Fn6DRi8ON+tT8neY3PTk7b
            ddt7G0zkGPfnBTeX1oyBAhM23Y1/KUSbDsQr35VYhldSgmOQ6On5q52S/5rY+70l83na9/vF3fJkWO6eX8nPi03Y+Dt5dpBbSugbH/z2vNwvxgvb
            f4Z4fgV+71meXyzgUwmNw/4P9fXvv1XihP8AgOIacOAKAAA=
            """.trimIndent(),
        )
    }
}
