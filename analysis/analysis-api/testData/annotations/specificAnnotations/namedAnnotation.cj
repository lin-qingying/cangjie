// FILE: namedAnnotation.cj
// CLASS_ID: sample.annotations/Named
package sample.annotations

@Annotation
public class Named {
    public const init(value!: String, level!: Int64) {}
}

@Named["demo", level: 2]
class <caret>User {
}
