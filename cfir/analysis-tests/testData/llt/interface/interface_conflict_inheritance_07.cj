// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    func foo(): Int
}

interface I2 {
    func foo(): String
}

func bar<T>(x: T) where <!sema_inherit_member_type_inconsistent!>T <: I1 & I2<!> {
}

class A<T> where <!sema_inherit_member_type_inconsistent!>T <: I1 & I2<!> {}

main() {
}
