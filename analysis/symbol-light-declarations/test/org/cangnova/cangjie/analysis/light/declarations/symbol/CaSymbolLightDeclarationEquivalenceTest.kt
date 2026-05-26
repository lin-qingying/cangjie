package org.cangnova.cangjie.analysis.light.declarations.symbol

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightExtendDeclaration
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationRenderer
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `analysis:symbol-light-declarations` 的结构与注解等价性测试。
 *
 * Kotlin `symbol-light-classes` 侧缺的不是“能不能拿到一个 light declaration”，
 * 而是 source/file view 与 symbol view 是否维持同一份结构和注解语义。
 * 这里先把本地 provider 自己负责的两条核心契约锁定下来。
 */
class CaSymbolLightDeclarationEquivalenceTest : AbstractAnalysisApiExecutionTest(
    "analysis/symbol-light-declarations/testData/equivalence",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(SymbolLightDeclarationsTestServiceRegistrar)

    @Test
    fun symbolAndFileViewsRenderSameTree(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val declarations = provider.getLightDeclarations(mainFile, mainModule.caModule)

        val fileDocument = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { declaration ->
            declaration.name == "Document" && declaration.members.isNotEmpty()
        }
        val fileAlias = declarations.filterIsInstance<CaLightClassLikeDeclaration>().single { declaration ->
            declaration.name == "DocAlias"
        }
        val fileExtend = declarations.single { declaration ->
            declaration is CaLightExtendDeclaration && declaration.name == "Document"
        }
        val fileTopLevel = declarations.filterIsInstance<CaLightCallableDeclaration>().single { declaration ->
            declaration.name == "topLevel"
        }

        analyzeForTest(mainFile) {
            val symbolDocument = provider.getLightDeclaration(mainFile.classDeclaration("Document").classSymbol)
            val symbolAlias = provider.getLightDeclaration(mainFile.typeAliasDeclaration("DocAlias").symbol)
            val symbolExtend = provider.getLightDeclaration(mainFile.extendDeclaration().symbol)
            val symbolTopLevel = provider.getLightDeclaration(mainFile.namedFunction("topLevel").symbol)

            assertEquals(renderTree(fileDocument), renderTree(symbolDocument))
            assertEquals(renderTree(fileAlias), renderTree(symbolAlias))
            assertEquals(renderTree(fileExtend), renderTree(symbolExtend))
            assertEquals(renderTree(fileTopLevel), renderTree(symbolTopLevel))
        }
    }

    @Test
    fun annotationPayloadMatchesBetweenFileAndSymbolViews(
        mainFile: CjFile,
        mainModule: CjTestModule,
    ) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val declarations = provider.getLightDeclarations(mainFile, mainModule.caModule)

        val classLikeDeclarations = declarations.filterIsInstance<CaLightClassLikeDeclaration>()
        val markerCandidates = classLikeDeclarations.filter { declaration ->
            declaration.name == "Marker"
        }
        val fileMarker = markerCandidates.singleOrNull()
            ?: error(
                "File view expected exactly one `Marker` light declaration, but got ${markerCandidates.size}. " +
                    "All class-like names: ${classLikeDeclarations.map { it.name }}",
            )

        analyzeForTest(mainFile) {
            val symbolMarker = provider.getLightDeclaration(mainFile.classDeclaration("Marker").classSymbol)

            assertEquals(renderAnnotations(fileMarker), renderAnnotations(symbolMarker))
        }
    }

    private fun renderTree(declaration: CaLightDeclaration?): String {
        requireNotNull(declaration) { "Light declaration should not be null." }
        return CaLightDeclarationRenderer.renderTree(listOf(declaration))
    }

    private fun renderAnnotations(declaration: CaLightDeclaration?): List<String> {
        requireNotNull(declaration) { "Light declaration should not be null." }
        return declaration.annotations.map { annotation ->
            buildString {
                append(annotation.classId?.asString() ?: "<unknown>")
                append("(")
                append(
                    annotation.arguments.joinToString(separator = ", ") { argument ->
                        "${argument.name.asString()}=${renderAnnotationValue(argument.expression)}"
                    },
                )
                append(")")
            }
        }
    }

    private fun renderAnnotationValue(value: CaAnnotationValue): String {
        return when (value) {
            is CaAnnotationValue.ConstantValue -> value.value.render()
            is CaAnnotationValue.EnumValue -> buildString {
                append(value.callableId?.toString() ?: "<enum>")
                append(value.arguments.joinToString(prefix = "(", postfix = ")") { renderAnnotationValue(it) })
            }

            is CaAnnotationValue.TupleValue ->
                value.values.joinToString(prefix = "(", postfix = ")") { renderAnnotationValue(it) }

            is CaAnnotationValue.ClassInstanceValue -> buildString {
                append(value.classId?.asString() ?: "<class>")
                append(
                    value.arguments.joinToString(prefix = "(", postfix = ")") { argument ->
                        "${argument.name.asString()}=${renderAnnotationValue(argument.expression)}"
                    },
                )
            }

            is CaAnnotationValue.StructInstanceValue -> buildString {
                append(value.classId?.asString() ?: "<struct>")
                append(
                    value.arguments.joinToString(prefix = "(", postfix = ")") { argument ->
                        "${argument.name.asString()}=${renderAnnotationValue(argument.expression)}"
                    },
                )
            }
        }
    }
}

private fun CjFile.classDeclaration(name: String): CjTypeStatement {
    return declarations.filterIsInstance<CjTypeStatement>().singleOrNull { declaration ->
        declaration !is CjExtend && declaration.name == name
    } ?: error("Cannot find class declaration `$name` in `${this.name}`.")
}

private fun CjFile.typeAliasDeclaration(name: String): CjTypeAlias {
    return declarations.filterIsInstance<CjTypeAlias>().singleOrNull { declaration ->
        declaration.name == name
    } ?: error("Cannot find typealias declaration `$name` in `${this.name}`.")
}

private fun CjFile.namedFunction(name: String): CjNamedFunction {
    return declarations.filterIsInstance<CjNamedFunction>().singleOrNull { declaration ->
        declaration.name == name
    } ?: error("Cannot find top-level function `$name` in `${this.name}`.")
}

private fun CjTypeStatement.namedFunction(name: String): CjNamedFunction {
    return declarations.filterIsInstance<CjNamedFunction>().singleOrNull { declaration ->
        declaration.name == name
    } ?: error("Cannot find member function `$name` in `${this.name}`.")
}

private fun CjFile.extendDeclaration(): CjExtend {
    return declarations.filterIsInstance<CjExtend>().singleOrNull()
        ?: error("Cannot find extend declaration in `${this.name}`.")
}
