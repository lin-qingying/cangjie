// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a = Some(15)
    while (<!VAR_IN_OR_CONDITION!>let Some(b) <- a || true<!>) {
        return 0
    }

    while (<!VAR_IN_OR_CONDITION!>false || let Some(b) <- a<!>) {
        return b - 1
    }

    return 2
}
