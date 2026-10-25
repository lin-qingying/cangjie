// 未闭合字符串边界：单行字符串遇到换行产生 DANGLING_NEWLINE，
// 随后换行符按普通空白继续词法分析。
// 官方依据：Cangjie 词法对单行字符串的生命周期约束；此文件专测词法边界而非合法程序。
func dangling(): Unit {
    let a = "unterminated
    let b = 1
}
