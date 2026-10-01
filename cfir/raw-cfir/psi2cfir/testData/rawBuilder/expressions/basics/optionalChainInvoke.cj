// RUN_PIPELINE_TILL: FRONTEND
class A {
    func f(): Int64 {
        return 1
    }

    prop p: Int64 {
        get() {
            return 1
        }
    }
}

func useOptionalChain(a: ?A): Unit {
    let x = a?.f()
    let y = a?.p
    let _ = x
    let _ = y
}
