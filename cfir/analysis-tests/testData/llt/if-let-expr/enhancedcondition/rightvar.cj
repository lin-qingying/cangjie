// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output

main() {
    let a = Some(1)
    let b = Some(3)

    if (let Some(c) <- a && let Some(d) <- b && c < d) {
        println(c + d)
    }
    while ((let Some(c) <- a && let Some(d) <- b) && c < d) {
        println(c + d)
        break
    }
}
