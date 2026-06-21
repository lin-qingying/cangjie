


public class B<T> {}

extend<R> B<R> {
    public func call() {
        this
    }
}

public abstract class A<U> {
    public func getB() {
        return B<Int>()
    }
}

public class C <: A<Any> {
    public C() {
        getB().call().call()
    }
}

main() {
}
