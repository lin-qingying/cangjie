// RUN_PIPELINE_TILL: FRONTEND
// 可选类型与可选链：?T 类型标注、a?.b、a?.f()、a?[i]、链式 a?.next?.value、?? 及其结合、Option 构造与 getOrThrow。
// 官方依据：Cangjie 文档《Option 类型》《操作符》（`?` 优先级 2）；cjc 1.0.5 / 1.1.3 零诊断（下标形态需要目标类型重载 []）。
class Node {
    var value: Int64 = 0
    var next: ?Node = None

    func get(): Int64 {
        return value
    }
}

struct Holder {
    var items: Array<Int64> = [1, 2]

    public operator func [](i: Int64): Int64 {
        return items[i]
    }
}

func questForms(n: ?Node, h: ?Holder, o: ?Int64): Unit {
    let member = n?.value
    let call = n?.get()
    let chain = n?.next?.value
    let subscript = h?[0]
    let coalesce = o ?? 0
    let coalesceNested = n?.value ?? -1
    let nested = n?.next?.next?.value ?? 0
    let unwrap = o.getOrThrow()
    let _ = (member, call, chain, subscript, coalesce, coalesceNested, nested, unwrap)
}
