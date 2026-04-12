// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output

enum E {
    A | B(Int64) | ...
}

enum D {
    D1 | D2(Int64)
}

let k = (D1, B(10))

main() {
    match (k) {
        case (D1, A) => 1
        case (D1, _) => 2
        case (D2(a), _) => a
    }
}

// ASSERT: scan error: non-exhaustive patterns
// ASSERT: scan # note: (D1, _) is not covered
// ASSERT: scan 1 error generated, 1 error printed.
