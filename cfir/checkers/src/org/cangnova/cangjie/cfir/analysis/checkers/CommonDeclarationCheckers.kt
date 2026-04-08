package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCallableDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirAnnotationDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirConflictsDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirConstructorChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirConstructorDelegationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDefaultImplementationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDuplicateInterfaceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendGenericUsageChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendImmutableMemberChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendImmutableMutInterfaceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendInterfaceKindChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendOrphanRuleChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendSpecializationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendTargetLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFieldVariableChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFieldVariableInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFileChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirForeignFunctionReturnTypeChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFunctionInitializationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirImportsChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInvalidDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirModifierChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirMutModifierApplicabilityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirNotImplementedOverrideChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirOperatorDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirOverrideChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPatternVariableChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirClassLikeInitializationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPatternVariableInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPropertyChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirSimpleFunctionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirStaticModifierCompatibilityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirSupertypesChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirTypeConstraintsChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirTypeParameterChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirTypeParameterBoundsChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirConstructorInitializationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationChecker>
        get() = setOf(
            CfirConflictsDeclarationChecker,
            CfirModifierChecker,
            CfirTypeConstraintsChecker,
        )

    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = emptySet()

    override val patternVariableCheckers: Set<CfirPatternVariableChecker>
        get() = setOf(CfirPatternVariableInitializerTypeMismatchChecker)

    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker>
        get() = setOf()

    override val functionCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFunctionChecker>
        get() = setOf(
            CfirFunctionInitializationChecker,
            CfirForeignFunctionReturnTypeChecker,
        )

    override val typeParameterCheckers: Set<CfirTypeParameterChecker>
        get() = setOf(
            CfirTypeParameterBoundsChecker,
        )

    override val simpleFunctionCheckers: Set<CfirSimpleFunctionChecker>
        get() = setOf(
            CfirOperatorDeclarationChecker,
        )

    override val fileCheckers: Set<CfirFileChecker>
        get() = setOf(
            CfirImportsChecker,
        )

    override val fieldVariableCheckers: Set<CfirFieldVariableChecker>
        get() = setOf(CfirFieldVariableInitializerTypeMismatchChecker)

    override val extendCheckers: Set<CfirExtendChecker>
        get() = setOf(
            CfirExtendTargetLegalityChecker,
            CfirExtendInterfaceKindChecker,
            CfirExtendDuplicateInterfaceChecker,
            CfirExtendOrphanRuleChecker,
            CfirExtendGenericUsageChecker,
            CfirExtendImmutableMutInterfaceChecker,
            CfirExtendImmutableMemberChecker,
            CfirExtendSpecializationConflictChecker,
            CfirExtendDefaultImplementationConflictChecker,
        )

    override val constructorCheckers: Set<CfirConstructorChecker>
        get() = setOf(
            CfirConstructorDelegationChecker,
            CfirConstructorInitializationChecker,
        )

    override val memberDeclarationCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirMemberDeclarationChecker>
        get() = setOf(
            CfirStaticModifierCompatibilityChecker,
            CfirMutModifierApplicabilityChecker,
        )

    override val classLikeCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirClassLikeChecker>
        get() = setOf(
            CfirAnnotationDeclarationChecker,
            CfirClassLikeInitializationChecker,
            CfirSupertypesChecker,
            CfirOverrideChecker,
            CfirNotImplementedOverrideChecker,
        )

    override val propertyCheckers: Set<CfirPropertyChecker>
        get() = emptySet()
}
