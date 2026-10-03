// RUN_PIPELINE_TILL: FRONTEND
class A {
    var base: Int64 = 1

    prop derived: Int64 {
        get() {
            return this.base * 2
        }
    }
}
