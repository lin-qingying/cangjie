// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

open class A {
    var a_ = ""
    public mut open prop a: String {
        get() {
            a_
        }
        set(v) {
            a_ = v
        }
    }
    init() {
        println("A")
    }
}

class B <: A {
    public mut override prop a: String {
        set(v) {}
    }
}

interface AA {
    mut prop a: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    mut prop b: Int64 {
        get() {
            0
        }
        set(v) {}
    }
}

class BB <: AA {
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public mut override prop a: Int64 {
        set(v) {}
    }<!>
    <!PROPERTY_MUST_IMPLEMENT_BOTH!>public mut override prop b: Int64 {
        get() {
            1
        }
    }<!>
}

main(): Int64 {
    return 0
}
