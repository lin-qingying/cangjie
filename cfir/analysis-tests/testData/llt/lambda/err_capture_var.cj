// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    var flag = false
    <!USE_FUNC_CAPTURE_VAR_ALONE!>spawn {
        =>
        var a = 1
        if (a + 1 == 2) {
            flag = true
        }
    }<!>
    return
}
