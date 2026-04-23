// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Printable {}

interface Runnable {}

interface I {
    // Another constraint than implementation. Must emit compilation error.
    static func id<T>(x: T): T where T <: Printable
}

class Implementation <: I {
    public static func id<T>(x: T): T where T <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>Runnable<!> {
        return x
    }
}

main() {}
