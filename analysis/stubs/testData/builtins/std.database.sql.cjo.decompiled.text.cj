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
    prop name: std.core.String

    prop typeName: std.core.String

    prop length: Int64

    prop scale: Int64

    prop nullable: Bool

    prop displaySize: Int64
}

interface Connection <: std.core.Resource {
    prop state: std.database.sql.ConnectionState

    func getMetaData: std.collection.Map<std.core.String, std.core.String>

    func prepareStatement(sql: std.core.String): std.database.sql.Statement

    func createTransaction: std.database.sql.Transaction
}

enum ConnectionState <: std.core.Equatable<std.database.sql.ConnectionState> {
    | Broken

    | Closed

    | Connecting

    | Connected

    operator func ==(rhs: std.database.sql.ConnectionState): Bool

    operator func !=(rhs: std.database.sql.ConnectionState): Bool
}

interface Statement <: std.core.Resource {
    prop parameterColumnInfos: std.core.Array<std.database.sql.ColumnInfo>

    func setOption(key: std.core.String, value: std.core.String): Unit

    func update(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.UpdateResult

    func query(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.QueryResult

    func `set`(index: Int64, value: T): Unit

    func setNull(index: Int64): Unit

    func update: std.database.sql.UpdateResult

    func query: std.database.sql.QueryResult
}

interface Transaction {
    prop isoLevel: std.database.sql.TransactionIsoLevel

    prop accessMode: std.database.sql.TransactionAccessMode

    prop deferrableMode: std.database.sql.TransactionDeferrableMode

    func begin: Unit

    func commit: Unit

    func rollback: Unit

    func rollback(savePointName: std.core.String): Unit

    func save(savePointName: std.core.String): Unit

    func release(savePointName: std.core.String): Unit
}

interface Datasource <: std.core.Resource {
    func setOption(key: std.core.String, value: std.core.String): Unit

    func connect: std.database.sql.Connection
}

interface Driver {
    prop name: std.core.String

    prop version: std.core.String

    prop preferredPooling: Bool

    func `open`(connectionString: std.core.String, opts: std.core.Array<(std.core.String, std.core.String)>): std.database.sql.Datasource
}

class DriverManager {
    let _regMut: std.sync.Mutex

    let _drivers: std.collection.HashMap<std.core.String, std.database.sql.Driver>

    init() { /* compiled code */ }

    func register(driverName: std.core.String, driver: std.database.sql.Driver): Unit

    func deregister(driverName: std.core.String): Unit

    func getDriver(driverName: std.core.String): std.core.Option<std.database.sql.Driver>

    func drivers: std.core.Array<std.core.String>
}

interface Pool <: std.core.Resource {
    func acquire(timeout: std.core.Duration): std.core.Option<std.database.sql.Entry<T>>

    func release(entry: std.database.sql.Entry<T>): std.core.Option<Unit>

    func discard(entry: std.database.sql.Entry<T>): std.core.Option<Unit>
}

class Entry {
    var value_: T

    let pool: std.database.sql.Pool<T>

    var creationTime: std.core.Duration

    var lastAccessed: std.core.Duration

    var poolResetCount: Int32

    var isValid: Bool

    init(pool: std.database.sql.Pool<T>, value: T) { /* compiled code */ }

    prop value: T

    func release: std.core.Option<Unit>

    func idleDuration: std.core.Duration
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

    prop idleTimeout: std.core.Duration

    prop maxLifeTime: std.core.Duration

    prop keepaliveTime: std.core.Duration

    prop maxSize: Int32

    prop maxIdleSize: Int32

    var _connTimeout: std.core.Duration

    prop connectionTimeout: std.core.Duration

    func setOption(key: std.core.String, value: std.core.String): Unit

    func connect: std.database.sql.Connection

    func isClosed: Bool

    func close: Unit
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

    prop maxSize: Int32

    prop maxIdleSize: Int32

    func backgroundCheckIdleLife: Unit

    func checkLife: Unit

    func checkMaxLifeTime: Unit

    func checkIdleTimeout: Unit

    func getIdleEntry(timeout: std.core.Duration): std.database.sql.Entry<T>

    func acquire(timeout: std.core.Duration): std.core.Option<std.database.sql.Entry<T>>

    func createEntry: std.database.sql.Entry<T>

    func release(entry: std.database.sql.Entry<T>): std.core.Option<Unit>

    func discard(entry: std.database.sql.Entry<T>): std.core.Option<Unit>

    func isClosed: Bool

    func close: Unit

    operator func !=(rhs: std.database.sql.ResourcePool<T>): Bool

    operator func ==(rhs: std.database.sql.ResourcePool<T>): Bool

    func appendLegalIdleEntry(v: std.database.sql.Entry<T>, idleCount: Int64, list: std.collection.ArrayList<std.database.sql.Entry<T>>): Unit
}

class ProxyConnection <: std.core.Object & std.database.sql.Connection {
    var delegate: std.database.sql.Connection

    var poolEntry: std.database.sql.Entry<std.database.sql.Connection>

    var isReleased: std.sync.AtomicBool

    init(conn: std.database.sql.Connection, poolEntry: std.database.sql.Entry<std.database.sql.Connection>) { /* compiled code */ }

    prop state: std.database.sql.ConnectionState

    func getMetaData: std.collection.Map<std.core.String, std.core.String>

    func isClosed: Bool

    func close: Unit

    func prepareStatement(sql: std.core.String): std.database.sql.Statement

    func createTransaction: std.database.sql.Transaction
}

interface QueryResult <: std.core.Resource {
    prop columnInfos: std.core.Array<std.database.sql.ColumnInfo>

    func next(values: std.core.Array<std.database.sql.SqlDbType>): Bool

    func next: Bool

    func `get`(index: Int64): T

    func getOrNull(index: Int64): std.core.Option<T>
}

interface SqlDbType {
    prop name: std.core.String
}

interface SqlNullableDbType <: std.database.sql.SqlDbType {
}

class SqlChar <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    prop value: std.core.String

    prop name: std.core.String
}

class SqlNullableChar <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    prop value: std.core.Option<std.core.String>

