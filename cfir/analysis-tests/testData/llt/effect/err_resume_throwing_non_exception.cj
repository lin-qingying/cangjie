// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output %diag_json
import stdx.effect.Command

main() {
    try {}
    handle (e: Command<Int64>) {
        resume throwing "Hello!"
    }
    0
}
// ASSERT: scan sema_resume_throwing_mismatch_type


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
