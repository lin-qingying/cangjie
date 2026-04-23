// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    static func f<T1, T2>(): Unit where T1 <: Any {}
}

class A <: I {
    public static func f<T1, T2>() where T1 <: <!GENERIC_CONSTRAINT_NOT_LOOSER, UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T2<!> {}
}

main() {}
