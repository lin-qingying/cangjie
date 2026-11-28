package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.*
import PackageFormat.Package
import com.google.flatbuffers.Table
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestBase
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CangJieCompilerVersion
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.TestPhase
import org.cangnova.cangjie.test.config.TestPhaseDirectives.RUN_PIPELINE_TILL
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.CJMP_MODE
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives.LANGUAGE_VERSION
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirAnalysisHandler
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.compilerConfigurationProvider
import org.cangnova.cangjie.test.services.globalMetadataInfoHandler
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import java.util.HexFormat
import java.util.zip.GZIPInputStream

/** PSI：从官方原始 SOURCE 经正式 frontend 写出新的 CJO，再验收冻结类型角色。 */
class CfirSourceFrozenTypesTest : AbstractCfirSourceFrozenTypesTest(CfirParser.Psi) {
    @Test
    fun orderedAliasRoles() = runOrderedAliasRoles()

    @Test
    fun functionAbi() = runFunctionAbi()
}

/** LightTree：独立执行同样的两个端到端正例，不复用 PSI 的 session 或产物。 */
class CfirSourceFrozenTypesLightTreeTest : AbstractCfirSourceFrozenTypesTest(CfirParser.LightTree) {
    @Test
    fun orderedAliasRoles() = runOrderedAliasRoles()

    @Test
    fun functionAbi() = runFunctionAbi()
}

/**
 * 框架沿用 CfirSourceExpressionExportTest 的诊断基类、私有 handler 和正式写出调用链。
 * Kotlin 固定 3c7a5041f238102285764d370665f56f587f9dc4：FirElementSerializer 的
 * underlying/expanded/abbreviation 各自有槽；仓颉的具体 wire 合同来自 native
 * 14276740ffec1262b81b6b5236b1d840af270b37 的 PreCheck、TypeManager 和 ASTWriter。
 * 证据见 docs/cjmp-source-helper-20261117/frozen-type-roles-authority-20261118.md
 * 及 frozen-type-roles-native-evidence-20261118.json；后者已包含 v1.1.3 实际编译产物。
 * CHIR unused 的 W7/W4 不属于本库 frontend，不向原 SOURCE 添加错误或警告期望。
 */
abstract class AbstractCfirSourceFrozenTypesTest(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    private lateinit var fixture: FrozenTypesFixture

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            LANGUAGE_VERSION with "1.1.3"
            CJMP_MODE with CfirCjmpMode.NONE
            RUN_PIPELINE_TILL with TestPhase.FRONTEND
            +WITH_STDLIB
        }
        builder.firHandlersStep {
            useHandlers({ services: TestServices -> FrozenTypesCjoHandler(services, parser, fixture) })
        }
    }

    protected fun runOrderedAliasRoles() = runCase(FrozenTypesFixture.ORDERED_ALIAS_ROLES)
    protected fun runFunctionAbi() = runCase(FrozenTypesFixture.FUNCTION_ABI)

    private fun runCase(case: FrozenTypesFixture) {
        fixture = case
        assertEquals(case.sourceSha256, frozenTypesSha256(Files.readAllBytes(case.source)), "只能使用官方成功编译的原始 SOURCE")
        runTest(case.source.toString())
    }
}

