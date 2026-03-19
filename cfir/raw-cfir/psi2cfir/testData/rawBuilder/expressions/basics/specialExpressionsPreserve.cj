// RUN_PIPELINE_TILL: FRONTEND
func specialExpressionsPreserve(lock: Int64): Unit {
    synchronized(lock) {
        let a = 1
    }

    unsafe {
        let b = 2
    }

    let q = quote(hello $(lock))
    let m = @demo(foo)
}

