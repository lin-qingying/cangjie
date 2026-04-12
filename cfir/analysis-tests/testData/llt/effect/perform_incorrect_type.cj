// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output %diag_json

main():Int64 {
    perform ("Hello" + 45)
}

// ASSERT: scan sema_invalid_binary_expr

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
