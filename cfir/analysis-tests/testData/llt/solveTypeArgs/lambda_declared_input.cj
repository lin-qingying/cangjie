open class Base {}
class Leaf <: Base {
    public func leafOnly(): Unit {}
}
func acceptLeaf(operation: (Leaf) -> Unit): Unit {}

func uses(): Unit {
    acceptLeaf({value: Base => value.<!NOT_MEMBER_OF!>leafOnly<!>()})
    acceptLeaf({value => value.leafOnly()})
}
