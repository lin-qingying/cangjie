// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package pkgs0018004a

internal import pkgs0018002a.run1
internal import pkgs0018003a.run2

main(): Int64 {
    let ans1 = run1()
    let ans2 = run1()
    let ans3 = run2()
    let ans4 = run2()

    if (ans1 == 1 && ans2 == 1 && ans3 == 2 && ans4 == 2) {
        return 0
    }
    return 1
}
