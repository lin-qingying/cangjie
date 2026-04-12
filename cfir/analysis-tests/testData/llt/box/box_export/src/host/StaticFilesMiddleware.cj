// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.host

import std.fs.{File, exists}
import stdx.net.http.{HttpContext, HttpHeaders}

public class StaticFilesMiddleware <: Middleware {
    private let _environment: HostEnvironment

    init(environment: HostEnvironment) {
        _environment = environment
    }

    public func invoke(context: HttpContext, next: () -> Unit): Unit {
        let path = context.request.url.path
        let file = "${_environment.contentRootPath}/wwwroot${path}"
        if (exists(file)) {
            let bytes = File.readFrom(file)
            context.responseBuilder.body(bytes)
            context.responseBuilder.status(200)
        } else {
            next()
        }
    }
}
