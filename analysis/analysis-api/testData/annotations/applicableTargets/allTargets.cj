// FILE: allTargets.cj
package sample.annotations.targets

@Annotation[target: [Type, Parameter, Init, MemberProperty, MemberFunction, MemberVariable, EnumConstructor, GlobalFunction, GlobalVariable, Extension]]
public class Anywhere {
    public const init() {}
}

@Anywhere
class <caret>Wide {
}
