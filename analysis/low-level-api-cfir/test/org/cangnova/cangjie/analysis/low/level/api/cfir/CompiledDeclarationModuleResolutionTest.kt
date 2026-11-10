package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * compiled 声明的模块解析必须按“文件所属模块”而不是 use-site module。
 *
 * 用户现场：completion 在临时文件中渲染库构造器参数时，`getModule(declaration, useSiteModule)`
 * 返回临时/source module，searcher 在错误的 session 中查 class-like 符号并抛
 * “We should be able to find a symbol for class-like declaration”。
 */
class CompiledDeclarationModuleResolutionTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun compiledDeclarationModuleResolution(mainFile: CjFile) {
        val facade = mainFile.getResolutionFacadeForTest()
        val stdCoreFile = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(mainFile.project)
            .mapNotNull { virtualFile -> PsiManager.getInstance(mainFile.project).findFile(virtualFile) }
            .filterIsInstance<CjFile>()
            .firstOrNull { file -> file.name.contains("std.core") }
        assertNotNull(stdCoreFile, "必须能定位 decompiled std.core PSI")

        val arrayDeclaration = PsiTreeUtil
            .findChildrenOfType(stdCoreFile!!, CjClassLikeDeclaration::class.java)
            .firstOrNull { declaration -> declaration.name == "Array" }
        assertNotNull(arrayDeclaration, "std.core 必须包含 Array 声明")

        val constructor = PsiTreeUtil
            .findChildrenOfType(arrayDeclaration!!, CjConstructor::class.java)
            .first { candidate ->
                PsiTreeUtil.findChildrenOfType(candidate, CjParameter::class.java).isNotEmpty()
            }
        val parameter = PsiTreeUtil
            .findChildrenOfType(constructor, CjParameter::class.java)
            .first()

        // facade 来自 source module（use-site），声明来自 builtins；必须解析到 builtins 模块的符号。
        val symbol = parameter.resolveToCfirSymbol(facade)
        assertNotNull(symbol)
    }
}
