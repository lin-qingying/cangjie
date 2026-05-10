// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

interface I1 {}

interface I2 {}

class A {
    func f<T>(a: T) where T <: I1 {}
    func f<T>(a: T) where T <: I2 {} // Error, not overloading
    func <!CONFLICTING_OVERLOADS!>f<!><T>(a: T) {} // Error, not overloading
}

func f<T>(a: T) where T <: I1 {}

func f<T>(a: T) where T <: I2 {} // Error, not overloading

func <!CONFLICTING_OVERLOADS!>f<!><T>(a: T) {} // Error, not overloading
