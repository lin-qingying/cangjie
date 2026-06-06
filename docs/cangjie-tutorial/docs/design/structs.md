# struct 建模值

## 目标

这一课把任务从 `String` 升级成值对象。你会用 `struct` 表达任务编号、标题和状态，并理解为什么它适合不可共享的业务值。

## 定义任务值

先写任务状态，再写任务本体：

```cj
enum TaskStatus {
    | Open | Done
}

struct Task {
    public Task(let id: Int64, let title: String, let status: TaskStatus) {}

    public func isDone(): Bool {
        match (status) {
            case Done => true
            case Open => false
        }
    }

    public func render(): String {
        let mark = if (isDone()) { "x" } else { " " }
        "[${mark}] ${id}. ${title}"
    }
}

main() {
    let task = Task(1, "学习 struct", Open)
    println(task.render())
}
```

`Task` 的主构造函数把 `id`、`title`、`status` 同时声明为成员变量和构造参数。因为它们都是 `let`，创建后不能被改写。

## 为什么任务适合 `struct`

任务记录本身是一个值：编号、标题、状态组成一条事实。把它传给函数时，你通常希望函数基于这条事实计算结果，而不是悄悄修改原对象。

例如，完成任务时可以创建新值：

```cj
func complete(task: Task): Task {
    Task(task.id, task.title, Done)
}

main() {
    let before = Task(1, "学习 struct", Open)
    let after = complete(before)

    println(before.render())
    println(after.render())
}
```

这种写法让状态变化变得显式。旧值还在，新值也清楚。

## 成员函数不一定要返回 `Unit`

成员函数和普通函数一样，可以用最后一个表达式作为结果：

```cj
struct TaskTitle {
    public TaskTitle(let value: String) {}

    public func isEmpty(): Bool {
        value == ""
    }
}
```

需要副作用时才返回 `Unit`。只做判断、格式化、计算时，返回业务值更直接。

## 用小 `struct` 保护边界

如果直接把编号到处写成 `Int64`，任何整数都能被当成任务编号。用小类型可以让代码更清楚：

```cj
struct TaskId {
    public TaskId(let value: Int64) {}

    public func isValid(): Bool {
        value > 0
    }
}

struct Task {
    public Task(let id: TaskId, let title: String, let status: TaskStatus) {}
}
```

现在创建任务时要显式构造 `TaskId`。这种轻量封装很适合业务含义强的值。

## 不要写递归 `struct`

`struct` 是值类型，不适合直接包含自身：

```cj
struct BadNode {
    let next: BadNode
}
```

这种形状没有固定大小。需要链式共享结构时，改用 `class` 或者让成员通过引用类型间接保存。

## 在集合中使用任务值

```cj
import std.collection.*

main() {
    let tasks = ArrayList<Task>()
    tasks.add(Task(1, "学习 struct", Open))
    tasks.add(Task(2, "完成任务值建模", Done))

    for (task in tasks) {
        println(task.render())
    }
}
```

现在集合里不再是零散字符串，而是一组结构化任务。后面会用 `class` 封装这组集合，避免任意位置都能修改它。

## 检查点

确认你能解释：

- 主构造函数里的 `let id: Int64` 做了哪两件事？
- 为什么 `complete` 返回新 `Task`，而不是修改原任务？
- 为什么递归 `struct` 不合适？
- 什么时候应该把 `Int64` 包成 `TaskId`？

## 练习

给 `Task` 增加 `withTitle(newTitle: String): Task`，返回标题被替换、编号和状态保持不变的新任务。
