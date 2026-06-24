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

/**
 * 模块间可见性检查器。
 *
 * 该组件只处理 friend module / internals 可见性这类模块关系判断。
 */
abstract class CfirModuleVisibilityChecker : CfirSessionComponent {
    /**
     * 判断声明是否位于当前会话可见的 friend module 中。
     */
    abstract fun isInFriendModule(declaration: CfirMemberDeclaration): Boolean

    /**
     * 基于会话 module data 的标准模块可见性检查器。
     */
    class Standard(
        /**
         * 当前解析会话。
         */
        private val session: CfirSession,
    ) : CfirModuleVisibilityChecker() {
        /**
         * 使用 module data 的 friend 关系判断 internals 可见性。
         */
        override fun isInFriendModule(declaration: CfirMemberDeclaration): Boolean {
            return session.moduleData.canSeeInternalsOf(declaration.moduleData)
        }
    }
}

/**
 * 平台可扩展的声明可见性检查器。
 *
 * 语言通用可见性规则由 `isVisible` 实现；该组件只承载平台层追加规则和 override 场景检查。
 */
abstract class CfirVisibilityChecker : CfirComposableSessionComponent<CfirVisibilityChecker> {
    /**
     * 默认平台可见性检查器，不追加任何平台限制。
     */
    object Default : CfirVisibilityChecker() {
        /**
         * 默认实现始终允许普通可见性检查通过。
         */
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            declaration: CfirMemberDeclaration,
            candidate: Candidate,
        ): Boolean = true

        /**
         * 默认实现始终允许 override 可见性检查通过。
         */
        override fun platformOverrideVisibilityCheck(
            derivedClassModuleData: CfirModuleData,
            candidateInBaseClass: CfirMemberDeclaration,
            visibilityInBaseClass: Visibility,
        ): Boolean = true
    }

    /**
     * 将多个平台可见性检查器组合为一个检查器。
     */
    @SessionConfiguration
    override fun createComposed(components: List<CfirVisibilityChecker>): Composed = Composed(components)

    /**
     * 多个平台可见性检查器的组合实现。
     */
    class Composed(
        /**
         * 参与组合的检查器列表。
         */
        override val components: List<CfirVisibilityChecker>,
    ) : CfirVisibilityChecker(), CfirComposableSessionComponent.Composed<CfirVisibilityChecker> {
        /**
         * 所有组件都允许时普通平台可见性才通过。
         */
        override fun platformVisibilityCheck(
            declarationVisibility: Visibility,
            declaration: CfirMemberDeclaration,
            candidate: Candidate,
        ): Boolean = components.all { it.platformVisibilityCheck(declarationVisibility, declaration, candidate) }

        /**
         * 所有组件都允许时 override 平台可见性才通过。
         */
        override fun platformOverrideVisibilityCheck(
            derivedClassModuleData: CfirModuleData,
            candidateInBaseClass: CfirMemberDeclaration,
            visibilityInBaseClass: Visibility,
        ): Boolean = components.all {
            it.platformOverrideVisibilityCheck(derivedClassModuleData, candidateInBaseClass, visibilityInBaseClass)
        }

        /**
         * 继续按组合检查器返回组合结果，保持会话组件组合协议一致。
         */
        @SessionConfiguration
        override fun createComposed(components: List<CfirVisibilityChecker>): Composed = Composed(components)
    }

    /**
     * 使用当前平台检查器判断声明对候选是否可见。
     */
    fun isVisible(declaration: CfirMemberDeclaration, candidate: Candidate): Boolean =
        isVisible(this, declaration, candidate)

    /**
     * 普通声明访问的平台代理可见性检查。
     */
    internal abstract fun platformVisibilityCheck(
        declarationVisibility: Visibility,
        declaration: CfirMemberDeclaration,
        candidate: Candidate,
    ): Boolean

    /**
     * override 场景中派生类对基类成员的平台代理可见性检查。
     */
    internal abstract fun platformOverrideVisibilityCheck(
        derivedClassModuleData: CfirModuleData,
        candidateInBaseClass: CfirMemberDeclaration,
        visibilityInBaseClass: Visibility,
    ): Boolean
}

/**
 * 当前会话可选的模块可见性检查器。
 */
val CfirSession.moduleVisibilityChecker: CfirModuleVisibilityChecker?
    by CfirSession.nullableSessionComponentAccessor()

/**
 * 当前会话的平台可见性检查器。
 */
val CfirSession.visibilityChecker: CfirVisibilityChecker
    by CfirSession.sessionComponentAccessorWithDefault(CfirVisibilityChecker.Default)
