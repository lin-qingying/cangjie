// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 跨包 extend 接口未导入则不可访问
// FILE: lib.cj
package lib

public interface Formattable {
    func format(): Unit {}
}

public interface Loggable {
    func log(): Unit {}
}

public class Record {}

// 两个 extend，使用不同接口
extend Record <: Formattable {}
extend Record <: Loggable {}

// FILE: main.cj
package main

import lib.Record
import lib.Formattable
// 注意：没有 import lib.Loggable

// Formattable 接口已导入 → 对应 extend 可访问
// Loggable 接口未导入 → 对应 extend 不可访问
func usePartialImport(r: Record): Unit {
    r.format()  // OK: Formattable 已导入，extend 可访问
}
