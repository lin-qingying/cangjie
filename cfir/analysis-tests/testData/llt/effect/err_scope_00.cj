// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output %diag_json
import stdx.effect.Command

class Eff <: Command<Int64> {}

main() {
    try {
    } handle (e: Eff) {
        let e = 0
        println(e)
    }
}

// ASSERT: scan sema_redefinition


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
