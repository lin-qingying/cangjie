package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

/**
 * Analysis API 测试配置组合过滤器。
 *
 * 过滤器接收一个 `AnalysisApiTestConfiguratorFactoryData`，返回该组合是否应参与当前 generated test 分组。
 */
typealias TestFilter = (AnalysisApiTestConfiguratorFactoryData) -> Boolean

/**
 * 将两个测试配置过滤器按逻辑与组合。
 *
 * 组合后的过滤器只有在左右两侧都接受同一配置组合时才返回 `true`。
 */
infix fun TestFilter.and(other: TestFilter): TestFilter =
    { data -> this(data) && other(data) }

/**
 * 将两个测试配置过滤器按逻辑或组合。
 *
 * 组合后的过滤器在任意一侧接受配置组合时返回 `true`。
 */
infix fun TestFilter.or(other: TestFilter): TestFilter =
    { data -> this(data) || other(data) }

/**
 * 创建按 frontend 维度筛选配置组合的过滤器。
 *
 * 生成 DSL 用它把只支持 CFIR 等特定前端的测试限制到对应配置。
 */
fun frontendIs(vararg frontends: FrontendKind): TestFilter =
    { it.frontend in frontends }

/**
 * 创建按测试模块形态筛选配置组合的过滤器。
 *
 * 生成 DSL 用它限制 source、library binary、library source、code fragment 等模块场景。
 */
fun testModuleKindIs(vararg moduleKinds: TestModuleKind): TestFilter =
    { it.moduleKind in moduleKinds }

/**
 * 创建按 Analysis session mode 筛选配置组合的过滤器。
 *
 * 该过滤器用于区分普通 session、dependent session 等不同测试宿主模式。
 */
fun analysisSessionModeIs(vararg modes: AnalysisSessionMode): TestFilter =
    { it.analysisSessionMode in modes }

/**
 * 创建按 Analysis API mode 筛选配置组合的过滤器。
 *
 * 该过滤器用于区分 IDE mode 和 standalone mode 的 generated tests。
 */
fun analysisApiModeIs(vararg modes: AnalysisApiMode): TestFilter =
    { it.analysisApiMode in modes }
