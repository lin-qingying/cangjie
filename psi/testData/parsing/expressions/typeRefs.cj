// 类型引用：嵌套泛型、函数类型形参、元组类型、可选类型与类型参数组合。
// 官方依据：Cangjie 文档《类型》；cjc 1.0.5 / 1.1.3 零诊断。
type Nested = Array<Array<Option<Int64>>>
type StringList = Array<String>

class Box<T> {
    private var value: T

    public init(value: T) {
        this.value = value
    }
}

func typeRefs(
    a: Array<Array<Int64>>,
    b: (Int64, String),
    c: ?Int64,
    d: (Int64) -> Int64,
    e: Array<Box<Int64>>
): Unit {
    let _ = a
    let _ = b
    let _ = c
    let _ = d
    let _ = e
}

func returnsFunction(): (Int64) -> Int64 {
    return { x: Int64 => x }
}
