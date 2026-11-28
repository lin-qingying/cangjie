package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 补全会话存储回归。
 *
 * 重点是**过期语义**：被淘汰的会话必须取不到（调用方据此报明确的过期错误），
 * 而不是取到一个区间已经不对的旧会话——后者会让用户在错误的位置插入候选。
 */
class LspCompletionSessionStoreTest {
    /**
     * 会话可按键取回，候选按下标可取。
     */
    @Test
    fun sessionIsRetrievableByKey() {
        val store = LspCompletionSessionStore(capacity = 2)
        val session = store.create(
            uri = DOC_URI,
            version = 3,
            replaceStart = 10,
            replaceEnd = 13,
            items = listOf(candidate("buildUser"), candidate("builder")),
        )

        val found = store.find(session.key)

        assertNotNull(found, "刚创建的会话必须可取回")
        assertEquals(DOC_URI, found!!.uri)
        assertEquals(3, found.version)
        assertEquals("buildUser", found.itemAt(0)?.lookupString)
        assertEquals("builder", found.itemAt(1)?.lookupString)
        assertNull(found.itemAt(2), "越界下标取不到候选")
    }

    /**
     * 超过容量时淘汰最早的会话：其键取不到，即「已过期」。
     */
    @Test
    fun oldestSessionIsEvictedBeyondCapacity() {
        val store = LspCompletionSessionStore(capacity = 2)
        val first = store.create(DOC_URI, 1, 0, 0, listOf(candidate("a")))
        val second = store.create(DOC_URI, 2, 0, 0, listOf(candidate("b")))
        val third = store.create(DOC_URI, 3, 0, 0, listOf(candidate("c")))

        assertNull(store.find(first.key), "最早的会话必须被淘汰（表现为过期）")
        assertNotNull(store.find(second.key))
        assertNotNull(store.find(third.key))
    }

    /**
     * 文档失效后该文档的会话全部不可用，其它文档不受影响。
     */
    @Test
    fun invalidateDropsOnlyThatDocument() {
        val store = LspCompletionSessionStore(capacity = 4)
        val mine = store.create(DOC_URI, 1, 0, 0, listOf(candidate("a")))
        val other = store.create(OTHER_URI, 1, 0, 0, listOf(candidate("b")))

        store.invalidate(DOC_URI)

        assertNull(store.find(mine.key), "已失效文档的会话不得再可取")
        assertNotNull(store.find(other.key), "别的文档的会话不受影响")
    }

    /**
     * 会话键由服务端生成且互不相同：客户端只回传，不参与生成。
     */
    @Test
    fun keysAreDistinctAndServerGenerated() {
        val store = LspCompletionSessionStore(capacity = 4)
        val keys = (1..4).map { store.create(DOC_URI, it, 0, 0, listOf(candidate("x"))).key }

        assertEquals(keys.size, keys.toSet().size, "会话键必须互不相同，实际 $keys")
        assertTrue(keys.all { it.startsWith("cj-completion-") }, "键带模块前缀便于日志识别，实际 $keys")
    }

    /**
     * 构造一个候选。
     */
    private fun candidate(name: String): CompletionSnapshot = completionSnapshot(
        lookupString = name,
        kind = CompletionItemKind.TOP_LEVEL,
        origin = CompletionOrigin.TOP_LEVEL,
    ).build()

    private companion object {
        const val DOC_URI = "file:///workspace/src/use.cj"
        const val OTHER_URI = "file:///workspace/src/other.cj"
    }
}
