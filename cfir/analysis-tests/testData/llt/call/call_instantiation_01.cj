

class A<K, V> <: Collection<(K, V)> {
    public func isEmpty(): Bool {
        true
    }
    public func iterator(): Iterator<(K, V)> {
        return AIter<(K, V)>()
    }
    public prop size: Int64 {
        get() {
            0
        }
    }
}

class AIter<T> <: Iterator<T> {
    public func next(): Option<T> {
        return None
    }
}

main() {
    let a: A<String, Int64> = A()
    let arr0 = a.toArray()
    let arr1: Array<(String, Int64)> = a.toArray()
    if (arr1.size != 0) {
        return 1
    }
    return 0
}
