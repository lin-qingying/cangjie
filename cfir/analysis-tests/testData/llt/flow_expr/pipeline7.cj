// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class C {
    static func f() {
        return 0
    }
    static func f(i: Int64) {
        return i
    }
}

main() {
    let v = 2 |> C.f
    if (v != 2) {
        return 1
    }
    return 0
}
