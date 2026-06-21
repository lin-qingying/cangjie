

class A {
    var a: Int64
    init() {
        if (true) {
            a = 1
        }
        throw Exception()
    }
}

main() {
    try {
        A()
    } catch (e: Exception) {
        return 0
    }
    return 1
}
