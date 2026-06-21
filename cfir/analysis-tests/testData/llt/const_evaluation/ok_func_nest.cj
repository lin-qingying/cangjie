


// f(x) = 2 * x + 1
const func f(x: Int64) {
    const func g(x: Int64) {
        2 * x
    }
    let double = g(x)
    1 - (-double)
}

const x = f(3)

main() {
    x
}
