// LANGUAGE_VERSION: 1.1.3

package returned_modifiers.illegal_property

class Owner {
    // 非法 target 是语义负例，不能为了合法化而删除这些修饰符。
    foreign unsafe prop illegalProperty: Int64 {
        get() { 0 }
    }

    prop ordinaryProperty: Int64 {
        get() { 0 }
    }
}
