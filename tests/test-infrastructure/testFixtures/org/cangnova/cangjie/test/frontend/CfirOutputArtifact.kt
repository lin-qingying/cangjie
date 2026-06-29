package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule

/**
 * 表示 `CfirOutputPartForDependsOnModule`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
data class CfirOutputPartForDependsOnModule(
    /**
     * 保存 `module`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val module: TestModule,
    /**
     * 保存 `session`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val session: CfirSession,
    /**
     * 保存 `scopeSession`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val scopeSession: ScopeSession,
    /**
     * 保存 `firAnalyzerFacade`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val firAnalyzerFacade: AbstractCfirAnalyzerFacade?,
    /**
     * 保存 `firFilesByTestFile`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val firFilesByTestFile: Map<TestFile, CfirFile>,
)

/**
 * 表示 `CfirOutputArtifact`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
abstract class CfirOutputArtifact(
    /**
     * 保存 `partsForDependsOnModules`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val partsForDependsOnModules: List<CfirOutputPartForDependsOnModule>,
) : ResultingArtifact.FrontendOutput<CfirOutputArtifact>() {
    /**
     * 保存 `allFirFilesByTestFile`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val allFirFilesByTestFile: Map<TestFile, CfirFile> =
        partsForDependsOnModules.fold(emptyMap()) { acc, part -> acc + part.firFilesByTestFile }

    /**
     * 保存 `kind`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val kind: FrontendKinds.CFIR
        get() = FrontendKinds.CFIR

    /**
     * 保存 `mainFirFilesByTestFile`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val mainFirFilesByTestFile: Map<TestFile, CfirFile> by lazy {
        allFirFilesByTestFile.filterKeys { !it.isAdditional }
    }

    /**
     * 保存 `allFirFiles`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    abstract val allFirFiles: Collection<CfirFile>
}

/**
 * 表示 `CfirOutputArtifactImpl`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirOutputArtifactImpl(parts: List<CfirOutputPartForDependsOnModule>) : CfirOutputArtifact(parts) {
    /**
     * 保存 `allFirFiles`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val allFirFiles: Collection<CfirFile>
        get() = allFirFilesByTestFile.values
}
