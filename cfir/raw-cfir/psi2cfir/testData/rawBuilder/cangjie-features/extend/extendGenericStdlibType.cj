// RUN_PIPELINE_TILL: FRONTEND
extend<T> Array<T> where T <: ToString {
    func dumpAll(): Unit {
        for (v in this) {
            let _ = v.toString()
        }
    }
}
