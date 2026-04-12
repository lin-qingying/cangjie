// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

open class Base {
    private let val: Int32 = 0
    private func f(): Int32 {
        return 0
    }
}

class Data <: Base {
    public static let val: Int32 = 1
    public static let f: Int32 = 2
}

main(): Int64 {
    var d = Data.val
    var f = Data.f
    if (d != 1 || f != 2) {
        return 1
    }
    return 0
}
