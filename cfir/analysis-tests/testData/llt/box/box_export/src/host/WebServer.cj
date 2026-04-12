// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.host

import stdx.net.http.*
import stdx.encoding.url.URL
import std.convert.Parsable

class WebServer {
    let _app: RequestDelegate

    init(app: RequestDelegate) {
        _app = app
    }

    public func start(url: String) {
        let urlParsed = URL.parse(url)
        let server = ServerBuilder()
            .addr(urlParsed.hostName)
            .port(UInt16.parse(urlParsed.port.toString()))
            .distributor(WebRequestDistributor(_app))
            .build()
        server.serve()
    }
}

private class WebRequestDistributor <: HttpRequestDistributor {
    private let _app: RequestDelegate

    init(app: RequestDelegate) {
        _app = app
    }

    public func register(_: String, _: HttpRequestHandler): Unit {
    }

    public func register(_: String, _: (HttpContext) -> Unit): Unit {
    }

    public func distribute(_: String) {
        return FuncHandler(_app)
    }
}
