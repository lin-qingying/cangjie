// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum TimeUnit {
    | Year | Month | Day | Hour
}

enum TimeUnit2 {
    | Year2(Int32)
    | Month2(Int32, Int32)
}

func codegenUnitTest(): TimeUnit {
    var time1 = TimeUnit.<!NOT_MEMBER_OF!>Year3<!>(1)
    var time2 = TimeUnit2.Month2<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    var time3 = TimeUnit2.<!NOT_MEMBER_OF!>Year<!>
    var time4 = <!NO_MATCHING_OPERATOR_INVOKE!>TimeUnit.Year<!>(1)
    return <!RETURN_TYPE_MISMATCH!>0<!>
}
