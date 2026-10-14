// RUN_PIPELINE_TILL: FRONTEND
// 官方 cjc 1.0.0 / 1.1.3 的 MainHint 都锚完整类声明。CFIR 也报告整个类声明，
// 并从声明的首个语法 token 开始，排除前置 pipeline 指令、注释与空白。
<!INHERIT_THREAD_CONTEXT_NOT_OPEN!>open class MyContext <: ThreadContext {
    public func end(): Unit {}

    public func hasEnded(): Bool {
        return false
    }
}<!>

<!INHERIT_THREAD_CONTEXT_INVALID!>class PlainContext <: ThreadContext {
    public func end(): Unit {}

    public func hasEnded(): Bool {
        return false
    }
}<!>
