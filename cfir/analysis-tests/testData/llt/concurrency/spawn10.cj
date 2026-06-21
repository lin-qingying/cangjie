


package ohos.base

internal import std.sync.*
internal import std.time.*

foreign func pthread_self(): Int64

foreign func CJ_MRT_RuntimeNewSubScheduler(): CPointer<Unit>

foreign func CJ_MRT_StopSubScheduler(schedule: CPointer<Unit>): Int8

foreign func CJ_ScheduleIsRunning(schedule: CPointer<Unit>): Bool

class MainThreadContext <: ThreadContext {
    private var schedule_: ?CPointer<Unit> = None
    private var hasEnded_: Bool = false

    private func getSchedulerHandle(): CPointer<Unit> {
        schedule_.getOrThrow()
    }

    public init() {
        let schedule = unsafe { CJ_MRT_RuntimeNewSubScheduler() }
        schedule_ = schedule
        if (schedule.isNull()) {
            throw Exception("Fail to create schedule for python thread context")
        }
        // Wait for sub-scheduler running
        while (unsafe { !CJ_ScheduleIsRunning(schedule) }) {}
    }

    public func end(): Unit {
        hasEnded_ = true
        unsafe { CJ_MRT_StopSubScheduler(schedule_.getOrThrow()) }
    }

    public func hasEnded(): Bool {
        hasEnded_
    }
}

main(): Int64 {
    let tid = unsafe { pthread_self() }
    println("current thread id ${tid}")

    let ctx0 = MainThreadContext()
    let ctx1 = MainThreadContext()
    let tid0 = spawn (ctx0) {
        unsafe { pthread_self() }
    }.get()

    let tid1 = spawn (ctx1) {
        unsafe { pthread_self() }
    }.get()

    let tid2 = spawn (ctx0) {
        unsafe { pthread_self() }
    }.get()

    let tid3 = spawn (ctx1) {
        unsafe { pthread_self() }
    }.get()

    if (tid0 != tid2 || tid1 != tid3) {
        println("failed")
        return 1
    }

    ctx0.end()
    ctx1.end()
    0
}
