// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func <!UNABLE_TO_INFER_RETURN_TYPE!>foo<!>() {
    let temp = 2
    return temp % match (foo) {
        case 2 => 0
        case _ => 1
    }
}

func foo1() {
    let temp = 2
    return foo % match (temp) {
        case 1 => 0
        case _ => 1
    }
}

main(): Int64 {
    return foo() + foo1()
}
