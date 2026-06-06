# 泛型复用组件

## 目标

这一课把“只处理任务”的函数改成“处理任意类型”的函数。你会写泛型函数、泛型类型，并用 `where` 约束泛型参数能调用哪些能力。

## 第一个泛型函数

从列表中取第一个元素，元素类型不应该固定成 `Task`：

```cj
import std.collection.*

func first<T>(items: ArrayList<T>): ?T {
    if (items.size == 0) {
        None<T>
    } else {
        items[0]
    }
}

main() {
    let titles = ArrayList<String>(["安装工具链", "编写 main"])
    let result = first<String>(titles)

    match (result) {
        case Some(title) => println(title)
        case None => println("没有元素")
    }
}
```

`T` 是类型形参。调用 `first<String>` 时，`T` 被替换成 `String`，返回值就是 `?String`。

## 类型能推断时少写类型实参

有些调用点可以从实参推断 `T`：

```cj
let titles = ArrayList<String>(["安装工具链"])
let result = first(titles)
```

推断不清楚时再写类型实参。教程中的示例会在第一次出现时显式写，后续根据上下文省略。

## 泛型类型

任务本里可以写一个简单结果类型：

```cj
enum Result<T> {
    | Ok(T)
    | Fail(String)
}

func parseTitle(args: Array<String>): Result<String> {
    if (args.size >= 2) {
        Ok(args[1])
    } else {
        Fail("缺少任务标题")
    }
}
```

`Result<String>` 表达“成功时得到字符串，失败时得到错误消息”。如果以后解析编号，可以得到 `Result<Int64>`；如果读取任务，可以得到 `Result<Task>`。

## `where` 约束能力

泛型参数默认只代表“某种类型”，不能随便调用它的成员。要调用成员，需要加约束：

```cj
import std.collection.*

func joinText<T>(items: ArrayList<T>): String where T <: ToString {
    var result = ""
    for (item in items) {
        result += item.toString()
    }
    result
}
```

`where T <: ToString` 表示 `T` 必须具备 `ToString` 能力。没有这个约束，`item.toString()` 就没有类型依据。

## 泛型约束适合抽象仓库能力

前面写过 `TaskSink`。同样可以把“可渲染”抽出来：

```cj
interface Renderable {
    func render(): String
}

func printAll<T>(items: ArrayList<T>, sink: TaskSink): Unit where T <: Renderable {
    for (item in items) {
        sink.emit(item.render())
    }
}
```

这样 `printAll` 不再只服务 `Task`。任何实现了 `Renderable` 的类型都可以进入这个流程。

## 不要过早泛型化

泛型应该解决真实重复。下面两个信号出现时再抽：

- 两个函数结构相同，只是元素类型不同。
- 函数需要调用的能力能用接口清楚表达。

如果只有一个 `Task` 场景，先写具体函数。具体函数稳定后，再抽泛型会更准确。

## 检查点

确认你能解释：

- `first<T>` 的 `T` 在声明处和调用处分别是什么？
- 为什么 `None<T>` 需要写出 `T`？
- 为什么 `joinText` 要加 `where T <: ToString`？
- 泛型函数什么时候不如具体函数清楚？

## 练习

写一个 `countBy<T>(items: ArrayList<T>, accept: (T) -> Bool): Int64`。它遍历列表，只统计 `accept(item)` 为 `true` 的元素。
