


main(): Int64 {
    func f() {
        let x = 1
        func g() {
            x
        }
        g
    }
    {
        x: Int64, y: Int64 =>
        let a = x + y
        {=> 2 * a}
    }
    return 0
}
