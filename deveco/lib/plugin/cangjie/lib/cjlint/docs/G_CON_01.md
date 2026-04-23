G.CON.01 禁止将系统内部使用的锁对象暴露给不可信代码

【级别】要求

【描述】

在仓颉中可以通过 synchronized 关键字和一个 ReentrantMutex 对象对所修饰的代码块进行保护，使得同一时间只允许一个线程执行里面的代码。攻击者可以通过获取该 ReentrantMutex 对象来触发条件竞争与死锁，进而引起拒绝服务（DoS）。

防御这个漏洞一种方法就是使用私有锁对象。

【正例】

```cangjie
import std.sync.*

class SomeObject {
    private let mtx: ReentrantMutex = ReentrantMutex()
    // CODE
    public func put(x: Object) {
        synchronized(mtx) {
            // CODE
        }
    }
}
```

将锁对象设置为 private 类型，攻击者无法无限持有锁。

【反例】

```cangjie
import std.sync.*
import std.time.*

class SomeObject {
    public let mtx: ReentrantMutex = ReentrantMutex()
    ...
    public func put(x: Object) {
        synchronized(mtx) {
            ...
        }
    }
}
//Trusted code
var so = SomeObject()
...
//Untrusted code
func untrusted() {
    synchronized(so.mtx) {
        while (true) {
            sleep(100 * Duration.nanosecond)
        }
    }
}
```

使用 public 修饰锁对象，攻击者可以直接无限持有 mtx 锁，使得其它调用 put 函数的线程被阻塞。

【例外场景】

- 包私有的类可以不受该规则的约束，因为他们无法被包外的非受信代码直接访问。
- 对于非受信代码无法获取执行同步操作的对象的场景下，可以不受该规则的约束。