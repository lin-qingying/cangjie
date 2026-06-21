

class A {}

class B<T> {}

class C {
    public var list: B<A> = B<A>()
    public func onInit() {
        let data = [A(), A()]
        this.list = <!TYPE_MISMATCH!>data<!>
    }
}
