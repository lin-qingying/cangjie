// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var x: <!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T<!>> = <!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(true)
    var (a1, a2): (<!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T<!>1>, <!UNDECLARED_TYPE_NAME!>C<!><<!UNDECLARED_TYPE_NAME!>T<!>2>) = (false, false)

    x = 1
    a1 = (false, false)
    a2 = false
    0
}
