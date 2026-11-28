package wire.constexpr

public const scale: Int64 = 10 ** 3
public const shifted: Int64 = (scale << 1) | 3
public const negative: Int64 = -7
public const opposite: Int64 = -scale
public const flipped: Bool = !false

public const func add(left: Int64, right: Int64): Int64 {
    left + right
}

public class Cell {
    public let value: Int64
    public const init(value: Int64) {
        this.value = value
    }
}

public const built: Cell = Cell(add(scale, 2))
public const memberValue: Int64 = built.value
public const comparison: Bool = memberValue == 1002
public const empty: String = String()
public const chosen: Int64 = if (true) { add(3, 4) } else { 9 }
public const tupleValue: (Int64, Bool) = (scale, true)
public const zeroArray: VArray<UInt8, $0> = []

public enum Selection {
    Ready(Int64) | Empty
}

public const selected: Selection = Ready(scale)

public class Arena<T> {
    public let counts = Array<UInt8>(3, { _: Int64 => UInt8(0) })
    public init() {}
}
