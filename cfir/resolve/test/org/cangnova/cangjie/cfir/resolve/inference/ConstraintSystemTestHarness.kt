@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TestSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConeTypeApproximator
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintIncorporator
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.type.model.CangJieTypeMarker

/**
 * 约束系统单元测试基础设施。
 *
 * 构造基于完整 [TestSession] 的 [ConstraintSystemImpl]，供约束系统系列测试
 * （约束传播、存储、依赖图、统一）使用。类型上下文来自
 * [org.cangnova.cangjie.cfir.types.TypeComponents]（绑定 session），
 * 与 Kotlin K2 约束系统单测的组织方式对齐。
 */
object ConstraintSystemTestHarness {

    /**
     * 构造完整测试 session（注册 module、语言设置、类型组件与推断组件）。
     */
    fun newSession(): TestSession = CallResolutionTestFixtures.newTestSession()

    /**
     * 获取绑定 [session] 的推断类型上下文。
     */
    fun typeContext(session: CfirSession): ConeInferenceContext = session.typeContext

    /**
     * 构造一个基于新 session 的可直接添加约束的新约束系统。
     */
    fun newSystem(): ConstraintSystemImpl = newSystem(newSession())

    /**
     * 构造一个绑定 [session] 的可直接添加约束的新约束系统。
     */
    fun newSystem(session: CfirSession): ConstraintSystemImpl {
        val typeContext = session.typeContext
        val languageVersionSettings = session.languageVersionSettings
        val approximator = ConeTypeApproximator(typeContext, languageVersionSettings)
        val incorporator = ConstraintIncorporator(
            approximator,
            TrivialConstraintTypeInferenceOracle.create(typeContext),
            ConeConstraintSystemUtilContext,
            languageVersionSettings,
        )
        val injector = ConstraintInjector(
            incorporator,
            approximator,
            languageVersionSettings,
        )
        return ConstraintSystemImpl(injector, typeContext, languageVersionSettings)
    }

    /**
     * 注册一个以 [name] 命名的类型变量并返回。
     */
    fun newVariable(system: ConstraintSystemImpl, name: String): ConeTypeVariable {
        val variable = ConeTypeVariable(name)
        system.registerVariable(variable)
        return variable
    }

    /**
     * 读取变量当前挂载的约束列表。
     */
    fun constraintsOf(system: ConstraintSystem, variable: ConeTypeVariable): List<Constraint> =
        system.asReadOnlyStorage().notFixedTypeVariables.getValue(variable.typeConstructor).constraints

    /**
     * 读取变量当前的全部下界约束类型。
     */
    fun lowerBoundsOf(system: ConstraintSystem, variable: ConeTypeVariable): List<CangJieTypeMarker> =
        constraintsOf(system, variable).filter { it.kind.isLower() }.map { it.type }

    /**
     * 读取变量当前的全部上界约束类型。
     */
    fun upperBoundsOf(system: ConstraintSystem, variable: ConeTypeVariable): List<CangJieTypeMarker> =
        constraintsOf(system, variable).filter { it.kind.isUpper() }.map { it.type }
}