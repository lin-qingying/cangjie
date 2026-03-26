package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration. DeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDuplicateInterfaceChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendDefaultImplementationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendGenericUsageChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendInterfaceKindChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendOrphanRuleChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendSpecializationConflictChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendTargetLegalityChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFieldVariableInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCallableDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFieldVariableChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInvalidDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPatternVariableInitializerTypeMismatchChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPatternVariableChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirPropertyChecker

object CommonDeclarationCheckers :  DeclarationCheckers() {
    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = emptySet()

    override val patternVariableCheckers: Set<CfirPatternVariableChecker>
        get() = setOf(CfirPatternVariableInitializerTypeMismatchChecker)

    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker>
        get() = setOf()
    override val fieldVariableCheckers: Set<CfirFieldVariableChecker>
        get() = setOf(CfirFieldVariableInitializerTypeMismatchChecker)
    override val extendCheckers: Set<CfirExtendChecker>
        get() = setOf(
            CfirExtendTargetLegalityChecker,
            CfirExtendInterfaceKindChecker,
            CfirExtendDuplicateInterfaceChecker,
            CfirExtendOrphanRuleChecker,
            CfirExtendGenericUsageChecker,
            CfirExtendSpecializationConflictChecker,
            CfirExtendDefaultImplementationConflictChecker,
        )
    override val memberDeclarationCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirMemberDeclarationChecker>
        get() = setOf(

        )




    override val classLikeCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirClassLikeChecker>
        get() = emptySet()

    override val propertyCheckers: Set<CfirPropertyChecker>
        get() = emptySet()
}
