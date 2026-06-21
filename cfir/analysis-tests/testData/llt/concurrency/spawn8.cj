


<!INHERIT_THREAD_CONTEXT_INVALID!>class MyCustomThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
}<!>

<!INHERIT_THREAD_CONTEXT_INVALID!>interface MyInterfaceThreadContext <: ThreadContext {
    func custom(): Unit
}<!>

<!INHERIT_THREAD_CONTEXT_INVALID!>class MyExtendThreadContext {}<!>

extend MyExtendThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
}

main(): Int64 {
    let fut = spawn (<!SPAWN_ARG_INVALID!>MyCustomThreadContext()<!>) {}
    return 0
}
