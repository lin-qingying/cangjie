# 异常和 I/O

## 目标

这一课把失败路径显式写出来。你会用 `throw` 抛出异常，用 `try` 捕获异常，用 `try-with-resources` 管理需要关闭的资源。

## 什么时候用异常

任务标题为空、命令不存在、文件读写失败，都可能让当前流程无法继续。对于调用者必须处理的失败，可以先用 `Option` 或业务 `enum`；对于无法在当前层恢复的异常路径，可以抛出异常。

```cj
func requireTitle(args: Array<String>): String {
    if (args.size < 2) {
        throw IllegalArgumentException("缺少任务标题")
    }

    args[1]
}
```

`throw` 后面必须是 `Exception` 的子类型。它会打断当前正常流程。

## 捕获异常

```cj
main(args: Array<String>) {
    try {
        let title = requireTitle(args)
        println("新增任务：${title}")
    } catch (e: IllegalArgumentException) {
        println(e.message)
    }
}
```

`catch` 使用模式捕获异常。更具体的异常类型放在前面，更宽泛的类型放在后面。

## `try` 也能产生值

当成功路径和失败路径都给出同一种业务值，`try` 可以放在赋值右侧：

```cj
main(args: Array<String>) {
    let title = try {
        requireTitle(args)
    } catch (e: IllegalArgumentException) {
        "未命名任务"
    }

    println(title)
}
```

不要滥用默认值。只有当“未命名任务”确实符合业务需求时，才这样写。否则应该把错误报告给调用者。

## `finally` 做清理

```cj
main(args: Array<String>) {
    try {
        println(requireTitle(args))
    } catch (e: IllegalArgumentException) {
        println(e.message)
    } finally {
        println("命令处理结束")
    }
}
```

`finally` 无论是否抛出异常都会执行，适合放清理动作。不要在 `finally` 里再写复杂业务。

## 资源自动释放

文件、网络连接、句柄这类资源需要关闭。实现 `Resource` 的对象可以放进 `try (name = resource)`：

```cj
class MemoryHandle <: Resource {
    var closed = false

    public func isClosed(): Bool {
        closed
    }

    public func close(): Unit {
        closed = true
        println("资源已关闭")
    }

    public func write(line: String): Unit {
        println(line)
    }
}

main() {
    try (handle = MemoryHandle()) {
        handle.write("保存任务")
    }
}
```

离开 `try` 块时，运行时会检查资源是否关闭；没有关闭时调用 `close()`。这比把关闭动作散落在多个分支里更稳。

## I/O 边界要薄

任务本后续如果读写文件，建议保持这个结构：

- 模型层只处理 `Task`、`Command`、`TaskStatus`。
- 仓库层只暴露 `add`、`all`、`find` 这类业务动作。
- I/O 层负责把外部字节或文本转换成模型。
- 入口负责把 I/O 层和业务层接起来。

这样文件格式变化不会影响任务状态的定义，任务状态变化也不会直接影响文件读写代码。

## 检查点

确认你能解释：

- `throw` 后面为什么必须是异常类型？
- `catch (e: IllegalArgumentException)` 的 `e` 作用域在哪里？
- `try` 产生值时，各分支为什么要能合成一个类型？
- `try-with-resources` 适合管理哪类对象？

## 练习

把 `parseCommand` 改成：参数不足时抛出 `IllegalArgumentException`，入口捕获后打印错误消息和帮助文本。
