// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %diag_json %f -o %output 2>&1 | compare %f

func test(): Int64 {
    let myLambda = {
        i_in: Int64 =>
        i_in = i_out
        return i_in
    }
    return 1
}

main(): Int64 {
    var i_out: Int64 = 0
    return test()
}
// ASSERT: scan sema_cannot_assign_to_immutable
// ASSERT: scan sema_undeclared_identifier