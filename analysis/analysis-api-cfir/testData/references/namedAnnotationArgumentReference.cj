package sample

@Annotation
public class Marked {
    const init(repeat!: Int64) {}
}

@!Marked[repeat: 0]
func annotated() {}
