// FILE: explicitTarget.cj
package sample.annotations.targets

@Annotation[target: [MemberFunction]]
public class FunctionScoped {
    public const init() {}
}

@FunctionScoped
class <caret>Provider {
    public func provide(): Int64 {
        return 1
    }
}
