package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.AssertionsService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.utils.convertLineSeparators
import org.cangnova.cangjie.utils.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

/**
 * 对齐 Kotlin `AbstractContextCollectorTest` 的 low-level context 收集 golden 测试。
 *
 * 这组测试同时覆盖：
 * 1. physical source file；
 * 2. copied/dangling file；
 * 3. `SELF` 与 `BODY` 两个上下文槽位。
 *
 * 当前仓颉 `ContextCollector` 只产出 `CfirTowerDataContext`，
 * 尚未接入 Kotlin 那套 smart-cast 快照，因此 golden 只断言 tower context 与 CFIR 文件渲染结果。
 */
abstract class AbstractContextCollectorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        performTest(mainFile, mainModule, testServices, outputVariant = null, preferBodyContext = false)
        performTest(mainFile, mainModule, testServices, outputVariant = "body", preferBodyContext = true)

        val copiedFile = createFileCopy(mainFile)
        performTest(copiedFile, mainModule, testServices, outputVariant = "copy", preferBodyContext = false)
        performTest(copiedFile, mainModule, testServices, outputVariant = "body.copy", preferBodyContext = true)
    }

    private fun createFileCopy(file: CjFile): CjFile {
        val copiedFile = file.copy() as CjFile
        check(!copiedFile.isPhysical) { "Copied file `${copiedFile.name}` must be non-physical." }
        check(!copiedFile.viewProvider.isEventSystemEnabled) {
            "Copied file `${copiedFile.name}` must disable event-system backed view provider."
        }
        return copiedFile
    }

    private fun performTest(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
        outputVariant: String?,
        preferBodyContext: Boolean,
    ) {
        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)
        val targetElement = testServices.expressionMarkerProvider.getBottommostSelectedElementOfType(
            mainFile,
            CjElement::class,
        )

        val elementContext = ContextCollector.process(
            resolutionFacade = resolutionFacade,
            file = cfirFile,
            targetElement = targetElement,
            preferBodyContext = preferBodyContext,
        ) ?: error("Context not found for `${targetElement.text}` in `${mainFile.name}`.")

        val actualText = buildString {
            ContextCollectorGoldenRenderer.render(elementContext, this)
            appendLine()
            append(CfirRenderer.withReadability().renderElementAsString(cfirFile))
        }

        testServices.assertions.assertMatchesContextCollectorOutput(
            expectedFile = contextCollectorOutputFile(outputVariant),
            actual = actualText,
        )
    }

    /**
     * `copy/body` 是独立 golden 变体，不能走默认 `assertEqualsToTestOutputFile()` 的回退规则；
     * 否则缺少变体文件时会误写回主 `.txt`。
     */
    private fun contextCollectorOutputFile(variant: String?): Path {
        val baseName = testDataPath.nameWithoutExtension
        val fileName = if (variant != null) "$baseName.$variant.txt" else "$baseName.txt"
        return testDataPath.parent.resolve(fileName)
    }
}

abstract class AbstractContextCollectorSourceTest : AbstractContextCollectorTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}

private object ContextCollectorGoldenRenderer {
    fun render(context: ContextCollector.Context, builder: StringBuilder) = with(builder) {
        renderTowerDataContext(context.towerDataContext)
    }

    private fun StringBuilder.renderTowerDataContext(towerDataContext: CfirTowerDataContext) {
        appendLine("Tower Data Context:")
        for ((index, towerDataElement) in towerDataContext.towerDataElements.withIndex()) {
            appendLine("    Element $index")

            towerDataElement.scope?.let { scope ->
                appendLine("        Scope: ${scope.javaClass.simpleName}")
                renderScope(scope, "            ")
            }

            towerDataElement.implicitReceiver?.let { implicitReceiver ->
                appendLine("        Implicit receiver:")
                appendLine("            ${renderSymbol(implicitReceiver.boundSymbol)}")
                appendLine("            Type: ${implicitReceiver.type.renderReadableType()}")
            }

            towerDataElement.staticScopeOwnerSymbol?.let { staticScopeOwnerSymbol ->
                appendLine("        Static scope owner symbol: ${renderSymbol(staticScopeOwnerSymbol)}")
            }
        }
    }

    private fun StringBuilder.renderScope(scope: CfirScope, indent: String) {
        val nameAwareScope = scope as? CfirContainingNamesAwareScope
        if (nameAwareScope == null) {
            appendLine("${indent}Opaque scope")
            return
        }

        val classifierNames = nameAwareScope.getClassifierNames().sortedBy(Name::asString)
        val callableNames = nameAwareScope.getCallableNames().sortedBy(Name::asString)

        appendDeclarations("Variables:", callableNames, indent, nameAwareScope::processVariablesByName)
        appendDeclarations("Classifiers:", classifierNames, indent, nameAwareScope::processClassifiersByName)
        appendDeclarations("Functions:", callableNames, indent, nameAwareScope::processFunctionsByName)
        appendDeclarations("Properties:", callableNames, indent, nameAwareScope::processPropertiesByName)
    }

    /**
     * 这里保持和 Kotlin `AbstractContextCollectorTest` 一样的“先收集、后统一渲染”结构，
     * 直接约束为 `CfirBasedSymbol`，避免把不同 scope processor 收集到 `Any` 后再回头做兜底分派。
     */
    private fun <T : CfirBasedSymbol<*>> StringBuilder.appendDeclarations(
        title: String,
        names: List<Name>,
        indent: String,
        collector: (Name, (T) -> Unit) -> Unit,
    ) {
        val declarations = collectDeclarations(names, collector)

        if (declarations.isEmpty()) {
            return
        }

        appendLine("$indent$title")
        for (declaration in declarations) {
            appendLine("$indent    ${renderSymbol(declaration)}")
        }
    }

    private fun <T : CfirBasedSymbol<*>> collectDeclarations(
        names: List<Name>,
        collector: (Name, (T) -> Unit) -> Unit,
    ): List<T> {
        return buildList {
            for (name in names) {
                collector(name) { declaration -> add(declaration) }
            }
        }
    }

    private fun renderSymbol(symbol: CfirBasedSymbol<*>): String {
        val renderer = CfirRenderer.withReadability()
        return "${symbol::class.simpleName} ${renderer.renderElementAsString(symbol.cfir)}"
    }

    private fun ConeCangJieType.renderReadableType(): String = renderForDebugging()
}

private fun AssertionsService.assertMatchesContextCollectorOutput(expectedFile: Path, actual: String) {
    if (System.getProperty("update.test.data")?.toBooleanStrictOrNull() == true) {
        expectedFile.parent.createDirectories()
        expectedFile.writeText(actual.normalizeGoldenText())
        return
    }

    assertEqualsToFile(expectedFile.toFile(), actual)
}

private fun String.normalizeGoldenText(): String {
    return trim { it <= ' ' }
        .convertLineSeparators()
        .trimTrailingWhitespacesAndAddNewlineAtEOF()
}
