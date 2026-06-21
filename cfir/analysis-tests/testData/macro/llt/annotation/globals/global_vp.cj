

@Annotation[target: [GlobalVariable]]
public class APMLevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APMLevel[11]
const (c, d) = (3, 4)

@APMLevel[11]
let (a, b) = (1, 2)

@APMLevel[11]
let _ = 1

@APMLevel[11]
var (_, _, _) = (1, 2, 3)

main() {}
