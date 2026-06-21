

package test.pkg0

public const func foo<T0>(): UInt32 {
    const func my<T0, T1>(param1: T1, param2!: Int32 = 49): UInt32 {
        196
    }
    my<Bool, Rune>(r'O')
    0
}

public const func bar(): Unit {
    foo<UInt32>()
    ()
}
