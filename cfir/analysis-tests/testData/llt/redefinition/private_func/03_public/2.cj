// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LLT_COMPANION_SOURCES

private func <!CONFLICTING_OVERLOADS!>foo<!>(): Unit {
    println("foo in 2.cj")
}

internal func hoo() {
   println("hoo in 2.cj")
   <!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
}
