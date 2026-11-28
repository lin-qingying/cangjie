// MODULE: cycleA
// MAIN_MODULE
// MAIN_FILE_NAME: callerA.cj
// FILE: callerA.cj
package g4cyclea

// 测试在首次 LL 请求之前补入 cycleA -> cycleB，形成普通源码依赖环。
private import g4cycleb.ReceiverB

public func useExtensionFromB(): Int64 {
    ReceiverB().valueFromB()
}

public func useOwnExtensionFromA(): Int64 {
    ReceiverA().valueFromA()
}

// FILE: receiverA.cj
package g4cyclea

public class ReceiverA {
    public init() {}
}

extend ReceiverA {
    public func valueFromA(): Int64 {
        17
    }
}

// MODULE: cycleB(cycleA)
// FILE: callerB.cj
package g4cycleb

private import g4cyclea.ReceiverA

public func useExtensionFromA(): Int64 {
    ReceiverA().valueFromA()
}

// FILE: receiverB.cj
package g4cycleb

public class ReceiverB {
    public init() {}
}

extend ReceiverB {
    public func valueFromB(): Int64 {
        29
    }
}
