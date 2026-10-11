// RUN_PIPELINE_TILL: FRONTEND
// 多行字符串字面量：三个双引号 / 三个单引号、空内容、保留缩进与换行、首行换行后开始内容。
// 官方依据：Cangjie 文档《字符串类型》「多行字符串字面量」；cjc 1.0.5 / 1.1.3 零诊断。
func multilineStringLiterals(): Unit {
    let a = """
        """
    let b = '''
        '''
    let c = """
        first line
        second line
    """
    let d = '''
        Hello,
        Cangjie Lang'''
    let e = """
        缩进与空行都会保留：

        第二段
    """
    let _ = (a, b, c, d, e)
}