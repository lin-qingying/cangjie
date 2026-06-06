# class 与 interface 协作

## 目标

这一课把可变集合藏进 `class`，再用 `interface` 抽象输出能力。你会区分：值对象用 `struct`，共享状态用 `class`，能力约束用 `interface`。

## 用 `class` 封装共享状态

任务仓库需要维护同一组可变数据。它适合用 `class`：

```cj
import std.collection.*

class TaskStore {
    let tasks = ArrayList<Task>()
    var nextId = 1

    public func add(title: String): Task {
        let task = Task(nextId, title, Open)
        tasks.add(task)
        nextId += 1
        task
    }

    public func size(): Int64 {
        tasks.size
    }

    public func printAll(): Unit {
        for (task in tasks) {
            println(task.render())
        }
    }
}
```

`tasks` 用 `let` 绑定，表示这个成员始终指向同一个列表；列表内部仍然可以增删。`nextId` 用 `var`，因为每次新增任务都会更新它。

## 引用语义适合仓库

多个变量指向同一个仓库时，会看到同一份状态：

```cj
main() {
    let store = TaskStore()
    let sameStore = store

    store.add("学习 class")
    sameStore.add("理解引用语义")

    println(store.size())
    println(sameStore.size())
}
```

这正是仓库对象想要的行为：所有调用者操作同一个任务集合。相反，单条 `Task` 不适合用这种共享方式表达。

## 用 `interface` 定义输出能力

不要让业务流程固定依赖 `println`。先定义一个输出接口：

```cj
interface TaskSink {
    func emit(line: String): Unit
}

class ConsoleSink <: TaskSink {
    public func emit(line: String): Unit {
        println(line)
    }
}
```

实现接口时，成员函数要用 `public`。接口成员默认就是对外能力，具体类型不能用更窄的可见性实现它。

## 让仓库依赖接口

```cj
class TaskStore {
    let tasks = ArrayList<Task>()
    var nextId = 1

    public func add(title: String): Task {
        let task = Task(nextId, title, Open)
        tasks.add(task)
        nextId += 1
        task
    }

    public func printAll(sink: TaskSink): Unit {
        for (task in tasks) {
            sink.emit(task.render())
        }
    }
}

main() {
    let store = TaskStore()
    store.add("学习 interface")
    store.add("解耦输出")

    let sink: TaskSink = ConsoleSink()
    store.printAll(sink)
}
```

现在 `TaskStore` 只知道“有人能接收一行文本”，不关心文本最终打印到终端、写入文件还是进入测试结果。

## 接口可以有默认实现

对于所有输出器都能共用的行为，可以放在接口默认函数里：

```cj
interface TaskSink {
    func emit(line: String): Unit

    func emitHeader(): Unit {
        emit("任务列表")
    }
}
```

默认实现适合无状态、普遍成立的行为。需要访问具体对象内部状态时，仍应让具体类型自己实现。

## 继承不是复用首选

`class` 支持继承，但教程主线优先用组合和接口。判断方法：

- 需要共享可变状态：用 `class`。
- 需要表达一组能力：用 `interface`。
- 只是复用一小段流程：先用函数。
- 只是保存一条业务事实：用 `struct`。

继承适合真正的子类型关系，而不是为了少写几行代码。

## 检查点

确认你能解释：

- 为什么 `TaskStore` 用 `class`，`Task` 用 `struct`？
- `let tasks = ArrayList<Task>()` 为什么仍能添加元素？
- 为什么 `ConsoleSink` 实现 `emit` 时要写 `public`？
- `TaskStore` 依赖 `TaskSink` 有什么好处？

## 练习

写一个 `BufferSink`，内部用 `ArrayList<String>` 保存收到的行，再提供 `size()` 返回已接收行数。
