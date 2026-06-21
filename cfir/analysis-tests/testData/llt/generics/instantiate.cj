

public class Buff<E> {
    public var myData2: Array<Option<E>> = [Option<E>.None]
    public var c = match (myData2[0]) {
        case Some(x) => "1"
        case _ => "3"
    }
}

main() {
    var a = Buff<Float32>();
    var b = Buff<Int32>();
    0
}
