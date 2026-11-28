// 属性访问器形态：只读 prop、mut get/set prop 与访问器中的 return。
// 官方依据：Cangjie 文档《属性》；cjc 1.0.5 / 1.1.3 零诊断。
class Props {
    private var store: Int64 = 0

    prop getOnly: Int64 {
        get() {
            return store
        }
    }

    mut prop getSet: Int64 {
        get() {
            return store
        }
        set(v) {
            store = v
        }
    }

    public func use(): Int64 {
        getSet = 5
        return getOnly + getSet
    }
}

interface Readonly {
    prop name: String {
        get() {
            return "ro"
        }
    }
}
