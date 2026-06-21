

main(): Int64 {
    let x = 4
    func f() {
        print("${x},")
        func f1() {
            print("${x},")
        }
        let f2 = {
            => print("${x}")
        }
        let x = 99
        f1()
        f2()
    }
    f()
    return 0
}
