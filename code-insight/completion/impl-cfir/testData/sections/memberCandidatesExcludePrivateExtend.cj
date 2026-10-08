package completion.sections

class Plain {}

extend Plain {
    private func secret(): Int64 {
        1
    }

    func open(): Int64 {
        2
    }
}

// private extend 成员不得作为候选出现；同 extend 里的公开成员必须出现（正反对照）。
func use(plain: Plain): Int64 {
    plain.completionTarget
}
