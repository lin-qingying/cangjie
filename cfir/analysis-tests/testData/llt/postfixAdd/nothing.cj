// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %diag_json 2>&1 | compare %f

main() {
    var a = return 10
    a++
    0
}

// ASSERT: scan "DiagKind": "sema_mismatched_types"
// ASSERT: scan "Errors": 1,
