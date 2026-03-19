// RUN_PIPELINE_TILL: FRONTEND
extend Int64 {
    func isPositive(): Bool {
        return this > 0
    }
}

