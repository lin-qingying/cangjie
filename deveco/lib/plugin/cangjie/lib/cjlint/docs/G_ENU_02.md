G.ENU.02 尽量避免不同 enum 构造器之间不必要的重载

【级别】建议

【描述】

因为 enum 构造器的名字在类型所在作用域下总是自动引入的，所以不同 enum 中定义同名且对应位置参数类型存在子类型关系的构造成员后，省略类型前缀的使用方式将不再可用。enum 构造器参与函数的重载决议，当无法决议时 enum 构造器和函数均不能直接使用，此时 enum 构造器需要使用类型前缀的方式使用，函数也需要通过前缀限定的方式使用。只要有多个 enum constructor 通过类型检查，或只要有 enum constructor 和函数同时通过类型检查，就会造成无法决议。

【正例】

```cangjie
enum TimeUnit1 {
    | Year1(Int64)
    | Month1(Int64, Int64)
    | Day1(Int64, Int64, Int64)
}
enum TimeUnit2 {
    | Year2(Int64)
    | Month2(Int64, Int64)
    | Day2(Int64, Int64, Int64)
}
main() {
    let a = Year1(1) // ok：无需使用 enum 类型前缀
    let b = Year2(2) // ok：无需使用 enum 类型前缀
    return 0
}
```

```cangjie
open class Base {}
class Derived <: Base {}
enum E1 {
    | A1(Base)
}
enum E2 {
    | A2(Derived)
}
main() {
    let a1 = A1(Derived()) // ok：无需使用 enum 类型前缀
    let a2 = A2(Derived()) // ok：无需使用 enum 类型前缀
    return 0
}
```

【反例】

```cangjie
enum TimeUnit1 {
    | Year(Int64)
    | Month(Int64, Int64)
    | Day(Int64, Int64, Int64)
}
enum TimeUnit2 {
    | Year(Int64)
    | Month(Int64, Int64)
    | Day(Int64, Int64, Int64)
}
main() {
    let a = Year(1) // error：无法决议调用的是哪个 Year(Int64)
    let b = TimeUnit1.Year(1) // ok：使用 enum 类型前缀
    let c = TimeUnit2.Year(2) // ok：使用 enum 类型前缀
    return 0
}
```

```cangjie
open class Base {}
class Derived <: Base {}
enum E1 {
    | A(Base)
}
enum E2 {
    | A(Derived)
}
main() {
    let a = A(Derived()) // error：无法决议调用的是哪个 enum 中的 constructor
    let a2 = E1.A(Derived()) // ok：使用 enum 类型前缀
    let a3 = E2.A(Derived()) // ok：使用 enum 类型前缀
    return 0
}
```