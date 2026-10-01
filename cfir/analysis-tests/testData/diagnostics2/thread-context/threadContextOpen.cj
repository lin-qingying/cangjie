// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   `open class MyContext <: ThreadContext {...}` → error: 'MyContext' cannot be modified with 'open' when inherit, implement or extend 'ThreadContext'（锚整条类声明）
//   `class PlainContext <: ThreadContext {...}`  → error: user defined decl 'PlainContext' not support to inherit, implement or extend 'ThreadContext'（锚整条类声明）
// CFIR CfirInheritanceThreadContextChecker 两条诊断都 reportOn(declaration.source)，锚点一致。
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
