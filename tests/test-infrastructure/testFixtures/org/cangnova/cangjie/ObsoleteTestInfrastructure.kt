package org.cangnova.cangjie

/**
 * 表示 `ObsoleteTestInfrastructure`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
@RequiresOptIn
annotation class ObsoleteTestInfrastructure(val replacer: String = "")
