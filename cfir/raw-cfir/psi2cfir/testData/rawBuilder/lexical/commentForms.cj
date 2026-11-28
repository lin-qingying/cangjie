// RUN_PIPELINE_TILL: FRONTEND
// 注释形态补充：纯 `/**/` 形式、三层以上嵌套块注释、行内块注释与 `//` 混排、文档注释标签（@brief / @param / @return / @deprecated）、
// 单行文档注释与空文档注释、成员与函数上的文档注释。官方依据：Cangjie 文档《注释》；官方 Lexer.cpp ScanLineComment / ScanBlockComment /
// ScanDocComment；cjc 1.0.5 / 1.1.3 零诊断。
/* a /* b /* c */ b */ a */
/**
 * 一段文档
 * @brief 摘要
 * @return 结果
 */
/** 单行文档 */
/**
 */
/*** 三星 */
class Documented {
    /**
     * 成员文档
     * @param x 输入
     */
    public func f(x: Int64): Int64 {
        return x
    }
}

/**
 * @deprecated
 */
func deprecatedDoc(): Unit {
}

func commentForms(): Int64 {
    /*+ x
       y */
    /* z // w */
    return 1
}
