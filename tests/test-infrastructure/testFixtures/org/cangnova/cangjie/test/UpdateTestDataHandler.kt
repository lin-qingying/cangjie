package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.services.TestServices

/**
 * 表示 `UpdateTestDataHandler`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
class UpdateTestDataHandler(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices)
