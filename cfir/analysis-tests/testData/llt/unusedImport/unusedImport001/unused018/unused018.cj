// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./a.cj ./b.cj ./c.cj

package d

import c.Type

public func test(typeNum: Type): Unit {
    match (typeNum) {
        case TypeA(a) => return
    }
}

main() {
}
