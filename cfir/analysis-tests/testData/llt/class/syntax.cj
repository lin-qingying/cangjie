<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {}

open class B {}

class C <: A & <!sema_illegal_multi_inheritance!>B<!> {}

interface I {}

class D <: I & <!sema_superclass_must_be_placed_at_first!>A<!> {}
