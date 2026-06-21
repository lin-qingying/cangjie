

class A {
    var x = 0
}

func g(x: Int64) {
    x
}

func h(x: Rune) {
    A()
}

main() {
    g ~> {
        x: Int64 => h(match (123) {
            case xxx => r'n'
        }).x += 1
    }
    ()
}
