package receiver_nested_this_invalid
public class Box {
    public let value: Int64
    public init() {
        func readCurrent(): Int64 { this.value }
        value = 1
    }
}
