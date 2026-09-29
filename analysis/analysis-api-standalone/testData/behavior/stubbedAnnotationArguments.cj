// MODULE: main
// MAIN_MODULE
// FILE: stubbedAnnotationArguments.cj
package sample.standalone.behavior

@Annotation
public class Payload {
    public const init(shape!: (Int64, String), label!: String, level!: Int64) {}
}

@Payload[shape: (1, "hello"), label: "demo", level: 2i64]
class Main {
}
