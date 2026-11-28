package org.cangnova.cangjie.cfir.analysis.tests

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirDiagnosticTestSpecBase
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.hasCjmpCommonDefaultAttribute
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirAnalysisHandler
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.frontend.cfirDiagnosticCollectorService
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.sourceFileProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** PSI 与 LightTree 分别走正规 CJMP module 图，禁止测试重新注册跨 session 模式。 */
class CfirStaticAbstractFunctionDiagnosticTest : AbstractCfirStaticAbstractBodyMatrixTest(CfirParser.Psi)
class CfirStaticAbstractFunctionLightTreeDiagnosticTest : AbstractCfirStaticAbstractBodyMatrixTest(CfirParser.LightTree)

/**
 * 完整消息与主范围由官方 SDK 1.1.3 输入指定，两个 parser 分别与这些期望比较。
 * common/specific 图、macro construction、STATUS 与 checker 都由共享测试框架配置。
 */
abstract class AbstractCfirStaticAbstractBodyMatrixTest(parser: CfirParser) : AbstractCfirDiagnosticTestSpecBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.firHandlersStep { useHandlers(::StaticAbstractBodyMatrixHandler) }
    }

    @Test
    fun legalCommonStaticAbstractFunctionKeepsAbstractAndReportsMissingBodyOnSignature() =
        runTest(ROOT + "commonSpecificStaticAbstractFunctionRequiresBody.cj")

    @Test
    fun commonStaticAbstractPropertyKeepsAbstractAndReportsMissingBodyOnSignature() =
        runTest(ROOT + "commonSpecificStaticAbstractPropertyRequiresBody.cj")

    @Test
    fun specificStaticAbstractMembersUseRealCommonModuleAndKeepModifierMismatch() =
        runTest(ROOT + "e2e/cjmpSpecificStaticAbstractBodyMatrix.cj")

    @Test
    fun propertyBodyChecksUseGetterPresenceAndPreserveStaticErrors() =
        runTest(ROOT + "commonSpecificAbstractPropertyGetterBodyMatrix.cj")

    @Test
    fun localFunctionDoesNotBecomeDirectMemberOfCommonAbstractClass() =
        runTest(ROOT + "commonSpecificLocalFunctionDirectOwner.cj")

    @Test
    fun commonWithDefaultMembersRemainConcreteAndProduceNoBodyErrors() =
        runTest(ROOT + "commonSpecificBodyDefaultExemption.cj")

    /** 官方四 probe：COMMON 缺体豁免不得跳过后续显式 abstract 带体诊断。 */
    @Test
    fun commonDefaultSkipsOnlyMissingBodyAndStillReportsExplicitAbstractBody() =
        runTest(ROOT + "commonSpecificStaticAbstractBodyCommonDefaultExemption.cj")

    private companion object {
        const val ROOT = "cfir/analysis-tests/testData/diagnostics2/common-specific/"
    }
}

/**
 * 读取真实 frontend 产物和诊断；raw 只用同一正式 module session/source 重建，既不发布也不改 STATUS。
 * 这使 raw/STATUS 两份独立结构都能验收 ABSTRACT，而无需额外创建或拼接测试 session。
 */
