// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output %diag_json
import stdx.effect.Command

class Eff <: Command<Unit> {}

main() {
    let x = try {
      34
    } handle (_: Eff) {
      "Hello"
    }
    0
}

// ASSERT: scan sema_mismatching_handle_block


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
