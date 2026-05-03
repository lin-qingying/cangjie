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
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

fun AnalysisApiTestGroup.generateAnalysisApiTests() {
    component("scopeProvider") {
        test<AbstractFileScopeTest> { model(it, "fileScope") }
        test<AbstractPackageScopeTest> { model(it, "packageScope") }
        test<AbstractMemberScopeTest> { model(it, "memberScope") }
        test<AbstractTypeScopeTest> { model(it, "typeScope") }
    }

    component("resolver") {
        test<AbstractResolveSymbolTest> { model(it, "singleByPsi") }
        test<AbstractResolveCallTest> { model(it, "singleByPsi") }
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
        test<AbstractExpressionTypeTest> { model(it, "expressionType") }
        test<AbstractDeclarationReturnTypeTest> { model(it, "declarationReturnType") }
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
        test<AbstractTopLevelSymbolProviderTest> { model(it, "topLevelLookup") }
    }

    group("symbols") {
        test<AbstractSymbolByReferenceTest> { model(it, "symbolByReference") }
    }

    component("renderer") {
        test<AbstractRendererTest> { model(it, "basicRendering") }
    }

    component("symbolDeclarationRenderer") {
        test<AbstractSymbolRenderingByReferenceTest> { model(it, "symbolRenderingByReference") }
    }

    component("imports") {
        test<AbstractDefaultImportsTest> { model(it, "defaultImports") }
        test<AbstractImportOptimizationPlanTest> { model(it, "importOptimization") }
    }

    component("references") {
        test<AbstractReferenceShorteningPlanTest> { model(it, "referenceShortening") }
        test<AbstractReferenceShortenerTest> { model(it, "shortenRange") }
        test<AbstractReferenceShortenerForWholeFileTest> { model(it, "shortenWholeFile") }
    }

    group("imports") {
        test<AbstractReferenceImportAliasTest> { model(it, "importAliases") }
    }

    group("references") {
        test<AbstractIsReferenceToTest> { model(it, "isReferenceTo") }
    }

    component("docProvider") {
        test<AbstractCDocProviderTest> { model(it, "cdoc") }
    }

    group("usages") {
        test<AbstractFindUsagesTest> { model(it, "findUsages") }
    }

    component("substitutors") {
        test<AbstractSignatureSubstitutionTest> { model(it, "signatureSubstitution") }
    }

    group("types/typePointers") {
        test<AbstractTypePointerConsistencyTest> { model(it, "consistency") }
    }

    group("sessions") {
        test<AbstractSymbolPointerRestoreTest> { model(it, "symbolPointers") }
    }

    group("restrictedAnalysis") {
        test<AbstractRestrictedAnalysisRejectionTest> { model(it, "restriction") }
        test<AbstractRestrictedAnalysisExceptionWrappingTest> { model(it, "exceptionWrapping") }
    }

    group("projectStructure/moduleKinds") {
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.CodeFragment)) {
            model(it, "codeFragment")
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.NotUnderContentRoot)) {
            model(it, "notUnderContentRoot")
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.LibrarySource)) {
            model(it, "librarySource")
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.LibraryBinary)) {
            model(it, "libraryBinary")
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.Builtins)) {
            model(it, "builtins")
        }
        test<AbstractModuleStructureTest>(filter = testModuleKindIs(TestModuleKind.LibraryFallbackDependencies)) {
            model(it, "libraryFallbackDependencies")
        }
    }
}
