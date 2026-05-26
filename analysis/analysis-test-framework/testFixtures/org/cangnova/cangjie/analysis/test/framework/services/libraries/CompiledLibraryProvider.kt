package org.cangnova.cangjie.analysis.test.framework.services.libraries

import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Path

class CompiledLibraryProvider(private val testServices: TestServices) : TestService {
    private val libraries = linkedMapOf<String, CompiledLibrary>()

    fun compileToLibrary(module: TestModule, dependencyBinaryRoots: Collection<Path>): CompiledLibrary {
        if (module.name in libraries) {
            error("Library for module `${module.name}` is already compiled.")
        }

        val compiledLibrary = testServices.testModuleCompiler.compileTestModuleToLibrary(
            module = module,
            dependencyBinaryRoots = dependencyBinaryRoots,
            testServices = testServices,
        )
        libraries[module.name] = compiledLibrary
        return compiledLibrary
    }

    fun getCompiledLibrary(moduleName: String): CompiledLibrary? = libraries[moduleName]
}

val TestServices.compiledLibraryProvider: CompiledLibraryProvider by TestServices.testServiceAccessor()
val TestServices.testModuleCompiler: TestModuleCompiler by TestServices.testServiceAccessor()

data class CompiledLibrary(
    val roots: List<Path>,
    val sourceRoots: List<Path>,
)
