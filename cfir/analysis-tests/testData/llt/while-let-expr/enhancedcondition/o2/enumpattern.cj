// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %enableO2 %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

main() {
    let a = 3
    if (let Some(2) | Some(3) | None <- Some(a)) {
        0
    } else {
        1
    }
}
