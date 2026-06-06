# 读懂源文件和入口

## 目标

这一课把单文件程序拆成几个概念：顶层声明、局部声明、成员声明、包声明和导入。你会知道什么能写在文件顶层，什么只能写在函数或类型内部。

## 从一个文件开始

下面这个文件包含了仓颉源文件里最常见的几种顶层声明：

```cj
let appName = "taskbook"

func title(): String {
    appName
}

struct TaskId {
    public TaskId(let value: Int64) {}
}

class TaskCounter {
    var value = 0

    public func next(): Int64 {
        value += 1
        value
    }
}

enum TaskStatus {
    | Open | Done
}

main() {
    println(title())
}
```

顶层可以放变量、函数、自定义类型和入口。函数体里可以继续定义局部变量和局部函数，但不能在函数体中定义新的 `class`、`struct`、`enum` 或 `interface`。

## 局部函数适合临时规则

当一个小函数只服务当前函数，可以放在函数体里：

```cj
main(args: Array<String>) {
    func hasCommand(): Bool {
        args.size > 0
    }

    if (hasCommand()) {
        println("命令：${args[0]}")
    } else {
        println("taskbook: add | list | done")
    }
}
```

局部函数能访问外层作用域里的名字。这个能力很方便，但不要用它藏太多业务逻辑；一旦函数需要复用，就应该提升为顶层函数或成员函数。

## 类型成员服务类型本身

成员变量和成员函数写在类型内部：

```cj
class TaskCounter {
    var value = 0

    public func next(): Int64 {
        value += 1
        value
    }
}

main() {
    let counter = TaskCounter()
    println(counter.next())
    println(counter.next())
}
```

`enum` 和 `interface` 中可以定义成员函数，但不定义成员变量。需要保存数据时用 `struct` 或 `class`，需要表达有限分支时用 `enum`，需要约束能力时用 `interface`。

## 包声明和导入的位置

有包声明时，它必须出现在文件中第一个非注释位置。导入放在包声明之后、其他声明之前：

```cj
package taskbook.app

import std.collection.*

func commandNames(): ArrayList<String> {
    ArrayList<String>(["add", "list", "done"])
}
```

不要把 `import` 写进函数体。导入是文件级别的声明，它决定当前文件后续代码能直接使用哪些包内名字。

## 入口属于模块根部

命令行程序最终只能有一个入口。被导入的包里即使有 `main`，也不会因为导入而成为当前程序的入口。实践上可以这样安排：

- 入口文件只解析命令行和调用应用逻辑。
- 业务模型放到单独包里。
- 存储、I/O、并发放到更靠后的包里。

先不用急着拆文件，下一课还会继续在单文件里学习变量和类型。等你知道哪些规则稳定后，再拆包会更自然。

## 检查点

判断下面写法是否合适：

```cj
main() {
    struct LocalTask {}
}
```

不合适。自定义类型只能放在顶层。把 `LocalTask` 移到文件顶层，再从 `main` 中使用。

## 练习

把上一课的命令列表提取成顶层函数 `usage(): String`，让无参数和 `help` 命令都调用它。
