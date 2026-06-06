# 第一个命令行程序

## 目标

这一课会写出第一个可编译程序，理解仓颉入口函数的特殊写法，并让程序读取命令行参数。

## 写入最小任务本

在 `main.cj` 中写入：

```cj
main() {
    println("taskbook")
    println("add    新增任务")
    println("list   查看任务")
    println("done   完成任务")
}
```

编译并运行：

```bash
cjc main.cj -o taskbook
./taskbook
```

Windows 终端中可执行文件后缀可能不同，按实际输出文件运行即可。

## `main` 不写 `func`

普通函数要写 `func`：

```cj
func banner(): String {
    "taskbook"
}
```

入口函数直接写 `main`：

```cj
main() {
    println(banner())
}
```

入口函数可以没有参数，也可以接收 `Array<String>` 类型的命令行参数。返回类型可以是 `Unit` 或整数类型。先记住两种最常见写法：

```cj
main() {
    println("无参数入口")
}

main(args: Array<String>) {
    println("参数个数：${args.size}")
}
```

同一个可执行模块里只能有一个入口。不要在多个源文件里同时写 `main`。

## 读取命令

把 `main.cj` 改成带参数版本：

```cj
main(args: Array<String>) {
    if (args.size == 0) {
        println("taskbook: add | list | done")
        return
    }

    let command = args[0]
    if (command == "add") {
        println("准备新增任务")
    } else if (command == "list") {
        println("准备列出任务")
    } else if (command == "done") {
        println("准备完成任务")
    } else {
        println("未知命令：${command}")
    }
}
```

再次编译后运行：

```bash
cjc main.cj -o taskbook
./taskbook list
./taskbook add
./taskbook remove
```

## 为什么先判断 `args.size`

`args[0]` 表示访问第一个参数。没有参数时访问它会越界。越界不是业务分支，而是运行时失败，所以命令行程序应该先确认参数数量，再访问下标。

这一点会贯穿后面的代码：能用类型或分支明确表达的缺失，就不要等到越界或空值类错误出现。

## 检查点

你应该能回答：

- 为什么 `main` 不写 `func`？
- `args` 的类型是什么？
- 为什么访问 `args[0]` 前要检查 `args.size`？
- `return` 在这里返回什么？

## 练习

给程序增加一个 `help` 命令。`help` 和无参数时都打印命令列表，未知命令仍打印错误信息。
