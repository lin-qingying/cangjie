package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDuplicateInterfaceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDefaultImplementationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendGenericUsageChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendInterfaceKindChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendOrphanRuleChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendSpecializationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendTargetLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFieldVariableChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFieldVariableInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInvalidDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPatternVariableInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPatternVariableChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirOverrideChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPropertyChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPropertyInitializerTypeMismatchChecker

object CommonDeclarationCheckers : CfirDeclarationCheckers() {
    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = emptySet()

    override val patternVariableCheckers: Set<CfirPatternVariableChecker>
        get() = setOf(CfirPatternVariableInitializerTypeMismatchChecker)

    override val fieldVariableCheckers: Set<CfirFieldVariableChecker>
        get() = setOf(CfirFieldVariableInitializerTypeMismatchChecker)


    override val classCheckers
        get() = setOf(CfirOverrideChecker)

    override val classLikeCheckers
        get() = setOf(
            CfirExtendTargetLegalityChecker,
            CfirExtendInterfaceKindChecker,
            CfirExtendDuplicateInterfaceChecker,
            CfirExtendOrphanRuleChecker,
            CfirExtendGenericUsageChecker,
            CfirExtendSpecializationConflictChecker,
            CfirExtendDefaultImplementationConflictChecker,
        )

    override val propertyCheckers: Set<CfirPropertyChecker>
        get() = setOf(CfirPropertyInitializerTypeMismatchChecker)
}
