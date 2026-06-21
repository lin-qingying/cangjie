

@Annotation[target: [Extension, MemberFunction, MemberProperty]]
public class APILevel {
    public const init() {}
}

extend Int8 {
    @APILevel
    func foo() {
        0
    }

    @APILevel
    static func bar() {
        1
    }

    @APILevel
    prop K: Int {
        get() {
            2
        }
    }

    @APILevel
    static prop K2: Int {
        get() {
            3
        }
    }
}

main() {}
