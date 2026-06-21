


enum MyOption<T> {
    | MyEmpty
    | MyValue(T)
}

extend<T> MyOption<T> <: ToString {
    public func toString() {
        match (this) {
            case MyEmpty => "empty"
            case MyValue(v: ToString) => "MyValue(${v})"
            case MyValue(_) => "cannot print"
        }
    }
}

main(): Int64 {
    let str = MyValue(2).toString()
    if (str != "MyValue(2)") {
        return 1
    }
    return 0
}
