

class A {}

extend A {
    public operator func ==(right: A) {
        return true;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (x == (A(), 1)) {
        return 0;
    }
    return 1;
}
