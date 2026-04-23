// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_VARIABLE

class ClassC<T> {}

struct A<T> {
    static var a: <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>ClassC<T><!> = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>ClassC<T>()<!>
}

main() {}
