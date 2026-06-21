

@Annotation[target: [EnumConstructor]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

enum E {
    @APILevel[11]
    caseA
    | @APILevel[11]
    caseB(Int64)
}

main() {}
