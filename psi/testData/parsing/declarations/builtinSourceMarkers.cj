// 内建编译标记：@sourceFile / @sourcePackage / @sourceLine 作为表达式、默认值与插值。
// 官方依据：Cangjie 文档《内置编译标记》之「源码位置」；cjc 1.0.5 / 1.1.3 零诊断。
let markerSrcFile: String = @sourceFile()
let markerSrcPackage: String = @sourcePackage()

func markerSrcExpr(line: Int64): Int64 {
    return line + @sourceLine()
}

func markerSrcDefault(line!: Int64 = @sourceLine()): Int64 {
    return line
}

func markerSrcInterp(): String {
    return "${@sourceFile()}:${@sourceLine()}"
}
