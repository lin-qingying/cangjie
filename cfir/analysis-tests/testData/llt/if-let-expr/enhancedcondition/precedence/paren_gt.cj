// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

//EXEC: %compiler %f

main() {
    let a = 1
    let b = Some(3)

    if (true && (a) > 3) {
        1
    } else if (let Some(x) <- b && (x) >= 3) {
        0
    } else {
        -1
    }
}
