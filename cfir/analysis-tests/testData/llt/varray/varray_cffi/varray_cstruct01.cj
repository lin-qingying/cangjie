// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct A {}

@C
struct S {
    var <!sema_illegal_member_of_cstruct!>a<!> = VArray<VArray<VArray<VArray<A, $1>, $1>, $1>, $1>(repeat: [[[A()]]]) // error
    var <!sema_illegal_member_of_cstruct!>b<!> = VArray<A, $1>(repeat: A()) // error
}

main() {}
