@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieFileBasedDeclarationProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.DiagnosticCollectionFailures
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolvableSessionForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 通过真实 IDE structure-element 收集入口验证 Sema 与后续常量流诊断的阶段边界。 */
class SourcePostSemaDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics/postSema",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /** 单声明根必须保留 CFA，且同一模式只能报告一次。 */
    @Test
    fun constantPattern(mainFile: CjFile) {
        assertDiagnosticNames(mainFile, listOf(CfirErrors.UNREACHABLE_PATTERN.name))
    }

    /** 即使语义错误位于后面的分支，CFA 也必须等待完整声明的 Sema 结果。 */
    @Test
    fun laterSemanticError(mainFile: CjFile) {
        assertDiagnosticNames(mainFile, listOf(CfirErrors.UNRESOLVED_REFERENCE.name))
    }

    /** 类内 private 成员访问必须使用真实的 use-site 容器链，不得被当成跨类访问。 */
    @Test
    fun privateMemberAccess(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 局部解构 pattern binding 参与比较时，必须能从 owner 取得已推断的返回类型。 */
    @Test
    fun patternBindingComparison(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 枚举构造器没有函数体，BODY_RESOLVE 不得把它当成可 body 解析的目标。 */
    @Test
    fun enumConstructor(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 顶层 pattern binding 的 initializer 由 owner 派生，LL 状态保持器不得改写它。 */
    @Test
    fun topLevelPatternVariable(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 类成员函数内的解构 binding 与 private 成员访问必须共享正确的外围类容器。 */
    @Test
    fun patternBindingInClassMember(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** match 分支 binding 在比较中使用时，类型必须已从分支模式写入。 */
    @Test
    fun matchPatternBindingComparison(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 跨文件包级 constant 必须进入包的顶层 callable 索引并可解析。 */
    @Test
    fun useCrossFileConstant(mainFile: CjFile, testServices: TestServices) {
        val constantsFile = testServices.cjTestModuleStructure.allCjFiles.single { it.name == "constants.cj" }
        assertCrossFileCallableIsIndexed(constantsFile, "SHARED_CONST")
        assertNoDiagnostics(mainFile)
    }

    /** 跨文件包级 let pattern binding 必须进入包的顶层 callable 索引并可解析。 */
    @Test
    fun useCrossFileLet(mainFile: CjFile, testServices: TestServices) {
        val sharedLetFile = testServices.cjTestModuleStructure.allCjFiles.single { it.name == "sharedLet.cj" }
        assertCrossFileCallableIsIndexed(sharedLetFile, "sharedRoot")
        assertNoDiagnostics(mainFile)
    }

    private fun assertCrossFileCallableIsIndexed(declarationFile: CjFile, name: String) {
        val callableId = CallableId(FqName("test"), Name.identifier(name))
        val provider = CangJieFileBasedDeclarationProvider(declarationFile)
        val callableNames = provider.getTopLevelCallableNamesInPackage(FqName("test"))
        val indexedFiles = provider.getTopLevelCallableFiles(callableId)
        val cfirFile = declarationFile.getOrBuildCfirFile(declarationFile.getResolutionFacadeForTest())
        val cfirBindings = cfirFile.declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirPatternVariable>()
            .flatMap { patternVariable -> patternVariable.pattern.bindingVariables() }
            .map { binding -> binding.name.asString() }
        val session = declarationFile.getResolvableSessionForTest()
        val symbols = session.symbolProvider.getTopLevelCallableSymbols(FqName("test"), Name.identifier(name))

        assertTrue(
            name in callableNames.map { it.asString() },
            "Cross-file callable `$name` missing from callable-name index; names=$callableNames",
        )
        assertTrue(
            indexedFiles.isNotEmpty(),
            "Cross-file callable `$name` must be found in the platform declaration index, " +
                "but getTopLevelCallableFiles returned no files. " +
                "package=${declarationFile.packageFqName.asString()}, " +
                "declarations=" + declarationFile.declarations.joinToString { declaration ->
                    "${declaration::class.simpleName}(${declaration.text.take(60).replace('\n', ' ')})"
                },
        )
        assertTrue(
            name in cfirBindings,
            "Cross-file callable `$name` missing from raw CFIR top-level bindings; bindings=$cfirBindings",
        )
        assertTrue(
            symbols.isNotEmpty(),
            "Cross-file callable `$name` must be present in the CFIR symbol provider of " +
                "${declarationFile.name}; indexedFiles=${indexedFiles.map { it.name }}, " +
                "callableNames=$callableNames, cfirBindings=$cfirBindings.",
        )
    }

    /** 函数类型 typealias 作为参数类型时必须可直接调用。 */
    @Test
    fun functionTypeAliasCall(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** match 分支从 Option<typealias> 投影出的函数值必须保持可调用。 */
    @Test
    fun matchedFunctionTypeAliasCall(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 从 HashMap 查询并 match 出的 typealias 函数值必须保持可调用。 */
    @Test
    fun matchedFunctionTypeAliasFromMap(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 跨文件 typealias 函数值经 HashMap + match 投影后仍必须可调用。 */
    @Test
    fun matchedCrossFileFunctionTypeAlias(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 标准库宏注解（`@Test`）名位于宏命名空间，不能被报成 NOT_A_TYPE。 */
    @Test
    fun macroAnnotationType(mainFile: CjFile) {
        assertNoDiagnostics(mainFile)
    }

    /** 普通 callable 用作类型位置时必须保留 NOT_A_TYPE，不能退化为 unresolved 或空诊断。 */
    @Test
    fun functionNameAsType(mainFile: CjFile) {
        assertDiagnosticNames(mainFile, listOf(CfirErrors.NOT_A_TYPE.name))
    }

    /** 直接把顶层 binding 推到 IMPLICIT_TYPES 时，必须从 owner 投影出 resolved 类型。 */
    @Test
    fun patternBindingImplicitType(mainFile: CjFile) {
        val cfirFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val patternVariable = cfirFile.declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirPatternVariable>()
            .single()
        val binding = (patternVariable.pattern as org.cangnova.cangjie.cfir.patterns.CfirBindingPattern)
            .bindingVariable
            ?: error("Test fixture must declare a simple binding pattern.")
        binding.lazyResolveToPhase(org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.IMPLICIT_TYPES)
        val returnTypeRef = binding.returnTypeRef
        assertTrue(
            returnTypeRef is org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef,
            "Pattern binding return type must be projected from its owner, but was ${returnTypeRef::class.simpleName}.",
        )
        assertEquals(
            org.cangnova.cangjie.cfir.types.ConePrimitiveType.INT64,
            (returnTypeRef as org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef).coneType,
        )
    }

    private fun assertNoDiagnostics(mainFile: CjFile) {
        val diagnostics = collectDiagnostics(mainFile)
        assertTrue(
            diagnostics.isEmpty(),
            "Expected no diagnostics but got: " +
                diagnostics.joinToString { diagnostic ->
                    "${diagnostic.factoryName}@${diagnostic.textRanges.firstOrNull()}"
                },
        )
    }

    private fun assertDiagnosticNames(mainFile: CjFile, expected: List<String>) {
        val diagnostics = collectDiagnostics(mainFile)
        assertEquals(expected.sorted(), diagnostics.map { it.factoryName }.sorted())
    }

    /**
     * 收集诊断，并断言没有元素级异常被隔离。
     *
     * 仅断言 diagnostics 为空无法区分“根因修复”和“异常被 warn 吞掉”；这里把
     * DiagnosticCollectionFailures 也纳入断言，使异常类用例可以独立失败。
     */
    private fun collectDiagnostics(mainFile: CjFile): List<org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic> {
        DiagnosticCollectionFailures.clear()
        var failures: List<Throwable> = emptyList()
        val diagnostics = try {
            mainFile.collectDiagnosticsForFile(
                mainFile.getResolutionFacadeForTest(),
                DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
            ).toList()
        } finally {
            failures = DiagnosticCollectionFailures.drain()
        }
        assertTrue(
            failures.isEmpty(),
            "Diagnostic collection isolated ${failures.size} exception(s):\n" +
                failures.joinToString("\n") { failure -> failure.stackTraceToString() },
        )
        return diagnostics
    }
}
