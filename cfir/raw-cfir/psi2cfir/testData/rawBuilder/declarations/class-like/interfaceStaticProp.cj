// RUN_PIPELINE_TILL: FRONTEND
interface WithConst {
    static prop limit: Int64 {
        get() {
            return 1
        }
    }
}
