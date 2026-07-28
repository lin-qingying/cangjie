package org.cangnova.cangjie.cfir.session

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** 验证 session 组件访问器始终以声明的接口类型作为注册键。 */
class CfirSessionComponentAccessorTest {
    @Test
    fun `registered interface implementation replaces default component`() {
        val session = TestSession()
        val configuredProvider = object : CfirApiLevelProvider {
            override val projectApiLevel: Int = 20
        }

        session.register(CfirApiLevelProvider::class, configuredProvider)

        assertSame(configuredProvider, session.apiLevelProvider)
    }

    /** 测试专用的最小源码 session。 */
    private class TestSession : CfirSession(Kind.Source)
}
