// 属性：prop 的 get / set、mut prop、static prop、接口属性与 this 限定。
// 官方依据：Cangjie 文档《属性》；cjc 1.0.5 / 1.1.3 零诊断。
class Gauge {
    private var store: Int64 = 0

    public mut prop value: Int64 {
        get() {
            return this.store
        }
        set(v) {
            this.store = v
        }
    }

    public static prop max: Int64 {
        get() {
            return 100
        }
    }

    public prop doubled: Int64 {
        get() {
            return this.store * 2
        }
    }
}

interface Named {
    prop name: String {
        get() {
            return "named"
        }
    }
}

class Widget <: Named {
    private var label: String = "w"

    public override prop name: String {
        get() {
            return this.label
        }
    }
}
