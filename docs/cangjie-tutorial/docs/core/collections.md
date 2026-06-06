# 集合组织数据

## 目标

这一课把任务本从“只打印命令”推进到“保存一组任务”。你会使用 `ArrayList` 保存有序任务，用 `HashMap` 按编号查任务，并理解集合的引用语义。

## 用 `ArrayList` 保存任务标题

先从字符串列表开始：

```cj
import std.collection.*

main() {
    let tasks = ArrayList<String>()
    tasks.add("安装工具链")
    tasks.add("编写入口")
    tasks.add("理解变量")

    for (task in tasks) {
        println(task)
    }
}
```

`ArrayList<T>` 的 `T` 是元素类型。这里的列表只能放 `String`，不能混入整数或其他对象。`add` 会把元素加到末尾，`for-in` 可以逐个遍历。

## 访问、修改和插入

下标从 `0` 开始：

```cj
import std.collection.*

main() {
    let tasks = ArrayList<String>(["安装工具链", "编写入口", "理解变量"])

    println(tasks[0])
    tasks[1] = "编写 main"
    tasks.add("学习 match", at: 2)

    for (task in tasks) {
        println(task)
    }
}
```

访问下标前要确认范围。负数下标或大于等于 `size` 的下标会触发运行时异常。命令行程序里，不要相信外部输入一定是合法下标。

## 预分配容量

如果大概知道会加入多少元素，可以构造时给容量：

```cj
import std.collection.*

main() {
    let tasks = ArrayList<String>(100)
    tasks.add("第一条任务")
    println(tasks.size)
}
```

这不会自动产生 100 个任务，只是提前准备空间。它适合批量导入数据的场景。

## 用 `HashMap` 按编号查任务

任务本通常需要按编号查找：

```cj
import std.collection.*

main() {
    let tasks = HashMap<Int64, String>()
    tasks.add(1, "安装工具链")
    tasks.add(2, "编写入口")

    if (tasks.contains(2)) {
        println(tasks[2])
    }
}
```

`HashMap<K, V>` 的键类型是 `K`，值类型是 `V`。这里用 `Int64` 做编号，`String` 做标题。查下标前先 `contains`，避免不存在的键导致运行时失败。

## 遍历 `HashMap`

`HashMap` 不保证遍历顺序：

```cj
import std.collection.*

main() {
    let tasks = HashMap<Int64, String>([(1, "安装工具链"), (2, "编写入口")])

    for ((id, title) in tasks) {
        println("${id}: ${title}")
    }
}
```

如果界面需要稳定顺序，保留一个 `ArrayList` 做顺序，或者排序后再展示。不要依赖哈希表的遍历顺序。

## 集合共享同一份数据

集合作为引用语义使用时，多个变量能看到同一个实例的变化：

```cj
import std.collection.*

main() {
    let first = ArrayList<String>(["安装工具链"])
    let second = first

    second.add("编写入口")

    println(first.size)
    println(second.size)
}
```

这适合仓库类对象内部维护同一份任务列表，但也意味着你要小心把可变集合暴露给外部调用者。后面会用 `class` 把可变集合封装起来。

## 选择集合

- 固定长度、只需要按位置修改：用 `Array<T>`。
- 经常添加、删除、按顺序遍历：用 `ArrayList<T>`。
- 只关心元素唯一性：用 `HashSet<T>`。
- 需要从键找到值：用 `HashMap<K, V>`。

任务本会同时用到 `ArrayList` 和 `HashMap`：前者保存展示顺序，后者加速编号查找。

## 检查点

确认你能解释：

- 为什么 `ArrayList<String>` 不能放 `Int64`？
- 为什么访问 `HashMap` 前应该先判断键是否存在？
- 为什么 `HashMap` 遍历顺序不能当作任务顺序？
- 为什么 `let tasks = ArrayList<String>()` 后还可以添加元素？

## 练习

写一个函数 `printTasks(tasks: ArrayList<String>): Unit`，逐行打印任务标题。再写一个 `findTitle(tasks: HashMap<Int64, String>, id: Int64): ?String`，存在时返回标题，不存在时返回 `None`。
