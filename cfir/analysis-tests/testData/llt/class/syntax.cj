// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {}

open class B {}

class C <: A & <!ILLEGAL_MULTI_INHERITANCE!>B<!> {}

interface I {}

class D <: I & <!SUPERCLASS_MUST_BE_PLACED_AT_FIRST!>A<!> {}
