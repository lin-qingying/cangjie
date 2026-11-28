// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.macroapi

import std.ast.*
import std.core.Option
import std.fs.Path

func consumeReportLevel(level: DiagReportLevel): Bool {
    return level.level() == 0
}

func useMacroStdlib(token: Token): Bool {
    print("macro IDE API", flush: true)
    return consumeReportLevel(DiagReportLevel.WARNING) ||
        token.kind == TokenKind.IDENTIFIER || token.kind == IDENTIFIER
}

func useStaticField(): String {
    return Path.Separator
}

func useGenericEnum(option: Option<Int32>): Bool {
    return true
}
