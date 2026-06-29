package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.packageScope
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * `scopeProvider.packageScope` 的抽象测试。
 *
 * 对齐 Kotlin `AbstractPackageScopeTest`：
 * 这里仅选择目标 package scope，本体渲染与名字断言由基座统一负责。
 */
abstract class AbstractPackageScopeTest : AbstractScopeTestBase() {
    context(analysisSession: CaSession)
    /**
     * 获取当前文件所属包的公开 package scope。
     *
     * 若 package symbol 无法恢复，测试直接失败以暴露项目结构或包 provider 问题。
     */
    override fun getScope(mainFile: CjFile, testServices: TestServices): CaScope =
        with(analysisSession) { getPackageSymbol(mainFile.packageFqName) }?.packageScope
            ?: error("Package symbol '${mainFile.packageFqName.asString()}' was not found")
}
