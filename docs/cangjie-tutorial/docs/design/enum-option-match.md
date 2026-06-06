# enum、Option 与 match

## 目标

这一课把“命令”和“查找结果”变成类型。你会用 `enum` 表达有限分支，用 `Option` 表达可能缺失，用 `match` 强制处理所有情况。

## 命令不应该只是字符串

字符串命令容易把错误拖到运行时。先定义命令类型：

```cj
enum Command {
    | Add(String)
    | List
    | Done(Int64)
    | Help
    | Unknown(String)
}
```

带参数的构造器可以携带数据。`Add(String)` 携带任务标题，`Done(Int64)` 携带任务编号。

## 把字符串解析成命令

```cj
func parseCommand(args: Array<String>): Command {
    if (args.size == 0) {
        return Help
    }

    match (args[0]) {
        case "add" =>
            if (args.size >= 2) { Add(args[1]) } else { Unknown("add 缺少标题") }
        case "list" => List
        case "done" =>
            if (args.size < 2) {
                Unknown("done 缺少编号")
            } else if (args[1] == "1") {
                Done(1)
            } else {
                Unknown("暂时只演示完成编号 1")
            }
        case "help" => Help
        case other => Unknown(other)
    }
}
```

真实程序里，字符串转整数要走解析函数并处理失败路径。这里先聚焦 `enum` 和 `match` 的形状，错误处理会在后面单独处理。

## `match` 让分支穷尽

处理命令时，所有构造器都要覆盖：

```cj
func describe(command: Command): String {
    match (command) {
        case Add(title) => "新增任务：${title}"
        case List => "列出任务"
        case Done(id) => "完成任务：${id}"
        case Help => "taskbook: add | list | done"
        case Unknown(raw) => "未知命令：${raw}"
    }
}
```

当以后给 `Command` 增加新构造器，遗漏处理的位置会被暴露出来。这比在多个字符串判断中漏掉分支更可靠。

## `Option` 表达可能没有

查找任务时，任务可能存在，也可能不存在。用 `?Task` 表达：

```cj
import std.collection.*

func findTask(tasks: HashMap<Int64, Task>, id: Int64): ?Task {
    if (tasks.contains(id)) {
        tasks[id]
    } else {
        None<Task>
    }
}
```

`?Task` 等价于 `Option<Task>`。有值时是 `Some(task)`，无值时是 `None`。当上下文明确需要 `?Task`，直接返回 `tasks[id]` 会被包装成有值结果。

## 使用前必须拆开

```cj
func printFound(result: ?Task): Unit {
    match (result) {
        case Some(task) => println(task.render())
        case None => println("没有找到任务")
    }
}
```

这段代码没有“忘记判断空”的空间。要拿到 `Task`，就必须从 `Some` 分支中取出来。

## 默认值运算

当缺失时能用默认值，可以用 `??`：

```cj
func titleOrDefault(title: ?String): String {
    title ?? "未命名任务"
}
```

只在默认值确实有业务意义时使用。找不到任务通常不应该静默替换成假任务，而应该返回 `None` 或报错。

## 不带匹配值的 `match`

有时你只是想按条件选择：

```cj
func priority(score: Int64): String {
    match {
        case score >= 80 => "高"
        case score >= 40 => "中"
        case _ => "低"
    }
}
```

这种写法适合分数、阈值、状态组合判断。仍然要保留 `_` 作为最后分支。

## 检查点

确认你能解释：

- 为什么 `Command` 比裸字符串更适合路由？
- `?Task` 和 `Option<Task>` 是什么关系？
- `None<Task>` 什么时候需要写出类型实参？
- 为什么 `match (result)` 必须处理 `Some` 和 `None`？

## 练习

给 `Command` 增加 `Stats` 构造器，并更新 `parseCommand`、`describe`。故意漏掉一个 `match` 分支，观察编译器如何提醒你。
