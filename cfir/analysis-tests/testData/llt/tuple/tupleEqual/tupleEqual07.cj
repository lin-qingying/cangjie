

interface Eq<T> {
    operator func ==(right: T): Bool
}

class A <: Eq<A> {
    public operator func ==(right: A): Bool {
        return true;
    }
}

main(): Int64 {
    let x = (A(), 1, A());
    if (x == (A(), 2, A())) {
        return 2;
    }
    if (x == (A(), 1, A())) {
        return 0;
    }
    return 1;
}
