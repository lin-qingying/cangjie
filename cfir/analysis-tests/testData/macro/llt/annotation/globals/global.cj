

@Annotation[target: [GlobalFunction]]
class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
func foo() {}

@Annotation[target: [GlobalVariable]]
class APMLevel {
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
