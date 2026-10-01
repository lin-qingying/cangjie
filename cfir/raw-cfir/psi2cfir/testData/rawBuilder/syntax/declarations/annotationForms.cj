// RUN_PIPELINE_TILL: FRONTEND
// 注解用法：@Deprecated（message / since / strict）、@Overflow*、@When（多条件 && / != / 版本格式）、@sourcePackage 编译标记，
// 注解出现在类 / 成员函数 / 成员变量 / 顶层函数 / 顶层变量位置。官方 1.0.5 / 1.1.3 没有 `annotation` 声明语法（见 recovery）。
// 官方依据：Cangjie 文档《注解》《条件编译》；cjc 1.0.5 / 1.1.3 零诊断。
@Deprecated["old api", since: "1.0.0", strict: true]
@When[os == "Windows" && arch == "x86_64"]
class Annotated {
    @Deprecated["field", since: "1.0.0"]
    var field: UInt8 = 0

    @OverflowWrapping
    public func wrapping(): UInt8 {
        return field + 1u8
    }
}

@When[os != "Linux"]
@Deprecated["fn", since: "1.0.0"]
func annotatedFunc(p: Int64): Int64 {
    return p
}

@Deprecated["top", since: "1.0.0"]
public let annotatedLet: Int64 = 1

func packageMacro(): String {
    return "${@sourcePackage()}"
}
