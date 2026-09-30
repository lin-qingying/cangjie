// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.database.sql

import std.core.*
import std.collection.Map
import std.collection.HashMap
import std.sync.Mutex
import std.sort.sort
import std.random.Random
import std.time.DateTime
import std.sync.AtomicBool
import std.collection.ArrayList
import std.collection.concurrent.BlockingQueue
import std.collection.concurrent.LinkedBlockingQueue
import std.sync.Timer
import std.sync.CatchupStyle
import std.sync.ReentrantMutex
import std.io.InputStream
import std.math.numeric.Decimal

interface ColumnInfo {
    @Annotation
    abstract open prop name: std.core.String

    @Annotation
    abstract open prop typeName: std.core.String

    @Annotation
    abstract open prop length: Int64

    @Annotation
    abstract open prop scale: Int64

    @Annotation
    abstract open prop nullable: Bool

    @Annotation
    abstract open prop displaySize: Int64
}

interface Connection <: std.core.Resource {
    @Annotation
    abstract open prop state: std.database.sql.ConnectionState

    abstract open func getMetaData(): std.collection.Map<std.core.String, std.core.String>

    abstract open func prepareStatement(sql: std.core.String): std.database.sql.Statement

    abstract open func createTransaction(): std.database.sql.Transaction
}

enum ConnectionState <: std.core.Equatable<std.database.sql.ConnectionState> {
    | Broken

    | Closed

    | Connecting

    | Connected

    operator func ==(rhs: std.database.sql.ConnectionState): Bool { /* compiled code */ }

    operator func !=(rhs: std.database.sql.ConnectionState): Bool { /* compiled code */ }
}

interface Statement <: std.core.Resource {
    @Annotation
    abstract open prop parameterColumnInfos: std.core.Array<std.database.sql.ColumnInfo>

    abstract open func setOption(key: std.core.String, value: std.core.String): Unit

