// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package adapter.lsk.pb

internal import protobuf.protobuf.*

public enum ServerEventType <: Enum<ServerEventType> {
    | EVENTTYPE_UNDEFINED

    public operator func ==(value: ServerEventType) {
        match ((this, value)) {
            case (EVENTTYPE_UNDEFINED, EVENTTYPE_UNDEFINED) => true
            case _ => false
        }
    }
}
