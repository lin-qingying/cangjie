// FILE: pkg.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface aa {
    prop hi: String
}

public open class bb {
    public prop hi: String {
        get() {
            hiStr
        }
    }

    var hiStr: String = "hi"

    public init(s: String) {
        hiStr = s
    }
}

public open class cc <: bb & aa {
    public init(s: String) {
        super(s)
    }
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg.*

class dd <: cc {
    init() {
        super("dd")
    }

    public func SayHi() {
        return hi // ok
    }
}

main(): Int64 {
    let str = dd().SayHi()
    if (str != "dd") {
        return 1
    }
    return 0
}