    @Deprecated[message: "Use memeber funcation `func update(): UpdateResult` instead."]
    abstract open func update(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.UpdateResult

    @Deprecated[message: "Use memeber funcation `func query(): QueryResult` instead."]
    abstract open func query(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.QueryResult

    abstract open func set<T>(index: Int64, value: T): Unit

    abstract open func setNull(index: Int64): Unit

    abstract open func update(): std.database.sql.UpdateResult

    abstract open func query(): std.database.sql.QueryResult
}

interface Transaction {
    @Annotation
    abstract open mut prop isoLevel: std.database.sql.TransactionIsoLevel

    @Annotation
    abstract open mut prop accessMode: std.database.sql.TransactionAccessMode

    @Annotation
    abstract open mut prop deferrableMode: std.database.sql.TransactionDeferrableMode

    abstract open func begin(): Unit

    abstract open func commit(): Unit

    abstract open func rollback(): Unit

    abstract open func rollback(savePointName: std.core.String): Unit

    abstract open func save(savePointName: std.core.String): Unit

    abstract open func release(savePointName: std.core.String): Unit
}

interface Datasource <: std.core.Resource {
    abstract open func setOption(key: std.core.String, value: std.core.String): Unit

    abstract open func connect(): std.database.sql.Connection
}

interface Driver {
    @Annotation
    abstract open prop name: std.core.String

    @Annotation
    abstract open prop version: std.core.String

    @Annotation
    abstract open prop preferredPooling: Bool

    abstract open func open(connectionString: std.core.String, opts: std.core.Array<(std.core.String, std.core.String)>): std.database.sql.Datasource
}

class DriverManager {
    let _regMut: std.sync.Mutex

    let _drivers: std.collection.HashMap<std.core.String, std.database.sql.Driver>

    init() { /* compiled code */ }

    static func register(driverName: std.core.String, driver: std.database.sql.Driver): Unit { /* compiled code */ }

    static func deregister(driverName: std.core.String): Unit { /* compiled code */ }

    static func getDriver(driverName: std.core.String): std.core.Option<std.database.sql.Driver> { /* compiled code */ }

    static func drivers(): std.core.Array<std.core.String> { /* compiled code */ }
}

interface Pool<T> <: std.core.Resource {
    abstract open func acquire(timeout: std.core.Duration): std.core.Option<std.database.sql.Entry<T>>

    abstract open func release(entry: std.database.sql.Entry<T>): std.core.Option<Unit>

    abstract open func discard(entry: std.database.sql.Entry<T>): std.core.Option<Unit>
}

class Entry<T> {
    var value_: T

    let pool: std.database.sql.Pool<T>

    var creationTime: std.core.Duration

    @Annotation
    var lastAccessed: std.core.Duration

    @Annotation
    var poolResetCount: Int32

    @Annotation
    var isValid: Bool

    init(pool: std.database.sql.Pool<T>, value: T) { /* compiled code */ }

    @Annotation
    prop value: T {
        get() { /* compiled code */ }
    }

    func release(): std.core.Option<Unit> { /* compiled code */ }

    func idleDuration(): std.core.Duration { /* compiled code */ }
}

struct Options<T> {
    var maxIdleSize: Int32

    var maxSize: Int32

    var idleTimeout: std.core.Duration

    var maxLifeTime: std.core.Duration

    var keepaliveTime: std.core.Duration

    let constructor: () -> std.core.Option<T>

    let destructor: (T) -> std.core.Option<Unit>

    init(maxIdleSize: Int32, maxSize: Int32, idleTimeout: std.core.Duration, maxLifeTime: std.core.Duration, keepaliveTime: std.core.Duration, constructor: () -> std.core.Option<T>, destructor: (T) -> std.core.Option<Unit>) { /* compiled code */ }
}

class PooledDatasource <: std.core.Object & std.database.sql.Datasource {
    let datasource: std.database.sql.Datasource

    var pool: std.database.sql.ResourcePool<std.database.sql.Connection>

    let isclosed: std.sync.AtomicBool

    init(datasource: std.database.sql.Datasource) { /* compiled code */ }

    @Annotation
    mut prop idleTimeout: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    mut prop maxLifeTime: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    mut prop keepaliveTime: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    mut prop maxSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    mut prop maxIdleSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    var _connTimeout: std.core.Duration

    @Annotation
    mut prop connectionTimeout: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func setOption(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

    func connect(): std.database.sql.Connection { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }
}

class ResourcePool<T> <: std.core.Object & std.database.sql.Pool<T> & std.core.Equatable<std.database.sql.ResourcePool<T>> {
    @Annotation
    let mux: std.sync.ReentrantMutex

    let entryQueue: std.collection.concurrent.LinkedBlockingQueue<std.database.sql.Entry<T>>

    let idleEntry: std.collection.concurrent.LinkedBlockingQueue<std.database.sql.Entry<T>>

    var options: std.database.sql.Options<T>

    var ticker: std.sync.Timer

    var entryCount: Int32

    @Annotation
    var isclosed: std.sync.AtomicBool

    init(options: std.database.sql.Options<T>) { /* compiled code */ }

    @Annotation
    mut prop maxSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    mut prop maxIdleSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func backgroundCheckIdleLife(): Unit { /* compiled code */ }

    func checkLife(): Unit { /* compiled code */ }

    func checkMaxLifeTime(): Unit { /* compiled code */ }

    func checkIdleTimeout(): Unit { /* compiled code */ }

    func getIdleEntry(timeout: std.core.Duration): std.database.sql.Entry<T> { /* compiled code */ }

    func acquire(timeout: std.core.Duration): std.core.Option<std.database.sql.Entry<T>> { /* compiled code */ }

    func createEntry(): std.database.sql.Entry<T> { /* compiled code */ }

    func release(entry: std.database.sql.Entry<T>): std.core.Option<Unit> { /* compiled code */ }

    func discard(entry: std.database.sql.Entry<T>): std.core.Option<Unit> { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }

    operator func !=(rhs: std.database.sql.ResourcePool<T>): Bool { /* compiled code */ }

    operator func ==(rhs: std.database.sql.ResourcePool<T>): Bool { /* compiled code */ }

    func appendLegalIdleEntry(v: std.database.sql.Entry<T>, idleCount: Int64, list: std.collection.ArrayList<std.database.sql.Entry<T>>): Unit { /* compiled code */ }
}

class ProxyConnection <: std.core.Object & std.database.sql.Connection {
    var delegate: std.database.sql.Connection

    var poolEntry: std.database.sql.Entry<std.database.sql.Connection>

    var isReleased: std.sync.AtomicBool

    init(conn: std.database.sql.Connection, poolEntry: std.database.sql.Entry<std.database.sql.Connection>) { /* compiled code */ }

    @Annotation
    prop state: std.database.sql.ConnectionState {
        get() { /* compiled code */ }
    }

    func getMetaData(): std.collection.Map<std.core.String, std.core.String> { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }

    func prepareStatement(sql: std.core.String): std.database.sql.Statement { /* compiled code */ }

    func createTransaction(): std.database.sql.Transaction { /* compiled code */ }
}

interface QueryResult <: std.core.Resource {
    @Annotation
    abstract open prop columnInfos: std.core.Array<std.database.sql.ColumnInfo>

    @Deprecated[message: "Use memeber funcation `func next(): Bool` instead."]
    abstract open func next(values: std.core.Array<std.database.sql.SqlDbType>): Bool

    abstract open func next(): Bool

    abstract open func get<T>(index: Int64): T

    abstract open func getOrNull<T>(index: Int64): std.core.Option<T>
}

@Deprecated
interface SqlDbType {
    @Annotation
    abstract open prop name: std.core.String
}

@Deprecated
interface SqlNullableDbType <: std.database.sql.SqlDbType {
}

@Deprecated[message: "Use `String` instead."]
class SqlChar <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?String` instead."]
class SqlNullableChar <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `String` instead."]
class SqlVarchar <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?String` instead."]
class SqlNullableVarchar <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Array<Byte>` instead."]
class SqlBinary <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Array<UInt8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Array<Byte>` instead."]
class SqlNullableBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.core.Array<UInt8>> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Array<Byte>` instead."]
class SqlVarBinary <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Array<UInt8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Array<Byte>` instead."]
class SqlNullableVarBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.core.Array<UInt8>> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `InputStream` instead."]
class SqlClob <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    @Annotation
    mut prop value: std.io.InputStream {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?InputStream` instead."]
class SqlNullableClob <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.io.InputStream> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `InputStream` instead."]
class SqlBlob <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    @Annotation
    mut prop value: std.io.InputStream {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?InputStream` instead."]
class SqlNullableBlob <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.io.InputStream> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Bool` instead."]
class SqlBool <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Bool

    init(v: Bool) { /* compiled code */ }

    @Annotation
    mut prop value: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Bool` instead."]
class SqlNullableBool <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Bool>

    init(v: std.core.Option<Bool>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<Bool> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int8` instead."]
class SqlByte <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int8

    init(v: Int8) { /* compiled code */ }

    @Annotation
    mut prop value: Int8 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int8` instead."]
class SqlNullableByte <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int8>

    init(v: std.core.Option<Int8>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<Int8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int16` instead."]
class SqlSmallInt <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int16

    init(v: Int16) { /* compiled code */ }

    @Annotation
    mut prop value: Int16 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int16` instead."]
class SqlNullableSmallInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int16>

    init(v: std.core.Option<Int16>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<Int16> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int32` instead."]
class SqlInteger <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int32

    init(v: Int32) { /* compiled code */ }

    @Annotation
    mut prop value: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int32` instead."]
class SqlNullableInteger <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int32>

    init(v: std.core.Option<Int32>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<Int32> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int64` instead."]
class SqlBigInt <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int64

    init(v: Int64) { /* compiled code */ }

    @Annotation
    mut prop value: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int64` instead."]
class SqlNullableBigInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int64>

    init(v: std.core.Option<Int64>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<Int64> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Float32` instead."]
class SqlReal <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Float32

    init(v: Float32) { /* compiled code */ }

    @Annotation
    mut prop value: Float32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Float32` instead."]
class SqlNullableReal <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Float32>

    init(v: std.core.Option<Float32>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<Float32> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Float64` instead."]
class SqlDouble <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Float64

    init(v: Float64) { /* compiled code */ }

    @Annotation
    mut prop value: Float64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Float64` instead."]
class SqlNullableDouble <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Float64>

    init(v: std.core.Option<Float64>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<Float64> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
class SqlDate <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    @Annotation
    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
class SqlNullableDate <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
class SqlTime <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    @Annotation
    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
class SqlNullableTime <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
class SqlTimeTz <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    @Annotation
    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
class SqlNullableTimeTz <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
class SqlTimestamp <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    @Annotation
    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
class SqlNullableTimestamp <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Duration` instead."]
class SqlInterval <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Duration

    init(v: std.core.Duration) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Duration` instead."]
class SqlNullableInterval <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Duration>

    init(v: std.core.Option<std.core.Duration>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Decimal` instead."]
class SqlDecimal <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.math.numeric.Decimal

    init(v: std.math.numeric.Decimal) { /* compiled code */ }

    @Annotation
    mut prop value: std.math.numeric.Decimal {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Decimal` instead."]
class SqlNullableDecimal <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.math.numeric.Decimal>

    init(v: std.core.Option<std.math.numeric.Decimal>) { /* compiled code */ }

    @Annotation
    mut prop value: std.core.Option<std.math.numeric.Decimal> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

open class SqlException <: std.core.Exception {
    var _sqlState: std.core.String

    var _errorCode: Int64

    @Annotation
    prop sqlState: std.core.String {
        get() { /* compiled code */ }
    }

    @Annotation
    prop errorCode: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    override prop message: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }

    init(message: std.core.String, sqlState: std.core.String, errorCode: Int64) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class SqlOption {
    @Annotation
    const URL: std.core.String

    @Annotation
    const Host: std.core.String

    @Annotation
    const Username: std.core.String

    @Annotation
    const Password: std.core.String

    @Annotation
    const Driver: std.core.String

    @Annotation
    const Database: std.core.String

    @Annotation
    const Encoding: std.core.String

    @Annotation
    const ConnectionTimeout: std.core.String

    @Annotation
    const UpdateTimeout: std.core.String

    @Annotation
    const QueryTimeout: std.core.String

    @Annotation
    const FetchRows: std.core.String

    @Annotation
    const SSLMode: std.core.String

    @Annotation
    const SSLModePreferred: std.core.String

    @Annotation
    const SSLModeDisabled: std.core.String

    @Annotation
    const SSLModeRequired: std.core.String

    @Annotation
    const SSLModeVerifyCA: std.core.String

    @Annotation
    const SSLModeVerifyFull: std.core.String

    @Annotation
    const SSLCA: std.core.String

    @Annotation
    const SSLCert: std.core.String

    @Annotation
    const SSLKey: std.core.String

    @Annotation
    const SSLKeyPassword: std.core.String

    @Annotation
    const SSLSni: std.core.String

    @Annotation
    const Tls12Ciphersuites: std.core.String

    @Annotation
    const Tls13Ciphersuites: std.core.String

    @Annotation
    const TlsVersion: std.core.String

    init() { /* compiled code */ }
}

interface UpdateResult {
    @Annotation
    abstract open prop rowCount: Int64

    @Annotation
    abstract open prop lastInsertId: Int64
}

enum TransactionIsoLevel <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionIsoLevel> {
    | Unspecified

    | ReadCommitted

    | ReadUncommitted

    | RepeatableRead

    | Snapshot

    | Serializable

    | Linearizable

    | Chaos

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(rhs: std.database.sql.TransactionIsoLevel): Bool { /* compiled code */ }

    operator func !=(rhs: std.database.sql.TransactionIsoLevel): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }
}

enum TransactionAccessMode <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionAccessMode> {
    | Unspecified

    | ReadWrite

    | ReadOnly

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(rhs: std.database.sql.TransactionAccessMode): Bool { /* compiled code */ }

    operator func !=(rhs: std.database.sql.TransactionAccessMode): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }
}

enum TransactionDeferrableMode <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionDeferrableMode> {
    | Unspecified

    | Deferrable

    | NotDeferrable

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(rhs: std.database.sql.TransactionDeferrableMode): Bool { /* compiled code */ }

    operator func !=(rhs: std.database.sql.TransactionDeferrableMode): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }
}
