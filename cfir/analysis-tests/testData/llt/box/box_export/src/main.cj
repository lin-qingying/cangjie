// FILE: host/DefautFilesMiddleware.cj


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

// FILE: host/HostEnvironment.cj


package asp.host

import std.process.Process
import asp.config.Configuration

/*
Host环境
 */
public class HostEnvironment {
    private let _configuration: Configuration

    protected init(configuration: Configuration) {
        _configuration = configuration
    }

    /*
    环境名称
     */
    public prop name: String {
        get() {
            return _configuration.get("environment") ?? "prod"
        }
    }

    /*
    是否生产环境
     */
    public func isProd() {
        name == "prod"
    }

    /*
    是否开发环境
     */
    public func isDev() {
        name == "dev"
    }

    /*
    是否测试环境
     */
    public func isTest() {
        name == "test"
    }

    public func isEnvironment(name: String) {
        this.name == name
    }

    public prop contentRootPath: String {
        get() {
            _configuration.get("contentRootPath") ?? Process.current.workingDirectory.toString()
        }
    }
}

// FILE: host/Middleware.cj


package asp.host

import stdx.net.http.HttpContext

public interface Middleware {
    func invoke(context: HttpContext, next: () -> Unit): Unit
}

// FILE: host/RequestDelegate.cj


package asp.host

import stdx.net.http.HttpContext

public type RequestDelegate = (HttpContext) -> Unit

// FILE: host/StaticFilesMiddleware.cj


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

// FILE: host/WebHost.cj


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

// FILE: host/WebHostBuilder.cj


package asp.host

import asp.service.ServiceProviderBuilder
import asp.config.ConfigurationManager

public class WebHostBuilder {
    private let _services: ServiceProviderBuilder
    private let _environment: HostEnvironment
    private let _configuration: ConfigurationManager

    protected init(services: ServiceProviderBuilder, environment: HostEnvironment, configuration: ConfigurationManager) {
        _services = services
        _environment = environment
        _configuration = configuration
    }

    public prop services: ServiceProviderBuilder {
        get() {
            _services
        }
    }

    public prop environment: HostEnvironment {
        get() {
            _environment
        }
    }

    public prop configuration: ConfigurationManager {
        get() {
            _configuration
        }
    }

    public func build() {
        let service = _services.build()
        let configuration = _configuration.build()
        WebHost(service, _environment, configuration)
    }
}

// FILE: host/WebServer.cj


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

// FILE: config/ArgOptsConfigurationSource.cj


package asp.config

import std.collection.HashMap

/*
命令行参数配置源
 */
class ArgOptsConfigurationSource <: ConfigurationSource {
    let _args: Array<String>

    init(args: Array<String>) {
        _args = args
    }

    public func build() {
        let items = HashMap<String, String>()
        for (item in _args) {
            let keyValue = item.split('=', 2)
            if (keyValue.size < 2) {
                continue
            }
            items.add(keyValue[0].trimEnd().trimStart(), keyValue[1].trimEnd().trimStart())
        }
        return items
    }
}

// FILE: config/Configuration.cj


package asp.config

/*
配置核心接口
 */
public interface Configuration <: Iterable<(String, String)> {
    operator func [](name: String): String
    func get(name: String): Option<String>
    func getConnectionString(name: String): ?String
}

// FILE: config/ConfigurationBuilder.cj


package asp.config

import std.collection.HashMap
import std.io.{IOStream}

/*
配置构建接口
 */
public interface ConfigurationBuilder {
    func add(source: ConfigurationSource): ConfigurationBuilder
    func addMemory(items: HashMap<String, String>): ConfigurationBuilder
    func addArgOpts(args: Array<String>): ConfigurationBuilder
    func addEnvVars(prefix: String): ConfigurationBuilder
    func addJsonStream(stream: IOStream): ConfigurationBuilder
    func addJsonFile(path: String, optional: Bool): ConfigurationBuilder
    func build(): Configuration
}

// FILE: config/ConfigurationManager.cj


package asp.config

import std.collection.HashMap
import std.io.{InputStream, StringReader, IOStream}
import std.fs.{exists, File, OpenMode}

/*
配置管理器
用于构建配置和读取配置
 */
public class ConfigurationManager <: Configuration & ConfigurationBuilder {
    let _items = HashMap<String, String>()

    public init() {
    }

    public init(builder: (ConfigurationBuilder) -> Unit) {
        builder(this)
    }

    public func iterator() {
        return _items.iterator()
    }

    public operator func [](name: String) {
        return _items[name]
    }

