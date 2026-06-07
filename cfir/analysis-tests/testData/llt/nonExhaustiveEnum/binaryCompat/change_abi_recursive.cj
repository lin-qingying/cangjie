// FILE: change_abi_recursive.old.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

public enum A {
    B | C(Int64) | ...
}

public struct X {
    public X(let value: A) {}
}

public let m = A.B

// FILE: change_abi_recursive.new.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

public enum A {
    B | C(Int64) | D(X) | ...
}

public struct X {
    public X(let value: A) {}
}

public let m = A.D(X(B))

// FILE: change_abi_recursive.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import a.*

main() {
    match (m) {
        case B => 0
        case C(_) => 1
        case _ => 2
    }
}
