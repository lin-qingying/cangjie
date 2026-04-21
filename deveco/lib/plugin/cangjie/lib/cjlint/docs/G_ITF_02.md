G.ITF.02 尽量在类型定义处就实现接口，而不是通过扩展实现接口

【级别】建议

【描述】

- 通过扩展实现接口不应该被滥用，如果一个类型在定义时就已知将要实现的接口信息，应该将接口直接声明出来，有利于使用者集中浏览信息。
- 通过扩展实现的接口，和类型定义处声明实现接口，在实现层面可能带来协变层面的问题。

【正例】

```cangjie
interface I {
    func f(): Unit
}

// 符合：类型定义处实现接口
class A <: I {
    public func f(): Unit {
        // CODE
    }
}
```

【反例】

```cangjie
interface I {
    func f(): Unit
}

class C {}

extend C <: I {
    public func f(): Unit {}
}

main() {
    let i: I = C() // ok

    let f1: () -> C = { => C() }
    let f2: () -> I = f1 // 报错，虽然 () -> C 是 () -> I 的子类型，但 C 通过扩展实现 I，此时不能协变，导致不能赋值。
    return 0
}
```