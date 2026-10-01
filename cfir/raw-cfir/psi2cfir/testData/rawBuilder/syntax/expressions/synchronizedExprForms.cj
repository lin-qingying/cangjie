// RUN_PIPELINE_TILL: FRONTEND
// `synchronized` 作为表达式返回值、作为语句块；锁为 std.sync 的互斥量。
// 官方依据：Cangjie 文档《同步》；cjc 1.0.5 / 1.1.3 零诊断。
import std.sync.*

func synchronizedExprForms(): Int64 {
    let m = ReentrantMutex()
    let a = synchronized(m) { 1 }
    let b = synchronized(m) { synchronized(m) { 2 } }
    synchronized(m) {
        a
    }
    m.lock()
    try {
        m.unlock()
    } finally {
        m.unlock()
    }
    return a + b
}
