// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 同包使用
// FILE: api.cj
package sample

public interface Serializable {
    func serialize(): Unit {}
}

public class Widget {}

// 同包中的 extend
extend Widget <: Serializable {}

// FILE: main.cj
package sample

func useSamePackageExtend(w: Widget): Unit {
    w.serialize()
}
