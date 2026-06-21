


class A {
    public operator func ==(right: A): Int64 {
        return 1;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (x <!INVALID_BINARY_OPERATOR!>==<!> (A(), 1)) {
        return 0;
    }
    return 1;
}
