// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {}

class B {}

public var (a, <!ACCESSIBILITY_WITH_MAIN_HINT!>b<!>): (Int64, A) = (1, A())

public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>c<!>, d): (A, Int64) = (A(), 1)

public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>a1<!>, <!ACCESSIBILITY_WITH_MAIN_HINT!>b1<!>): (B, A) = (B(), A())