/** 数量仅固定这两个 SHA 对应的 native 候选，不能套用原 SOURCE14 或其他 fixture。 */
private enum class FrozenTypesFixture(
    val stem: String,
    val packageName: String,
    val ownerName: String,
    val methods: List<String>,
    val sourceSha256: String,
    val nativeCjoSha256: String,
    val nativeCjoBytes: Int,
    val declarationCount: Int,
    val expressionCount: Int,
    val nativeTypeCount: Int,
) {
    ORDERED_ALIAS_ROLES(
        "orderedAliasRoles", "s4_frozen_type_roles", "FrozenTypeSource",
        listOf("aliasUse", "tupleUse", "functionUse", "functionReturn", "carrierUse", "publicArrayUse"),
        "6B911947B0533ED9D7889C1CCAF820527A0349BF68F90B9B6130E41ACF92C996",
        "5BA49A5DE228E98195C52878F029DB0B5261978D04EFD090676415E9354A54A9", 19208, 48, 27, 42,
    ),
    FUNCTION_ABI(
        "functionAbi", "s4_function_abi", "FunctionAbiSource",
        listOf("fixed", "variadic", "callback", "callbackUse", "ordinary"),
        "786C2DA1E0B7063A93280F708DAE892877224D9092ED905464BACC4FE1C869B7",
        "2096007F3D12836B7FCB75847F2F2BEB2CA879CA5B180F50B53A4E7CBBB3B558", 14776, 36, 42, 11,
    );

    val source: Path
        get() = Path.of("cfir/analysis-tests/testData/diagnostics2/common-specific/source-frozen-types/$stem.cj")

    /**
     * 完整 JSON 摘要锁定提交、SDK、sourceSHA、诊断、计数及所有记录，再提取唯一的归档字段。
     * 不依赖 build/ 下易被清理的官方产物，也不把官方字节送入生产管线或输出目录。
     */
    fun nativeBytes(): ByteArray {
        val evidence = Files.readAllBytes(Path.of("docs/cjmp-source-helper-20261117/frozen-type-roles-native-evidence-20261118.json"))
        assertEquals("FA40F8BA9AB2C286FDE65FA2BD373D71654F5987EECAE6E9651CE084338D41CA", frozenTypesSha256(evidence), "完整 native authority 必须保持原取证内容")
        val archives = Regex("\"cjoGzipBase64\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"")
            .findAll(evidence.toString(Charsets.UTF_8)).map { it.groupValues[1] }.toList()
        assertEquals(2, archives.size, "固定证据只有两个有序的 CJO 归档")
        val bytes = GZIPInputStream(ByteArrayInputStream(Base64.getDecoder().decode(archives[ordinal]))).use { it.readAllBytes() }
        assertEquals(nativeCjoBytes, bytes.size)
        assertEquals(nativeCjoSha256, frozenTypesSha256(bytes), "不能拿本库 actual 充当 native 期望")
        return bytes
    }
}

private fun frozenTypesSha256(bytes: ByteArray): String =
    HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

/** 独立复制现有私有 handler 的公共 frontend 路径；所有类型、DEFAULT 和正文都由生产代码提供。 */
@OptIn(CompilerConfiguration.Internals::class)
private class FrozenTypesCjoHandler(
    testServices: TestServices,
    private val parser: CfirParser,
    private val fixture: FrozenTypesFixture,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedModules = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        assertEquals(0, checkedModules, "每个原始正例只编译一个独立包")
        val part = info.partsForDependsOnModules.single { it.module === module }
        val file = part.firFilesByTestFile.keys.filter { !it.isAdditional }.single()
        assertTrue(testServices.globalMetadataInfoHandler.getExistingMetaInfosForFile(file).isEmpty(), "原 SOURCE 没有 frontend inline 诊断")
        val original = file.originalFile.toPath().toAbsolutePath().normalize()
        assertEquals(fixture.source.toAbsolutePath().normalize(), original)
        assertEquals(fixture.sourceSha256, frozenTypesSha256(Files.readAllBytes(original)))
        assertEquals(Files.readString(original, Charsets.UTF_8), testServices.sourceFileProvider.getContentOfSourceFile(file))
        val native = FrozenTypesGraph(Package.getRootAsPackage(ByteBuffer.wrap(fixture.nativeBytes())))
        assertEquals(fixture.nativeTypeCount, native.typeCount, "归档 native 类型表来自该候选的实际证据")
        native.assertFixture(fixture, "1.1.3")
        val root = Files.createTempDirectory("source-frozen-types-")
        val disposable = Disposer.newDisposable("source-frozen-types")
        try {
            val moduleConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
            val diagnostics = DiagnosticsCollectorImpl()
            val messages = FrozenTypesMessages()
            val configuration = CompilerConfiguration().apply {
                moduleName = module.name
                languageVersionSettings = moduleConfiguration.languageVersionSettings
                useLightTree = parser == CfirParser.LightTree
                cjmpMode = module.directives[CJMP_MODE].single()
                diagnosticsCollector = diagnostics
                messageCollector = messages
                moduleConfiguration.classpathRoots.forEach { addClasspathRoot(it.path) }
                addCangJieSourceRoot(original.toString())
                cjoOutputDirectory = root.toString()
            }
            assertEquals(CfirCjmpMode.NONE, configuration.cjmpMode)
            val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            assertTrue(diagnostics.diagnostics.isEmpty(), diagnostics.diagnostics.joinToString("\n") { "${it.factoryName}: ${it.renderMessage()}" })
            assertTrue(messages.entries.none { it.first.isWarning || it.first.isError }, messages.entries.joinToString("\n"))
            val output = checkNotNull(artifact) { "合法 SOURCE 必须完成正式 frontend 与 SAVE_CJO" }.frontendOutput.outputs.single()
            assertNotSame(part.session, output.session, "不能复用诊断 harness 的 session 或 SOURCE 产品")
            val owner = output.fir.single().declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == fixture.ownerName }
            val methods = owner.declarations.filterIsInstance<CfirNamedFunction>()
            assertEquals(fixture.methods, methods.map { it.name.asString() })
            assertTrue(methods.all { it.status.isDefault }, "DEFAULT 来自实际接口正文")
            val cjo = root.resolve(CjoConstants.packageNameToPath(fixture.packageName))
            assertTrue(Files.isRegularFile(cjo), "必须读取正常 MetadataProducer/PackageWriter 刚写出的 CJO")
            val bytes = Files.readAllBytes(cjo)
            val actual = FrozenTypesGraph(Package.getRootAsPackage(ByteBuffer.wrap(bytes)))
            actual.assertFixture(fixture, CangJieCompilerVersion.VERSION)
            assertEquals(native.declarationShapes(), actual.declarationShapes(), "完整 D/E 闭包及每条有序边必须符合官方产物；类型编号可以共享或不同")
            assertArrayEquals(bytes, Files.readAllBytes(cjo), "只读验收不能改变正式产物")
            assertEquals(fixture.sourceSha256, frozenTypesSha256(Files.readAllBytes(original)))
            checkedModules++
        } finally {
            Disposer.dispose(disposable)
            root.toFile().deleteRecursively()
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertEquals(1, checkedModules, "不能跳过失败的类型角色或未完成的 SOURCE→CJO")
    }
}

private class FrozenTypesMessages : MessageCollector {
    val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
    override fun clear() = entries.clear()
    override fun hasErrors(): Boolean = entries.any { it.first.isError }
    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        entries += severity to message
    }
}

/**
 * 只读 wire 验收。所有 UInt 只在所属实际表内解释；TP 身份来自 declPtr 和 owner 的 ordinal。
 * 类型语义比较递归保留有序重复槽，不要求等值类型分配不同 ID，也不要求 native/JVM 编号相同。
 */
private class FrozenTypesGraph(private val pkg: Package) {
    val typeCount: Int get() = pkg.allTypesLength
    private val declarations = (1..pkg.allDeclsLength).map { it.toUInt() }
    private val expressions = (1..pkg.allExprsLength).map { it.toUInt() }
    private val labels = mutableMapOf<UInt, String>()

