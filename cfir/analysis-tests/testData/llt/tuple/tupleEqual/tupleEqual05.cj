

class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

main(): Int64 {
    let x = (A(), 1, A());
    if (x == (A(), 1, A())) {
        return 0;
    }
    return 1;
}
