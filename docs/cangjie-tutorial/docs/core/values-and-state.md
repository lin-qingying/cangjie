# 值、变量和类型边界

## 目标

这一课把任务本里的数据放进变量，区分不可变绑定、可变绑定、编译期常量、显式类型转换、值语义和引用语义。

## `let`、`var`、`const` 的分工

先写一个任务计数程序：

```cj
const DEFAULT_LIMIT = 20

main() {
    let title = "学习仓颉"
    var done = false
    var remaining: Int64 = DEFAULT_LIMIT

    println("任务：${title}")
    println("完成：${done}")

    done = true
    remaining = remaining - 1
    println("剩余配额：${remaining}")
}
```

选择规则：

- 用 `let` 表达“这个名字初始化后不再绑定到别的值”。
- 用 `var` 表达“这个名字后续会被重新赋值”。
- 用 `const` 表达“这个值要在编译期求出，并且运行时不改变”。

局部 `let` 可以先声明后初始化，但必须在使用前完成初始化：

```cj
main() {
    let command: String
    command = "list"
    println(command)
}
```

全局变量和静态成员变量定义时必须有初始值。不要把“稍后初始化”的写法放到顶层。

## 类型推断不是隐式转换

仓颉能从初始值推断变量类型：

```cj
main() {
    let count = 3
    let title = "学习仓颉"
    println("${title}: ${count}")
}
```

但不同类型之间不会自动互转。需要数值转换时显式写目标类型：

```cj
main() {
    let small: Int32 = 10
    let large: Int64 = Int64(small)
    println(large)
}
```

这种显式写法会让数据边界更清楚：读取外部数据、跨接口传参、保存到集合前，都应该确认目标类型。

## `as` 得到的是可能失败的结果

当你把值当作更宽泛的类型保存，再尝试取回具体类型时，转换可能失败。`as` 的结果用 `Option` 表达：

```cj
main() {
    let value: Any = "task"
    let text: ?String = value as String

    match (text) {
        case Some(s) => println("文本：${s}")
        case None => println("不是字符串")
    }
}
```

这里没有空值偷跑进程序。要使用转换后的值，必须处理 `Some` 和 `None` 两种情况。

## 值语义和引用语义

`struct` 更像独立值，赋值后修改副本不会影响原值：

```cj
struct CounterValue {
    var value = 0
}

main() {
    let first = CounterValue()
    var second = first
    second.value = 10

    println(first.value)
    println(second.value)
}
```

`class` 是引用语义，两个变量可以指向同一个对象：

```cj
class CounterRef {
    var value = 0
}

main() {
    let first = CounterRef()
    let second = first
    second.value = 10

    println(first.value)
    println(second.value)
}
```

集合类型也常被作为共享对象使用。`let` 只限制变量不能重新绑定，不代表集合内容不能改：

```cj
import std.collection.*

main() {
    let tasks = ArrayList<String>()
    tasks.add("学习入口函数")
    tasks.add("理解变量")
    println(tasks.size)
}
```

## 检查点

你应该能解释下面三点：

- 为什么 `let tasks = ArrayList<String>()` 后还能 `tasks.add(...)`？
- 为什么 `Int64(small)` 不能省略？
- 为什么 `as` 后要处理 `None`？

## 练习

把任务标题、是否完成、剩余配额放进三个变量。尝试把 `remaining` 从 `var` 改成 `let`，观察后续赋值位置为什么不再成立。
