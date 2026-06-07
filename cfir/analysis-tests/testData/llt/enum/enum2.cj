// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum TimeUnit {
    Year | Month | Day | Hour
}

func codegenUnitTest(): Int32 {
    var time1 = TimeUnit.Year
    time1.<!NOT_MEMBER_OF!>Year<!>
    time1.<!NO_MATCHING_OPERATOR_INVOKE!>Year<!>()
    return 0
}
