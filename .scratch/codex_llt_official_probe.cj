enum E1 {
    EE | ER(Int32)

    public prop a: Int64 {
        get() {
            0
        }
    }

    public prop c: Int64 {
        get() {}
        set(v) {}
    }
}

func foo1(): (Float32, String) {}

func foo2(): (Float64, String) {}

func forinProbe() {
    let arr2: Array<Int32> = [1, 2]
    for (a in arr2 where 10) {}
}

main(): Int64 {
    return 0
}
