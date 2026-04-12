// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %diag_json %f 2>&1 | compare %f

class X {}

interface Y {}

enum R<T> where T <: X & Y {
    e
}

var a = R<X>.e
var b = R<Y>.e

main(): Int64 {
    return 0
}

// ASSERT: scan-2 sema_generic_type_argument_not_match_constraint
// ASSERT: scan "Errors": 2
