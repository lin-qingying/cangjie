

struct Day {
    public let width: Int32 = 1
    public let length: Int32 = 2
    public let perimeter2: (Unit) -> Int32 = <!TYPE_MISMATCH!>{=> var res: Int32 = 0}<!>
}
