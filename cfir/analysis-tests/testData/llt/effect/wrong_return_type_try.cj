// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output %diag_json

import stdx.effect.Command

main():Int64 {
    try {
        "Hello"
    } handle (_: Command<Unit>) {
        42
    }
}

// ASSERT: scan sema_mismatched_types

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
