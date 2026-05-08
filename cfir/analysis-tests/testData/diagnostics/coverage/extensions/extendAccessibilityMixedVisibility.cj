// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 多个 extend 跨包均可见（仓颉 extend 不支持访问修饰符）
// FILE: provider.cj
package provider

public interface Hashable {
    func hashCode(): Int64 {
        return 0
    }
}

public interface Equatable {
    func equals(other: Unit): Bool {
        let _ = other
        return false
    }
}

public class Entity {}

extend Entity <: Hashable {}
extend Entity <: Equatable {}

// FILE: consumer.cj
package consumer

import provider.*

// 这里两个 extend 都可见
func useMixedExtends(e: Entity): Unit {
    e.hashCode()
    e.equals(())
}
