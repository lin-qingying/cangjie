// RUN_PIPELINE_TILL: FRONTEND
// 集合类型引用：Array<接口>、HashMap<K, V>（需 import std.collection）、Array<类>、Array<元组>、Array<Option<T>>。
// 官方依据：Cangjie 文档《集合》《数组类型》；cjc 1.0.5 / 1.1.3 零诊断。
import std.collection.*

interface I {
    func f(): Int64 { return 1 }
}

class C <: I {
    public init() {}

    public func f(): Int64 { return 2 }
}

func arrayOfInterfaceTypes(xs: Array<I>, m: HashMap<String, Int64>, cs: Array<C>, t: Array<(Int64, String)>, o: Array<?Int64>): Int64 {
    let _ = (xs, m, cs, t, o)
    return xs.size
}
