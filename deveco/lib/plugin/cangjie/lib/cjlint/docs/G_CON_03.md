G.CON.03 禁止使用非线程安全的函数来覆写线程安全的函数

【级别】要求

【描述】

使用非线程安全的函数覆写基类的线程安全函数，可能会导致不恰当的同步。比如，子类将基类的线程安全的函数覆写为非安全函数，这样就违背了覆写同步函数的要求。这样很容易导致难以定位的问题的产生。

被设计为可继承的类，这些类对应的锁策略必须要详细记录说明。方便子类继承时，沿用正确的锁策略。

【正例】

```cangjie
import std.sync.*

open class Base {
    private let baseMtx: ReentrantMutex = ReentrantMutex()
    public open func doSomething() {
        synchronized(baseMtx) {
            // CODE
        }
    }
}

class Derived <: Base {
    private let mtx: ReentrantMutex = ReentrantMutex()
    public override func doSomething() {
         synchronized(mtx) {
             // CODE
         }
    }
}
```

上述正确示例中，通过使用一个私有的锁对象来同步的函数覆写 Base 类中的同步函数 doSomething()，确保了 Derived 类是线程安全的。

另外，上面示例中，子类与基类的 doSomething() 函数使用的是不同的锁，实际编码过程中，要考虑是否会产生影响。在设计过程中，要尽量避免类似的继承导致的同步问题。

【反例】

```cangjie
import std.sync.*

open class Base {
    private let baseMtx: ReentrantMutex = ReentrantMutex()
    public open func doSomething() {
        synchronized(baseMtx) {
            // CODE
        }
    }
}

class Derived <: Base {
    public override func doSomething() {
         // CODE
    }
}
```

上述错误示例中，子类 Derived 覆写了基类 Base 的同步函数 doSomething() 为非线程同步函数。Base 类的 doSomething() 函数可被多线程正确使用，但 Derived 类不可以。因为接受 Base 实例的线程同时也可以接受其子类，所以可能会导致难以诊断的程序错误。