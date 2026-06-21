

package pkga

private const func foo() {
    return MemberVariable
}

private const func bar<T>(a: T) {
    return Type
}

private const b = Init

@Annotation[target: [foo(), bar(1), b]]
public class B {
    public const init() {}
}
