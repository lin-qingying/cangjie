// FILE: noTargetDeclaration.cj
package sample.annotations.targets

@Annotation
public class Plain {
    public const init() {}
}

@Plain
class <caret>Holder {
}
