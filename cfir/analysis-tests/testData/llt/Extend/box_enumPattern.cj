// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output

enum E {
    EE(Int64, String)
}

main() {
    let t = EE(1, "1")
    match (t) {
        case EE(x: ToString, y: ToString) =>
            if (x.toString() != "1" || y.toString() != "1") {
                return 1
            }
    }
    return 0
}
