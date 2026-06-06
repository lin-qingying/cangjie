# 表达式驱动流程

## 目标

这一课用表达式改写命令分发逻辑。你会看到 `if` 和 `match` 如何产生值，循环为什么只适合做副作用，`Nothing` 为什么能让提前退出更自然。

## 用 `if` 产生值

先把命令行参数转成命令文本：

```cj
func readCommand(args: Array<String>): String {
    if (args.size == 0) {
        "help"
    } else {
        args[0]
    }
}

main(args: Array<String>) {
    let command = readCommand(args)
    println(command)
}
```

`if` 的两个分支都给出 `String`，整个 `if` 就可以作为函数体最后一个表达式。函数体最后一项是表达式时，函数体类型就是这个表达式的类型。

## 用 `match` 分发命令

命令分支变多后，用 `match` 更清楚：

```cj
func actionName(command: String): String {
    match (command) {
        case "add" => "新增任务"
        case "list" => "列出任务"
        case "done" => "完成任务"
        case "help" => "打印帮助"
        case _ => "未知命令"
    }
}

main(args: Array<String>) {
    let command = readCommand(args)
    println(actionName(command))
}
```

`match` 必须覆盖所有可能输入。最后的 `_` 是兜住其他字符串的通配分支。没有这一支，编译器无法确认每个字符串都有结果。

## 带条件的分支

`case` 后可以增加 `where`，用于匹配成功后的额外条件：

```cj
func priorityLabel(score: Int64): String {
    match (score) {
        case s where s >= 80 => "高"
        case s where s >= 40 => "中"
        case _ => "低"
    }
}
```

分支从上往下匹配。先写更具体或更高优先级的条件，再写兜底分支。

## 循环处理副作用

循环表达式的类型是 `Unit`，适合做遍历、打印、累计修改：

```cj
import std.collection.*

main() {
    let tasks = ArrayList<String>(["安装工具链", "编写 main", "处理参数"])
    var index = 0

    for (task in tasks) {
        println("${index}: ${task}")
        index += 1
    }
}
```

当你想“算出一个值”时，优先考虑 `if`、`match` 或函数返回值；当你想“对一批数据做动作”时，再使用循环。

## 提前退出和 `Nothing`

`return`、`break`、`continue`、`throw` 这类表达式不会正常产出一个业务值，它们的类型是 `Nothing`。这让它们可以出现在需要其他类型的分支里：

```cj
func requireCommand(args: Array<String>): String {
    if (args.size == 0) {
        println("taskbook: add | list | done")
        return "help"
    }

    args[0]
}
```

如果函数要在缺少参数时真正结束，也可以直接返回 `Unit` 路径：

```cj
main(args: Array<String>) {
    if (args.size == 0) {
        println("taskbook: add | list | done")
        return
    }

    println("命令：${args[0]}")
}
```

## 常见改造

把第一课的多层 `if` 改成这个形状：

```cj
func route(command: String): String {
    match (command) {
        case "add" => "准备新增任务"
        case "list" => "准备列出任务"
        case "done" => "准备完成任务"
        case "help" => "taskbook: add | list | done"
        case _ => "未知命令：${command}"
    }
}

main(args: Array<String>) {
    let command = readCommand(args)
    println(route(command))
}
```

现在命令解析和命令分发被拆开了。下一课加入集合时，这种拆分会继续保留。

## 检查点

确认你能判断：

- `if` 的两个分支类型不同会发生什么？
- `match` 为什么需要 `_`？
- 为什么循环不适合直接作为业务值返回？
- `return` 后面的表达式类型为什么不等于 `return` 表达式本身的类型？

## 练习

给 `route` 增加 `stats` 命令，返回“准备统计任务”。再把未知命令文案改成带命令名的字符串插值。
