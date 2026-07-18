

func foo1(): (Float32, String) <!RETURN_TYPE_MISMATCH!>{}<!>

func foo2(): (Float64, String) <!RETURN_TYPE_MISMATCH!>{}<!>

main() {}
