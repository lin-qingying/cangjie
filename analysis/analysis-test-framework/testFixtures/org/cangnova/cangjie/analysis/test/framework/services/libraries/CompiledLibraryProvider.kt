package org.cangnova.cangjie.analysis.test.framework.services.libraries

import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Path

/**
 * 管理测试模块编译产物的测试服务。
 */
class CompiledLibraryProvider(
    /**
     * 当前测试服务容器，用于访问 compiler 与临时目录等测试基础设施。
     */
    private val testServices: TestServices,
) : TestService {
    /**
     * 已按测试模块名缓存的编译库产物。
     */
    private val libraries = linkedMapOf<String, CompiledLibrary>()

    /**
     * 将指定测试模块编译为 library，并记录其 binary/source root。
     */
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

    /**
     * 按模块名查询已经编译完成的 library。
     */
    fun getCompiledLibrary(moduleName: String): CompiledLibrary? = libraries[moduleName]
}

/**
 * 当前测试服务容器中的 compiled library provider。
 */
val TestServices.compiledLibraryProvider: CompiledLibraryProvider by TestServices.testServiceAccessor()

/**
 * 当前测试服务容器中的 test module compiler。
 */
val TestServices.testModuleCompiler: TestModuleCompiler by TestServices.testServiceAccessor()

/**
 * 一个测试模块编译后的 library 描述。
 */
data class CompiledLibrary(
    /**
     * 编译产物的 binary root 列表。
     */
    val roots: List<Path>,
    /**
     * 与 binary 产物关联的源码 root 列表。
     */
    val sourceRoots: List<Path>,
)
