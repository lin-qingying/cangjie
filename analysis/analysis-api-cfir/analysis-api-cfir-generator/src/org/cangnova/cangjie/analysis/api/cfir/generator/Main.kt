/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import org.cangnova.cangjie.analysis.api.cfir.generator.DiagnosticClassGenerator.generate
import org.cangnova.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST
import java.nio.file.Paths

fun main(args: Array<String>) {
    require(args.size == 2) {
        """
        Generator requires the following arguments (in this particular order):
        - generated classes package name
        - path to the directory where generated classes will be placed
        """.trimIndent()
    }
    val packageName = args.first()
    val rootPath = Paths.get(args.last()).toAbsolutePath()
    val diagnostics = DIAGNOSTICS_LIST
    generate(rootPath, diagnostics, packageName)
}
