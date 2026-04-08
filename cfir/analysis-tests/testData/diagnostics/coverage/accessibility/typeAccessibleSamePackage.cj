// RUN_PIPELINE_TILL: FRONTEND
// 测试类型可访问性 — 同包类型始终可访问
// FILE: types.cj
package sample

public interface Alpha {}
public interface Beta {}

class LocalOnly <: Alpha & Beta {}

// FILE: main.cj
package sample

// 同包引用 LocalOnly — 可访问（即使没有 public 修饰符，同包始终可见）
func useSamePackageType(): Unit {
    let obj = LocalOnly()
    let a: Alpha = obj
}
