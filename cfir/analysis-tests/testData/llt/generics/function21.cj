


func foo<T0>(_: (() -> UInt8) -> Int64) {}

func goo<T0>(_: () -> UInt8): Int64 {
    1
}

public func bar(_: Range<Int32>): Int64 {
    goo<Unit>(
        {
            =>
            foo<UInt16>(goo<Unit>)
            2
        }
    )
}

main() {}
