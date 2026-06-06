# 函数和 Lambda

## 目标

这一课把命令解析、任务打印和条件判断拆成函数，再用 Lambda 表达“把一个规则传给另一个函数”。完成后，任务本的入口会更薄，业务规则会更容易测试和替换。

## 函数体可以直接返回最后一个表达式

先抽出命令读取函数：

```cj
func readCommand(args: Array<String>): String {
    if (args.size == 0) {
        "help"
    } else {
        args[0]
    }
}
```

也可以显式 `return`：

```cj
func readCommand(args: Array<String>): String {
    if (args.size == 0) {
        return "help"
    }

    return args[0]
}
```

两种写法都可以。教程后续会优先让短函数使用最后一个表达式，让提前退出使用 `return`。

## 参数默认不可变

函数参数在函数体内不能被重新赋值。需要修改时创建局部变量：

```cj
func normalize(command: String): String {
    var result = command
    if (result == "") {
        result = "help"
    }
    result
}
```

这样能避免调用者传入的概念和函数内部临时状态混在一起。

## 命名参数让调用点更清楚

命名参数在定义时写 `!`：

```cj
func renderTask(id!: Int64, title!: String, done!: Bool = false): String {
    let status = if (done) { "x" } else { " " }
    "[${status}] ${id}. ${title}"
}

main() {
    println(renderTask(id: 1, title: "学习函数"))
    println(renderTask(id: 2, title: "学习 Lambda", done: true))
}
```

默认值只能给命名参数。调用时用 `参数名: 值`，比一串同类型参数更不容易写错。

## Lambda 是可以传递的规则

Lambda 的完整形式是 `{ 参数列表 => 函数体 }`。没有参数也要写 `=>`：

```cj
let nextId = { current: Int64 => current + 1 }
let show = { => println("taskbook") }
```

把 Lambda 用作函数参数，可以让函数接收一段策略：

```cj
import std.collection.*

func count(tasks: ArrayList<String>, accept: (String) -> Bool): Int64 {
    var result = 0
    for (task in tasks) {
        if (accept(task)) {
            result += 1
        }
    }
    result
}

main() {
    let tasks = ArrayList<String>(["安装工具链", "编写 main", "学习 match"])
    let all = count(tasks, { task: String => task != "" })
    println(all)
}
```

这里 `accept` 的类型是 `(String) -> Bool`，意思是接收一个 `String`，返回一个 `Bool`。

## 尾随 Lambda

当函数最后一个参数是函数类型，并且调用点传入 Lambda，可以把 Lambda 放到圆括号后面：

```cj
func once(action: () -> Unit): Unit {
    action()
}

main() {
    once {
        println("执行一次")
    }
}
```

这种写法适合让调用点像控制结构一样自然。不要为了炫技把普通参数都改成 Lambda；只有当调用者确实要传一段行为时再用。

## 在任务本中应用

把路由拆成三层：

```cj
func readCommand(args: Array<String>): String {
    if (args.size == 0) { "help" } else { args[0] }
}

func route(command: String): String {
    match (command) {
        case "add" => "准备新增任务"
        case "list" => "准备列出任务"
        case "done" => "准备完成任务"
        case "help" => "taskbook: add | list | done"
        case _ => "未知命令：${command}"
    }
}

func run(args: Array<String>, printer: (String) -> Unit): Unit {
    let command = readCommand(args)
    printer(route(command))
}

main(args: Array<String>) {
    run(args, { message: String => println(message) })
}
```

现在 `run` 不直接依赖 `println`，而是依赖一个“打印消息”的函数值。后续可以把它换成写文件、测试收集器或 GUI 输出。

## 检查点

确认你能解释：

- 普通函数为什么写 `func`，入口为什么不写？
- 函数最后一个表达式如何影响返回类型？
- 命名参数调用为什么要写 `name: value`？
- Lambda 里的 `=>` 什么时候能省略？

## 练习

把 `route` 的返回类型显式写成 `String`。再把 `printer` 换成一个先加前缀再打印的 Lambda。
