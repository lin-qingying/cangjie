package org.cangnova.cangjie.analysis.test.framework.services.libraries

import org.cangnova.cangjie.test.builders.TestConfigurationBuilder

fun TestConfigurationBuilder.configureLibraryCompilationSupport() {
    useAdditionalService<TestModuleCompiler> { CjcTestModuleCompiler }
    useAdditionalService<TestModuleDecompiler> { TestModuleDecompilerDirectory() }
    useAdditionalService(::CompiledLibraryProvider)
}
