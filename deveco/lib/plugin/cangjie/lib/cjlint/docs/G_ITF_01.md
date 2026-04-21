G.ITF.01 对于需要原地修改对象自身的抽象函数，尽量使用 mut 修饰，以支持 struct 类型实现或扩展该接口

【级别】建议

【描述】

**说明：** 如果对于可能需要原地修改的函数不声明为 mut 函数，未来就不能被 struct 类型实现，会导致接口的抽象能力降低。

【正例】

```cangjie.compile
interface Increasable {
    mut func increase(): Unit
}

struct R <: Increasable {
    var item = 0
    public mut func increase(): Unit {
        item += 1
    }
}
```

【反例】

```cangjie.compile.fail
interface Increasable {
    func increase(): Unit // 不符合：struct 类型实现该接口时，无法实际被修改
}

struct R <: Increasable {
    var item = 0
    public func increase(): Unit {
        item += 1  // item 不能被实际修改
    }
}
```