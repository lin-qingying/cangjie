// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import std.collection.*

class A {
    A(var run!: () -> Unit = {=>}) {}
}

func foo() {}

var test = ArrayList<A>([A(<!NEED_NAMED_ARGUMENT!>foo<!>)])

main() {}
