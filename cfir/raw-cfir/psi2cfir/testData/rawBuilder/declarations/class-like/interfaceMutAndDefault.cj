// RUN_PIPELINE_TILL: FRONTEND
interface I {
    mut func go(): Unit

    func name(): String {
        return "i"
    }
}
