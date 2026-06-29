package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.Directive
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure
import java.io.File

/**
 * 提供 `cfirSideFile` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
internal fun File.cfirSideFile(suffix: String): File {
    return parentFile.resolve("${nameWithoutExtension.removeSuffix(".cfir")}.cfir.$suffix")
}

/**
 * 提供 `cfirSideFile` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
internal fun TestFile.cfirSideFile(suffix: String): File = originalFile.cfirSideFile(suffix)

/**
 * 提供 `originalNonAdditionalFiles` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
internal fun TestModule.originalNonAdditionalFiles(): Sequence<File> {
    return files.asSequence()
        .filterNot(TestFile::isAdditional)
        .map(TestFile::originalFile)
        .distinct()
}

/**
 * 提供 `assertNoUnexpectedSideFile` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
internal fun TestServices.assertNoUnexpectedSideFile(expectedFile: File, enablingDirective: Directive) {
    val directives = moduleStructure.allDirectives
    if (DiagnosticsDirectives.RENDER_ALL_DIAGNOSTICS_FULL_TEXT !in directives) {
        assertions.assertFileDoesntExist(expectedFile) { enablingDirective.name }
    }
}
