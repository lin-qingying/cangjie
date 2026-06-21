

main(): Int64 {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<Int32><!>()
    return 0
}

class B<T> {
    public operator func +(x: Int32) {
        return;
    }
    public operator func +(x: T) {
        return 2
    }
}
