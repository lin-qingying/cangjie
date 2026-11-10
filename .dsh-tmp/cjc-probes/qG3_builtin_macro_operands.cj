main(): Int64 {
    let line: Int64 = 4
    let a = line + @sourceLine()
    let b = @sourceLine() + line
    let c = @sourceLine() * 2 + line
    let d = -@sourceLine() + line
    let s: String = @sourceFile() + @sourcePackage()
    return a + b + c + d + s.size
}
