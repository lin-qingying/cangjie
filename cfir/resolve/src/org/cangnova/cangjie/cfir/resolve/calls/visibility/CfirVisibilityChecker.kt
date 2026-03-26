package org.cangnova.cangjie.cfir.resolve.calls.visibility

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.canSeeInternalsOf
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.isVisible
import org.cangnova.cangjie.cfir.session.CfirComposableSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.descriptors.Visibility

abstract class CfirModuleVisibilityChecker : CfirSessionComponent {
    abstract fun isInFriendModule(declaration: CfirMemberDeclaration): Boolean

    class Standard(private val session: CfirSession) : CfirModuleVisibilityChecker() {
        override fun isInFriendModule(declaration: CfirMemberDeclaration): Boolean {
            return session.moduleData.canSeeInternalsOf(declaration.moduleData)
        }
    }
}

abstract class CfirVisibilityChecker : CfirComposableSessionComponent<CfirVisibilityChecker> {
    object Default : CfirVisibilityChecker() {
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            declaration: CfirMemberDeclaration,
            candidate: Candidate,
        ): Boolean = true

        override fun platformOverrideVisibilityCheck(
            derivedClassModuleData: CfirModuleData,
            candidateInBaseClass: CfirMemberDeclaration,
            visibilityInBaseClass: Visibility,
        ): Boolean = true
    }

    @SessionConfiguration
    override fun createComposed(components: List<CfirVisibilityChecker>): Composed = Composed(components)

    class Composed(
        override val components: List<CfirVisibilityChecker>,
    ) : CfirVisibilityChecker(), CfirComposableSessionComponent.Composed<CfirVisibilityChecker> {
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            declaration: CfirMemberDeclaration,
            candidate: Candidate,
        ): Boolean = components.all { it.platformVisibilityCheck(declarationVisibility, declaration, candidate) }

        override fun platformOverrideVisibilityCheck(
            derivedClassModuleData: CfirModuleData,
            candidateInBaseClass: CfirMemberDeclaration,
            visibilityInBaseClass: Visibility,
        ): Boolean = components.all {
            it.platformOverrideVisibilityCheck(derivedClassModuleData, candidateInBaseClass, visibilityInBaseClass)
        }

        @SessionConfiguration
        override fun createComposed(components: List<CfirVisibilityChecker>): Composed = Composed(components)
    }

    fun isVisible(declaration: CfirMemberDeclaration, candidate: Candidate): Boolean =
        isVisible(this, declaration, candidate)

    internal abstract fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        declaration: CfirMemberDeclaration,
        candidate: Candidate,
    ): Boolean

    internal abstract fun platformOverrideVisibilityCheck(
        derivedClassModuleData: CfirModuleData,
        candidateInBaseClass: CfirMemberDeclaration,
        visibilityInBaseClass: Visibility,
    ): Boolean
}

val CfirSession.moduleVisibilityChecker: CfirModuleVisibilityChecker?
    by CfirSession.nullableSessionComponentAccessor()

val CfirSession.visibilityChecker: CfirVisibilityChecker
    by CfirSession.sessionComponentAccessorWithDefault(CfirVisibilityChecker.Default)
