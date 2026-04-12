// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: config host service
// EXEC: %compiler %import-cangjie-stdx %cmp_opt %compile_lib_opt -g --no-sub-pkg -p ./config -o asp.config.%middle
// EXEC: %compiler %import-cangjie-stdx %cmp_opt %compile_lib_opt -g --no-sub-pkg -p ./service asp.config.%middle -o asp.service.%middle
// EXEC: %compiler %import-cangjie-stdx %cmp_opt %compile_lib_opt -g --no-sub-pkg -p ./host asp.service.%middle asp.config.%middle -o asp.host.%middle
// (CJNATIVE) EXEC-PIPE: %compiler %cmp_opt -g %f asp.host.%middle asp.service.%middle asp.config.%middle %import-cangjie-stdx

package asp

import asp.host.WebHost
import asp.config.Configuration

main(args: Array<String>): Int64 {
    var builder = WebHost.createBuilder(args)

    if (builder.environment.isDev()) {
        println("${builder.configuration.getConnectionString("mysql").getOrThrow()}")
    }
    //注册服务
    builder
        .services
        .addScoped<DbConnection, MySqlConnection> {
            sp =>
            let configuration = sp.getRequiredService<Configuration>()
            return MySqlConnection(configuration.getConnectionString("mysql").getOrThrow())
        }

    //判断环境
    if (builder.environment.isDev()) {
        println("这是开发环境")
    }

    let app = builder.build()
    //使用默认文件中间件
    app.useDefaultFiles()
    //使用静态文件中间件
    app.useStaticFiles()
    //从子容器解析服务
    try (scope = app.services.createScope()) {
        let connection = scope.getRequiredService<DbConnection>()
        connection.open()
    }
    //配置请求管道
    app.use {
        _, next =>
        println("中间件A开始...")
        next()
        println("中间件A结束...")
    }
    app.use {
        _, next =>
        println("中间件B开始...")
        next()
        println("中间件B结束...")
    }
    //运行主机
    app.run()
    return 0
}

public interface DbConnection <: ToString & Resource {
    func open() {
    }
}

public class MySqlConnection <: DbConnection {
    let _connectionString: String

    public init(connectionString: String) {
        _connectionString = connectionString
    }

    public func toString() {
        _connectionString
    }

    public func open() {
        println("连接已打开${_connectionString}...")
    }

    public func close() {
        println("连接已释放${_connectionString}...")
    }

    public func isClosed() {
        return false
    }
}
