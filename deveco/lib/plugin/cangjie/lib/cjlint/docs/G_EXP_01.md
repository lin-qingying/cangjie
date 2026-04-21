G.EXP.01 match 表达式同一层尽量避免不同类别的 pattern 混用

【级别】建议

【描述】

仓颉提供了丰富的模式种类，包括：常量模式、通配符模式、变量模式、tuple 模式、类型模式、enum 模式。在类型匹配的前提下，根据是否总是能匹配分为两种：refutable pattern 和 irrefutable pattern，其中 irrefutable pattern 总是可以和它所要匹配的值匹配成功。

对 pattern 的使用建议如下：

- match 表达式的不同 case 的 pattern 之间尽量保持互斥，避免依赖匹配顺序；
- match 不能互斥时，由于匹配的顺序是从前往后，要避免前面的 case 遮盖后面的 case，比如 irrefutable pattern 的 case 需要放到所有 refutable pattern 的 case 之后；
- match 表达式同一层中尽量避免混用不同判断维度的模式：
    - 类型模式和其它判断的维度也不一样，比如常量模式是根据值来判断，类型模式是判断类型，混用后对 exhaustive 的可读性会有影响；
    - tuple 模式、enum 模式属于解构，可以和常量模式、变量模式结合使用。

【正例】

```cangjie
enum TimeUnit {
    | Year(Int64)
    | Month(Int64, Int64)
    | Day(Int64, Int64, Int64)
    | Hour(Int64, Int64, Int64, Int64)
}

let oneYear = Year(1)
let howManyHours = match (oneYear) {
    case Year(y) => //...
    case Month(y, m) => //...
    case Day(y, m, d) => //...
    case Hour(y, m, d, h) => //...
}
```

【反例】

```cangjie
enum TimeUnit {
    | Year(Int64)
    | Month(Int64, Int64)
    | Day(Int64, Int64, Int64)
    | Hour(Int64, Int64, Int64, Int64)
}

let oneYear = Year(1)
let howManyHours = match (oneYear) { // 不符合：enum 模式、类型模式混用
    case Month(y, m) => ...
    case _: TimeUnit => ...
    case Day(y, m, d) => ...
    case Hour(y, m, d, h) => ...
}
```