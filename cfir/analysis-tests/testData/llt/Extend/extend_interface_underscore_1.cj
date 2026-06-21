// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    func foo() {}
}

interface I2 {
    func foo() {}
}

class A {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend A <: I1 {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend A <: I2 {}

main(): Int64 {
    0
}
