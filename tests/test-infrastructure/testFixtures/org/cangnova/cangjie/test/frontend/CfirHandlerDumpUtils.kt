package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.Directive
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure
import java.io.File

internal fun File.cfirSideFile(suffix: String): File {
    return parentFile.resolve("${nameWithoutExtension.removeSuffix(".cfir")}.cfir.$suffix")
}

internal fun TestFile.cfirSideFile(suffix: String): File = originalFile.cfirSideFile(suffix)

internal fun TestModule.originalNonAdditionalFiles(): Sequence<File> {
    return files.asSequence()
        .filterNot(TestFile::isAdditional)
        .map(TestFile::originalFile)
        .distinct()
}

internal fun TestServices.assertNoUnexpectedSideFile(expectedFile: File, enablingDirective: Directive) {
    val directives = moduleStructure.allDirectives
    if (DiagnosticsDirectives.RENDER_ALL_DIAGNOSTICS_FULL_TEXT !in directives) {
        assertions.assertFileDoesntExist(expectedFile) { enablingDirective.name }
    }
}
