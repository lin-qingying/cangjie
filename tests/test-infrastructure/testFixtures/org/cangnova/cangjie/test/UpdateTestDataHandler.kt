package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.services.TestServices

class UpdateTestDataHandler(
    testServices: TestServices,
) : AfterAnalysisChecker(testServices)
