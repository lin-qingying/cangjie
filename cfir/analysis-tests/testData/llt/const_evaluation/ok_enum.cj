


enum E {
    A | B(Int64)

    const func zero() {
        0
    }
}

const a = A
const b = B(0)

const func f(x: E) {
    match (x) {
        case A => x.zero()
        case B(t) => t
    }
}

const x = f(a) + f(b)

main() {
    x
}
