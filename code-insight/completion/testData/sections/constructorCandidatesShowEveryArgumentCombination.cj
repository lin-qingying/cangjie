package completion.sections

class Widget {
    let alpha: Int64
    init(alpha: Int64) {
        this.alpha = alpha
    }
    init(alpha: Int64, beta: Int64) {
        this.alpha = alpha + beta
    }
}

// 两个构造器共享同一个 ClassId，必须靠尾文本区分，否则用户无从选择调用哪一种。
func site(): Int64 {
    completionTarget
}
