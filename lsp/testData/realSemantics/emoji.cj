package sample.emoji

// 😀 注释里的非 BMP 字符让 UTF-16 码元与 UTF-8 字节数在这里分叉
// 🎯 第二行也放一个，确保后续行首偏移必须按 UTF-16 累加
func twice(value: Int64): Int64 {
    return value * 2
}

func caller(): Int64 {
    return twice(2)
}
