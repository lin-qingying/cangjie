// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package dep

import ohos.labels.*

@!APILevel[since: "9"]
public func f9(): Unit {
}

@!APILevel[since: "11"]
public func f11(): Unit {
}

// 没写，相当于 @!APILevel[since: "0"]
public func f0(): Unit {
}
