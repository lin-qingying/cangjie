

@Annotation[target: [GlobalFunction]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[11]<!>
var (a, b) = (3, "")

<!ANNOTATION_NOT_APPLICABLE_JFFI!>@APILevel[11]<!>
const c = -1

main() {}
