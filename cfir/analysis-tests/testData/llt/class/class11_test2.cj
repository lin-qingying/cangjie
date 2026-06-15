// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface A {
    func i() {}
    static func test(<!CANNOT_HAVE_DEFAULT_PARAM!>input!: Unit = <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.i()<!>) {
        <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.i()
    }
}

main(): Int64 {
    return 1
}
