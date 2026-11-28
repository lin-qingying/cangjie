// LANGUAGE_VERSION: 1.1.3

package returned_modifiers.rejected_common

// 真实 NONE 调用下 common 角色被拒绝，FOREIGN/UNSAFE 返回属性一起清空。
public foreign unsafe common func rejectedFunction(): Int64 {
    return 0
}

class Owner {
    public foreign unsafe common prop rejectedProperty: Int64 {
        get() { 0 }
    }
}
