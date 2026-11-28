package org.cangnova.cangjie.psi.stubs

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.parsing.CjmpParsedRoleEvent

/** 通过当前文件 stub 树中的真实父子身份读取事件；没有绑定时返回未知。 */
fun CangJieFileStub.cjmpRoleEventsFor(stub: StubElement<*>): List<CjmpParsedRoleEvent>? {
    val snapshot = cjmpRoleSnapshot ?: return null
    val path = stub.cjmpPathIn(this) ?: return null
    val ids = snapshot.stubBindings[path] ?: return null
    return ids.map { id -> checkNotNull(snapshot.eventById(id)) }
}

/** 只沿实际父子关系计算路径，不使用名字、源码偏移或 PSI。 */
internal fun StubElement<*>.cjmpPathIn(root: StubElement<*>): List<Int>? {
    val reversed = mutableListOf<Int>()
    var current: StubElement<*> = this
    while (current !== root) {
        val parent = current.parentStub ?: return null
        val index = parent.childrenStubs.indexOfFirst { it === current }
        if (index < 0) return null
        reversed += index
        current = parent
    }
    if (reversed.isEmpty()) return null
    return reversed.asReversed().toList()
}
