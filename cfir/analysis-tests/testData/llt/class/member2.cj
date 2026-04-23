// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public var a: Int64 = 1
}

main() {
    <!GENERIC_ARGUMENT_NO_MATCH!>A().a<Bool><!>
    let a1: ?A = A()
    let r1 = <!GENERIC_ARGUMENT_NO_MATCH!>a1?.a<Bool><!>
    return 0
}
