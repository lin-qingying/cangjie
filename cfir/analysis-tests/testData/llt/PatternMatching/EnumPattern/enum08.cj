// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d: Int64 = match (time) {
        case Year(a) => <!TYPE_MISMATCH!>a<!>
        case Month(a, <!REDECLARATION!>a<!>) => <!TYPE_MISMATCH!>a + a<!>
        case _ => -1
    }
    if (d == 6) {
        return 0
    }
    return 1
}
