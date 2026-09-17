package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [StateKeeper] DSL 状态保存/恢复语义测试。
 *
 * 覆盖：简单属性恢复、arranger 临时调整、keeper 嵌套包含、entity / entityList
 * 子实体登记、postProcess 后处理。对齐 Kotlin `StateKeeperTest`。
 */
class StateKeeperTest {
    @Test
    fun testSimple() {
        class Foo(private var value: String) {
            fun getValue() = value
            fun setValue(newValue: String) {
                value = newValue
            }
        }

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { builder, _, _ ->
            builder.add(Foo::getValue, Foo::setValue)
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Foo", foo.getValue())
            foo.setValue("Bar")
            assertEquals("Bar", foo.getValue())
        }

        assertEquals("Foo", foo.getValue())
    }

    @Test
    fun testProperty() {
        class Foo(var value: String)

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { builder, _, _ ->
            builder.add(Foo::value::get, Foo::value::set)
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Foo", foo.value)
            foo.value = "Bar"
            assertEquals("Bar", foo.value)
        }

        assertEquals("Foo", foo.value)
    }

    @Test
    fun testArranger() {
        class Foo(var value: String)

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { builder, _, _ ->
            builder.add(Foo::value::get, Foo::value::set) { "Baz" }
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Baz", foo.value)
            foo.value = "Bar"
            assertEquals("Bar", foo.value)
        }

        assertEquals("Foo", foo.value)
    }

    @Test
    fun testInclusion() {
        open class Foo(var one: String)

        class Bar(one: String, var two: Int) : Foo(one)

        val fooKeeper: StateKeeper<Foo, Unit> = stateKeeper { builder, _, _ ->
            builder.add(Foo::one::get, Foo::one::set)
        }

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { builder, _, context ->
            builder.add(fooKeeper, context)
            builder.add(Bar::two::get, Bar::two::set)
        }

        val bar = Bar("Foo", 1)

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(1, bar.two)
            bar.one = "Bar"
            bar.two = 2
            assertEquals("Bar", bar.one)
            assertEquals(2, bar.two)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.two)
    }

    @Test
    fun testEntity() {
        data class Foo(var value: Int)

        class Bar(var one: String, var foo: Foo)

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { builder, bar, context ->
            builder.add(Bar::one::get, Bar::one::set)
            builder.entity(bar.foo, context) { _, _ ->
                builder.add(Foo::value::get, Foo::value::set)
            }
        }

        val foo = Foo(1)
        val bar = Bar("Foo", foo)

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(1, bar.foo.value)

            bar.one = "Bar"
            bar.foo.value = 2

            assertEquals("Bar", bar.one)
            assertEquals(2, bar.foo.value)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.foo.value)
        check(bar.foo === foo)
    }

    @Test
    fun testEntityInclusion() {
        data class Foo(var value: Int)

        class Bar(var one: String, var foo: Foo)

        val fooKeeper: StateKeeper<Foo, Unit> = stateKeeper { builder, _, _ ->
            builder.add(Foo::value::get, Foo::value::set)
        }

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { builder, bar, context ->
            builder.add(Bar::one::get, Bar::one::set)
            builder.entity(bar.foo, fooKeeper, context)
        }

        val foo = Foo(1)
        val bar = Bar("Foo", foo)

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(1, bar.foo.value)

            bar.one = "Bar"
            bar.foo.value = 2

            assertEquals("Bar", bar.one)
            assertEquals(2, bar.foo.value)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.foo.value)
        check(bar.foo === foo)
    }

    @Test
    fun testEntityList() {
        data class Foo(var value: Int)

        class Bar(var one: String, var foos: List<Foo>)

        val barKeeper: StateKeeper<Bar, Unit> = stateKeeper { builder, bar, context ->
            builder.add(Bar::one::get, Bar::one::set)
            builder.entityList(bar.foos, context) { _, _ ->
                builder.add(Foo::value::get, Foo::value::set) { it + 1 }
            }
        }

        val foo0 = Foo(1)
        val foo1 = Foo(2)
        val bar = Bar("Foo", listOf(foo0, foo1))

        barKeeper.withRestoration(bar) {
            assertEquals("Foo", bar.one)
            assertEquals(2, bar.foos[0].value) // +1 because of the arranger
            assertEquals(3, bar.foos[1].value)

            bar.one = "Bar"
            bar.foos[0].value = 100
            bar.foos[1].value = 200

            assertEquals("Bar", bar.one)
            assertEquals(100, bar.foos[0].value)
            assertEquals(200, bar.foos[1].value)
        }

        assertEquals("Foo", bar.one)
        assertEquals(1, bar.foos[0].value)
        assertEquals(2, bar.foos[1].value)
        check(bar.foos[0] === foo0)
        check(bar.foos[1] === foo1)
    }

    @Test
    fun testPostProcess() {
        class Foo(var value: String)

        val keeper: StateKeeper<Foo, Unit> = stateKeeper { builder, foo, _ ->
            builder.add(Foo::value::get, Foo::value::set) { "Baz" }

            builder.postProcess {
                foo.value = "Boo"
            }
        }

        val foo = Foo("Foo")

        keeper.withRestoration(foo) {
            assertEquals("Boo", foo.value)
            foo.value = "Bar"
            assertEquals("Bar", foo.value)
        }

        assertEquals("Foo", foo.value)
    }
}

/**
 * 在 [StateKeeper] 保护下执行 [block]，结束后恢复状态。
 *
 * 对齐 Kotlin `StateKeeperTest.withRestoration`。
 */
private fun <T : Any> StateKeeper<T, Unit>.withRestoration(owner: T, block: () -> Unit) {
    val state = prepare(owner, Unit)
    state.postProcess()
    block()
    state.restore()
}