private class StaticAbstractBodyMatrixHandler(testServices: TestServices) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedFiles = 0

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        if (module.name == "specific") {
            val common = info.partsForDependsOnModules.single { it.module.name == "common" }
            val specific = info.partsForDependsOnModules.single { it.module.name == "specific" }
            assertNotSame(common.session, specific.session)
            val commonData = common.firFilesByTestFile.values.first().moduleData
            val specificData = specific.firFilesByTestFile.values.first().moduleData
            assertTrue(commonData in specificData.refinementDependencies, "specific 必须有真实 common refinement dependency")
        }
        val diagnosticsByFile = testServices.cfirDiagnosticCollectorService.getFrontendDiagnosticsForModule(info)
        for (part in info.partsForDependsOnModules) {
            for ((testFile, file) in part.firFilesByTestFile) {
                if (testFile.isAdditional) continue
                val source = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                val sourceFile = checkNotNull(file.sourceFile)
                val raw = if (sourceFile is CjPsiSourceFile) {
                    PsiRawCfirBuilder(part.session, annotationMetadata = CfirAnnotationMetadataBuilder())
                        .buildPreMacroCfirFile(sourceFile.psiFile as CjFile).cfirFile
                } else {
                    LightTree2Cfir(part.session, part.session.cangjieScopeProvider)
                        .buildPreMacroCfirFile(source, sourceFile, source.toSourceLinesMapping()).cfirFile
                }
                val owner = file.declarations.filterIsInstance<CfirClass>().single()
                val rawOwner = raw.declarations.filterIsInstance<CfirClass>().single()
                val ownerName = owner.name.asString()
                val commonDefaultBodyCase = commonDefaultBodyCases[testFile.name]
                val seed = ownerName == "Matrix" && testFile.name == "common.cj"
                val isLocalCase = ownerName == "LocalOwner"
                val hasCommonDefault = ownerName == "DefaultOwner" && commonDefaultBodyCase == null
                val expectedAbstract = !seed && !isLocalCase && !hasCommonDefault
                val expectedStatic = ownerName != "InstanceGetterOwner" && !isLocalCase && !hasCommonDefault
                val rawMembers = members(rawOwner)
                val resolvedMembers = members(owner)
                assertEquals(rawMembers.keys, resolvedMembers.keys)
                for ((name, member) in resolvedMembers) {
                    for (status in listOf(rawMembers.getValue(name).status, member.status)) {
                        assertEquals(expectedAbstract, status.isAbstractExplicit, ownerName + "." + name + " explicit ABSTRACT")
                        assertEquals(expectedAbstract, status.isAbstract, ownerName + "." + name + " raw/STATUS ABSTRACT")
                        assertEquals(expectedStatic, status.isStatic, ownerName + "." + name + " STATIC")
                    }
                    if (hasCommonDefault) {
                        // 源码查询与 common CJO 存储位是不同表示；语言属性仍须由正式源码产物提供。
                        assertTrue(rawMembers.getValue(name).hasCjmpCommonDefaultAttribute(), "raw COMMON_WITH_DEFAULT 必须来自真实源码")
                        assertTrue(member.hasCjmpCommonDefaultAttribute(), "STATUS 后 COMMON_WITH_DEFAULT 必须保持同一语言事实")
                    }
                    if (commonDefaultBodyCase != null) {
                        for (actual in listOf(rawMembers.getValue(name), member)) {
                            assertEquals(commonDefaultBodyCase.explicitCommon, actual.status.isCommon, "显式 COMMON 必须来自原 probe")
                            assertFalse(actual.status.isSpecific, "四 probe 都是 COMMON 源码，不派生 SPECIFIC")
                            assertEquals(
                                commonDefaultBodyCase.explicitCommon,
                                actual.hasCjmpCommonDefaultAttribute(),
                                "raw/STATUS 语言属性须区分显式 COMMON，不能读取 CJO 存储位",
                            )
                            when (actual) {
                                is CfirNamedFunction -> assertTrue(actual.body != null, "保留真实函数体")
                                is CfirProperty -> assertTrue(checkNotNull(actual.getter).body != null, "保留真实 getter 和函数体")
                                else -> error("Body matrix member must be a named function or property: ${actual::class.simpleName}")
                            }
                        }
                    }
                }
                if (ownerName == "GetterOwner" || ownerName == "InstanceGetterOwner") {
                    for (property in listOf(rawOwner, owner).flatMap { it.declarations.filterIsInstance<CfirProperty>() }) {
                        val getter = checkNotNull(property.getter)
                        assertTrue(getter.body != null, "源码 get() {} 仍有 getter body")
                    }
                }
                if (isLocalCase) {
                    for (nominal in listOf(rawOwner, owner)) {
                        val host = nominal.declarations.filterIsInstance<CfirNamedFunction>().single()
                        val local = checkNotNull(host.body).statements.filterIsInstance<CfirNamedFunction>().single()
                        assertTrue(local.isLocal)
                        assertFalse(local.status.isAbstract)
                        assertTrue(local.body != null)
                        assertEquals("local", local.name.asString())
                    }
                }
                val expected = commonDefaultBodyCase?.diagnostics ?: expectations(ownerName, seed)
                val diagnostics = diagnosticsByFile[file].orEmpty()
                assertEquals(expected.map { it.message }.sorted(), diagnostics.map { it.renderMessage() }.sorted(), ownerName)
                for (missing in expected.filter { it.signature != null }) {
                    val diagnostic = diagnostics.single { it.renderMessage() == missing.message }
                    assertEquals(CfirErrors.MISSING_FUNC_BODY.name, diagnostic.factoryName)
                    assertSignature(source, checkNotNull(missing.signature), diagnostic)
                }
                checkedFiles++
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertTrue(checkedFiles > 0, "必须验收真实 frontend 产物")
    }

    private fun members(owner: CfirClass): Map<String, CfirCallableDeclaration> =
        owner.declarations.filterIsInstance<CfirCallableDeclaration>()
            .filter { it is CfirNamedFunction || it is CfirProperty }
            .associateBy { it.symbol.callableId.callableName.asString() }

    private fun assertSignature(source: String, signature: String, diagnostic: CjDiagnostic) {
        val start = source.indexOf(signature)
        assertTrue(start >= 0, signature)
        assertEquals(listOf(TextRange(start, start + signature.length)), (diagnostic as CjDiagnosticWithSource).textRanges)
    }

    private data class Expected(val message: String, val signature: String? = null)

    private data class CommonDefaultBodyCase(val explicitCommon: Boolean, val diagnostics: List<Expected>)

    /** 与 authority 四个独立 COMMON module 逐项对应；所有例子都保留 ABSTRACT、STATIC 和真实 body。 */
    private val commonDefaultBodyCases = mapOf(
        "function-with-common.cj" to CommonDefaultBodyCase(true, listOf(
            Expected("abstract function can not have body"),
        )),
        "function-without-common.cj" to CommonDefaultBodyCase(false, listOf(
            Expected("function 'fn' can not be abstract", "func fn(): Unit"),
            Expected("abstract function can not have body"),
        )),
        "property-with-common.cj" to CommonDefaultBodyCase(true, listOf(
            Expected("abstract property can not have body"),
        )),
        "property-without-common.cj" to CommonDefaultBodyCase(false, listOf(
            Expected("property 'value' can not be abstract", "prop value: Unit"),
            Expected("abstract property can not have body"),
        )),
    )

    /** 表项按官方实际消息指定；specific 的两个 mismatch 不被过滤，局部声明正例必须零诊断。 */
    private fun expectations(owner: String, seed: Boolean): List<Expected> = when (owner) {
        "AuthorityCommon" -> listOf(Expected("function 'absFn' can not be abstract", "func absFn(): Unit"))
        "PropertyOwner", "EmptyOwner" -> listOf(Expected("property 'value' can not be abstract", "prop value: Int64"))
        "Matrix" -> if (seed) emptyList() else listOf(
            Expected("function 'fn' can not be abstract", "func fn(): Unit"),
            Expected("property 'value' can not be abstract", "prop value: Int64"),
            Expected("'specific' function modifier is not match 'common' modifier"),
            Expected("'specific' property modifier is not match 'common' modifier"),
        )
        "GetterOwner" -> listOf(
            Expected("property 'value' can not be abstract", "prop value: Unit"),
            Expected("abstract property can not have body"),
        )
        "InstanceGetterOwner" -> listOf(Expected("abstract property can not have body"))
        "LocalOwner", "DefaultOwner" -> emptyList()
        else -> error("未定义权威 body matrix 期望：" + owner)
    }
}
