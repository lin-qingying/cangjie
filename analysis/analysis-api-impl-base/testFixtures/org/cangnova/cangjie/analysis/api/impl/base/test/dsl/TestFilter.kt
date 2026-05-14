package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind

typealias TestFilter = (AnalysisApiTestConfiguratorFactoryData) -> Boolean

infix fun TestFilter.and(other: TestFilter): TestFilter =
    { data -> this(data) && other(data) }

infix fun TestFilter.or(other: TestFilter): TestFilter =
    { data -> this(data) || other(data) }

fun frontendIs(vararg frontends: FrontendKind): TestFilter =
    { it.frontend in frontends }

fun testModuleKindIs(vararg moduleKinds: TestModuleKind): TestFilter =
    { it.moduleKind in moduleKinds }

fun analysisSessionModeIs(vararg modes: AnalysisSessionMode): TestFilter =
    { it.analysisSessionMode in modes }

fun analysisApiModeIs(vararg modes: AnalysisApiMode): TestFilter =
    { it.analysisApiMode in modes }
