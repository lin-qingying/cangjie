// EXEC: %compiler %cmp_opt -g %n.cj -o %n.%suffix

struct A {
    A(var `this`: Int64) {}
}
class B {
    B(var `this`: Int32) {}
}
main() {}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
