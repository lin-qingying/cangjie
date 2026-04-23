// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct RA<T> {
    public RA(let a!: Int64 = 10) {}
}

main() {
    var ra = RA<Int64>(a: 5)
    if (ra.a == 5) {
        return 0
    }
    return 1
}
