


package ohos.base

class MainThreadContext <: ThreadContext {
    public func end(): Unit {}
    public func hasEnded(): Bool {
        false
    }
    func getSchedulerHandle(): CPointer<Unit> {
        CPointer<Unit>()
    } // inner implement
}

type NewName = MainThreadContext

main(): Int64 {
    let fut = spawn (MainThreadContext()) {
        println("hello world spawn9")
    }

    let fut1: Future<Unit> = spawn (MainThreadContext()) {
        println("hello world spawn9")
    }

    let fut2: Future<Unit> = spawn (NewName()) {
        println("hello world spawn9")
    }
    return 0
}
