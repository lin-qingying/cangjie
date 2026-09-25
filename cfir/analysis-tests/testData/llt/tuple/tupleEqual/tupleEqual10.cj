



class A {
    public operator func ==(right: A): Int64 {
        return 1;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (<!TUPLE_ELEMENT_CMP_NOT_BOOL!>x == (A(), 1)<!>) {
        return 0;
    }
    return 1;
}

