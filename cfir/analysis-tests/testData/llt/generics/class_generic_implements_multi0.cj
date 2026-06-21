

interface A<T> {
    func test(a: T): T {
        return a
    }
}

interface B<T> <: A<T> {}

class C<T> <: B<T> {}

main(): Int64 {
    var ans1 = C<Int32>()
    if (ans1.test(1) == 1) {
        return 0;
    }
    return 1
}
