# 并发任务

## 目标

这一课把耗时任务放到仓颉线程中执行。你会使用 `spawn` 创建线程，用 `Future<T>` 等待结果，用 `Mutex` 保护共享状态。

## 创建线程

`spawn` 接收无参数 Lambda：

```cj
main(): Int64 {
    spawn { =>
        println("后台任务开始")
        println("后台任务结束")
    }

    println("主线程")
    return 0
}
```

主线程结束时，新线程可能还没执行完。需要结果或完成保证时，保存 `spawn` 返回的 `Future`。

## 等待完成

```cj
import std.sync.*

main(): Int64 {
    let future: Future<Unit> = spawn { =>
        println("加载任务")
    }

    println("等待后台任务")
    future.get()
    println("后台任务已完成")
    return 0
}
```

`get()` 会阻塞当前线程直到对应线程结束。如果后台线程抛出异常，`get()` 会把异常继续抛出到等待方。

## 获取结果

```cj
import std.sync.*

main(): Int64 {
    let future: Future<Int64> = spawn {
        return 42
    }

    let count = future.get()
    println("任务数量：${count}")
    return 0
}
```

`Future<T>` 的 `T` 来自 Lambda 返回类型。返回 `Int64`，就得到 `Future<Int64>`；只做打印，通常是 `Future<Unit>`。

## 多个任务一起等待

```cj
import std.collection.*
import std.sync.*

main(): Int64 {
    let futures = ArrayList<Future<Unit>>()

    for (i in 0..10) {
        let future = spawn { =>
            println("处理任务 ${i}")
        }
        futures.add(future)
    }

    for (future in futures) {
        future.get()
    }

    return 0
}
```

先创建所有线程，再统一等待，可以让它们并发推进。不要在创建后立即 `get()`，否则会变成一个接一个执行。

## 保护共享状态

多个线程修改同一个变量时，需要锁：

```cj
import std.collection.*
import std.sync.*

var completed: Int64 = 0
let mutex = Mutex()

main(): Int64 {
    let futures = ArrayList<Future<Unit>>()

    for (i in 0..100) {
        futures.add(spawn { =>
            mutex.lock()
            completed += 1
            mutex.unlock()
        })
    }

    for (future in futures) {
        future.get()
    }

    println("完成数量：${completed}")
    return 0
}
```

锁保护的是临界区：从读取共享变量到写回共享变量的整段代码。只锁一半没有意义。

## 解锁必须配对

`Mutex` 是可重入锁，同一个线程可以重复获得同一把锁。但调用 `lock()` 的次数必须和 `unlock()` 配对。忘记解锁会让其他线程一直等待；没持有锁却调用 `unlock()` 会抛出异常。

更稳的写法是把锁包进小函数，减少手写配对次数：

```cj
func incrementCompleted(): Unit {
    mutex.lock()
    completed += 1
    mutex.unlock()
}
```

如果临界区中可能抛异常，就要用 `try` 和 `finally` 保证释放锁。不要把复杂逻辑塞进持锁区。

## 检查点

确认你能解释：

- 为什么只写 `spawn` 不一定能看到后台线程完成？
- `Future<T>` 的 `T` 从哪里来？
- 为什么创建线程后立即 `get()` 会削弱并发？
- `Mutex` 的 `lock()` 和 `unlock()` 为什么必须配对？

## 练习

把任务本的“批量标记完成”设计成多个后台任务：每个线程处理一个编号，主线程等待全部 `Future` 完成后打印统计。
