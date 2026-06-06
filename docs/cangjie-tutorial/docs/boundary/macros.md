# 宏的使用边界

## 目标

这一课不把你带进完整元编程实现，而是让你知道宏解决什么问题、文件应该怎么放、调用点怎么看。任务本里只有一个适合宏的场景：调试时打印表达式和它的值。

## 宏和函数的差异

函数接收运行时值：

```cj
func show(value: Int64): Unit {
    println(value)
}
```

宏接收和返回程序片段。调用时要加 `@`：

```cj
main() {
    let x = 3
    let y = 2
    @dprint(x)
    @dprint(x + y)
}
```

`@dprint(x + y)` 既能拿到表达式的值，也能拿到表达式文本。这类需求普通函数做不到。

## 宏必须放进宏包

宏包使用 `macro package` 声明。假设宏包名为 `define`，文件放在 `src/define/dprint.cj`：

```cj
macro package define

import std.ast.*

public macro dprint(input: Tokens): Tokens {
    let inputText = input.toString()
    let result = quote(
        print($(inputText) + " = ")
        println($(input))
    )
    return result
}
```

几个关键点：

- `Tokens` 表达一段程序片段。
- `quote(...)` 构造新的程序片段。
- `$(...)` 把已有值或片段插入到 `quote` 中。
- 宏返回的片段会参与后续编译。

## 使用宏

入口文件导入宏包：

```cj
import define.*

main() {
    let openCount = 3
    let doneCount = 2

    @dprint(openCount)
    @dprint(openCount + doneCount)
}
```

编译宏包和主程序：

```bash
cjc define/*.cj --compile-macro
cjc main.cj -o taskbook
```

## 什么时候该用宏

适合用宏的信号：

- 需要读取调用点表达式本身，而不只是表达式值。
- 需要生成重复而机械的代码。
- 生成结果能保持清晰，调用者能理解展开后的效果。

不适合用宏的信号：

- 普通函数就能表达。
- 只是想少写一两个参数。
- 宏展开后会隐藏业务分支。
- 失败信息比普通代码更难定位。

任务本中，调试打印可以用宏；新增任务、完成任务、保存任务这些业务规则不应该用宏。

## 检查点

确认你能解释：

- 为什么宏调用要写 `@`？
- `Tokens` 表达的是什么？
- `quote` 和 `$()` 分别做什么？
- 为什么业务规则通常不应该藏进宏里？

## 练习

把 `@dprint(openCount + doneCount)` 改成打印 `openCount * 2 + doneCount`。观察输出中表达式文本是否也跟着变化。
