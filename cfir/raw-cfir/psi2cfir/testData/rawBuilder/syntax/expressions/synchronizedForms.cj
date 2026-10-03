// RUN_PIPELINE_TILL: FRONTEND
// 同步：`synchronized(锁) { }` 表达式形式与语句形式、嵌套、锁的 lock / unlock + try-finally。锁类型是 std.sync 的具体互斥量（ReentrantMutex）。
// 官方依据：Cangjie 文档《同步》《sync》；官方 Parse/ParseExpr.cpp SYNCHRONIZED；cjc 1.0.5 / 1.1.3 零诊断。
import std.sync.*

func synchronizedForms(): Int64 {
    let a = ReentrantMutex()
    let b = ReentrantMutex()
    let c = synchronized(a) { 1 }
    let d = synchronized(a) { synchronized(b) { 2 } }
    synchronized(a) {
        c
    }
    a.lock()
    try {
        a.unlock()
    } finally {
        a.unlock()
    }
    return c + d
}
