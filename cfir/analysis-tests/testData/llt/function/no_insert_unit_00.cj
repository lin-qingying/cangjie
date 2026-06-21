

func f(x: Int64) {
    return {
        =>
        let t = x - 1
        println(t)
        ()
    }
}

main() {
    f(1)()
}