    fun assertFixture(fixture: FrozenTypesFixture, producerVersion: String) {
        // Native归档锁SDK1.1.3；JVM产物锁真实构建身份，不将schema版本冒充producer版本。
        assertEquals(producerVersion, pkg.version)
        assertEquals(fixture.packageName, pkg.fullPkgName)
        assertEquals(fixture.declarationCount, pkg.allDeclsLength, "该 sourceSHA 的完整 native D 表")
        assertEquals(fixture.expressionCount, pkg.allExprsLength, "该 sourceSHA 的完整 native E 表")
        buildDeclarationLabels()
        val owner = named(fixture.ownerName, DeclKind.InterfaceDecl)
        val info = declInfo(owner, DeclInfo.InterfaceInfo, InterfaceInfo())
        val methods = (0 until info.bodyLength).map { info.body(it) }
        assertEquals(fixture.methods, methods.map { decl(it).identifier })
        methods.forEach { method ->
            assertTrue(hasAttribute(method, Attribute.DEFAULT))
            assertNotEquals(0u, functionBody(method).body, "实际默认方法正文非零")
            assertEquals(ExprKind.Block, expr(functionBody(method).body).kind)
        }
        assertEquals(methods.toSet(), declarations.filter { decl(it).kind == DeclKind.FuncDecl && functionBody(it).body != 0u }.toSet(), "只有原生选择的六个/五个默认方法有正文")
        when (fixture) {
            FrozenTypesFixture.ORDERED_ALIAS_ROLES -> assertAliasRoles()
            FrozenTypesFixture.FUNCTION_ABI -> assertFunctionAbi()
        }
        assertClosure()
    }

    private fun assertAliasRoles() {
        val pair = named("Pair", DeclKind.TypeAliasDecl)
        val link = named("PairLink", DeclKind.TypeAliasDecl)
        val intPair = named("IntPair", DeclKind.TypeAliasDecl)
        val chain = named("IntPairChain", DeclKind.TypeAliasDecl)
        val carrier = named("Carrier", DeclKind.ClassDecl)
        val pairOf = named("pairOf", DeclKind.FuncDecl)
        val pairT = ownTypeParameters(pair, listOf("T")).single()
        val linkT = ownTypeParameters(link, listOf("T")).single()
        val carrierParameters = ownTypeParameters(carrier, listOf("T", "U"))
        val functionT = ownTypeParameters(pairOf, listOf("T")).single()
        val t = carrierParameters[0]
        val u = carrierParameters[1]
        assertEquals(5, listOf(pairT, linkT, t, u, functionT).toSet().size, "同名 T 的声明身份不能合并")
        nominal(decl(pair).type, TypeKind.Type, pair, listOf(decl(pairT).type))
        tuple(aliasRhs(pair), listOf(decl(pairT).type, decl(pairT).type))
        nominal(decl(link).type, TypeKind.Type, link, listOf(decl(linkT).type))
        nominal(aliasRhs(link), TypeKind.Type, pair, listOf(decl(linkT).type))
        nominal(decl(intPair).type, TypeKind.Type, intPair, emptyList())
        nominal(aliasRhs(intPair), TypeKind.Type, link, listOf(primitive(TypeKind.Int64)))
        nominal(decl(chain).type, TypeKind.Type, chain, emptyList())
        nominal(aliasRhs(chain), TypeKind.Type, intPair, emptyList())
        nominal(decl(carrier).type, TypeKind.Class, carrier, listOf(decl(t).type, decl(u).type))
        val classInfo = declInfo(carrier, DeclInfo.ClassInfo, ClassInfo())
        val classMethods = (0 until classInfo.bodyLength).map { classInfo.body(it) }
        assertEquals(listOf("asThis", "asNominal", "choose", "pair", "init"), classMethods.map { decl(it).identifier })
        classMethods.forEach { assertEquals(0u, functionBody(it).body, "Carrier 普通方法及隐式 init 的 native body 为 0") }
        nominal(signature("asThis", emptyList()), TypeKind.Class, carrier, listOf(decl(t).type, decl(u).type), isThis = true)
        nominal(signature("asNominal", emptyList()), TypeKind.Class, carrier, listOf(decl(t).type, decl(u).type))
        assertTypeEquals(decl(u).type, signature("choose", listOf(decl(t).type, decl(u).type, decl(t).type)))
        nominal(signature("pair", listOf(decl(t).type)), TypeKind.Type, pair, listOf(decl(t).type))
        nominal(signature("pairOf", listOf(decl(functionT).type)), TypeKind.Type, pair, listOf(decl(functionT).type))
        val i64 = primitive(TypeKind.Int64)
        val bool = primitive(TypeKind.Bool)
        assertTypeEquals(bool, signature("orderedSlots", listOf(i64, bool, i64)))
        assertTypeEquals(bool, signature("reorderedSlots", listOf(bool, i64, i64)))
        assertTypeEquals(i64, signature("repeatedReturn", listOf(i64, i64)))
        nominal(signature("aliasUse", listOf(decl(chain).type)), TypeKind.Type, chain, emptyList())
        val expandedTuple = returnedValue("aliasUse")
        tuple(expr(expandedTuple).type, listOf(i64, i64))
        reference(expandedTuple, parameters(named("aliasUse", DeclKind.FuncDecl)).single())
        assertEquals(TypeKind.Type, type(decl(parameters(named("aliasUse", DeclKind.FuncDecl)).single()).type).kind, "显式参数类型与 RefExpr 展开类型分别验收")
        val tupleType = expr(returnedValue("tupleUse")).type
        tuple(tupleType, listOf(i64, i64))
        assertTypeEquals(tupleType, signature("tupleUse", listOf(tupleType)))
        val inner = decl(parameters(named("functionReturn", DeclKind.FuncDecl)).single()).type
        functionType(inner, listOf(i64, bool, i64), bool)
        assertTypeEquals(inner, signature("functionReturn", listOf(inner)))
        reference(returnedValue("functionReturn"), parameters(named("functionReturn", DeclKind.FuncDecl)).single())
        assertTypeEquals(bool, signature("functionUse", listOf(inner, i64, bool, i64)))
        val functionUse = named("functionUse", DeclKind.FuncDecl)
        call(returnedValue("functionUse"), parameters(functionUse).first(), parameters(functionUse).drop(1), CallKind.CallStructCreation)
        val carrierUse = named("carrierUse", DeclKind.FuncDecl)
        val concrete = decl(parameters(carrierUse).single()).type
        nominal(concrete, TypeKind.Class, carrier, listOf(i64, bool))
        assertTypeEquals(concrete, signature("carrierUse", listOf(concrete)))
        val carrierCall = returnedValue("carrierUse")
        val member = call(carrierCall, named("asThis", DeclKind.FuncDecl), emptyList(), CallKind.CallDeclaredFunction, ExprKind.MemberAccess)
        reference(operands(member).single(), parameters(carrierUse).single())
        nominal(functionType(expr(member).type, emptyList(), null), TypeKind.Class, carrier, listOf(i64, bool), isThis = true)
        assertTypeEquals(concrete, expr(carrierCall).type)
        val parent = exprInfo(member, ExprInfo.ReferenceInfo, ReferenceInfo()).matchedParentTy
        assertEquals(TypeKind.Invalid, type(parent).kind, "官方保留非零 Invalid parent 槽，不能擅改为缺省 0")
        val arrayMethod = named("publicArrayUse", DeclKind.FuncDecl)
        val array = decl(parameters(arrayMethod).single()).type
        publicArray(array, publicArray(typeArgs(array).single(), i64))
        assertTypeEquals(array, signature("publicArrayUse", listOf(array)))
        reference(returnedValue("publicArrayUse"), parameters(arrayMethod).single())
        assertTypeEquals(array, expr(returnedValue("publicArrayUse")).type)
    }

