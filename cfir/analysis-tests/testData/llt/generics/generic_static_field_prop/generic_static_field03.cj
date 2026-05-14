// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class C {
    static var b: Int64 = 1
}

struct A<T> {
    static var b: Int64 = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>T<!>.b // error
}

main() {}
