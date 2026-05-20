// FILE: metaAnnotation.cj
package sample.annotations

@Annotation
public class Meta {
    public const init() {}
}

@Meta
@Annotation
public class Marker {
    public const init() {}
}

@Marker
class <caret>User {
}
