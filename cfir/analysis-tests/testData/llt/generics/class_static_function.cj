

class ObservableCreate<T> {}

class Observable<T> {
    public static func create<U>(a: U): ObservableCreate<U> {
        return ObservableCreate<U>()
    }
}

main() {
    let obj = Observable<Bool>.create<Int64>(1)
    return 0
}
