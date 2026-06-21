

open class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

class B <: A {}

main(): Int64 {
    let x = (A(), 1, B());
    if (x == (B(), 2, A())) {
        return 2;
    }
    if (x == (B(), 1, A())) {
        return 0;
    }
    return 1;
}
