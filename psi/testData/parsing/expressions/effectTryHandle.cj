// 异常处理：try / catch / finally 与 throw（cjc 1.0.5 可编译验证）。
// 官方依据：Cangjie 文档《异常处理》；cjc 1.0.5 / 1.1.3 零诊断。
// 代数效果（try { perform } handle { resume with }）为解析器前瞻语法，
// cjc 1.0.5 尚未支持，单独见 expressions/effectAlgebra.cj。
class LogCommand {
    public init() {}
}

func tryCatchFinally(): Int64 {
    let v = try {
        throw Exception()
    } catch (e: Exception) {
        0
    } finally {
        let _ = 0
    }
    return v
}

func multiCatch(e: Exception): Int64 {
    let v = try {
        if (e is IllegalArgumentException) {
            1
        } else {
            throw e
        }
    } catch (err: IllegalArgumentException) {
        2
    } catch (err: Exception) {
        3
    } finally {
        let _ = 0
    }
    return v
}
