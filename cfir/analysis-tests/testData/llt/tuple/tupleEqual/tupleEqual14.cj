

open class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

class B <: A {}

main(): Int64 {
    let x = (A(), (A(), B(), (B(), A())));
    if (x == (B(), (B(), A(), (A(), B())))) {
        return 0;
    }
    return 1;
}
