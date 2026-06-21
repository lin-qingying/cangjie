

class A {}

interface I {}

extend A <: I {}

func f(xxx: I) {
    xxx
}

main() {
    let l = {aaa: I => aaa}
    if (!(l(A()) is A)) {
        return 1
    }
    if (!(f(A()) is A)) {
        return 2
    }
    return 0
}
