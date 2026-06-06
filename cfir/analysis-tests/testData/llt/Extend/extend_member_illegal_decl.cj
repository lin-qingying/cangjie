// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A { }

extend A {
    <!parse_unexpected_declaration_in_scope!>var<!> x: Int64 = 1
    <!parse_unexpected_declaration_in_scope!>var<!> y: Int64 = 2
}
