const func myTarget(x: Int64) {
    match(x) {
        case 0 => x
        case 1 => x
        case 2 => x
        case 3 => x
        case 4 => x
        case _ => Type
    }
}
@Annotation[target:[(MemberVariable, myTarget(2))[0]]]
public class A {
    const init() {
    }
}
<!ANNOTATION_NOT_APPLICABLE_JFFI!>@A<!>
class B {
}
main() {
}
