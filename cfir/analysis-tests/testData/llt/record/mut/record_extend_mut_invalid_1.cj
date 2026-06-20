// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C1 {
    public var i: Int64 = 0
}

extend C1 {
    public <!MUT_ONLY_ON_FUNCTION!>mut<!> func foo() {
        this.i = 1
    }
}

main() {
    return 1
}
