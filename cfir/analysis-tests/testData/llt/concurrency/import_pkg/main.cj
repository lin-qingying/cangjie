// FILE: pkg.cj


package ohos.base

public class MainThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
    protected func getSchedulerHandle(): CPointer<Unit> {
        CPointer<Unit>()
    } // inner implement
}

public let Main = MainThreadContext()

// FILE: main.cj



internal import ohos.base.*

main() {
    let fut0 = spawn (MainThreadContext()) {
        println("hello world")
    }
    let fut1 = spawn (Main) {
        println("hello world")
    }
    return 0
}
