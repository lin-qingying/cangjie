/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.diagnostics

import org.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * Contains all diagnostic factories that could be used in the current compilation
 */
class CjRegisteredDiagnosticFactoriesStorage {
    private val factories = mutableSetOf<BaseDiagnosticRendererFactory>()

    fun registerDiagnosticContainers(vararg containers: CjDiagnosticsContainer) {
        registerDiagnosticContainers(containers.toList())
    }

    fun registerDiagnosticContainers(containers: List<CjDiagnosticsContainer>) {
        this.factories += containers.map { it.getRendererFactory() }
    }

    val allDiagnosticFactories: List<AbstractCjDiagnosticFactory>
        get() = factories.flatMap { it.MAP.factories }
}

