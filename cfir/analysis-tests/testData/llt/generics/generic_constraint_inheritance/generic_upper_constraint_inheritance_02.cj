// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %diag_json %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I1 {}

interface I2 <: I1 {}

// Constraint check should pass
interface I {
    static func f<T1, T2>(): Unit where T1 <: I2 {}
}

class A <: I {
    public static func f<T1, T2>() where T1 <: I1 {}
}

main() {
}
