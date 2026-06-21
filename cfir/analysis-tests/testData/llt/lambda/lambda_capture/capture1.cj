

main(): Int64 {
    let x = 4
    func f() {
        print("${x},")
        let x = 99
        func f1() {
            print("${x},")
        }
        let f2 = {
            => print("${x}")
        }
        f1()
        f2()
    }
    f()
    return 0
}