    private fun assertFunctionAbi() {
        val i32 = primitive(TypeKind.Int32)
        val i64 = primitive(TypeKind.Int64)
        val ordered = listOf(i32, i64, i32)
        assertTypeEquals(i32, signature("cjOrdered", ordered))
        assertTypeEquals(i32, signature("cOrdered", ordered, isC = true))
        assertTypeEquals(i32, signature("s4ForeignFixed", ordered, isC = true))
        assertTypeEquals(i32, signature("s4ForeignVariadic", ordered, isC = true, variadic = true))
        listOf("cjOrdered", "cOrdered", "s4ForeignFixed", "s4ForeignVariadic").forEach {
            assertEquals(0u, functionBody(named(it, DeclKind.FuncDecl)).body)
        }
        val alias = named("OrderedC", DeclKind.TypeAliasDecl)
        nominal(decl(alias).type, TypeKind.Type, alias, emptyList())
        val cFunction = aliasRhs(alias)
        functionType(cFunction, ordered, i32, isC = true)
        nominal(signature("callback", emptyList()), TypeKind.Type, alias, emptyList())
        reference(returnedValue("callback"), named("cOrdered", DeclKind.FuncDecl))
        assertTypeEquals(cFunction, expr(returnedValue("callback")).type, "callback 的表达式展开为 CFunc，显式返回仍为 OrderedC")
        assertTypeEquals(cFunction, signature("callbackUse", listOf(cFunction)))
        val callbackUse = named("callbackUse", DeclKind.FuncDecl)
        reference(returnedValue("callbackUse"), parameters(callbackUse).single())
        assertTypeEquals(cFunction, expr(returnedValue("callbackUse")).type)
        assertTypeEquals(i32, signature("ordinary", ordered))
        val ordinary = named("ordinary", DeclKind.FuncDecl)
        call(returnedValue("ordinary"), named("cjOrdered", DeclKind.FuncDecl), parameters(ordinary), CallKind.CallDeclaredFunction)
        foreignBody("fixed", "s4ForeignFixed", ordered, i32)
        foreignBody("variadic", "s4ForeignVariadic", ordered + i32, i32)
    }

    /** 两条 unsafe initializer 路径必须真实进入 SOURCE E 表，包括局部声明与 return 的共享绑定。 */
    private fun foreignBody(name: String, target: String, argumentTypes: List<UInt>, resultType: UInt) {
        val function = named(name, DeclKind.FuncDecl)
        assertTypeEquals(resultType, signature(name, argumentTypes))
        val slots = block(functionBody(function).body, listOf(false, true), unsafe = false)
        val result = slots[0]
        assertEquals(DeclKind.VarDecl, decl(result).kind)
        assertEquals("result", decl(result).identifier)
        val initializer = declInfo(result, DeclInfo.VarInfo, VarInfo()).initializer
        val invocation = block(initializer, listOf(true), unsafe = true).single()
        assertTypeEquals(resultType, expr(initializer).type)
        call(invocation, named(target, DeclKind.FuncDecl), parameters(function), CallKind.CallDeclaredFunction)
        assertTypeEquals(resultType, expr(invocation).type)
        assertEquals(ExprKind.ReturnExpr, expr(slots[1]).kind)
        reference(operands(slots[1]).single(), result)
    }

    private fun ownTypeParameters(owner: UInt, names: List<String>): List<UInt> {
        val generic = checkNotNull(decl(owner).generic)
        val ids = (0 until generic.typeParametersLength).map { generic.typeParameters(it) }
        assertEquals(names, ids.map { decl(it).identifier })
        assertEquals(0, generic.constraintsLength, "这两个候选没有 where constraints")
        ids.forEachIndexed { ordinal, id ->
            assertEquals(DeclKind.GenericParamDecl, decl(id).kind)
            val info = typeInfo(decl(id).type, SemaTyInfo.GenericTyInfo, GenericTyInfo())
            localTarget(checkNotNull(info.declPtr), id)
            assertEquals("${labels.getValue(owner)}/tp[$ordinal]", labels.getValue(id), "绑定通过 owner ordinal，不通过 T 的文本")
            assertEquals(0, info.upperBoundsLength)
        }
        return ids
    }

