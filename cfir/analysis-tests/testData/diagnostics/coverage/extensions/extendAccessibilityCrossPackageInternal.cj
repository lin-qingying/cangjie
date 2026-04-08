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
package main

import internal_lib.Countable
// 注意：没有 import internal_lib.Bag — 目标类型未导入

// 只导入了接口 Countable，没有导入目标类型 Bag
// 无法直接构造或引用 Bag，extend 间接不可达
func demonstrateTargetNotImported(c: Countable): Int64 {
    return c.count()
}
