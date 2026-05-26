package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.containingDeclarationProvider.AbstractContainingDeclarationProviderByReferenceTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.analysisScopeProvider.AbstractCanBeAnalysedTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.compileTimeConstantProvider.AbstractCompileTimeConstantEvaluatorTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider.AbstractDataFlowInfoTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.dataFlowInfoProvider.AbstractSmartCastInfoTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCollectDiagnosticsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractCodeFragmentCollectDiagnosticsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractDanglingFileCollectDiagnosticsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.AbstractElementDiagnosticsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.docProvider.AbstractCDocProviderTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionInfoProvider.AbstractExpressionInformationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractDeclarationReturnTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpressionTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpectedExpressionTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports.AbstractDefaultImportsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.imports.AbstractImportOptimizationPlanTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShortenerForWholeFileTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShortenerTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.references.AbstractReferenceShorteningPlanTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.renderer.AbstractRendererTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveCallByFileTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveCallTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveReferenceByFileTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveReferenceTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveSymbolByFileTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver.AbstractResolveSymbolTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractCombinedDeclaredMemberScopeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider.AbstractDeclaredMemberScopeTest
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
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractFunctionClassKindTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeInfoProvider.AbstractSuperTypesTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider.AbstractDefaultTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider.AbstractHaveCommonSubtypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider.AbstractTypeReferenceTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeProvider.AbstractVarargArrayTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeRelationChecker.AbstractTypeRelationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.usages.AbstractFindUsagesTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.visibilityChecker.AbstractVisibilityCheckerTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.projectStructure.AbstractModuleStructureTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.references.AbstractIsReferenceToTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.references.AbstractReferenceImportAliasTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisExceptionWrappingTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.restrictedAnalysis.AbstractRestrictedAnalysisRejectionTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnDeclarationsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnTypesTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiSpecificAnnotationOnDeclarationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractAnalysisSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractModuleStateModificationAnalysisSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractSessionInvalidationTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.sessions.AbstractSymbolPointerRestoreTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractPackageSymbolTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSingleSymbolByPsiTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByPsiTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByReferenceTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSymbolRestoreFromDifferentModuleTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractAbbreviatedTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractAnalysisApiSubstitutorsTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractBuiltInTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractTypeByDeclarationReturnTypeTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.types.AbstractTypePointerConsistencyTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.generators.dsl.TestGroup

