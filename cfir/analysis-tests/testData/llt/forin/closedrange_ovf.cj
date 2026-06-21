


class Counter {
    var c = 0
    func count(): Unit {
        c++
        if (c >= 100) {
            throw Exception("count exceeded")
        }
    }
}

func range2e(bottom: Int64, top: Int64): Unit {
    let c = Counter()
    for (_ in bottom..=top : 2) {
        c.count()
    }
}

func test(fun: (Int64, Int64) -> Unit, bottom: Int64, top: Int64) {
    try {
        fun(bottom, top)
        println(": ok")
    } catch (e: Exception) {
        println("${e}")
    }
}

main() {
    let max = 0x7FFFFFFFFFFFFFFF
    test(range2e, max - 1, max)
}
