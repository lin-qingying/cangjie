// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test(): Int64 {
    var i : Int32 = 2
    let a = 3
    match (i) {
        case a<!parse_expected_double_arrow_in_case!>@<!>t =>
            print(t.toString()+",")
            print(a.toString())
            1
        case _ => 0
    }
    return 0
}

main(): Int64 {
    return test()
}
