// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

public open class Remote {
    private var remote_call: Bool

    protected init() {
        remote_call = true
    }

    public prop remoteCall: Bool {
        get() {
            remote_call
        }
    }
}
