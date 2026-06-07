// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum TimeUnit {
    Year(Int32) | Month(Int32)
}

enum TimeUnit1<T> {
    Year(T)
}

func codegenUnitTest(): Int32 {
    var time1 = <!AMBIGUOUS_CONSTRUCTOR_CALL!>Year<!>(1)
    var time2 = TimeUnit.Month(<!TYPE_MISMATCH!>r'a'<!>)
    return 0
}
