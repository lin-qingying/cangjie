// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1 {
    func a(a: Int32): Int32
}

interface I2 <: I1 {
    func a(): Int32
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class A<!> <: I1 & I2 {
    public func a(): Int32 {
        1
    }
}

main() {
    var res = A().a()
    if (res == 1) {
        return 0
    }
    return 1
}
