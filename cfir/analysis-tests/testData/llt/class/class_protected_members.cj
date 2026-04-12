// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

public open class C {
    protected var id: String = ""
    protected func setID(id: String) {
        this.id = id
        print(id)
        return id
    }

    public func SetID(id: String) {
        setID(id)
        return id
    }
}

public open class D <: C {
    protected var myId: String = "abc"
}

public class E <: D {
    public func test2() {
        id = "hahah2" + myId
        setID(id)
    }
}

main(): Int64 {
    var e = E()
    if (e.test2() == "hahah2abc" && e.SetID("bye") == "bye") {
        return 0
    } else {
        return 1
    }
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
