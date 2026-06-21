

class A {
    public operator func [](arr: Array<Array<Int64>>) {
        return arr.size - 1
    }
}

main() {
    <!UNRESOLVED_REFERENCE!>retur<!> * A()[[1]]
}
