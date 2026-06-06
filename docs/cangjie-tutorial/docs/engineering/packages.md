# 包和模块拆分

## 目标

这一课把任务本从单文件拆成包。你会安排入口、模型、仓库和输出位置，并掌握包声明与导入的顺序。

## 先确定目录

从这个结构开始：

- `src/main.cj`
- `src/taskbook/model/task.cj`
- `src/taskbook/store/memory.cj`
- `src/taskbook/output/console.cj`

入口保留在 `src/main.cj`。模型、仓库、输出按职责分到子目录。

## 模型包

`src/taskbook/model/task.cj`：

```cj
package taskbook.model

public enum TaskStatus {
    | Open | Done
}

public struct Task {
    public Task(let id: Int64, let title: String, let status: TaskStatus) {}

    public func render(): String {
        let mark = match (status) {
            case Done => "x"
            case Open => " "
        }
        "[${mark}] ${id}. ${title}"
    }
}
```

包声明必须放在第一个非注释位置。需要跨包使用的类型写 `public`，只在包内使用的内容保持默认可见性即可。

## 仓库包

`src/taskbook/store/memory.cj`：

```cj
package taskbook.store

import std.collection.*
import taskbook.model.*

public class MemoryTaskStore {
    let tasks = ArrayList<Task>()
    var nextId = 1

    public func add(title: String): Task {
        let task = Task(nextId, title, Open)
        tasks.add(task)
        nextId += 1
        task
    }

    public func all(): ArrayList<Task> {
        tasks
    }
}
```

导入写在包声明后、其他声明前。不要把 `import` 写进 `class` 或函数体。

## 输出包

`src/taskbook/output/console.cj`：

```cj
package taskbook.output

public interface LineSink {
    func emit(line: String): Unit
}

public class ConsoleSink <: LineSink {
    public func emit(line: String): Unit {
        println(line)
    }
}
```

接口和实现放在同一个包里，是小项目常见做法。项目变大后，可以把接口放在更稳定的抽象包里，把实现放在适配层。

## 入口文件

`src/main.cj`：

```cj
import taskbook.store.*
import taskbook.output.*

main(args: Array<String>) {
    let store = MemoryTaskStore()
    let sink: LineSink = ConsoleSink()

    store.add("拆分包")
    store.add("整理入口")

    for (task in store.all()) {
        sink.emit(task.render())
    }
}
```

入口只负责组装对象、解析参数和调用流程。模型规则不要写进入口；入口越厚，后续越难测试。

## 包和模块的边界

包是编译组织和名字空间边界，同一个包内不能随意重复顶层名字。模块是发布边界，一个模块可以包含多个包。任务本这种小程序可以先保持一个模块、多个包。

拆分时按依赖方向安排：

- `model` 不依赖 `store`、`output`。
- `store` 依赖 `model`。
- `output` 可以独立，也可以只依赖稳定接口。
- `main` 依赖所有需要组装的包。

不要让底层模型反向导入入口或输出层。

## 检查点

确认你能解释：

- `package` 应该写在文件哪里？
- `import` 应该写在文件哪里？
- 为什么入口最好留在源码根部？
- 为什么 `model` 不应该依赖 `store`？

## 练习

新增 `src/taskbook/command/command.cj`，定义 `public enum Command`。让 `main.cj` 只导入命令包并调用解析函数，不再直接判断字符串。
