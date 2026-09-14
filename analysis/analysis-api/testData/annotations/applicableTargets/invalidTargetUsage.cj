// FILE: invalidTargetUsage.cj
package sample.annotations.targets

@Annotation[target: [GlobalFunction]]
public class GlobalOnly {
    public const init() {}
}

@GlobalOnly
class <caret>Misplaced {
}
