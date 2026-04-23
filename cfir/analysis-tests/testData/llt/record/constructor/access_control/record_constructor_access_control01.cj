// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {
    public let b: Int64 = 1
    public init() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 1
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.b<!> = 2
    }
}

main(): Int64 {
    0
}
