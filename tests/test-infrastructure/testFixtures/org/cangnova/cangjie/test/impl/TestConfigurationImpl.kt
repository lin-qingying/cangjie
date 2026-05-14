package org.cangnova.cangjie.test.impl

import org.cangnova.cangjie.test.TestConfigurationImplBase
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.services.TestServices

/**
 * 对齐 Kotlin `org.jetbrains.kotlin.test.impl.TestConfigurationImpl.kt`。
 */
@TestInfrastructureInternals
val TestServices.testConfiguration: TestConfigurationImplBase<*> by TestServices.testServiceAccessor()
