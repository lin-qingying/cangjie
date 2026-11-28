

func foo1(): (Float32, String) <!MISMATCHED_TYPES_BECAUSE!>{}<!>

func foo2(): (Float64, String) <!MISMATCHED_TYPES_BECAUSE!>{}<!>

main() {}
