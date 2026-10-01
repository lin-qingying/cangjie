// RUN_PIPELINE_TILL: FRONTEND
class WithFinalizer {
    var v: Int64 = 0

    ~init() {
        this.v = 0
    }
}
