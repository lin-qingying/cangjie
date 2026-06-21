


const func f(x: Int64) {
    try {
        x
    } catch (_) {
        x + 1
    } finally {
        x - 1
    }
}

const x = f(0)

main() {
    x
}
