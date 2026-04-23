// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// RUN-EXEC: %run %run_opt %output

internal import pkg.*

main() {
    let v = test(B())
    if (v != 2) {
        return 1
    }
    return 0
}
