// 官方 896235c9: ParseDecl.cpp 的枚举项作用域必须贯穿 ParseMacro.cpp 的注解链。
// 枚举项、payload 调用及普通成员共同约束 lowering 的声明归属与类型信息。

@Annotation[target: [EnumConstructor]]
public class CaseTag {
    public let value: Int64
    public const init(v: Int64) {
        value = v
    }
}

@Annotation[target: [EnumConstructor]]
public class CaseVersion {
    public const init() {}
}

@Annotation[target: [MemberFunction, MemberProperty]]
public class MemberTag {
    public const init() {}
}

enum E {
    @CaseTag[1]
    @CaseVersion[]
    caseA
    | @CaseTag[2]
    @CaseVersion[]
    caseB(Int64)
    | @CaseVersion[]
    caseC(Int64, Bool)

    @MemberTag[]
    public func number(): Int64 {
        1
    }

    @MemberTag[]
    public prop value: Int64 {
        get() {
            2
        }
    }
}

enum WithLeadingBar {
    | @CaseTag[3]
    @CaseVersion[]
    first(Int64)
    | @CaseTag[4]
    empty
}

func identity(value: WithLeadingBar): WithLeadingBar {
    value
}

main() {
    let a: E = E.caseA
    let b: E = E.caseB(1)
    let c: E = E.caseC(2, true)
    identity(WithLeadingBar.first(3))
    identity(WithLeadingBar.empty)
    println(a.number() + b.value + c.number())
}
