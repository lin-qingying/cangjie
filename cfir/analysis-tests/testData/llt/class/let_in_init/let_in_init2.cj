// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public let a: Int32 = 0
    public init(aa: Int32) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.a<!> = aa
    }
}

main() {
    let x: Int32 = 10
    var a = A(x)
    return 0
}
