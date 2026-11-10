package completion.sections

class Plain {
    func member(): Int64 {
        1
    }
}

// 非枚举类型的成员访问：不得出现任何 ENUM_ENTRY 候选。
// （同名引用取最后一个，故本夹具只放这一处引用点。）
func nonEnumMemberSite(): Int64 {
    Plain().completionTarget
}