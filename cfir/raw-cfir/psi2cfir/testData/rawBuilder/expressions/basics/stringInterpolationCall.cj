// RUN_PIPELINE_TILL: FRONTEND
class A {
    func name(): String {
        return "a"
    }
}

func greet(a: A, x: Int64): String {
    return "v=${a.name()} w=${x * 2}"
}
