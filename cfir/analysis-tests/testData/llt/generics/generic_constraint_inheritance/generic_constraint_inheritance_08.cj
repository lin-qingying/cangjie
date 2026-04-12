// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output

interface Printable {}

interface Runnable {}

interface I {
    // Another constraint than implementation. Must emit compilation error.
    static func id<T>(x: T): T where T <: Printable
}

class Implementation <: I {
    public static func id<T>(x: T): T where T <: Runnable {
        return x
    }
}

main() {}

// ASSERT: scan-1 sema_generic_constraint_not_looser
// ASSERT: scan "Errors": 1
