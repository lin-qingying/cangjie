// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_VARIABLE

class A {
    func foo<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>T<!>, V>(a: T, b: V): Unit where T <: V, V <: T {}
}

main() {}
