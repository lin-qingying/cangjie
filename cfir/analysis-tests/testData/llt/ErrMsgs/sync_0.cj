// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %diag_json %f -o %output

main() {
    let b = synchronized(()) {}
    let c: Bool = synchronized(false) {
        1
    }
    0
}

// ASSERT: scan sema_use_expr_without_import
// ASSERT: scan sema_cannot_convert_literal
// ASSERT: regex-not no position error occurred
