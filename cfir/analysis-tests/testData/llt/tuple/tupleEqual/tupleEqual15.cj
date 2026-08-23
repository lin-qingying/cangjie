


class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

class B {}

main(): Int64 {
    let x = (A(), B());
    if (x == (A(), B())) {
        return 0;
    }
    return 1;
}

