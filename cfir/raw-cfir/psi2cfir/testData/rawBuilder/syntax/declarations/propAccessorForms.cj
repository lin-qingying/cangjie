// RUN_PIPELINE_TILL: FRONTEND
// 属性：只读 get、mut get+set、静态 prop、接口中的抽象 prop 与静态 prop、enum 中的 prop、extend 中的 prop。
// 官方依据：Cangjie 文档《属性》；cjc 1.0.5 / 1.1.3 零诊断（非 mut prop 带 setter 是语义错误，见 diagnostics2/property；abstract prop 不能写在 class 里，见 recovery/abstractPropInClass.cj）。
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

    static prop staticGetOnly: Int64 {
        get() {
            return 1
        }
    }
}

interface PropInterface {
    prop interfaceAbstract: String

    static prop interfaceStatic: Int64 {
        get() {
            return 2
        }
    }
}

enum PropEnum {
    | A

    prop isA: Bool {
        get() {
            return true
        }
    }
}

struct PropStruct {
    var v: Int64 = 0

    prop doubled: Int64 {
        get() {
            return v * 2
        }
    }
}

extend PropStruct {
    public prop tripled: Int64 {
        get() {
            return v * 3
        }
    }
}
