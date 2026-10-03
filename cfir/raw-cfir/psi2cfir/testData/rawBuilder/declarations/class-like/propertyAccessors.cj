// RUN_PIPELINE_TILL: FRONTEND
class C {
    private var store: Int64 = 0

    public mut prop value: Int64 {
        get() {
            return this.store
        }
        set(v) {
            this.store = v
        }
    }

    public static prop counter: Int64 {
        get() {
            return 1
        }
    }
}

interface WithProp {
    prop name: String {
        get() {
            return "x"
        }
    }
}