    prop name: std.core.String
}

class SqlVarchar <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    prop value: std.core.String

    prop name: std.core.String
}

class SqlNullableVarchar <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    prop value: std.core.Option<std.core.String>

    prop name: std.core.String
}

class SqlBinary <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    prop value: std.core.Array<UInt8>

    prop name: std.core.String
}

class SqlNullableBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    prop value: std.core.Option<std.core.Array<UInt8>>

    prop name: std.core.String
}

class SqlVarBinary <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    prop value: std.core.Array<UInt8>

    prop name: std.core.String
}

class SqlNullableVarBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    prop value: std.core.Option<std.core.Array<UInt8>>

    prop name: std.core.String
}

class SqlClob <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    prop value: std.io.InputStream

    prop name: std.core.String
}

class SqlNullableClob <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    prop value: std.core.Option<std.io.InputStream>

    prop name: std.core.String
}

class SqlBlob <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    prop value: std.io.InputStream

    prop name: std.core.String
}

class SqlNullableBlob <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    prop value: std.core.Option<std.io.InputStream>

    prop name: std.core.String
}

class SqlBool <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Bool

    init(v: Bool) { /* compiled code */ }

    prop value: Bool

    prop name: std.core.String
}

class SqlNullableBool <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Bool>

    init(v: std.core.Option<Bool>) { /* compiled code */ }

    prop value: std.core.Option<Bool>

    prop name: std.core.String
}

class SqlByte <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int8

    init(v: Int8) { /* compiled code */ }

    prop value: Int8

    prop name: std.core.String
}

class SqlNullableByte <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int8>

    init(v: std.core.Option<Int8>) { /* compiled code */ }

    prop value: std.core.Option<Int8>

    prop name: std.core.String
}

class SqlSmallInt <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int16

    init(v: Int16) { /* compiled code */ }

    prop value: Int16

    prop name: std.core.String
}

class SqlNullableSmallInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int16>

    init(v: std.core.Option<Int16>) { /* compiled code */ }

    prop value: std.core.Option<Int16>

    prop name: std.core.String
}

class SqlInteger <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int32

    init(v: Int32) { /* compiled code */ }

    prop value: Int32

    prop name: std.core.String
}

class SqlNullableInteger <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int32>

    init(v: std.core.Option<Int32>) { /* compiled code */ }

    prop value: std.core.Option<Int32>

    prop name: std.core.String
}

