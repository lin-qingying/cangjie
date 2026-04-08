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
        return false
    }
}

public class Entity {}

extend Entity <: Hashable {}
extend Entity <: Equatable {}

// FILE: consumer.cj
package consumer

import provider.*

// Hashable 和 Equatable 的 extend 均可见（默认 public + 已导入）
func useMixedExtends(e: Entity): Unit {
    e.hashCode()   // OK: extend 可访问
    e.equals(())   // OK: extend 可访问
}
