package org.cangnova.cangjie.test.testFramework

import junit.framework.TestCase

open class CjUsefulTestCase : TestCase() {
    companion object {
        init {
            // 对齐 Kotlin IDEA 测试基类，在测试类加载阶段就固定为 headless，
            // 避免 IntelliJ 在 Application 建立前预热 UI/AWT 默认值。
            System.setProperty("java.awt.headless", "true")
        }
    }
}
