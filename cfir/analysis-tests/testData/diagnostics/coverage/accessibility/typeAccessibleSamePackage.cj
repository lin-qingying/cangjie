// RUN_PIPELINE_TILL: FRONTEND
// 测试类型可访问性 — 同包类型使用
// FILE: types.cj
package sample

public interface Alpha {}
public interface Beta {}

class LocalOnly <: Alpha & Beta {}

// FILE: main.cj
package sample

func useSamePackageType(): Unit {
    let obj = LocalOnly()
    let a: Alpha = obj
    let _ = a
}
