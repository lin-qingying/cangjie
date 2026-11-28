package completion.applicability

class Holder<T> {}

extend<T> Holder<T> {
    func first(): T {
        throw Exception()
    }
}

func use(holder: Holder<Int64>): Int64 {
    holder.first()
}
