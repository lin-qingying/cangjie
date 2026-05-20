// FILE: classAnnotations.cj
package sample.annotations

@Annotation
public class First {
    public const init() {}
}

@Annotation
public class Second {
    public const init(level!: Int64) {}
}

@First
@Second[2]
class <caret>User {
}
