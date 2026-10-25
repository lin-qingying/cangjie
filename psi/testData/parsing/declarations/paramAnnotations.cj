// 形参注解：成员函数形参与构造函数形参上的 @Attribute 注解。
// 官方依据：Cangjie 文档《注解》；cjc 1.0.5 / 1.1.3 零诊断。
class ParamAnnotations {
    private var v: Int64 = 0

    public func m(@Attribute[In] x: Int64): Int64 {
        return x + v
    }

    init(@Attribute[In] y: Int64) {
        this.v = y
    }
}

extend Int64 {
    @Attribute[State]
    func annotatedExtendMember(): Int64 {
        return this
    }
}
