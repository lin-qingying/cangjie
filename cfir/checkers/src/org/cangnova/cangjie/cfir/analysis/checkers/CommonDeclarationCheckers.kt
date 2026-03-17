package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDuplicateInterfaceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDefaultImplementationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendGenericUsageChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendInterfaceKindChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendOrphanRuleChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendSpecializationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendTargetLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInvalidDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPropertyChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPropertyInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirVariableChecker

object CommonDeclarationCheckers : CfirDeclarationCheckers() {
    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = emptySet()

    override val variableCheckers: Set<CfirVariableChecker>
        get() = setOf(CfirInitializerTypeMismatchChecker)

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

