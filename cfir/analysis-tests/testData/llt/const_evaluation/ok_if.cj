


const func f(x: Int64) {
    if (x < 0) {
        "negative"
    } else if (x == 0) {
        "zero"
    } else {
        return "positive"
    }
}

const x = f(0)

main() {
    println(x)
}
