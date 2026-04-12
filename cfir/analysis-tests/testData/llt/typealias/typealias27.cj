// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt --diagnostic-format json %f -o %output

// ASSERT: scan sema_generic_type_argument_not_match_constraint

struct Foo<T> where T <: ToString {}

type Foobar<R> = Foo<R>

struct S {}

main() {
    let a = Foobar<S>()
}
