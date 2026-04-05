// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 同包 extend 始终可访问
// FILE: api.cj
package sample

public interface Serializable {
    func serialize(): Unit {}
}

public class Widget {}

// 同包中的 extend — 同包均可见
extend Widget <: Serializable {}

// FILE: main.cj
package sample

// 同包内，直接使用 extend 提供的成员
func useSamePackageExtend(w: Widget): Unit {
    w.serialize()  // OK: 同包 extend 可访问
}
