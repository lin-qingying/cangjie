// 官方 ParseDecl.cpp 保留枚举项 payload 类型；cjc 探针只报告实参类型不匹配。
@Annotation[target: [EnumConstructor]]
public class PayloadTag {
    public const init() {}
}

enum E {
    @PayloadTag[]
    caseA
    | @PayloadTag[]
    caseB(Int64)
}

main() {
    E.caseB(<!ARGUMENT_TYPE_MISMATCH!>"wrong"<!>)
}
