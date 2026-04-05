package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

/**
 * Analysis API generated tests 的统一矩阵注册表。
 *
 * 这里集中维护三类框架事实：
 * 1. 支持的前端、平台模式与 session 组合；
 * 2. 每个组件目录对应的抽象测试模型；
 * 3. 模块种类与 testData 目录的绑定关系。
 *
 * 这样生成器本身只负责遍历和输出，不再同时承载矩阵定义。
 */
object GeneratedAnalysisApiTestRegistry {
    private val configuratorFactory = CaCfirAnalysisApiTestConfiguratorFactory

    val candidateVariants: List<GeneratedAnalysisApiVariant> = listOf(
        GeneratedAnalysisApiVariant(FrontendKind.Cfir, AnalysisApiMode.Ide, AnalysisSessionMode.Normal),
        GeneratedAnalysisApiVariant(FrontendKind.Cfir, AnalysisApiMode.Ide, AnalysisSessionMode.Dependent),
        GeneratedAnalysisApiVariant(FrontendKind.Cfir, AnalysisApiMode.Standalone, AnalysisSessionMode.Normal),
        GeneratedAnalysisApiVariant(FrontendKind.Cfir, AnalysisApiMode.Standalone, AnalysisSessionMode.Dependent),
        GeneratedAnalysisApiVariant(FrontendKind.Cfir, AnalysisApiMode.LspCompatible, AnalysisSessionMode.Normal),
        GeneratedAnalysisApiVariant(FrontendKind.Cfir, AnalysisApiMode.LspCompatible, AnalysisSessionMode.Dependent),
    )

    val models: List<GeneratedAnalysisApiModel> = listOf(
        GeneratedAnalysisApiModel(
            baseName = "FileScopeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractFileScopeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/scopeProvider/fileScope",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("fileScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "PackageScopeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractPackageScopeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/scopeProvider/packageScope",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("packageScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "MemberScopeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractMemberScopeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/scopeProvider/memberScope",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("memberScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "TypeScopeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractTypeScopeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/scopeProvider/typeScope",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("typeScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ResolveSymbolTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveSymbolTest",
            modelRelativePath = "analysis/analysis-api/testData/components/resolver/singleByPsi",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("resolveSymbol"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ResolveCallTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveCallTest",
            modelRelativePath = "analysis/analysis-api/testData/components/resolver/singleByPsi",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("memberCallInfo"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ExpressionTypeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpressionTypeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/expressionTypeProvider/expressionType",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("expressionType"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "DeclarationReturnTypeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/expressionTypeProvider/declarationReturnType",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("declarationReturnType"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "TopLevelSymbolProviderTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolProvider.AbstractTopLevelSymbolProviderTest",
            modelRelativePath = "analysis/analysis-api/testData/components/symbolProvider/topLevelLookup",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("topLevelLookup"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "RendererTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.renderer.AbstractRendererTest",
            modelRelativePath = "analysis/analysis-api/testData/components/renderer/basicRendering",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("basicRendering"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "DefaultImportsTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports.AbstractDefaultImportsTest",
            modelRelativePath = "analysis/analysis-api/testData/components/imports/defaultImports",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("defaultImports"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "SignatureSubstitutionTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.substitutors.AbstractSignatureSubstitutionTest",
            modelRelativePath = "analysis/analysis-api/testData/components/substitutors/signatureSubstitution",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("signatureSubstitution"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "TypePointerConsistencyTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractTypePointerConsistencyTest",
            modelRelativePath = "analysis/analysis-api/testData/types/typePointers/consistency",
            supportedModuleKinds = listOf(TestModuleKind.Source, TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("typePointerRestoration"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ProjectStructureTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest",
            modelRelativePath = "analysis/analysis-api/testData/projectStructure/moduleKinds/scriptSource",
            supportedModuleKinds = listOf(TestModuleKind.ScriptSource),
            includedFilePatternProvider = exactStemPattern("scriptSource"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ProjectStructureTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest",
            modelRelativePath = "analysis/analysis-api/testData/projectStructure/moduleKinds/codeFragment",
            supportedModuleKinds = listOf(TestModuleKind.CodeFragment),
            includedFilePatternProvider = exactStemPattern("codeFragment"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ProjectStructureTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest",
            modelRelativePath = "analysis/analysis-api/testData/projectStructure/moduleKinds/notUnderContentRoot",
            supportedModuleKinds = listOf(TestModuleKind.NotUnderContentRoot),
            includedFilePatternProvider = exactStemPattern("notUnderContentRoot"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ProjectStructureTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest",
            modelRelativePath = "analysis/analysis-api/testData/projectStructure/moduleKinds/librarySource",
            supportedModuleKinds = listOf(TestModuleKind.LibrarySource),
            includedFilePatternProvider = exactStemPattern("librarySource"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ProjectStructureTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest",
            modelRelativePath = "analysis/analysis-api/testData/projectStructure/moduleKinds/libraryBinary",
            supportedModuleKinds = listOf(TestModuleKind.LibraryBinary),
            includedFilePatternProvider = exactStemPattern("libraryBinary"),
        ),
    )

    fun supportedVariantsFor(moduleKind: TestModuleKind): List<GeneratedAnalysisApiVariant> {
        return candidateVariants.filter { variant ->
            configuratorFactory.supportMode(
                AnalysisApiTestConfiguratorFactoryData(
                    frontend = variant.frontend,
                    moduleKind = moduleKind,
                    analysisSessionMode = variant.analysisSessionMode,
                    analysisApiMode = variant.analysisApiMode,
                ),
            )
        }
    }

    private fun exactStemPattern(fileStem: String): (TestModuleKind) -> String = { moduleKind ->
        "^$fileStem\\.${moduleKind.defaultTestFileExtension()}$"
    }

    private fun TestModuleKind.defaultTestFileExtension(): String = when (this) {
        TestModuleKind.ScriptSource -> "cjs"
        else -> "cj"
    }
}

data class GeneratedAnalysisApiModel(
    val baseName: String,
    val abstractClassQualifiedName: String,
    val modelRelativePath: String,
    val supportedModuleKinds: List<TestModuleKind>,
    val includedFilePatternProvider: (TestModuleKind) -> String,
) {
    fun includedFilePattern(moduleKind: TestModuleKind): String = includedFilePatternProvider(moduleKind)
}

data class GeneratedAnalysisApiVariant(
    val frontend: FrontendKind,
    val analysisApiMode: AnalysisApiMode,
    val analysisSessionMode: AnalysisSessionMode,
) {
    fun generatedClassName(moduleKind: TestModuleKind, baseName: String): String {
        return buildString {
            append(frontend.suffix)
            append(analysisApiMode.suffix)
            append(analysisSessionMode.suffix)
            append(moduleKind.suffix)
            append("Module")
            append(baseName)
            append("Generated")
        }
    }
}
