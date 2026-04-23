// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs

internal import pkgs.*

main() {
    var ans1: B<Int32> = C()
    print(ans1.default_foo().toString())
    var ans2: G<Float64> = obj
    var ans3: Data<Int32> = data
    if (ans1.default_foo() == 2 && ans2.count == 1.00 && ans3.area() == 3) {
        return 0
    }
    return 1
}
