

class A {
    public operator func ==(right: A) {
        return true;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (x <!INVALID_BINARY_OPERATOR!>!=<!> (A(), 2)) {
        return 0;
    }
    return 1;
}
