

class A {
    public operator func ==(right: B): Bool {
        return true;
    }
}

class B {}

main(): Int64 {
    let x = (A(), 1);
    if (x == (B(), 1)) {
        return 0;
    }
    return 1;
}
