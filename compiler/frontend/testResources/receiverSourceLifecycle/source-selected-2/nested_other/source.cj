package receiver_nested_other
public class Box {
    public let value: Int64
    @Frozen
    public init(n: Int64) { value = n }
    @Frozen
    public init(other: Box) {
        func readOther(): Int64 { other.value }
        value = readOther()
    }
}
