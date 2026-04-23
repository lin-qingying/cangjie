// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Boo {
    mut prop m: Int32 {
        get() {
            return 1
        }
        set(input) {
            ()
        }
    }
}

class Coo <: Boo {
    public mut override prop m: Int32 {
        get() {
            return (10)
        }
        set(input) {
            ()
        }
    }
}

main() {
    return Int64(Coo().m - 10)
}
