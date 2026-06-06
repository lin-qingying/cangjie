// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct A<T>{
    public operator func []<b<!parse_expected_right_delimiter!>:<!>Float32<!parse_expected_decl!>)<!>:Bool {true}
}
main() {}
