// @Deprecated 全形态：位置实参、具名实参、混合与多宿主（类 / 字段 / 函数）。
// 官方依据：Cangjie 文档《内置编译标记》；cjc 1.0.5 / 1.1.3 零诊断。
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
func deprecatedFn(): Int64 {
    return 1
}

@Deprecated
class BareDeprecated {}

@Deprecated[]
class EmptyDeprecated {}
