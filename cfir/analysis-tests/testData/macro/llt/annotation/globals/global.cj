

@Annotation[target: [GlobalFunction]]
class <!ANNOTATION_NON_PUBLIC!>APILevel<!> {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
func foo() {}

@Annotation[target: [GlobalVariable]]
class <!ANNOTATION_NON_PUBLIC!>APMLevel<!> {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APMLevel[11]
const c = 1

@APMLevel[11]
let d = 2

main() {}
