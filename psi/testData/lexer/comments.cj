// 注释：行注释、块注释（含嵌套）、文档注释与行内混排。
// 官方依据：Cangjie 文档《注释》；cjc 1.0.5 / 1.1.3 零诊断。

// 行注释：到行尾结束
func lineComment(): Unit {} // 尾部行注释

/* 块注释 */
func blockComment(): Unit {}

/* 嵌套块注释
   /* 内层注释 */
   仍然属于外层注释
*/
func nestedBlockComment(): Unit {}

/**
 * 文档注释
 * @brief 摘要
 * @param x 参数
 * @return 返回值
 */
func docComment(x: Int64): Int64 {
    return x
}

// 文档注释中的标签与行内块注释混排：/* inline */ 之后继续
func mixedComments(): Unit {
    /* inline */ let a = 1 // trailing
}
