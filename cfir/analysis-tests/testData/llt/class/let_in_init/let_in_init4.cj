// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public static let b: Float32 = 0.0
    public init(bb: Float32) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>A.b<!> = bb
    }
}

main() {
    let y: Float32 = 9.0
    var a = A(y)
    return 0
}
