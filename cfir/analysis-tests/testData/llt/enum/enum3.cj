// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum TimeUnit1 {
    Hour
    | Year
}

enum TimeUnit2 {
    Month
}

func run(): Int64 {
    var time = TimeUnit2.Month
    var res: Int32 = match (time) {
        case <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Month(Hour)<!> => 2020
    }
    return 0
}
