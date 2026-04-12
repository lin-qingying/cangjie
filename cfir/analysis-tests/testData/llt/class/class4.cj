// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT:regex-auto class 'D' missing abstract modifier, otherwise abstract function or property should be implemented

interface A {
    func foo1(input: Bool): Int32
}

interface B {
    func foo1(input: Bool): Int32
}

interface C <: A & B {}

class D <: C {}

abstract class E <: C {}
