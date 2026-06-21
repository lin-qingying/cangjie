

class A {
    public operator func ==(right: B): Bool {
        return true;
    }
}

class B {
    public operator func ==(right: A): Bool {
        return false;
    }
}

main(): Int64 {
    if ((A(), B()) == (B(), A())) {
        return 1;
    }
    return 0;
}
