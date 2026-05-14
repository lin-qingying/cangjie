// RUN_PIPELINE_TILL: FRONTEND
// 测试 extend 可访问性 — 跨包场景下，不要求把每个接口都单独 import 才能使用扩展成员
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
package localmain

import lib.Record
import lib.Formattable
// 注意：没有 import lib.Loggable

// 两个 extend 都可见；这里不要求把每个接口都单独 import 才能使用扩展成员
func usePartialImport(r: Record): Unit {
    r.format()
    r.log()
}
