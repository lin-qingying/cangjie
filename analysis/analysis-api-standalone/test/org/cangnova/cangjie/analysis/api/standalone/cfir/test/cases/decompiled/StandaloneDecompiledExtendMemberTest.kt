package org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.decompiled

import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder.StandaloneBuilderPlatformTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionBuilder
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 反序列化 `.cjo` 库模块中 `extend` 成员的 standalone 解析回归测试。
 *
 * 锁定的是 `.cjo` 路径上的四条契约：
 * 1. 反编译 PSI 的 `extend` 没有 `ClassId`，稳定身份只能用 `getExtendId()`；
 * 2. 库 `extend` 成员对源码调用点可见，公开符号 origin 为 `LIBRARY`；
 * 3. 库 `extend` 的**符号层查询可用**（`getTopLevelExtendSymbols` / `getExtendSymbols`），
 *    身份由 `resolveExtendIdentity` 的降级路径给出，而不是抛
 *    `missing semantic model in extendIndexStore`；
 * 4. 库 `extend` 能按目标 `ClassId` 被查到，且 `extendId` 与反编译 PSI 一致。
 *
 * 这条路径与 IDE 内置库模块（同样由 `.cjo` 反编译而来）走同一份 deserializer，
 * 但 IDE 使用 `STUBS` origin、standalone 使用 `BINARIES` origin，因此本测试只覆盖 `BINARIES` 分支。
 */
class StandaloneDecompiledExtendMemberTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-standalone/testData/decompiledExtend",
) {
    /**
     * 使用 standalone CFIR 配置执行反编译库 extend 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注册 `.cjo` 反编译服务与 standalone 平台服务，使库模块能以真实反编译 PSI 进入分析。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(
            CaAnalysisApiDecompiledTestServiceRegistrar,
            StandaloneBuilderPlatformTestServiceRegistrar,
        )

    /**
     * 验证源码模块能解析 `.cjo` 库中 `extend` 提供的成员，并锁定其公开身份形状。
     *
     * 同时覆盖库 `extend` 的符号层查询：基线在 `getTopLevelExtendSymbols` 上会抛
     * `IllegalStateException: Extend CfirExtendSymbol is missing semantic model in extendIndexStore`，
     * 该异常会被 `suppressAndLogExceptions` 吞掉并连带中止同一元素上剩余 checker——
     * 这正是 IDE 里“extend 成员整片变红”的成因，因此本用例是该症状的直接护栏。
     */
    @Test
    fun extendMemberInDecompiledLibrary(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val libraryModule = testServices.cjTestModuleStructure.getModule("lib")
        assertTrue(
            libraryModule.caModule in mainModule.caModule.directRegularDependencies,
            "主模块必须直接依赖 LibraryBinaryDecompiled 库模块，才能覆盖真实的反序列化库解析路径。",
        )

        val context = CaStandaloneSessionBuilder(mainFile.project).build(mainModule.caModule)

        val libraryFile = libraryModule.cjFiles.single()
        assertTrue(
            libraryFile is CjDecompiledFile,
            "库模块文件必须是 `.cjo` 反编译 PSI，实际=${libraryFile::class.qualifiedName}",
        )
        libraryFile as CjDecompiledFile
        val decompiledExtend = libraryFile.declarations.filterIsInstance<CjExtend>().single()
        assertNull(decompiledExtend.getClassId(), "反编译的 extend 不得持有 `ClassId`，其身份只能是 `getExtendId()`。")
        val extendId = decompiledExtend.getExtendId()
        assertTrue(extendId.isNotBlank(), "反编译 extend 必须能给出稳定 `extendId`。")
        val decompiledMember = decompiledExtend.declarations.filterIsInstance<CjNamedFunction>()
            .single { function -> function.name == "render" }
        assertSame(decompiledExtend, decompiledMember.containingTypeStatement)

        val callExpression = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjCallExpression>(mainFile)

        context.analyze(callExpression) {
            val resolvedCall = callExpression.resolveToCall()?.successfulFunctionCallOrNull()
                ?: error("源码调用点必须解析到库 extend 提供的 `Document.render`。")
            val resolvedSymbol = resolvedCall.partiallyAppliedSymbol.signature.symbol
            assertTrue(
                resolvedSymbol is CaNamedFunctionSymbol,
                "解析结果应为命名函数符号，实际=${resolvedSymbol::class.qualifiedName}",
            )
            resolvedSymbol as CaNamedFunctionSymbol

            assertEquals("render", resolvedSymbol.name?.asString())
            assertEquals(CaSymbolOrigin.LIBRARY, resolvedSymbol.origin)
            val callableId = resolvedSymbol.callableId
            assertNotNull(callableId, "库 extend 成员必须能给出 `callableId`。")
            callableId!!
            assertEquals(FqName("sample.lib"), callableId.packageName)
            assertNull(callableId.classId, "库 extend 成员不是 class-like 成员，`callableId` 只能是包级身份。")

            // 库 extend 的**符号层**查询：语义模型只在源码 extend 的 EXTENSIONS 阶段建立，
            // 因此 `.cjo` 反序列化的 extend 在 extendIndexStore 里没有条目。这里锁定修复后的
            // 契约——`resolveExtendIdentity` 必须为库 extend 构造降级身份，而不是抛
            // `Extend `CfirExtendSymbol` is missing semantic model in extendIndexStore`；
            // 后者会连带中止该 CFIR 元素上剩余 checker，表现为 IDE 里“extend 成员整片变红”。
            assertTrue(extendId.isNotBlank())

            val libraryExtends = getTopLevelExtendSymbols(FqName("sample.lib"))
            assertEquals(
                1,
                libraryExtends.size,
                "库模块的 extend 必须能按包查询到；查询失败说明库 extend 身份还原仍在抛错。",
            )
            val libraryExtend = libraryExtends.single()
            assertEquals(
                1,
                libraryExtend.declaredMemberScope.callables(Name.identifier("render")).count(),
                "库 extend 的声明成员 render 必须可查询——extend 成员补全与导航都依赖它。",
            )
            assertEquals(
                1,
                libraryExtend.declaredMemberScope.callables(Name.identifier("combine")).count(),
                "库 extend 的第二个声明成员 combine 同样必须可查询。",
            )

            val documentSymbol = getTopLevelClassLikeSymbols(FqName("sample.lib"), Name.identifier("Document")).single()
            val documentClassId = requireNotNull(documentSymbol.classId) { "库 class Document 必须有 ClassId。" }
            assertEquals(
                1,
                getExtendSymbols(documentClassId).size,
                "库 extend 必须能按目标 ClassId 查到，且不抛错。",
            )
        }
    }
}