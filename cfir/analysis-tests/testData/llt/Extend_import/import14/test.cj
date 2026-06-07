// FILE: pkga.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg.a

public interface ValueType {
    prop code: String {
        get() {
            "in pkg.a"
        }
    }

    func fCode(): String {
        return "in pkg.a fCode"
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend Int64 <: ValueType {}<!>

// FILE: pkgb.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg.b

public interface ValueType {
    prop code: String {
        get() {
            "in pkg.b"
        }
    }

    func fCode(): String {
        return "in pkg.b fCode"
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend Int64 <: ValueType {}<!>

// FILE: test.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package pkg

import pkg.a.<!UNUSED_IMPORT!>ValueType as AValueType<!>
import pkg.b.<!UNUSED_IMPORT!>ValueType as BValueType<!>

main() {}
