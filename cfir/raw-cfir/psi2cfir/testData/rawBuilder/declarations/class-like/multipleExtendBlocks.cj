// RUN_PIPELINE_TILL: FRONTEND
extend Int64 {
    func plusOne(): Int64 {
        return this + 1
    }
}

extend Int64 {
    func plusTwo(): Int64 {
        return this + 2
    }
}
