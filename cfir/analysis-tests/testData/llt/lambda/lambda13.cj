// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    var a = 1
}

class B {}

func test(a: (B) -> Int64) {}

func test(a: (A) -> Int64) {}

func test(a: (A) -> Unit) {}

main() {
    <!AMBIGUOUS_FUNCTION_CALL!>test<!> {r => r.a}
    0
}
