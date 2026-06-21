

interface I {}

class A <: I {}

func f(): Any {
    return A()
}

main(): Int64 {
    let i: I = (f() as I).getOrThrow()
    let v = i as A
    if (let Some(x) <- v) {
        return 0
    } else {
        return 1
    }
}
