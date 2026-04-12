// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %diag_json %typecheck %f 2>&1 | compare %f

main() {
    var range1 = 1..Int64(10) : 3
    var range2 = 1..10 : true
    var range3 = 1.0..10.0 : 2.0
    var range4 = 1..10 : 0
    return 0
}

// ASSERT: scan-2 sema_cannot_convert_literal
// ASSERT: scan-1 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-1 sema_step_non_zero_range
// ASSERT: scan "Errors": 4
