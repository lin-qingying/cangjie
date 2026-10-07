package completion.sections

class Widget {
    let alpha: Int64
    init(alpha: Int64) {
        this.alpha = alpha
    }
}

// 表达式位置上用户写的是「用这个类型造一个值」，候选应当是可以直接调用的形态。
func site(): Int64 {
    completionTarget
}
