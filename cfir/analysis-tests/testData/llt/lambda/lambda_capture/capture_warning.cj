

main(): Int64 {
    let x = 4
    func f() {
        print("${x},")
        func f1() {
            print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>},")
        }
        let f2 = {
            => print("${<!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>}")
        }
        let x = 99
        f1()
        f2()
    }
    f()
    return 0
}
