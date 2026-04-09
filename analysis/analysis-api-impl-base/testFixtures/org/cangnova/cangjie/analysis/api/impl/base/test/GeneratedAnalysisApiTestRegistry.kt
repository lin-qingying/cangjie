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
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("fileScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "PackageScopeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractPackageScopeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/scopeProvider/packageScope",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("packageScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "MemberScopeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractMemberScopeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/scopeProvider/memberScope",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("memberScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "TypeScopeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractTypeScopeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/scopeProvider/typeScope",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("typeScopeQueries"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ResolveSymbolTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveSymbolTest",
            modelRelativePath = "analysis/analysis-api/testData/components/resolver/singleByPsi",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("resolveSymbol"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ResolveCallTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveCallTest",
            modelRelativePath = "analysis/analysis-api/testData/components/resolver/singleByPsi",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("memberCallInfo"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ContainingDeclarationProviderByReferenceTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByReferenceTest",
            modelRelativePath = "analysis/analysis-api/testData/components/containingDeclarationProvider/containingDeclarationByReference",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "VisibilityCheckerTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.visibilityChecker.AbstractVisibilityCheckerTest",
            modelRelativePath = "analysis/analysis-api/testData/components/visibilityChecker/isVisible",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "TypeCreatorTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeCreator.AbstractTypeCreatorTest",
            modelRelativePath = "analysis/analysis-api/testData/components/typeCreator",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "OverriddenDeclarationProviderTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolRelationProvider.AbstractOverriddenDeclarationProviderTest",
            modelRelativePath = "analysis/analysis-api/testData/components/symbolRelationProvider/overriddenSymbols",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "IsSubclassOfTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolRelationProvider.AbstractIsSubclassOfTest",
            modelRelativePath = "analysis/analysis-api/testData/components/symbolRelationProvider/isSubclassOf",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ExpressionTypeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpressionTypeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/expressionTypeProvider/expressionType",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("expressionType"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "DeclarationReturnTypeTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest",
            modelRelativePath = "analysis/analysis-api/testData/components/expressionTypeProvider/declarationReturnType",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("declarationReturnType"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "TopLevelSymbolProviderTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolProvider.AbstractTopLevelSymbolProviderTest",
            modelRelativePath = "analysis/analysis-api/testData/components/symbolProvider/topLevelLookup",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("topLevelLookup"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "SymbolByReferenceTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByReferenceTest",
            modelRelativePath = "analysis/analysis-api/testData/symbols/symbolByReference",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "RendererTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.renderer.AbstractRendererTest",
            modelRelativePath = "analysis/analysis-api/testData/components/renderer/basicRendering",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("basicRendering"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "SymbolRenderingByReferenceTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractSymbolRenderingByReferenceTest",
            modelRelativePath = "analysis/analysis-api/testData/components/symbolDeclarationRenderer/symbolRenderingByReference",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "DefaultImportsTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports.AbstractDefaultImportsTest",
            modelRelativePath = "analysis/analysis-api/testData/components/imports/defaultImports",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("defaultImports"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ReferenceShorteningPlanTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShorteningPlanTest",
            modelRelativePath = "analysis/analysis-api/testData/components/references/referenceShortening",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("referenceShortening"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ReferenceShortenerTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShortenerTest",
            modelRelativePath = "analysis/analysis-api/testData/components/references/shortenRange",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("shortenRange"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ReferenceShortenerForWholeFileTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShortenerForWholeFileTest",
            modelRelativePath = "analysis/analysis-api/testData/components/references/shortenWholeFile",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("shortenWholeFile"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ReferenceImportAliasTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.references.AbstractReferenceImportAliasTest",
            modelRelativePath = "analysis/analysis-api/testData/imports/importAliases",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "IsReferenceToTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.references.AbstractIsReferenceToTest",
            modelRelativePath = "analysis/analysis-api/testData/references/isReferenceTo",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ImportOptimizationPlanTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports.AbstractImportOptimizationPlanTest",
            modelRelativePath = "analysis/analysis-api/testData/components/imports/importOptimization",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("importOptimization"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "CDocProviderTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.docProvider.AbstractCDocProviderTest",
            modelRelativePath = "analysis/analysis-api/testData/components/docProvider/cdoc",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("cdoc"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "FindUsagesTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.usages.AbstractFindUsagesTest",
            modelRelativePath = "analysis/analysis-api/testData/usages/findUsages",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = allFilesPattern(),
        ),
        GeneratedAnalysisApiModel(
            baseName = "SignatureSubstitutionTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.substitutors.AbstractSignatureSubstitutionTest",
            modelRelativePath = "analysis/analysis-api/testData/components/substitutors/signatureSubstitution",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("signatureSubstitution"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "TypePointerConsistencyTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractTypePointerConsistencyTest",
            modelRelativePath = "analysis/analysis-api/testData/types/typePointers/consistency",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("typePointerRestoration"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "SymbolPointerRestoreTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractSymbolPointerRestoreTest",
            modelRelativePath = "analysis/analysis-api/testData/sessions/symbolPointers",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("symbolPointerRestore"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "RestrictedAnalysisRejectionTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisRejectionTest",
            modelRelativePath = "analysis/analysis-api/testData/restrictedAnalysis/restriction",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("restrictedAnalysisRejection"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "RestrictedAnalysisExceptionWrappingTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisExceptionWrappingTest",
            modelRelativePath = "analysis/analysis-api/testData/restrictedAnalysis/exceptionWrapping",
            supportedModuleKinds = listOf(TestModuleKind.Source),
            includedFilePatternProvider = exactStemPattern("restrictedAnalysisException"),
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
        GeneratedAnalysisApiModel(
            baseName = "ProjectStructureTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest",
            modelRelativePath = "analysis/analysis-api/testData/projectStructure/moduleKinds/builtins",
            supportedModuleKinds = listOf(TestModuleKind.Builtins),
            includedFilePatternProvider = exactStemPattern("builtins"),
        ),
        GeneratedAnalysisApiModel(
            baseName = "ProjectStructureTest",
            abstractClassQualifiedName = "org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest",
            modelRelativePath = "analysis/analysis-api/testData/projectStructure/moduleKinds/libraryFallbackDependencies",
            supportedModuleKinds = listOf(TestModuleKind.LibraryFallbackDependencies),
            includedFilePatternProvider = exactStemPattern("libraryFallbackDependencies"),
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

    private fun allFilesPattern(): (TestModuleKind) -> String = { moduleKind ->
        "^.+\\.${moduleKind.defaultTestFileExtension()}$"
    }

    private fun TestModuleKind.defaultTestFileExtension(): String = "cj"
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
