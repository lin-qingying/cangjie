

func foo1(): (Float32, String) <!TYPE_MISMATCH!>{}<!>

func foo2(): (Float64, String) <!TYPE_MISMATCH!>{}<!>

main() {}
