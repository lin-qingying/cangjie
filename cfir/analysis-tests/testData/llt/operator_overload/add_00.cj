

extend Int32 {
    func b() {
        1
    }
    operator func +(x: Unit) {
        2
    }
}

main() {
    let x = 1.b()
    let y = 1 + ()
    println(x)
    println(y)
}
