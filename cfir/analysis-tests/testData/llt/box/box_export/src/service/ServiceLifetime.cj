// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.service

/*
服务声明周期
 */
public enum ServiceLifetime {
    Singleton | Scoped | Transient

    public func isScoped() {
        match (this) {
            case Scoped => true
            case _ => false
        }
    }

    public func isTransient() {
        match (this) {
            case Transient => true
            case _ => false
        }
    }

    public func isSingleton() {
        match (this) {
            case Singleton => true
            case _ => false
        }
    }
}
