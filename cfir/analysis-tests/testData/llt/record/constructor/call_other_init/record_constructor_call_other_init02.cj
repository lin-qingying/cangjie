// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_VARIABLE

struct A {
    public let a: Int64
    public let b: Int64
    public let c: Int64
    <!CLASS_UNINITIALIZED_FIELD!>public init(x: Int64, y: Int64)<!> {
        this.a = x
        this.b = y
    }
    public init(x: Int64, y: Int64, z: Int64) {
        this.a = x
        this.b = y
        this.c = z
    }
}

main(): Int64 {
    0
}
