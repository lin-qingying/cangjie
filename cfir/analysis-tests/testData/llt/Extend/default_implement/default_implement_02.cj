

interface I {
    operator func [](index: Int64) {
        return this
    }
}

open class empty <: I {} // empty obj define. To replace big obj for gc free.

struct A0 <: I {}

func create_obj(a: Int64): I {
    match (a) {
        case 0 => A0()
        case _ => empty()
    }
}

main() {
    let ins = create_obj(0)[0];
    if (ins is A0) {
        return 0
    }
    return 1
}