    private fun nominal(id: UInt, kind: UShort, target: UInt, arguments: List<UInt>, isThis: Boolean = false) {
        assertEquals(kind, type(id).kind)
        val info = typeInfo(id, SemaTyInfo.CompositeTyInfo, CompositeTyInfo())
        localTarget(checkNotNull(info.declPtr), target)
        assertEquals(isThis, info.isThisTy)
        assertTypesEqual(arguments, typeArgs(id))
    }

    private fun publicArray(id: UInt, element: UInt): UInt {
        assertEquals(TypeKind.Struct, type(id).kind, "普通 std.core.Array 是 nominal struct")
        val info = typeInfo(id, SemaTyInfo.CompositeTyInfo, CompositeTyInfo())
        val target = checkNotNull(info.declPtr)
        externalTarget(target)
        assertEquals("std.core", pkg.imports(target.pkgId))
        assertEquals("_CNat5ArrayIG_E", target.decl)
        assertEquals(0u, target.index)
        assertFalse(info.isThisTy)
        assertTypesEqual(listOf(element), typeArgs(id))
        return id
    }

    private fun tuple(id: UInt, elements: List<UInt>) {
        assertEquals(TypeKind.Tuple, type(id).kind)
        assertEquals(SemaTyInfo.NONE, type(id).infoType)
        assertTypesEqual(elements, typeArgs(id))
    }

    /** SemaTy.typeArgs 只有参数；retType 独立，重复参数和重复返回不因共享 ID 丢槽。 */
    private fun functionType(id: UInt, arguments: List<UInt>, result: UInt?, isC: Boolean = false, variadic: Boolean = false): UInt {
        assertEquals(TypeKind.Func, type(id).kind)
        val info = typeInfo(id, SemaTyInfo.FuncTyInfo, FuncTyInfo())
        assertTypesEqual(arguments, typeArgs(id))
        if (result != null) assertTypeEquals(result, info.retType)
        type(info.retType)
        assertEquals(isC, info.isC)
        assertEquals(variadic, info.hasVariableLenArg)
        return info.retType
    }

    private fun signature(name: String, arguments: List<UInt>, isC: Boolean = false, variadic: Boolean = false): UInt {
        val function = named(name, DeclKind.FuncDecl)
        assertTypesEqual(arguments, parameters(function).map { decl(it).type })
        val result = functionType(decl(function).type, arguments, null, isC, variadic)
        assertTypeEquals(result, functionBody(function).retType)
        return result
    }

    private fun returnedValue(name: String): UInt {
        val returned = block(functionBody(named(name, DeclKind.FuncDecl)).body, listOf(true), unsafe = false).single()
        assertEquals(ExprKind.ReturnExpr, expr(returned).kind)
        assertEquals(TypeKind.Nothing, type(expr(returned).type).kind)
        return operands(returned).single()
    }

    private fun block(id: UInt, flags: List<Boolean>, unsafe: Boolean): List<UInt> {
        assertEquals(ExprKind.Block, expr(id).kind)
        val info = exprInfo(id, ExprInfo.BlockInfo, BlockInfo())
        val children = operands(id)
        assertEquals(flags.size, children.size)
        assertEquals(flags.size + 1, info.isExprLength)
        assertEquals(flags + unsafe, (0 until info.isExprLength).map { info.isExpr(it) })
        return children
    }

