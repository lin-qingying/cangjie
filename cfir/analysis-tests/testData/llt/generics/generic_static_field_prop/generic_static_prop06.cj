

class A {}

class B<T> where T <: A {
    B(var b: T) {
    }
}

type C<T> = B<T>

type I = II<C<A>>

interface II<T> {
    static func foos<T>(a: T) {
        return a
    }

    static prop pas: C<A> {
        get() {
            C<A>(A())
        }
    }
}

extend<T> C<T> <: I where T <: A {
    func foo<T>(a: T) {
        return a
    }

    prop pa: C<A> {
        get() {
            C<A>(A())
        }
    }
}

main() {
    B<A>.pas
    B<A>(A()).pa
    return B<A>.foos(1) + B<A>(A()).foo(-1)
}
