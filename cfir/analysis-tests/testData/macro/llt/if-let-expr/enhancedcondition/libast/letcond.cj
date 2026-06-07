// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

import std.ast.*

main() {
    let aa = [quote(let a <- 1), quote(let 1|2|3 <- b), quote(let Some(a) | None <- None)]
    for (b in aa) {
        let input = LetPatternExpr(b)
        input.toTokens()
    }
}
