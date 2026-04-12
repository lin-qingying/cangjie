// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f -o %output %cmp_opt

enum A {
    | B | C(Int64) | ...
}

var a = B

main() {
    while (let B | _ <- a) {
        break
    }
    if (let B | _ <- a) {
        0
    } else {
        1
    }
}

// ASSERT: scan-2 error: patterns connected by '|' should be of the same kind
