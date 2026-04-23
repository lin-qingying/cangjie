G.CON.02 在异常可能出现的情况下，保证释放已持有的锁

【级别】要求

【描述】

一个线程中没有正确释放持有的锁会使其他线程无法获取该锁对象，导致阻塞。在发生异常时，要确保程序正确释放当前持有的锁。

> **注意：**
>  
> 在发生异常时，通过 synchronized 进行同步的代码块的锁会被自动释放，但是通过 mtx.lock() 获得的锁不会被自动释放，需要开发者手动释放。

【正例】

```cangjie
import std.sync.*

class Foo {
    private let mtx: ReentrantMutex = ReentrantMutex()

    public func doSomething(a: Int64, b: Int64) {
        var c: Int64
        try {
            mtx.lock()
            // CODE
            c = a / b
        } catch (e: ArithmeticException) {
            // Handle exception
            // CODE
        } finally {
            mtx.unlock()
            // CODE
        }
    }
}
```

上述正确示例中，成功执行锁定操作后，将可能抛出异常的操作封装在 try 代码块中。锁在执行可能发生异常的代码块前获取，可保证在执行 finally 代码时正确持有锁。在 finally 代码块中调用 mtx.unlock()，可以保证不管是否发生异常都可以释放锁。

【反例】

```cangjie
import std.sync.*

class Foo {
    private let mtx: ReentrantMutex = ReentrantMutex()

    public func doSomething(a: Int64, b: Int64) {
        var c: Int64
        try {
            mtx.lock()
            // CODE
            c = a / b
            mtx.unlock()
        } catch (e: ArithmeticException) {
            // Handle exception
            // CODE
        } finally {
            // CODE
        }
    }
}
```

上述错误示例中，使用 ReentrantMutex 锁，发生算数运算错误时，catch 及 finally 代码块中没有释放锁操作，导致锁没有释放。