    public func get(name: String) {
        return _items.get(name)
    }

    public func getConnectionString(name: String) {
        return get("connectionStrings:${name}")
    }

    public func add(source: ConfigurationSource): ConfigurationBuilder {
        let items = source.build()
        _items.add(all: items)
        return this
    }

    public func addMemory(items: HashMap<String, String>): ConfigurationBuilder {
        add(MemoryConfigurationSource(items))
        return this
    }

    public func addArgOpts(args: Array<String>): ConfigurationBuilder {
        add(ArgOptsConfigurationSource(args))
        return this
    }

    public func addEnvVars(prefix: String): ConfigurationBuilder {
        add(EnvVarsConfigurationSource(prefix))
        return this
    }

    public func addJsonStream(stream: IOStream): ConfigurationBuilder {
        let sr = StringReader(stream)
        let json = sr.readToEnd()
        add(JosnConfigurationSource(json))
        return this
    }

    public func addJsonFile(path: String, optional: Bool): ConfigurationBuilder {
        if (optional && !exists(path)) {
            return this
        }
        try (file = File(path, OpenMode.Read)) {
            addJsonStream(file)
        }
        return this
    }

    public func build(): Configuration {
        return this
    }
}

// FILE: config/ConfigurationSource.cj


package asp.config

import std.collection.HashMap

/*
配置源
 */
public abstract class ConfigurationSource {
    public func build(): HashMap<String, String>
}

// FILE: config/EnvVarsConfigurationSource.cj


package asp.config

import std.process.Process
import std.collection.HashMap

/*
环境变量配置源
 */
class EnvVarsConfigurationSource <: ConfigurationSource {
    let _prefix: String

    init(prefix: String) {
        _prefix = prefix.toAsciiUpper()
    }

    public func build() {
        let items = HashMap<String, String>()
        let envVars = Process.current.environment
        for ((key, value) in envVars) {
            if (!key.toAsciiUpper().startsWith(_prefix)) {
                continue
            }
            let name = key.toAsciiUpper().removePrefix("${_prefix}_").toAsciiLower()
            items.add(name, value)
        }
        return items
    }
}

// FILE: config/JosnConfigurationSource.cj


package asp.config

import std.fs.{exists, File}
import stdx.encoding.json.{JsonValue, JsonObject, JsonArray, JsonKind}
import std.collection.{HashMap, HashSet, collectArray, enumerate}

/*
Json配置源
 */
class JosnConfigurationSource <: ConfigurationSource {
    let _json: String
    let _keys = HashSet<String>()
    let _items = HashMap<String, String>()

    init(json: String) {
        _json = json
    }

    public func build() {
        let jsonValue = JsonValue.fromStr(_json)
        parse(jsonValue)
        return _items
    }

    func parse(jsonValue: JsonValue): Unit {
        match (jsonValue.kind()) {
            case JsonKind.JsArray => parseArray(jsonValue.asArray())
            case JsonKind.JsObject => parseObject(jsonValue.asObject())
            case _ => parseValue(jsonValue)
        }
    }

    func parseObject(jsonObject: JsonObject) {
        for ((key, value) in jsonObject.getFields()) {
            _keys.add(key)
            parse(value)
            _keys.remove(key)
        }
    }

    func parseArray(jsonArray: JsonArray) {
        for ((i, item) in jsonArray.getItems() |> enumerate) {
            _keys.add("${i}")
            parse(item)
            _keys.remove("${i}")
        }
    }

    func parseValue(jsonValue: JsonValue): Unit {
        match (jsonValue.kind()) {
            case JsonKind.JsBool => addValue(jsonValue.asBool().toString())
            case JsonKind.JsNull => addValue(jsonValue.asNull().toString())
            case JsonKind.JsString => addValue(jsonValue.asString().getValue())
            case JsonKind.JsInt => addValue(jsonValue.asInt().getValue().toString())
            case JsonKind.JsFloat => addValue(jsonValue.asFloat().getValue().toString())
            case _ => Exception()
        }
    }

    func addValue(value: String) {
        let key = String.join(_keys |> collectArray, delimiter: ":")
        _items.add(key, value)
    }
}

// FILE: config/MemoryConfigurationSource.cj


package asp.config

import std.collection.HashMap

/*
内存配置源
 */
class MemoryConfigurationSource <: ConfigurationSource {
    private let _items: HashMap<String, String>

    init(items: HashMap<String, String>) {
        _items = items
    }

    public func build() {
        _items
    }
}

// FILE: main.cj



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
