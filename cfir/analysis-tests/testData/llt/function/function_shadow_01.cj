

class C {}

struct S {
    func C(): String {
        "ok"
    }
    func test() {
        let str: String = C()
        if (str == "ok") {
            return 0
        }
        return 1
    }
}

main() {
    let s = S()
    return s.test()
}
