// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Coo {
    public mut prop d: Int32 {
        get() {
            d = 2
            return 12
        }
        set(input) {
            ()
        }
    }
}

main(): Int64 {
    if (Coo().d == 12) {
        return 0
    }
    return 1
}
