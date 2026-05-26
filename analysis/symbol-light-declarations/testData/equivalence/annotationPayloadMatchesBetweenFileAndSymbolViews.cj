package sample.symbol.light.annotations

@Annotation[target: [Type, MemberFunction]]
public class Marker {
    public const init(value!: String, level!: Int64) {}
}

class Document {
    func member(): Int64 {
        return 1
    }
}
