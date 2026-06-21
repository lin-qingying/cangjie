

main() {
    let a = Sub<Rune>()
    let b = g(a, r'a')
    if (b is Sub<Rune>) {
        return 0
    }
    return 1
}

open class Base<T> {
    protected open func f(): This {
        this
    }
}

class Sub<T> <: Base<T> {}

func g<T, K>(a: T, b: K) where T <: Base<K> {
    a.f()
}
