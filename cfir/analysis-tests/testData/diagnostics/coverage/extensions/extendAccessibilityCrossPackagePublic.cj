// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 跨包 extend + 已导入 → 可访问
// FILE: lib.cj
package library

public interface Printable {
    func prettyPrint(): Unit {}
}

public class Document {}

// extend 默认 public，跨包可见（前提是导入了接口和目标类型）
extend Document <: Printable {}

// FILE: main.cj
package main

import library.*

func useCrossPackageExtend(doc: Document): Unit {
    doc.prettyPrint()  // OK: extend + 目标类型和接口已导入
}
