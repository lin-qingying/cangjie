

internal import std.collection.*

extend<K, X> HashMap<K, X> where K <: Hashable & Equatable<K> {
    func foo() {
        var key: K = HashMap<String, K>().get("123").getOrThrow()
    }
    func test(a: K) {
        let v = HashMap<Rune, K>()
        let r = v.add(r'a', a)
        let get = v.get(r'a')
        if (r != get) {
            return get.getOrThrow()
        }
        throw Exception()
    }
}

main() {
    let a = HashMap<Int8, Rune>()
    var cnt = 0
    try {
        a.foo()
    } catch (_) {
        cnt++
    }
    try {
        cnt += Int64(a.test(2))
    } catch (_) {
        println("error")
    }
    if (cnt != 3) {
        return 1
    }
    return 0
}
