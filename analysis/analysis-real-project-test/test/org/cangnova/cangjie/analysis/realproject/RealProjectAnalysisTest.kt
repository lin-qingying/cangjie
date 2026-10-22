package org.cangnova.cangjie.analysis.realproject

import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 真实项目对照测试。
 *
 * 流程由 [RealProjectTestBase] 完成：git 检出固定提交 → `cjpm clean/build`（ground truth）→
 * cjpm 源码根 + `.cjo` 依赖组装为 standalone 模块图 → 逐文件 CFIR 诊断 → JSON 报告。
 *
 * 项目列表来自 `analysis/analysis-real-project-test/projects.properties`，
 * 或 `-PrealProjectConfig` / `-PrealProjectRepo` 指定。
 */
@Tag("realProject")
class RealProjectAnalysisTest : RealProjectTestBase(
    "analysis/analysis-real-project-test/testData/realProject",
) {
    @Test
    fun realProject(mainFile: CjFile, mainModule: CjTestModule) {
        val languageVersionSettings = (mainModule.caModule as? CaSourceModule)?.languageVersionSettings
            ?: error("测试主模块不是 CaSourceModule: ${mainModule.name}")

        runRealProjects(mainFile.project, languageVersionSettings) { session ->
            val analyses = session.analyzeAll()
            session.writeReport(analyses)
            assertNoAnalysisExceptions(session, analyses)
        }
    }
}
