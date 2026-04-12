// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

struct C {
    static var a: Range<Int64>
    static let b: Int64
    static init() {
        a = 1..10
        b = 11
    }
}

var x = C.a

main() {
    if (C.a.start != 1 || C.a.end != 10 || x != C.a) {
        return 1
    }
    if (C.b != 11) {
        return 2
    }
    return 0
}
