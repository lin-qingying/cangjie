// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 跨包 extend 目标类型未导入
// FILE: internal_lib.cj
package internal_lib

public interface Countable {
    func count(): Int64 {
        return 0
    }
}

public class Bag {}

extend Bag <: Countable {}

// FILE: main.cj
package localmain

import internal_lib.Countable
// 注意：没有 import internal_lib.Bag — 目标类型未导入

// 没有导入目标类型 Bag，因此这里只能通过已导入的接口类型 Countable 使用对应成员
func demonstrateTargetNotImported(c: Countable): Int64 {
    return c.count()
}
