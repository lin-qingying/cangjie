// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output

interface I<T> {
    static func f<T1, T2>(): Unit where T1 <: T2, T2 <: T {}
}

class A <: I<Int64> {
    public static func f<T1, T2>(): Unit where T1 <: Int64 {}
}

main() {}

// ASSERT: scan-3 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 3
