package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByReferenceTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider.AbstractDataFlowInfoTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCollectDiagnosticsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractElementDiagnosticsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.docProvider.AbstractCDocProviderTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractExpressionInformationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpressionTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports.AbstractDefaultImportsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports.AbstractImportOptimizationPlanTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShortenerForWholeFileTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShortenerTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShorteningPlanTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.renderer.AbstractRendererTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveCallTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveSymbolTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractFileScopeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractMemberScopeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractPackageScopeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractTypeScopeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.substitutors.AbstractSignatureSubstitutionTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer.AbstractSymbolRenderingByReferenceTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolProvider.AbstractTopLevelSymbolProviderTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolRelationProvider.AbstractIsSubclassOfTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.symbolRelationProvider.AbstractOverriddenDeclarationProviderTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeCreator.AbstractTypeCreatorTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeRelationChecker.AbstractTypeRelationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.usages.AbstractFindUsagesTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.visibilityChecker.AbstractVisibilityCheckerTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.references.AbstractIsReferenceToTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.references.AbstractReferenceImportAliasTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisExceptionWrappingTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisRejectionTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractSymbolPointerRestoreTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByReferenceTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractTypePointerConsistencyTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

fun AnalysisApiTestGroup.generateAnalysisApiTests() {
    component("scopeProvider") {
        test<AbstractFileScopeTest> { model(it, "fileScope", pattern = exactStemPattern("fileScopeQueries", it)) }
        test<AbstractPackageScopeTest> { model(it, "packageScope", pattern = exactStemPattern("packageScopeQueries", it)) }
        test<AbstractMemberScopeTest> { model(it, "memberScope", pattern = exactStemPattern("memberScopeQueries", it)) }
        test<AbstractTypeScopeTest> { model(it, "typeScope", pattern = exactStemPattern("typeScopeQueries", it)) }
    }

    component("resolver") {
        group("singleByPsi") {
            test<AbstractResolveSymbolTest> { model(it, "", pattern = exactStemPattern("resolveSymbol", it)) }
            test<AbstractResolveCallTest> { model(it, "", pattern = exactStemPattern("memberCallInfo", it)) }
        }
    }

    component("containingDeclarationProvider") {
        test<AbstractContainingDeclarationProviderByReferenceTest> { model(it, "containingDeclarationByReference") }
    }

    component("visibilityChecker") {
        test<AbstractVisibilityCheckerTest> { model(it, "isVisible") }
    }

    component("typeCreator") {
        test<AbstractTypeCreatorTest> { model(it, "") }
    }

    component("typeRelationChecker") {
        test<AbstractTypeRelationTest> { model(it, "subtypingAndEquality") }
    }

    component("symbolRelationProvider") {
        test<AbstractOverriddenDeclarationProviderTest> { model(it, "overriddenSymbols") }
        test<AbstractIsSubclassOfTest> { model(it, "isSubclassOf") }
    }

    component("expressionTypeProvider") {
        test<AbstractExpressionTypeTest> { model(it, "expressionType", pattern = exactStemPattern("expressionType", it)) }
        test<AbstractDeclarationReturnTypeTest> { model(it, "declarationReturnType", pattern = exactStemPattern("declarationReturnType", it)) }
    }

    component("expressionInfoProvider") {
        test<AbstractExpressionInformationTest> { model(it, "basicInfo") }
    }

    component("dataFlowInfoProvider") {
        test<AbstractDataFlowInfoTest> { model(it, "basicInfo") }
    }

    component("diagnosticProvider") {
        test<AbstractCollectDiagnosticsTest> { model(it, "collectDiagnostics") }
        test<AbstractElementDiagnosticsTest> { model(it, "elementDiagnostics") }
    }

    component("symbolProvider") {
        test<AbstractTopLevelSymbolProviderTest> { model(it, "topLevelLookup", pattern = exactStemPattern("topLevelLookup", it)) }
    }

    group("symbols") {
        test<AbstractSymbolByReferenceTest> { model(it, "symbolByReference") }
    }

    component("renderer") {
        test<AbstractRendererTest> { model(it, "basicRendering", pattern = exactStemPattern("basicRendering", it)) }
    }

    component("symbolDeclarationRenderer") {
        test<AbstractSymbolRenderingByReferenceTest> { model(it, "symbolRenderingByReference") }
    }

    component("imports") {
        test<AbstractDefaultImportsTest> { model(it, "defaultImports", pattern = exactStemPattern("defaultImports", it)) }
        test<AbstractImportOptimizationPlanTest> { model(it, "importOptimization", pattern = exactStemPattern("importOptimization", it)) }
    }

    component("references") {
        test<AbstractReferenceShorteningPlanTest> { model(it, "referenceShortening", pattern = exactStemPattern("referenceShortening", it)) }
        test<AbstractReferenceShortenerTest> { model(it, "shortenRange", pattern = exactStemPattern("shortenRange", it)) }
        test<AbstractReferenceShortenerForWholeFileTest> { model(it, "shortenWholeFile", pattern = exactStemPattern("shortenWholeFile", it)) }
    }

    group("imports") {
        test<AbstractReferenceImportAliasTest> { model(it, "importAliases") }
    }

    group("references") {
        test<AbstractIsReferenceToTest> { model(it, "isReferenceTo") }
    }

    component("docProvider") {
        test<AbstractCDocProviderTest> { model(it, "cdoc", pattern = exactStemPattern("cdoc", it)) }
    }

    group("usages") {
        test<AbstractFindUsagesTest> { model(it, "findUsages") }
    }

    component("substitutors") {
        test<AbstractSignatureSubstitutionTest> { model(it, "signatureSubstitution", pattern = exactStemPattern("signatureSubstitution", it)) }
    }

    group("types/typePointers") {
        test<AbstractTypePointerConsistencyTest> { model(it, "consistency", pattern = exactStemPattern("typePointerRestoration", it)) }
    }

    group("sessions") {
        test<AbstractSymbolPointerRestoreTest> { model(it, "symbolPointers", pattern = exactStemPattern("symbolPointerRestore", it)) }
    }

    group("restrictedAnalysis") {
        test<AbstractRestrictedAnalysisRejectionTest> { model(it, "restriction", pattern = exactStemPattern("restrictedAnalysisRejection", it)) }
        test<AbstractRestrictedAnalysisExceptionWrappingTest> { model(it, "exceptionWrapping", pattern = exactStemPattern("restrictedAnalysisException", it)) }
    }

    group("projectStructure/moduleKinds") {
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.CodeFragment)) {
            model(it, "codeFragment", pattern = exactStemPattern("codeFragment", it))
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.NotUnderContentRoot)) {
            model(it, "notUnderContentRoot", pattern = exactStemPattern("notUnderContentRoot", it))
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.LibrarySource)) {
            model(it, "librarySource", pattern = exactStemPattern("librarySource", it))
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.LibraryBinary)) {
            model(it, "libraryBinary", pattern = exactStemPattern("libraryBinary", it))
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.Builtins)) {
            model(it, "builtins", pattern = exactStemPattern("builtins", it))
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.LibraryFallbackDependencies)) {
            model(it, "libraryFallbackDependencies", pattern = exactStemPattern("libraryFallbackDependencies", it))
        }
    }
}

private fun exactStemPattern(fileStem: String, data: AnalysisApiTestConfiguratorFactoryData): String =
    "^$fileStem\\.${data.defaultTestFileExtension()}$"

private fun AnalysisApiTestConfiguratorFactoryData.defaultTestFileExtension(): String = "cj"
