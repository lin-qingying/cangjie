


struct Boo {
    public mut prop aaa: Int32 {
        get() {
            return aaa
        }
        set(input) {
            aaa = 250
        }
    }
    public prop bbb: Unit {
        get() {
            aaa = 1
        }
    }
}

main(): Int32 {
    var b = Boo()
    let t = b.bbb
    return b.aaa
}