class SqlBigInt <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Int64

    init(v: Int64) { /* compiled code */ }

    prop value: Int64

    prop name: std.core.String
}

class SqlNullableBigInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Int64>

    init(v: std.core.Option<Int64>) { /* compiled code */ }

    prop value: std.core.Option<Int64>

    prop name: std.core.String
}

class SqlReal <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Float32

    init(v: Float32) { /* compiled code */ }

    prop value: Float32

    prop name: std.core.String
}

class SqlNullableReal <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Float32>

    init(v: std.core.Option<Float32>) { /* compiled code */ }

    prop value: std.core.Option<Float32>

    prop name: std.core.String
}

class SqlDouble <: std.core.Object & std.database.sql.SqlDbType {
    var _value: Float64

    init(v: Float64) { /* compiled code */ }

    prop value: Float64

    prop name: std.core.String
}

class SqlNullableDouble <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<Float64>

    init(v: std.core.Option<Float64>) { /* compiled code */ }

    prop value: std.core.Option<Float64>

    prop name: std.core.String
}

class SqlDate <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    prop value: std.time.DateTime

    prop name: std.core.String
}

class SqlNullableDate <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    prop value: std.core.Option<std.time.DateTime>

    prop name: std.core.String
}

class SqlTime <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    prop value: std.time.DateTime

    prop name: std.core.String
}

class SqlNullableTime <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    prop value: std.core.Option<std.time.DateTime>

    prop name: std.core.String
}

class SqlTimeTz <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    prop value: std.time.DateTime

    prop name: std.core.String
}

class SqlNullableTimeTz <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    prop value: std.core.Option<std.time.DateTime>

    prop name: std.core.String
}

class SqlTimestamp <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    prop value: std.time.DateTime

    prop name: std.core.String
}

class SqlNullableTimestamp <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    prop value: std.core.Option<std.time.DateTime>

    prop name: std.core.String
}

class SqlInterval <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.core.Duration

    init(v: std.core.Duration) { /* compiled code */ }

    prop value: std.core.Duration

    prop name: std.core.String
}

class SqlNullableInterval <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.core.Duration>

    init(v: std.core.Option<std.core.Duration>) { /* compiled code */ }

    prop value: std.core.Option<std.core.Duration>

    prop name: std.core.String
}

class SqlDecimal <: std.core.Object & std.database.sql.SqlDbType {
    var _value: std.math.numeric.Decimal

    init(v: std.math.numeric.Decimal) { /* compiled code */ }

    prop value: std.math.numeric.Decimal

    prop name: std.core.String
}

class SqlNullableDecimal <: std.core.Object & std.database.sql.SqlNullableDbType {
    var _value: std.core.Option<std.math.numeric.Decimal>

    init(v: std.core.Option<std.math.numeric.Decimal>) { /* compiled code */ }

    prop value: std.core.Option<std.math.numeric.Decimal>

    prop name: std.core.String
}

class SqlException <: std.core.Exception {
    var _sqlState: std.core.String

    var _errorCode: Int64

    prop sqlState: std.core.String

    prop errorCode: Int64

    prop message: std.core.String

    init() { /* compiled code */ }

    init(message: std.core.String, sqlState: std.core.String, errorCode: Int64) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
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
    prop rowCount: Int64

    prop lastInsertId: Int64
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

    func toString: std.core.String

    operator func ==(rhs: std.database.sql.TransactionIsoLevel): Bool

    operator func !=(rhs: std.database.sql.TransactionIsoLevel): Bool

    func hashCode: Int64
}

enum TransactionAccessMode <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionAccessMode> {
    | Unspecified

    | ReadWrite

    | ReadOnly

    func toString: std.core.String

    operator func ==(rhs: std.database.sql.TransactionAccessMode): Bool

    operator func !=(rhs: std.database.sql.TransactionAccessMode): Bool

    func hashCode: Int64
}

enum TransactionDeferrableMode <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionDeferrableMode> {
    | Unspecified

    | Deferrable

    | NotDeferrable

    func toString: std.core.String

    operator func ==(rhs: std.database.sql.TransactionDeferrableMode): Bool

    operator func !=(rhs: std.database.sql.TransactionDeferrableMode): Bool

    func hashCode: Int64
}
