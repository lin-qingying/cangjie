// spawn / synchronized / unsafe 表达式：线程创建、互斥同步块与不安全块。
// 官方依据：Cangjie 文档《并发》《同步》《unsafe》；cjc 1.0.5 / 1.1.3 零诊断。
import std.sync.*

class Account {
    private var balance: Int64 = 0
    private let lock: ReentrantMutex = ReentrantMutex()

    public init() {}

    public func deposit(amount: Int64): Unit {
        synchronized(lock) {
            balance += amount
        }
    }

    public func transfer(target: Account, amount: Int64): Unit {
        synchronized(lock) {
            balance -= amount
            target.balance += amount
        }
    }
}

func spawnWorker(): Int64 {
    let captured: Int64 = 1
    let handle = spawn {
        => captured
    }
    let _ = handle
    return captured
}

func unsafeBlock(): Int64 {
    return unsafe {
        let p = 0
        p
    }
}
