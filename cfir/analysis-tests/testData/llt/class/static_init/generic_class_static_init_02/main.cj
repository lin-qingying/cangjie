// FILE: pkg.cj
package pkg

public var initCount = 0
public open class A<T> {
    static let a : Unit = {=> initCount++}()
}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: pkg1.cj
package pkg1
internal import pkg.*

let _ = A<Int64>()


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: pkg2.cj
package pkg2

internal import pkg.*

let _ = A<Rune>()


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// FILE: main.cj
internal import pkg.*
internal import pkg1.*
internal import pkg2.*

main() {
    let _ = (A<Int64>(), A<Rune>())
    if (initCount != 1) {
        return 1
    }
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
