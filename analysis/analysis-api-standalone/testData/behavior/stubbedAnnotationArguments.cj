// MODULE: main
// MAIN_MODULE
// FILE: stubbedAnnotationArguments.cj
package sample.standalone.behavior

@Annotation
public class Payload {
    public const init(shape!: (Int64, String), label!: String, level!: Int64) {}
}

@Payload[(1, "hello"), "demo", 2i64]
class Main {
}
