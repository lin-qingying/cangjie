// RUN_PIPELINE_TILL: FRONTEND
type MyInt = Int64

extend MyInt {
    func twice(): Int64 {
        return this * 2
    }
}

extend String {
    func shouted(): String {
        return this + "!"
    }
}
