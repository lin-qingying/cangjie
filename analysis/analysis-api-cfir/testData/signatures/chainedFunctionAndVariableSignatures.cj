package completion.signatures

class User {}
class Other {}

func identity<T, U>(value: T, witness: U): T {
    return value
}

class Holder<T, U> {
    let field: T
    let witness: U

    init(value: T, marker: U) {
        field = value
        witness = marker
    }

    prop item: T {
        get() { field }
    }

    func reflect(value: T): T {
        let local: T = value
        return local
    }
}
