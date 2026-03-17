package org.cangnova.cangjie.test.runners

import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder

/**
 * 带有目标后端的抽象编译器测试基类
 *
 * 对应 Kotlin K2 的 AbstractKotlinCompilerWithTargetBackendTest
 */
abstract class AbstractCangjieCompilerWithTargetBackendTest(
    val targetBackend: TargetBackend,
) : AbstractCangjieCompilerTest() {

    @TestInfrastructureInternals
    final override fun configureInternal(builder: TestConfigurationBuilder) {
        val myTargetBackend = targetBackend
        configure(builder)
        with(builder) {
            globalDefaults {
                if (this.targetBackend == null) {
                    this.targetBackend = myTargetBackend
                } else {
                    require(this.targetBackend == myTargetBackend) {
                        """Target backend in configuration specified to ${this.targetBackend} but in
                          |AbstractCangjieCompilerWithTargetBackendTest parent it is set to $myTargetBackend""".trimMargin()
                    }
                }
            }
        }
    }
}
