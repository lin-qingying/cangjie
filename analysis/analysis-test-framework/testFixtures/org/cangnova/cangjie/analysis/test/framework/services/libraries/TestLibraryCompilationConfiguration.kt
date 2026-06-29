package org.cangnova.cangjie.analysis.test.framework.services.libraries

import org.cangnova.cangjie.test.builders.TestConfigurationBuilder

/**
 * 为测试配置注册 library 编译、反编译与产物缓存服务。
 */
fun TestConfigurationBuilder.configureLibraryCompilationSupport() {
    useAdditionalService<TestModuleCompiler> { CjcTestModuleCompiler }
    useAdditionalService<TestModuleDecompiler> { TestModuleDecompilerDirectory() }
    useAdditionalService(::CompiledLibraryProvider)
}
