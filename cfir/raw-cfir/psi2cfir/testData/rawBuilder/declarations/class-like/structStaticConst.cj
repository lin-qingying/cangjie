// RUN_PIPELINE_TILL: FRONTEND
struct S {
    public static const k: Int64 = 2

    static func get(): Int64 {
        return k
    }
}
