// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_VARIABLE

enum TimeUnit {
    Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}

main(): Int64 {
    var time = Day(1, 2, 3)
    var d = match (time) {
        case Year(<!VAR_IN_OR_PATTERN!>y<!>) | Month(<!VAR_IN_OR_PATTERN!>y<!>, <!VAR_IN_OR_PATTERN!>m<!>) => <!UNRESOLVED_REFERENCE!>y<!>
        case _ => -1
    }
    return 0
}
