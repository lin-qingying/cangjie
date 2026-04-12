// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.host

import std.fs.{File, exists}
import stdx.net.http.{HttpContext, HttpHeaders}

public class DefaultFilesMiddleware <: Middleware {
    private let _files: Array<String> = ["index.html", "index.htm", "default.html", "default.htm"]
    private let _environment: HostEnvironment

    init(environment: HostEnvironment) {
        _environment = environment
    }

    public func invoke(context: HttpContext, next: () -> Unit): Unit {
        if (context.request.url.path == "/" && let Some(file) <- find()) {
            let bytes = File.readFrom(file)
            context.responseBuilder.body(bytes)
            context.responseBuilder.status(200)
            context.responseBuilder.header("content-type", "text/html; charset=ISO-8859-1")
        } else {
            next()
        }
    }

    private func find(): ?String {
        for (file in _files) {
            let path = "${_environment.contentRootPath}/wwwroot/${file}"
            if (exists(path)) {
                return path
            }
        }
        None
    }
}
