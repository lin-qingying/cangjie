/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.cfir.analysis.tests.services.LltCompanionSourceFilesProvider
import org.cangnova.cangjie.cfir.analysis.tests.services.MacroConstructionEnvironmentConfigurator
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.CfirTestDataConsistencyHandler
import org.cangnova.cangjie.test.config.configureDiagnosticTest
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.AdditionalFilesDirectives.SPEC_HELPERS
import org.cangnova.cangjie.test.directives.CangjieTestDirectives.WITH_STDLIB
import org.cangnova.cangjie.test.directives.MacroConstructionDirectives
import org.cangnova.cangjie.test.frontend.CfirFailingTestSuppressor
import org.cangnova.cangjie.test.frontend.MacroExpandedCfirDumpHandler
import org.cangnova.cangjie.test.runners.AbstractCangjieCompilerTest
import org.cangnova.cangjie.test.services.sourceProviders.SpecHelpersSourceFilesProvider

/**
 * CFIR 诊断测试基类
 *
 * 对应 Kotlin K2 的 AbstractFirDiagnosticTestBase
 */
abstract class AbstractCfirDiagnosticTestBase(val parser: CfirParser) : AbstractCangjieCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        configureDiagnosticTest(parser)
    }
}

abstract class AbstractCfirPsiDiagnosticTest : AbstractCfirDiagnosticTestSpecBase(CfirParser.Psi)
abstract class AbstractCfirLightTreeDiagnosticsTest : AbstractCfirDiagnosticTestSpecBase(CfirParser.LightTree)

/**
 * 官方 LLT 诊断测试基类。
 *
 * LLT 默认按官方语义中的 type-check / 非可执行诊断场景运行；
 * 只有显式声明 `CHECK_PROGRAM_ENTRY` 的用例才模拟可执行目标缺失 `main` 检查。
 */
abstract class AbstractCfirLightTreeLlTDiagnosticsTest : AbstractCfirLightTreeDiagnosticsTest()

abstract class AbstractCfirPsiLlTDiagnosticsTest : AbstractCfirPsiDiagnosticTest()

/**
 * 宏端到端诊断测试基类。
 *
 * 该类只为 `testData/macro` 使用：测试数据中的 `macro package` 源文件
 * 会由 [MacroConstructionEnvironmentConfigurator] 自动识别并接入宏包编译请求。
 */
abstract class AbstractCfirLightTreeMacroDiagnosticsTest : AbstractCfirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useDirectives(MacroConstructionDirectives)
            useConfigurators(::MacroConstructionEnvironmentConfigurator)
            firHandlersStep {
                useHandlers(::MacroExpandedCfirDumpHandler)
            }
        }
    }
}

/**
 * PSI 宏端到端诊断测试基类。
 *
 * 与 [AbstractCfirLightTreeMacroDiagnosticsTest] 使用同一套宏测试环境，
 * 仅前端 parser 切换为 [CfirParser.Psi]。
 */
abstract class AbstractCfirPsiMacroDiagnosticsTest : AbstractCfirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useDirectives(MacroConstructionDirectives)
            useConfigurators(::MacroConstructionEnvironmentConfigurator)
            firHandlersStep {
                useHandlers(::MacroExpandedCfirDumpHandler)
            }
        }
    }
}

abstract class AbstractCfirDiagnosticTestSpecBase(parser: CfirParser) : AbstractCfirDiagnosticTestBase(parser) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            baseCfirSpecDiagnosticTestConfiguration()
        }
    }
}
fun TestConfigurationBuilder.baseCfirSpecDiagnosticTestConfiguration() {
    defaultDirectives {
        +SPEC_HELPERS
        +WITH_STDLIB
    }

    useAdditionalSourceProviders(::SpecHelpersSourceFilesProvider)
    useAdditionalSourceProviders(::LltCompanionSourceFilesProvider)

    useAfterAnalysisCheckers(
        ::CfirTestDataConsistencyHandler,
        ::CfirFailingTestSuppressor,
    )

}
