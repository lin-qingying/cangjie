// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %diag_json %f -o %output
// ASSERT: scan-2 sema_generic_type_without_type_argument

func foo(a: Int64): Unit {}

func foo(a: Bool): Unit {}

main(): Int64 {
    foo(None is Int)
    println(None is Int)
    return 0
}
