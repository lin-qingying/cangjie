package completion.sections

class Widget {
    let alpha: Int64
    init(alpha: Int64) {
        this.alpha = alpha
    }
}

// 类型位置上写的是类型名本身：跟随左括号会让用户写出 `Widget(` 这种非法源码。
func site(w: completionTarget): Int64 {
    0
}
