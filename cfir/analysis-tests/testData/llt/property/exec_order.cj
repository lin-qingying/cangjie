


class A {
    mut prop m: Int64 {
        get() {
            println("a=${a}")
            a
        }
        set(value) {
            println("a=${a} value=${value}")
            a = value
        }
    }
    var a = 1

    mut static prop stm: Int64 {
        get() {
            println("sta=${sta}")
            sta
        }
        set(value) {
            println("sta=${sta} value=${value}")
            sta = value
        }
    }
    static var sta = 3
}

var a = A()

func geta(n: String) {
    println(n)
    a
}

var b = VArray<Int64, $3>({i => i})

func getb(n: String) {
    println(n)
    b
}

main() {
    geta("left").m = geta("right").m + 1
    geta("left").m += geta("right").m
    A.stm = A.stm + 2
    A.stm += 2
}
