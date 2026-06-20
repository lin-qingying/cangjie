// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func a() {
    let a = b()
    let c = <!UNRESOLVED_REFERENCE!>a<!>1()

    func a1() {
    }
}

func b() {
    123
}
