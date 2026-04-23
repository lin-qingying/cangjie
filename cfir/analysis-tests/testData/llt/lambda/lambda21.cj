// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test(): Int64 {
    let myLambda = {
        i_in: Int64 =>
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>i_in<!> = <!UNRESOLVED_REFERENCE!>i_out<!>
        return i_in
    }
    return 1
}

main(): Int64 {
    var i_out: Int64 = 0
    return test()
}
