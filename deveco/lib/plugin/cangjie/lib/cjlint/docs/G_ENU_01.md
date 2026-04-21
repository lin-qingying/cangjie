G.ENU.01：避免 enum 的构造器与顶层元素同名

【级别】要求

【描述】

enum 构造器名字在类型所在作用域下总是自动引入，可以省略类型前缀使用。

但是当 enum 构造器与变量名、函数名、类型名、包名冲突的时候，会优先选择变量名、函数名、类型名或包名，不容易发现冲突，也难以直观看出实际使用的版本。

所以应尽量保证 enum 的构造器与顶层函数使用不同的名字，以避免不必要的重载所带来的困惑。

【正例】

```cangjie
enum TimeUnit {
    | Year(Int64)
    | Month(Int64, Int64)
    | Day(Int64, Int64, Int64)
}
class MyYear {
    let a: Int64
    init(a: Int64) {
        this.a = a
    }
}
main() {
    let y1 = Year(100)   // ok，Year(100) 调用的是 TimeUnit 中的 Year(Int64) 构造器
    let y2 = MyYear(100) // ok，调用的是 class MyYear 的构造函数
    return 0
}
```

【反例】

```cangjie
enum TimeUnit {
    | Year(Int64)  // 不符合：enum 构成成员与顶层的 class 类型同名
    | Month(Int64, Int64)
    | Day(Int64, Int64, Int64)
}
class Year {
    Year(let a: Int64) {
    }
}
main() {
    let y = Year(100) // 实际使用的是 class Year 的构造函数
    return 0
}
```

```cangjie
enum E {
    | f1(Int64)  // 不符合：enum 构成成员与顶层的函数同名
    | f2(Int64, Int64)
}
func f1(a: Int64) {}
func f2(a: Int64, b: Int64) {}
main() {
    f1(1)    // 实际使用的是 func f1
    f2(1, 2) // 实际使用的是 func f2
    return 0
}
```