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
    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop typeName: std.core.String {
        get() { /* compiled code */ }
    }

    prop length: Int64 {
        get() { /* compiled code */ }
    }

    prop scale: Int64 {
        get() { /* compiled code */ }
    }

    prop nullable: Bool {
        get() { /* compiled code */ }
    }

    prop displaySize: Int64 {
        get() { /* compiled code */ }
    }
}

interface Connection <: std.core.Resource {
    prop state: std.database.sql.ConnectionState {
        get() { /* compiled code */ }
    }

    func getMetaData(): std.collection.Map<std.core.String, std.core.String> { /* compiled code */ }

    func prepareStatement(sql: std.core.String): std.database.sql.Statement { /* compiled code */ }

    func createTransaction(): std.database.sql.Transaction { /* compiled code */ }
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
    prop parameterColumnInfos: std.core.Array<std.database.sql.ColumnInfo> {
        get() { /* compiled code */ }
    }

    func setOption(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

    func update(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.UpdateResult { /* compiled code */ }

    func query(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.QueryResult { /* compiled code */ }

    func `set`(index: Int64, value: T): Unit { /* compiled code */ }

    func setNull(index: Int64): Unit { /* compiled code */ }

    func update(): std.database.sql.UpdateResult { /* compiled code */ }

    func query(): std.database.sql.QueryResult { /* compiled code */ }
}

interface Transaction {
    mut prop isoLevel: std.database.sql.TransactionIsoLevel {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop accessMode: std.database.sql.TransactionAccessMode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop deferrableMode: std.database.sql.TransactionDeferrableMode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func begin(): Unit { /* compiled code */ }

    func commit(): Unit { /* compiled code */ }

    func rollback(): Unit { /* compiled code */ }

    func rollback(savePointName: std.core.String): Unit { /* compiled code */ }

    func save(savePointName: std.core.String): Unit { /* compiled code */ }

    func release(savePointName: std.core.String): Unit { /* compiled code */ }
}

interface Datasource <: std.core.Resource {
    func setOption(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

    func connect(): std.database.sql.Connection { /* compiled code */ }
}

interface Driver {
    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop version: std.core.String {
        get() { /* compiled code */ }
    }

    prop preferredPooling: Bool {
        get() { /* compiled code */ }
    }

    func `open`(connectionString: std.core.String, opts: std.core.Array<(std.core.String, std.core.String)>): std.database.sql.Datasource { /* compiled code */ }
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

interface Pool <: std.core.Resource {
    func acquire(timeout: std.core.Duration): std.core.Option<std.database.sql.Entry<T>> { /* compiled code */ }

    func release(entry: std.database.sql.Entry<T>): std.core.Option<Unit> { /* compiled code */ }

    func discard(entry: std.database.sql.Entry<T>): std.core.Option<Unit> { /* compiled code */ }
}

class Entry {
    var value_: T

    let pool: std.database.sql.Pool<T>

    var creationTime: std.core.Duration

    var lastAccessed: std.core.Duration

    var poolResetCount: Int32

    var isValid: Bool

    init(pool: std.database.sql.Pool<T>, value: T) { /* compiled code */ }

    prop value: T {
        get() { /* compiled code */ }
    }

    func release(): std.core.Option<Unit> { /* compiled code */ }

    func idleDuration(): std.core.Duration { /* compiled code */ }
}

struct Options {
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

    mut prop idleTimeout: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop maxLifeTime: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keepaliveTime: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop maxSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop maxIdleSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    var _connTimeout: std.core.Duration

    mut prop connectionTimeout: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func setOption(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

    func connect(): std.database.sql.Connection { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }
}

class ResourcePool <: std.core.Object & std.database.sql.Pool<T> & std.core.Equatable<std.database.sql.ResourcePool<T>> {
    let mux: std.sync.ReentrantMutex

    let entryQueue: std.collection.concurrent.LinkedBlockingQueue<std.database.sql.Entry<T>>

    let idleEntry: std.collection.concurrent.LinkedBlockingQueue<std.database.sql.Entry<T>>

    var options: std.database.sql.Options<T>

    var ticker: std.sync.Timer

    var entryCount: Int32

    var isclosed: std.sync.AtomicBool

    init(options: std.database.sql.Options<T>) { /* compiled code */ }

    mut prop maxSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

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
    prop columnInfos: std.core.Array<std.database.sql.ColumnInfo> {
        get() { /* compiled code */ }
    }

    func next(values: std.core.Array<std.database.sql.SqlDbType>): Bool { /* compiled code */ }

    func next(): Bool { /* compiled code */ }

    func `get`(index: Int64): T { /* compiled code */ }

    func getOrNull(index: Int64): std.core.Option<T> { /* compiled code */ }
}

interface SqlDbType {
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

interface SqlNullableDbType <: std.database.sql.SqlDbType {
}

class SqlChar <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    mut prop value: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableChar <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    mut prop value: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlVarchar <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    mut prop value: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableVarchar <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    mut prop value: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlBinary <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    mut prop value: std.core.Array<UInt8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    mut prop value: std.core.Option<std.core.Array<UInt8>> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlVarBinary <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    mut prop value: std.core.Array<UInt8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableVarBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    mut prop value: std.core.Option<std.core.Array<UInt8>> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlClob <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    mut prop value: std.io.InputStream {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableClob <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    mut prop value: std.core.Option<std.io.InputStream> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlBlob <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    mut prop value: std.io.InputStream {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableBlob <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    mut prop value: std.core.Option<std.io.InputStream> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlBool <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Bool

    init(v: Bool) { /* compiled code */ }

    mut prop value: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableBool <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Bool>

    init(v: std.core.Option<Bool>) { /* compiled code */ }

    mut prop value: std.core.Option<Bool> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlByte <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int8

    init(v: Int8) { /* compiled code */ }

    mut prop value: Int8 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableByte <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int8>

    init(v: std.core.Option<Int8>) { /* compiled code */ }

    mut prop value: std.core.Option<Int8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlSmallInt <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int16

    init(v: Int16) { /* compiled code */ }

    mut prop value: Int16 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableSmallInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int16>

    init(v: std.core.Option<Int16>) { /* compiled code */ }

    mut prop value: std.core.Option<Int16> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlInteger <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int32

    init(v: Int32) { /* compiled code */ }

    mut prop value: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableInteger <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int32>

    init(v: std.core.Option<Int32>) { /* compiled code */ }

    mut prop value: std.core.Option<Int32> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlBigInt <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int64

    init(v: Int64) { /* compiled code */ }

    mut prop value: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableBigInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int64>

    init(v: std.core.Option<Int64>) { /* compiled code */ }

    mut prop value: std.core.Option<Int64> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlReal <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Float32

    init(v: Float32) { /* compiled code */ }

    mut prop value: Float32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableReal <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Float32>

    init(v: std.core.Option<Float32>) { /* compiled code */ }

    mut prop value: std.core.Option<Float32> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlDouble <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Float64

    init(v: Float64) { /* compiled code */ }

    mut prop value: Float64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableDouble <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Float64>

    init(v: std.core.Option<Float64>) { /* compiled code */ }

    mut prop value: std.core.Option<Float64> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlDate <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableDate <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlTime <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableTime <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlTimeTz <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableTimeTz <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlTimestamp <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableTimestamp <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlInterval <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Duration

    init(v: std.core.Duration) { /* compiled code */ }

    mut prop value: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableInterval <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Duration>

    init(v: std.core.Option<std.core.Duration>) { /* compiled code */ }

    mut prop value: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlDecimal <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.math.numeric.Decimal

    init(v: std.math.numeric.Decimal) { /* compiled code */ }

    mut prop value: std.math.numeric.Decimal {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlNullableDecimal <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.math.numeric.Decimal>

    init(v: std.core.Option<std.math.numeric.Decimal>) { /* compiled code */ }

    mut prop value: std.core.Option<std.math.numeric.Decimal> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

class SqlException <: std.core.Exception {
    var _sqlState: std.core.String

    var _errorCode: Int64

    prop sqlState: std.core.String {
        get() { /* compiled code */ }
    }

    prop errorCode: Int64 {
        get() { /* compiled code */ }
    }

    override prop message: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }

    init(message: std.core.String, sqlState: std.core.String, errorCode: Int64) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class SqlOption {
    let URL: std.core.String

    let Host: std.core.String

    let Username: std.core.String

    let Password: std.core.String

    let Driver: std.core.String

    let Database: std.core.String

    let Encoding: std.core.String

    let ConnectionTimeout: std.core.String

    let UpdateTimeout: std.core.String

    let QueryTimeout: std.core.String

    let FetchRows: std.core.String

    let SSLMode: std.core.String

    let SSLModePreferred: std.core.String

    let SSLModeDisabled: std.core.String

    let SSLModeRequired: std.core.String

    let SSLModeVerifyCA: std.core.String

    let SSLModeVerifyFull: std.core.String

    let SSLCA: std.core.String

    let SSLCert: std.core.String

    let SSLKey: std.core.String

    let SSLKeyPassword: std.core.String

    let SSLSni: std.core.String

    let Tls12Ciphersuites: std.core.String

    let Tls13Ciphersuites: std.core.String

    let TlsVersion: std.core.String

    init() { /* compiled code */ }
}

interface UpdateResult {
    prop rowCount: Int64 {
        get() { /* compiled code */ }
    }

    prop lastInsertId: Int64 {
        get() { /* compiled code */ }
    }
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