    private fun reference(id: UInt, target: UInt) {
        assertTrue(expr(id).kind in listOf(ExprKind.RefExpr, ExprKind.MemberAccess))
        val info = exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo())
        localTarget(checkNotNull(info.target), target)
        assertEquals(decl(target).identifier, info.reference)
        assertEquals(0, info.instTysLength)
        if (expr(id).kind == ExprKind.RefExpr) {
            assertEquals(emptyList<UInt>(), operands(id))
            assertEquals(0u, info.matchedParentTy)
        }
    }

    private fun call(id: UInt, target: UInt, arguments: List<UInt>, kind: UByte, baseKind: UShort = ExprKind.RefExpr): UInt {
        assertEquals(ExprKind.CallExpr, expr(id).kind)
        val info = exprInfo(id, ExprInfo.CallInfo, CallInfo())
        assertEquals(kind, info.callKind)
        assertFalse(info.hasSideEffect)
        val children = operands(id)
        assertEquals(arguments.size + 1, children.size)
        assertEquals(baseKind, expr(children[0]).kind)
        reference(children[0], target)
        arguments.zip(children.drop(1)).forEach { (parameter, argument) ->
            assertEquals(ExprKind.InvalidExpr, expr(argument).kind, "原生 FuncArg wrapper 的 kind 是 InvalidExpr")
            val argumentInfo = exprInfo(argument, ExprInfo.FuncArgInfo, FuncArgInfo())
            assertFalse(argumentInfo.withInout)
            assertFalse(argumentInfo.isDefaultVal)
            val value = operands(argument).single()
            reference(value, parameter)
            assertTypeEquals(decl(parameter).type, expr(argument).type)
            assertTypeEquals(expr(value).type, expr(argument).type)
        }
        return children[0]
    }

    /** 完整 D 表的规范身份：命名 owner、自己的 TP ordinal、自己的参数 ordinal 和正文局部声明。 */
    private fun buildDeclarationLabels() {
        declarations.filter { decl(it).kind !in listOf(DeclKind.FuncParam, DeclKind.GenericParamDecl, DeclKind.VarDecl) }.forEach {
            bindLabel(it, "${decl(it).kind}:${decl(it).identifier}")
        }
        declarations.forEach { owner ->
            val generic = decl(owner).generic
            if (generic != null) {
                (0 until generic.typeParametersLength).forEach { bindLabel(generic.typeParameters(it), "${labels.getValue(owner)}/tp[$it]") }
            }
            if (decl(owner).kind == DeclKind.FuncDecl) {
                parameters(owner).forEachIndexed { ordinal, id -> bindLabel(id, "${labels.getValue(owner)}/param[$ordinal]") }
                val body = functionBody(owner).body
                if (body != 0u) {
                    assertEquals(ExprKind.Block, expr(body).kind)
                    val info = exprInfo(body, ExprInfo.BlockInfo, BlockInfo())
                    operands(body).forEachIndexed { ordinal, id ->
                        if (!info.isExpr(ordinal)) bindLabel(id, "${labels.getValue(owner)}/local[$ordinal]:${decl(id).identifier}")
                    }
                }
            }
        }
        assertEquals(declarations.toSet(), labels.keys, "D 闭包不能遗漏参数、TP、局部声明或 implicit init")
        assertEquals(declarations.size, labels.values.toSet().size, "真实声明身份不能被同名合并")
    }

    private fun bindLabel(id: UInt, label: String) {
        decl(id)
        assertNull(labels.put(id, label), "声明 $id 不能同时绑定不同 owner/ordinal")
    }

    /** 遍历实际 SOURCE 根、声明成员、参数、initializer、引用及类型边，拒绝孤立 D/E 表项。 */
    private fun assertClosure() {
        val seenD = mutableSetOf<UInt>()
        val seenE = mutableSetOf<UInt>()
        val seenT = mutableSetOf<UInt>()
        lateinit var visitD: (UInt) -> Unit
        lateinit var visitE: (UInt) -> Unit
        lateinit var visitT: (UInt) -> Unit
        fun visitFullId(id: FullId) {
            if (id.pkgId == -2) visitD(id.index) else externalTarget(id)
        }
        visitT = { id ->
            val value = type(id)
            if (seenT.add(id)) {
                typeArgs(id).forEach { visitT(it) }
                when (value.infoType) {
                    SemaTyInfo.NONE -> Unit // 包括官方非零 Invalid matchedParentTy。
                    SemaTyInfo.FuncTyInfo -> visitT(typeInfo(id, SemaTyInfo.FuncTyInfo, FuncTyInfo()).retType)
                    SemaTyInfo.CompositeTyInfo -> visitFullId(checkNotNull(typeInfo(id, SemaTyInfo.CompositeTyInfo, CompositeTyInfo()).declPtr))
                    SemaTyInfo.GenericTyInfo -> {
                        val info = typeInfo(id, SemaTyInfo.GenericTyInfo, GenericTyInfo())
                        visitFullId(checkNotNull(info.declPtr))
                        (0 until info.upperBoundsLength).forEach { visitT(info.upperBounds(it)) }
                    }
                    else -> fail<Unit>("本批 nominal Array 不能伪装成 RawArray payload：${value.infoType}")
                }
            }
        }
        visitD = { id ->
            if (seenD.add(id)) {
                val value = decl(id)
                visitT(value.type)
                value.generic?.let { generic ->
                    assertEquals(0, generic.constraintsLength)
                    (0 until generic.typeParametersLength).forEach { visitD(generic.typeParameters(it)) }
                }
                value.genericDecl?.let { genericDecl ->
                    // 固定官方两CJO在非实例化声明的该可选槽写INVALID FullId，不是外部引用。
                    if (genericDecl.pkgId == -1) {
                        assertEquals(0u, genericDecl.index)
                        assertTrue(genericDecl.decl.isNullOrEmpty())
                    } else visitFullId(genericDecl)
                }
                (0 until value.dependenciesLength).forEach { visitFullId(checkNotNull(value.dependencies(it))) }
                when (value.kind) {
                    DeclKind.FuncDecl -> {
                        val body = functionBody(id)
                        parameters(id).forEach { visitD(it) }
                        visitT(body.retType)
                        if (body.body != 0u) visitE(body.body)
                    }
                    DeclKind.TypeAliasDecl -> visitT(aliasRhs(id))
                    DeclKind.ClassDecl -> {
                        val info = declInfo(id, DeclInfo.ClassInfo, ClassInfo())
                        (0 until info.bodyLength).forEach { visitD(info.body(it)) }
                        (0 until info.inheritedTypesLength).forEach { visitT(info.inheritedTypes(it)) }
                    }
                    DeclKind.InterfaceDecl -> {
                        val info = declInfo(id, DeclInfo.InterfaceInfo, InterfaceInfo())
                        (0 until info.bodyLength).forEach { visitD(info.body(it)) }
                        (0 until info.inheritedTypesLength).forEach { visitT(info.inheritedTypes(it)) }
                    }
                    DeclKind.FuncParam -> assertEquals(0u, declInfo(id, DeclInfo.ParamInfo, ParamInfo()).defaultVal)
                    DeclKind.GenericParamDecl -> {
                        assertEquals(TypeKind.Generic, type(value.type).kind)
                        localTarget(checkNotNull(typeInfo(value.type, SemaTyInfo.GenericTyInfo, GenericTyInfo()).declPtr), id)
                    }
                    DeclKind.VarDecl -> visitE(declInfo(id, DeclInfo.VarInfo, VarInfo()).initializer)
                    else -> fail<Unit>("官方两例没有此声明种类：${value.kind}")
                }
            }
        }
        visitE = { id ->
            if (seenE.add(id)) {
                val value = expr(id)
                visitT(value.type)
                assertEquals(0u, value.mapExpr, "本批没有 loop 或 side-effect map 回边")
                val children = operands(id)
                if (value.kind == ExprKind.Block) {
                    val info = exprInfo(id, ExprInfo.BlockInfo, BlockInfo())
                    assertEquals(children.size + 1, info.isExprLength)
                    children.forEachIndexed { ordinal, child -> if (info.isExpr(ordinal)) visitE(child) else visitD(child) }
                } else {
                    children.forEach { visitE(it) }
                    when (value.kind) {
                        ExprKind.RefExpr, ExprKind.MemberAccess -> {
                            val info = exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo())
                            visitFullId(checkNotNull(info.target))
                            (0 until info.instTysLength).forEach { visitT(info.instTys(it)) }
                            if (info.matchedParentTy != 0u) visitT(info.matchedParentTy)
                            assertEquals(if (value.kind == ExprKind.RefExpr) 0 else 1, children.size)
                        }
                        ExprKind.ReturnExpr -> {
                            assertEquals(ExprInfo.NONE, value.infoType)
                            assertEquals(1, children.size)
                            assertEquals(TypeKind.Nothing, type(value.type).kind)
                        }
                        ExprKind.CallExpr -> {
                            exprInfo(id, ExprInfo.CallInfo, CallInfo())
                            assertTrue(children.isNotEmpty())
                            children.drop(1).forEach { assertEquals(ExprKind.InvalidExpr, expr(it).kind) }
                        }
                        ExprKind.InvalidExpr -> {
                            exprInfo(id, ExprInfo.FuncArgInfo, FuncArgInfo())
                            assertEquals(1, children.size)
                        }
                        else -> fail<Unit>("官方两例没有此 SOURCE 表达式：${value.kind}")
                    }
                }
            }
        }
        declarations.filter { decl(it).isTopLevel }.forEach { visitD(it) }
        assertEquals(declarations.toSet(), seenD, "完整声明闭包必须从真实顶层 SOURCE 根可达")
        assertEquals(expressions.toSet(), seenE, "完整表达式闭包包含原默认正文、unsafe initializer 和所有 FuncArg wrapper")
        // 类型表允许原生共享；闭包检查依赖边，不以数量或 ID 不同推断类型语义。
        declarations.forEach { assertTrue(decl(it).type in seenT) }
        expressions.forEach { assertTrue(expr(it).type in seenT) }
    }

    /**
     * 对完整 D/E 图按实际声明身份比较；只有表编号被消去，类型角色、重复槽、所有必要边均保留。
     * 不比较后端 hash、路径或 mangling 的非类型字段，不从 actual 反向生成期望。
     */
    fun declarationShapes(): Map<String, String> = declarations.associate { id ->
        val value = decl(id)
        val generic = value.generic
        val genericShape = if (generic == null) "none" else
            (0 until generic.typeParametersLength).map { labels.getValue(generic.typeParameters(it)) }.toString()
        // Native无genericDecl引用槽仍写出INVALID FullId；只在这个无引用槽解释为none。
        val genericDeclShape = value.genericDecl?.let {
            if (it.pkgId == -1 && it.index == 0u && it.decl.isNullOrEmpty()) "none" else targetShape(it)
        } ?: "none"
        val dependencies = (0 until value.dependenciesLength).map { targetShape(checkNotNull(value.dependencies(it))) }
        val detail = when (value.kind) {
            DeclKind.FuncDecl -> {
                val body = functionBody(id)
                "params=${parameters(id).map { labels.getValue(it) }};ret=${typeShape(body.retType)};body=${optionalExpressionShape(body.body)}"
            }
            DeclKind.TypeAliasDecl -> "rhs=${typeShape(aliasRhs(id))}"
            DeclKind.ClassDecl -> {
                val info = declInfo(id, DeclInfo.ClassInfo, ClassInfo())
                "members=${(0 until info.bodyLength).map { labels.getValue(info.body(it)) }};supers=${(0 until info.inheritedTypesLength).map { typeShape(info.inheritedTypes(it)) }}"
            }
            DeclKind.InterfaceDecl -> {
                val info = declInfo(id, DeclInfo.InterfaceInfo, InterfaceInfo())
                "members=${(0 until info.bodyLength).map { labels.getValue(info.body(it)) }};supers=${(0 until info.inheritedTypesLength).map { typeShape(info.inheritedTypes(it)) }}"
            }
            DeclKind.FuncParam -> {
                val info = declInfo(id, DeclInfo.ParamInfo, ParamInfo())
                "named=${info.isNamedParam};member=${info.isMemberParam};default=${optionalExpressionShape(info.defaultVal)}"
            }
            DeclKind.GenericParamDecl -> "tp=${labels.getValue(id)}"
            DeclKind.VarDecl -> {
                val info = declInfo(id, DeclInfo.VarInfo, VarInfo())
                "var=${info.isVar};const=${info.isConst};initializer=${expressionShape(info.initializer)}"
            }
            else -> fail<String>("未验收的声明 ${value.kind}")
        }
        labels.getValue(id) to "${value.kind}:${value.identifier};top=${value.isTopLevel};type=${typeShape(value.type)};generic=$genericShape;genericDecl=$genericDeclShape;dependencies=$dependencies;default=${hasAttribute(id, Attribute.DEFAULT)};$detail"
    }

    private fun typeShape(id: UInt): String {
        val value = type(id)
        val args = typeArgs(id).map { typeShape(it) }
        val info = when (value.infoType) {
            SemaTyInfo.NONE -> "none"
            SemaTyInfo.FuncTyInfo -> typeInfo(id, SemaTyInfo.FuncTyInfo, FuncTyInfo()).let {
                "ret=${typeShape(it.retType)};C=${it.isC};variadic=${it.hasVariableLenArg}"
            }
            SemaTyInfo.CompositeTyInfo -> typeInfo(id, SemaTyInfo.CompositeTyInfo, CompositeTyInfo()).let {
                "decl=${targetShape(checkNotNull(it.declPtr))};This=${it.isThisTy}"
            }
            SemaTyInfo.GenericTyInfo -> typeInfo(id, SemaTyInfo.GenericTyInfo, GenericTyInfo()).let { info ->
                "tp=${targetShape(checkNotNull(info.declPtr))};uppers=${(0 until info.upperBoundsLength).map { typeShape(info.upperBounds(it)) }}"
            }
            else -> fail<String>("不能以 RawArray 代替公共 nominal Array")
        }
        return "${value.kind}($args;$info)"
    }

    private fun expressionShape(id: UInt): String {
        val value = expr(id)
        val children = operands(id)
        val detail = when (value.kind) {
            ExprKind.Block -> exprInfo(id, ExprInfo.BlockInfo, BlockInfo()).let { info ->
                "unsafe=${info.isExpr(children.size)};slots=${children.mapIndexed { ordinal, child -> if (info.isExpr(ordinal)) "E:${expressionShape(child)}" else "D:${labels.getValue(child)}" }}"
            }
            ExprKind.RefExpr, ExprKind.MemberAccess -> exprInfo(id, ExprInfo.ReferenceInfo, ReferenceInfo()).let { info ->
                "ref=${info.reference};target=${targetShape(checkNotNull(info.target))};inst=${(0 until info.instTysLength).map { typeShape(info.instTys(it)) }};parent=${if (info.matchedParentTy == 0u) "none" else typeShape(info.matchedParentTy)};operands=${children.map { expressionShape(it) }}"
            }
            ExprKind.CallExpr -> exprInfo(id, ExprInfo.CallInfo, CallInfo()).let {
                "call=${it.callKind};sideEffect=${it.hasSideEffect};operands=${children.map { expressionShape(it) }}"
            }
            ExprKind.InvalidExpr -> exprInfo(id, ExprInfo.FuncArgInfo, FuncArgInfo()).let {
                "inout=${it.withInout};default=${it.isDefaultVal};operands=${children.map { expressionShape(it) }}"
            }
            ExprKind.ReturnExpr -> "operands=${children.map { expressionShape(it) }}"
            else -> fail<String>("未验收的表达式 ${value.kind}")
        }
        return "${value.kind}:${typeShape(value.type)};$detail"
    }

    private fun optionalExpressionShape(id: UInt): String = if (id == 0u) "none" else expressionShape(id)
    private fun targetShape(id: FullId): String = if (id.pkgId == -2) labels.getValue(id.index) else {
        externalTarget(id)
        "${pkg.imports(id.pkgId)}:${id.decl}"
    }

    private fun externalTarget(id: FullId) {
        assertTrue(id.pkgId >= 0 && id.pkgId < pkg.importsLength, "非法外部FullId: pkg=${id.pkgId}, index=${id.index}, decl=${id.decl}")
        assertEquals(0u, id.index)
        assertFalse(id.decl.isNullOrEmpty())
    }

    private fun localTarget(id: FullId, target: UInt) {
        assertEquals(-2, id.pkgId, "当前包 FullId 的 native 编号为 -2")
        assertEquals(target, id.index, "必须绑定当前实际 D 表的同一声明")
        decl(target)
    }

    private fun primitive(kind: UShort): UInt =
        (1..pkg.allTypesLength).map { it.toUInt() }.first { type(it).kind == kind }

    private fun assertTypeEquals(expected: UInt, actual: UInt, message: String? = null) =
        assertEquals(typeShape(expected), typeShape(actual), message)
    private fun assertTypesEqual(expected: List<UInt>, actual: List<UInt>) = assertEquals(expected.map { typeShape(it) }, actual.map { typeShape(it) }, "有序向量保留长度、重复槽与真实绑定")
    private fun typeArgs(id: UInt): List<UInt> = type(id).let { value -> (0 until value.typeArgsLength).map { value.typeArgs(it) } }
    private fun operands(id: UInt): List<UInt> = expr(id).let { value -> (0 until value.operandsLength).map { value.operands(it) } }
    private fun named(name: String, kind: UShort): UInt = declarations.single { decl(it).identifier == name && decl(it).kind == kind }
    private fun aliasRhs(id: UInt): UInt = declInfo(id, DeclInfo.AliasInfo, AliasInfo()).aliasedTy
    private fun functionBody(id: UInt): FuncBody = checkNotNull(declInfo(id, DeclInfo.FuncInfo, FuncInfo()).funcBody)

    private fun parameters(id: UInt): List<UInt> {
        val body = functionBody(id)
        assertEquals(1, body.paramListsLength)
        val list = checkNotNull(body.paramLists(0))
        assertEquals(list.paramsLength, list.desugarsLength)
        return (0 until list.paramsLength).map {
            assertEquals(0u, list.desugars(it), "两例没有默认实参 helper；variadic 的 ... 不是参数")
            val parameter = list.params(it)
            assertEquals(DeclKind.FuncParam, decl(parameter).kind)
            parameter
        }
    }

    private fun decl(id: UInt): Decl {
        assertTrue(id > 0u && id.toLong() <= pkg.allDeclsLength, "声明索引 $id 缺失或越界")
        return checkNotNull(pkg.allDecls(id.toInt() - 1))
    }

    private fun expr(id: UInt): Expr {
        assertTrue(id > 0u && id.toLong() <= pkg.allExprsLength, "表达式索引 $id 缺失或越界")
        return checkNotNull(pkg.allExprs(id.toInt() - 1))
    }

    private fun type(id: UInt): SemaTy {
        assertTrue(id > 0u && id.toLong() <= pkg.allTypesLength, "类型索引 $id 缺失或越界")
        return checkNotNull(pkg.allTypes(id.toInt() - 1))
    }

    private fun <T : Table> declInfo(id: UInt, kind: UByte, table: T): T {
        assertEquals(kind, decl(id).infoType)
        assertNotNull(decl(id).info(table))
        return table
    }

    private fun <T : Table> exprInfo(id: UInt, kind: UByte, table: T): T {
        assertEquals(kind, expr(id).infoType)
        assertNotNull(expr(id).info(table))
        return table
    }

    private fun <T : Table> typeInfo(id: UInt, kind: UByte, table: T): T {
        assertEquals(kind, type(id).infoType)
        assertNotNull(type(id).info(table))
        return table
    }

    private fun hasAttribute(id: UInt, attribute: Attribute): Boolean {
        val word = attribute.ordinal / 64
        assertTrue(word < decl(id).attributesLength, "必须保留官方属性向量")
        return decl(id).attributes(word) and (1uL shl (attribute.ordinal % 64)) != 0uL
    }
}
