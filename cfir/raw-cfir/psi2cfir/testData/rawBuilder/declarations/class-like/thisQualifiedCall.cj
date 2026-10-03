// RUN_PIPELINE_TILL: FRONTEND
class A {
    func m(): Int64 {
        return 1
    }

    func n(): Int64 {
        return this.m()
    }
}
