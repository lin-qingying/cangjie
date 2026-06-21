

open class A {}

class B <: A {
    public operator func ==(right: B): Bool {
        return true;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (x <!INVALID_BINARY_OPERATOR!>==<!> (B(), 1)) {
        return 0;
    }
    return 1;
}
