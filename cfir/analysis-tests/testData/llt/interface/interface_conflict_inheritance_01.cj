// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class C {}

class D {}

class E {}

interface I0 {
    func foo(): C
}

interface I1 {
    func foo(): D
}

interface I2 {
    func foo(): E
}

interface <!INHERIT_MEMBER_TYPE_INCONSISTENT!>II<!> <: I0 & I1 & I2 {}

interface III <: I1 & I2 {
    func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(): E
}

main() {
    return 0
}
