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

package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

/**
 * 定义 `CfirDiagnosticsMap` 类型别名，统一CFIR 前端测试中的回调或构造签名。
 */
private typealias CfirDiagnosticsMap = Map<CfirFile, List<CjDiagnostic>>

/**
 * 表示 `CfirDiagnosticCollectorService`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
open class CfirDiagnosticCollectorService(
    @Suppress("UNUSED_PARAMETER") val testServices: TestServices,
) : TestService {
    /**
     * 保存 `cache`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val cache: MutableMap<CfirOutputArtifact, CfirDiagnosticsMap> = mutableMapOf()

    /**
     * 提供 `getFrontendDiagnosticsForModule` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    open fun getFrontendDiagnosticsForModule(info: CfirOutputArtifact): CfirDiagnosticsMap {
        return cache.getOrPut(info) { computeDiagnostics(info) }
    }

    /**
     * 保存 `containsErrorDiagnostics`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val containsErrorDiagnostics: Boolean
        get() = cache.values.any { perFile ->
            perFile.values.flatten().any { it.severity == Severity.ERROR }
        }

    /**
     * 执行 `containsErrors` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    fun containsErrors(info: CfirOutputArtifact): Boolean {
        return getFrontendDiagnosticsForModule(info).values.flatten().any { it.severity == Severity.ERROR }
    }

    /**
     * 提供 `computeDiagnostics` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun computeDiagnostics(info: CfirOutputArtifact): CfirDiagnosticsMap {
        val allFiles = info.partsForDependsOnModules.flatMap { it.firFilesByTestFile.values }
        val diagnosticsByFile = linkedMapOf<CfirFile, MutableList<CjDiagnostic>>()
        allFiles.forEach { diagnosticsByFile[it] = mutableListOf() }

        val platformPart = info.partsForDependsOnModules.last()
        val lazyDeclarationResolver = platformPart.session.lazyDeclarationResolver

        lazyDeclarationResolver.disableLazyResolveContractChecksInside {
            for (part in info.partsForDependsOnModules) {
                val diagnosticsCollector = DiagnosticsCollectorImpl()
                val diagnostics = part.session.runCheckers(
                    scopeSession = part.scopeSession,
                    firFiles = part.firFilesByTestFile.values,
                    diagnosticsCollector = diagnosticsCollector,
                )
                appendComputedDiagnostics(diagnostics, diagnosticsByFile)
            }
        }

        return diagnosticsByFile.mapValues { (_, value) -> value.toList() }
    }

    /**
     * 提供 `appendComputedDiagnostics` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun appendComputedDiagnostics(
        diagnostics: CfirDiagnosticsMap,
        destination: MutableMap<CfirFile, MutableList<CjDiagnostic>>,
    ) {
        for ((file, fileDiagnostics) in diagnostics) {
            if (fileDiagnostics.isEmpty()) continue
            destination.getOrPut(file) { mutableListOf() }.addAll(fileDiagnostics)
        }
    }
}

/**
 * 保存 `TestServices.cfirDiagnosticCollectorService`，供CFIR 前端测试在测试执行期间读取或传递。
 */
val TestServices.cfirDiagnosticCollectorService: CfirDiagnosticCollectorService by TestServices.testServiceAccessor()
