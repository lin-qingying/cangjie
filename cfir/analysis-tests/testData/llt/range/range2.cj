// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %diag_json %typecheck %f 2>&1 | compare %f

main() {
    var range1 = <!GENERIC_ARGUMENT_NO_MATCH!>1..Int64(10)<!> : 3
    var range2 = 1..10 : <!TYPE_MISMATCH!>true<!>
    var range3 = 1.0..10.0 : <!TYPE_MISMATCH!>2.0<!>
    var range4 = 1..10 : <!RANGE_STEP_CANNOT_BE_ZERO!>0<!>
    return 0
}
