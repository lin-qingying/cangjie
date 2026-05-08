// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 跨包 extend + 已导入 → 可访问
// FILE: lib.cj
package library

public interface Printable {
    func prettyPrint(): Unit {}
}

public class Document {}

// extend 默认 public，相关类型在当前文件可见时可跨包使用
extend Document <: Printable {}

// FILE: main.cj
package localmain

import library.*

func useCrossPackageExtend(doc: Document): Unit {
    doc.prettyPrint()
}
