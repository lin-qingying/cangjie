package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.entrypoint.configuration.initializeCfirFrontendConfiguration
import org.cangnova.cangjie.config.CompilerConfiguration

/**
 * 装配 CFIR frontend 侧的默认编排服务。
 *
 * 这里负责把宏包独立编译 orchestrator 接入 frontend pipeline；
 * entrypoint 侧只保留基础配置初始化，避免反向依赖 frontend 模块。
 */
fun CompilerConfiguration.initializeCfirFrontendMacroCompilationConfiguration() {
    initializeCfirFrontendConfiguration()
    if (macroPackageCompilationOrchestrator == null) {
        macroPackageCompilationOrchestrator = ExternalCjcMacroPackageCompilationOrchestrator()
    }
}
