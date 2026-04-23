G.ITF.04 尽量通过泛型约束使用接口，而不是直接将接口作为类型使用

【级别】建议

【描述】

class 以外的类型转型到 interface 可能会附带装箱操作，而作为泛型约束的方式使用 interface 可以直接静态派发，避免装箱和动态派发带来的开销，提升性能。

```cangjie
interface I {
    func f(): Unit
}

// 符合
func g<T>(i: T): Unit where T <: I {
    return i.f()
}

// 不符合
func g(i: I): Unit {
    return i.f()
}
```