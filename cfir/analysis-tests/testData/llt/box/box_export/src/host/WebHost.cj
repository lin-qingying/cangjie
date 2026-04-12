// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.host

import std.collection.ArrayList
import stdx.net.http.HttpContext
import asp.config.{Configuration, ConfigurationManager}
import asp.service.{ServiceProvider, ServiceProviderBuilder}

/*
Web主机
 */
public class WebHost {
    let _services: ServiceProvider
    let _environment: HostEnvironment
    let _configuration: Configuration
    let _middlewares = ArrayList<(RequestDelegate) -> RequestDelegate>()

    protected init(services: ServiceProvider, environment: HostEnvironment, configuration: Configuration) {
        _services = services
        _environment = environment
        _configuration = configuration
    }

    public prop services: ServiceProvider {
        get() {
            _services
        }
    }

    public prop environment: HostEnvironment {
        get() {
            _environment
        }
    }

    public prop configuration: Configuration {
        get() {
            _configuration
        }
    }

    /*
    创建Web主机构建器
     */
    public static func createBuilder(args: Array<String>) {
        let environment = createHostEnvironment(args)

        let configuration = createConfigurationManager(args, environment)

        let services = ServiceProviderBuilder()
            .addSingleton<HostEnvironment>(environment)
            .addSingleton<Configuration>(configuration.build())

        return WebHostBuilder(services, environment, configuration)
    }

    private static func createHostEnvironment(args: Array<String>) {
        let configuration = ConfigurationManager() {
            builder => builder.addEnvVars("cangjie").addJsonFile("./appsettings.json", true).addArgOpts(args)
        }.build()
        return HostEnvironment(configuration)
    }

    /*
    按照如下顺序加载配置（从最低优先级到最高优先级）
        1. 以cangjie开头的环境变量
        2. appsettings.json文件
        3. appsettings.${environment.name}.json
        4. 命令行参数
     */
    private static func createConfigurationManager(args: Array<String>, environment: HostEnvironment) {
        return ConfigurationManager() {
            builder => builder
                .addEnvVars("cangjie")
                .addJsonFile("${environment.contentRootPath}/appsettings.json", true)
                .addJsonFile("${environment.contentRootPath}/appsettings.${environment.name}.json", true)
                .addArgOpts(args)
        }
    }

    public func use(middleware: (RequestDelegate) -> RequestDelegate) {
        _middlewares.add(middleware)
    }

    public func use(middleware: (HttpContext, () -> Unit) -> Unit) {
        use {
            next => {
                context => middleware(context) {
                    next(context)
                }
            }
        }
    }

    public func use<TMiddleware>(middleware: TMiddleware) where TMiddleware <: Middleware {
        use {
            context, next => middleware.invoke(context, next)
        }
    }

    public func useDefaultFiles() {
        use(DefaultFilesMiddleware(_environment))
    }

    public func useStaticFiles() {
        use(StaticFilesMiddleware(_environment))
    }

    public func run() {
        run("http://127.0.0.1:80")
    }

    public func run(url: String) {
        logging(url)
        WebServer(build()).start(url)
    }

    private func build() {
        var app: RequestDelegate = {
            context => context.responseBuilder.status(404)
        }
        var index = _middlewares.size - 1;
        while (index >= 0) {
            let middleware = _middlewares[index]
            app = middleware(app)
            index--
        }
        return app
    }

    private func logging(url: String) {
        println("Now listening on: ${url}")
        println("Hosting environment: ${_environment.name}")
        println("Content root path: ${_environment.contentRootPath}")
        println("Application started. Press Ctrl+C to shut down.")
    }
}