fun AnalysisApiTestGroup.generateAnalysisApiTests() {
    component(
        "analysisScopeProvider",
        filter = analysisSessionModeIs(AnalysisSessionMode.Normal),
    ) {
        test<AbstractCanBeAnalysedTest> { model(it, "canBeAnalysed") }
    }

    group(
        "projectStructure",
        filter = testModuleKindIs(
            TestModuleKind.LibraryBinary,
            TestModuleKind.LibrarySource,
            TestModuleKind.CodeFragment,
        ),
    ) {
        test<AbstractModuleStructureTest> { model(it, "moduleKinds") }
    }

    component("scopeProvider") {
        group(filter = frontendIs(FrontendKind.Cfir)) {
            test<AbstractTypeScopeTest> { model(it, "typeScope") }
        }

        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
            test<AbstractFileScopeTest> { model(it, "fileScopeTest") }
            test<AbstractPackageScopeTest> { model(it, "packageScope") }

            group(filter = frontendIs(FrontendKind.Cfir)) {
                test<AbstractMemberScopeTest> { model(it, "memberScope") }
                test<AbstractDeclaredMemberScopeTest> { model(it, "declaredMemberScope") }
                test<AbstractCombinedDeclaredMemberScopeTest> { model(it, "combinedDeclaredMemberScope") }
            }
        }
    }

    component(
        "resolver",
        filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibrarySource) and
            analysisSessionModeIs(AnalysisSessionMode.Normal),
    ) {
        val singleByPsiInit: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit = { data ->
            val excludeDirs = buildList {
                if (data.analysisApiMode == AnalysisApiMode.Standalone) {
                    add("withTestCompilerPluginEnabled")
                }

                if (data.moduleKind == TestModuleKind.LibrarySource) {
                    add("withErrors")
                    add("missingDependency")
                    add("cloneable")
                }
            }

            model(data, "singleByPsi", excludeDirsRecursively = excludeDirs + "reference")
        }

        test<AbstractResolveSymbolTest>(init = singleByPsiInit)
        test<AbstractResolveCallTest>(init = singleByPsiInit)
        test<AbstractResolveReferenceTest> { model(it, "singleByPsi/reference") }
        group("allByPsi", filter = testModuleKindIs(TestModuleKind.Source)) {
            test<AbstractResolveCallByFileTest> { model(it, "") }
            test<AbstractResolveSymbolByFileTest> { model(it, "") }
            test<AbstractResolveReferenceByFileTest> { model(it, "") }
        }
    }

    component("containingDeclarationProvider") {
        test<AbstractContainingDeclarationProviderByReferenceTest> { model(it, "containingDeclarationByReference") }
    }

    component("visibilityChecker") {
        test<AbstractVisibilityCheckerTest> { model(it, "isVisible") }
    }

    component("compileTimeConstantProvider") {
        test<AbstractCompileTimeConstantEvaluatorTest> { model(it, "evaluate") }
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
        test<AbstractExpectedExpressionTypeTest> { model(it, "expectedExpressionType") }
        test<AbstractExpressionTypeTest> { model(it, "expressionType") }
        test<AbstractDeclarationReturnTypeTest> { model(it, "declarationReturnType") }
    }

    component("expressionInfoProvider") {
        test<AbstractExpressionInformationTest> { model(it, "basicInfo") }
    }

    component("dataFlowInfoProvider") {
        test<AbstractDataFlowInfoTest> { model(it, "basicInfo") }
        test<AbstractSmartCastInfoTest> { model(it, "smartCastInfo") }
    }

    component("diagnosticProvider") {
        test<AbstractCollectDiagnosticsTest> {
            model(
                it,
                "collectDiagnostics",
                excludedPattern = if (it.moduleKind == TestModuleKind.Source) {
                    null
                } else {
                    """^(interfaceMember|interfaceSupertype|topLevelInterface)\.cj$"""
                },
            )
        }
        test<AbstractDanglingFileCollectDiagnosticsTest> { model(it, "collectDiagnostics") }
        test<AbstractCodeFragmentCollectDiagnosticsTest>(
            filter = testModuleKindIs(TestModuleKind.Source),
        ) {
            model(it, "codeFragmentDiagnostics")
        }
        test<AbstractElementDiagnosticsTest> { model(it, "elementDiagnostics") }
    }

    component("symbolProvider") {
        test<AbstractTopLevelSymbolProviderTest> { model(it, "topLevelLookup") }
    }

    group(
        "symbols",
        filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and
            testModuleKindIs(TestModuleKind.Source),
    ) {
        test<AbstractSymbolByPsiTest> { model(it, "symbolByPsi") }
        test<AbstractSingleSymbolByPsiTest> { model(it, "singleSymbolByPsi") }
        test<AbstractSymbolRestoreFromDifferentModuleTest> { model(it, "symbolRestoreFromDifferentModule") }
        test<AbstractSymbolByFqNameTest> { model(it, "symbolByFqName") }
        test<AbstractSymbolByReferenceTest> { model(it, "symbolByReference") }
        test<AbstractPackageSymbolTest> { model(it, "packages") }
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
        group(filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and testModuleKindIs(TestModuleKind.Source)) {
            test<AbstractAnalysisApiSubstitutorsTest> { model(it, "typeSubstitution") }
        }
    }

    group("types/typePointers") {
        test<AbstractTypePointerConsistencyTest> { model(it, "consistency") }
    }

    component("typeInfoProvider") {
        test<AbstractFunctionClassKindTest> { model(it, "functionClassKind") }
        test<AbstractSuperTypesTest> { model(it, "superTypes") }
    }

    component("typeProvider") {
        test<AbstractHaveCommonSubtypeTest> { model(it, "haveCommonSubtype") }
        test<AbstractTypeReferenceTest> { model(it, "typeReference") }
        test<AbstractDefaultTypeTest> { model(it, "defaultType") }
        test<AbstractVarargArrayTypeTest> { model(it, "varargArrayType") }
    }

    group("annotations", filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and testModuleKindIs(TestModuleKind.Source)) {
        test<AbstractAnalysisApiAnnotationsOnTypesTest> { model(it, "annotationsOnTypes") }
        test<AbstractAnalysisApiAnnotationsOnDeclarationsTest> { model(it, "annotationsOnDeclaration") }
        test<AbstractAnalysisApiSpecificAnnotationOnDeclarationTest> { model(it, "specificAnnotations") }
        test<AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest> { model(it, "metaAnnotations") }
    }

    group("types", filter = analysisSessionModeIs(AnalysisSessionMode.Normal)) {
        group(filter = testModuleKindIs(TestModuleKind.Source)) {
            test<AbstractTypeByDeclarationReturnTypeTest> { model(it, "byDeclarationReturnType") }
            test<AbstractBuiltInTypeTest> { model(it, "builtins") }
        }

        group(filter = testModuleKindIs(TestModuleKind.Source, TestModuleKind.LibraryBinary)) {
            test<AbstractAbbreviatedTypeTest> { model(it, "abbreviatedType") }
        }
    }

    group("sessions") {
        test<AbstractSymbolPointerRestoreTest> { model(it, "symbolPointers") }

        group(
            filter = analysisSessionModeIs(AnalysisSessionMode.Normal) and
                testModuleKindIs(TestModuleKind.Source) and
                frontendIs(FrontendKind.Cfir) and
                analysisApiModeIs(AnalysisApiMode.Ide),
        ) {
            test<AbstractModuleStateModificationAnalysisSessionInvalidationTest> {
                model(it, "sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractModuleOutOfBlockModificationAnalysisSessionInvalidationTest> {
                model(it, "sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractGlobalModuleStateModificationAnalysisSessionInvalidationTest> {
                model(it, "sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractGlobalSourceModuleStateModificationAnalysisSessionInvalidationTest> {
                model(it, "sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractGlobalSourceOutOfBlockModificationAnalysisSessionInvalidationTest> {
                model(it, "sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }

            test<AbstractCodeFragmentContextModificationAnalysisSessionInvalidationTest> {
                model(it, "sessionInvalidation", excludeDirsRecursively = AbstractSessionInvalidationTest.TEST_OUTPUT_DIRECTORY_NAMES)
            }
        }
    }

    group("restrictedAnalysis") {
        test<AbstractRestrictedAnalysisRejectionTest> { model(it, "restriction") }
        test<AbstractRestrictedAnalysisExceptionWrappingTest> { model(it, "exceptionWrapping") }
    }

}
