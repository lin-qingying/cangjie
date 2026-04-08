// RUN_PIPELINE_TILL: FRONTEND
// 测试类型可访问性 — public 类型跨包已导入可访问
// FILE: lib.cj
package library

public interface Renderable {}
public interface Cacheable {}
public class View <: Renderable & Cacheable {}

// FILE: main.cj
package main

import library.*

// 跨包引用 — public 类型 + 已导入 → 可访问
func useCrossPackagePublicType(): Unit {
    let v = View()
    let r: Renderable = v
    let c: Cacheable = v
}
