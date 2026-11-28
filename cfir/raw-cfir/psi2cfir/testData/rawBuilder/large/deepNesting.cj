// RUN_PIPELINE_TILL: FRONTEND
// 大文件场景：两层 for + 6 层 if/else 链，最内层 else 分支里再放 for 与 if/else（最深 9 层），
// 每个分支放不同的字面量与运算表达式，用于覆盖 CFIR 构树时的嵌套控制流节点与分支终结符。cjc 1.0.5 / 1.1.3 零诊断。
func deepNesting(): Int64 {
    let seed = 1
    for (i in 0..1) {
        let outer = i + seed
        for (j in 0..1) {
            let inner = j + outer
            if (inner > 0) { let v0 = inner + 0
                if (inner > 1) { let v1 = inner + 1
                    if (inner > 2) { let v2 = inner + 2
                        if (inner > 3) { let v3 = inner + 3
                            if (inner > 4) { let v4 = inner + 4
                                if (inner > 5) { let v5 = inner + 5
                                    let deepest = v5 * 6 - 7 / 2 + 8 % 3
                                } else { let v5 = 0 }
                            } else { let v4 = 0 }
                        } else { let v3 = 0 }
                    } else { let v2 = 0 }
                } else { let v1 = 0 }
            } else {
                for (k in 0..2) {
                    let m = k * inner
                    if (m > 1) { let n1 = m + 1
                        if (n1 > 2) { let n2 = n1 + 2
                            let n3 = n2 + 3
                        } else { let n2 = 0 }
                    } else { let n1 = 0 }
                }
            }
        }
    }
    return seed
}
