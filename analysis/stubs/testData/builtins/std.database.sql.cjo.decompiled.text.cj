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

public interface ColumnInfo {
    public open prop name: std.core.String

    public open prop typeName: std.core.String

    public open prop length: Int64

    public open prop scale: Int64

    public open prop nullable: Bool

    public open prop displaySize: Int64
}

public interface Connection <: std.core.Resource {
    public open prop state: std.database.sql.ConnectionState

    public open func getMetaData(): std.collection.Map<std.core.String, std.core.String>

    public open func prepareStatement(sql: std.core.String): std.database.sql.Statement

    public open func createTransaction(): std.database.sql.Transaction
}

public enum ConnectionState <: std.core.Equatable<std.database.sql.ConnectionState> {
    | Broken

    | Closed

    | Connecting

    | Connected

    public operator func ==(rhs: std.database.sql.ConnectionState): Bool { /* compiled code */ }

    public operator func !=(rhs: std.database.sql.ConnectionState): Bool { /* compiled code */ }
}

public interface Statement <: std.core.Resource {
    public open prop parameterColumnInfos: std.core.Array<std.database.sql.ColumnInfo>

    public open func setOption(key: std.core.String, value: std.core.String): Unit

    @Deprecated[message: "Use memeber funcation `func update(): UpdateResult` instead."]
    public open func update(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.UpdateResult

    @Deprecated[message: "Use memeber funcation `func query(): QueryResult` instead."]
    public open func query(params: std.core.Array<std.database.sql.SqlDbType>): std.database.sql.QueryResult

    public open func set<T>(index: Int64, value: T): Unit

    public open func setNull(index: Int64): Unit

    public open func update(): std.database.sql.UpdateResult

    public open func query(): std.database.sql.QueryResult
}

public interface Transaction {
    public open mut prop isoLevel: std.database.sql.TransactionIsoLevel

    public open mut prop accessMode: std.database.sql.TransactionAccessMode

    public open mut prop deferrableMode: std.database.sql.TransactionDeferrableMode

    public open func begin(): Unit

    public open func commit(): Unit

    public open func rollback(): Unit

    public open func rollback(savePointName: std.core.String): Unit

    public open func save(savePointName: std.core.String): Unit

    public open func release(savePointName: std.core.String): Unit
}

public interface Datasource <: std.core.Resource {
    public open func setOption(key: std.core.String, value: std.core.String): Unit

    public open func connect(): std.database.sql.Connection
}

public interface Driver {
    public open prop name: std.core.String

    public open prop version: std.core.String

    public open prop preferredPooling: Bool

    public open func open(connectionString: std.core.String, opts: std.core.Array<(std.core.String, std.core.String)>): std.database.sql.Datasource
}

public class DriverManager {
    internal static let _regMut: std.sync.Mutex

    internal static let _drivers: std.collection.HashMap<std.core.String, std.database.sql.Driver>

    init() { /* compiled code */ }

    public static func register(driverName: std.core.String, driver: std.database.sql.Driver): Unit { /* compiled code */ }

    public static func deregister(driverName: std.core.String): Unit { /* compiled code */ }

    public static func getDriver(driverName: std.core.String): std.core.Option<std.database.sql.Driver> { /* compiled code */ }

    public static func drivers(): std.core.Array<std.core.String> { /* compiled code */ }
}

internal interface Pool<T> <: std.core.Resource {
    public open func acquire(timeout: std.core.Duration): std.core.Option<std.database.sql.Entry<T>>

    public open func release(entry: std.database.sql.Entry<T>): std.core.Option<Unit>

    public open func discard(entry: std.database.sql.Entry<T>): std.core.Option<Unit>
}

internal class Entry<T> {
    internal var value_: T

    internal let pool: std.database.sql.Pool<T>

    internal var creationTime: std.core.Duration

    internal var lastAccessed: std.core.Duration = COMPILED_CODE /* compiled code */

    internal var poolResetCount: Int32 = COMPILED_CODE /* compiled code */

    internal var isValid: Bool = COMPILED_CODE /* compiled code */

    init(pool: std.database.sql.Pool<T>, value: T) { /* compiled code */ }

    internal prop value: T {
        get() { /* compiled code */ }
    }

    internal func release(): std.core.Option<Unit> { /* compiled code */ }

    internal func idleDuration(): std.core.Duration { /* compiled code */ }
}

internal struct Options<T> {
    internal var maxIdleSize: Int32

    internal var maxSize: Int32

    internal var idleTimeout: std.core.Duration

    internal var maxLifeTime: std.core.Duration

    internal var keepaliveTime: std.core.Duration

    internal let constructor: () -> std.core.Option<T>

    internal let destructor: (T) -> std.core.Option<Unit>

    init(maxIdleSize: Int32, maxSize: Int32, idleTimeout: std.core.Duration, maxLifeTime: std.core.Duration, keepaliveTime: std.core.Duration, constructor: () -> std.core.Option<T>, destructor: (T) -> std.core.Option<Unit>) { /* compiled code */ }
}

public class PooledDatasource <: std.core.Object & std.database.sql.Datasource {
    private let datasource: std.database.sql.Datasource

    private var pool: std.database.sql.ResourcePool<std.database.sql.Connection>

    private let isclosed: std.sync.AtomicBool

    init(datasource: std.database.sql.Datasource) { /* compiled code */ }

    public mut prop idleTimeout: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop maxLifeTime: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keepaliveTime: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop maxSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop maxIdleSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal var _connTimeout: std.core.Duration

    public mut prop connectionTimeout: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func setOption(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

    public func connect(): std.database.sql.Connection { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }
}

internal class ResourcePool<T> <: std.core.Object & std.database.sql.Pool<T> & std.core.Equatable<std.database.sql.ResourcePool<T>> {
    internal let mux: std.sync.ReentrantMutex = COMPILED_CODE /* compiled code */

    internal let entryQueue: std.collection.concurrent.LinkedBlockingQueue<std.database.sql.Entry<T>>

    internal let idleEntry: std.collection.concurrent.LinkedBlockingQueue<std.database.sql.Entry<T>>

    internal var options: std.database.sql.Options<T>

    internal var ticker: std.sync.Timer

    internal var entryCount: Int32

    internal var isclosed: std.sync.AtomicBool = COMPILED_CODE /* compiled code */

    init(options: std.database.sql.Options<T>) { /* compiled code */ }

    public mut prop maxSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop maxIdleSize: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal func backgroundCheckIdleLife(): Unit { /* compiled code */ }

    internal func checkLife(): Unit { /* compiled code */ }

    internal func checkMaxLifeTime(): Unit { /* compiled code */ }

    internal func checkIdleTimeout(): Unit { /* compiled code */ }

    internal func getIdleEntry(timeout: std.core.Duration): std.database.sql.Entry<T> { /* compiled code */ }

    public func acquire(timeout: std.core.Duration): std.core.Option<std.database.sql.Entry<T>> { /* compiled code */ }

    internal func createEntry(): std.database.sql.Entry<T> { /* compiled code */ }

    public func release(entry: std.database.sql.Entry<T>): std.core.Option<Unit> { /* compiled code */ }

    public func discard(entry: std.database.sql.Entry<T>): std.core.Option<Unit> { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }

    public operator func !=(rhs: std.database.sql.ResourcePool<T>): Bool { /* compiled code */ }

    public operator func ==(rhs: std.database.sql.ResourcePool<T>): Bool { /* compiled code */ }

    internal func appendLegalIdleEntry(v: std.database.sql.Entry<T>, idleCount: Int64, list: std.collection.ArrayList<std.database.sql.Entry<T>>): Unit { /* compiled code */ }
}

internal class ProxyConnection <: std.core.Object & std.database.sql.Connection {
    internal var delegate: std.database.sql.Connection

    internal var poolEntry: std.database.sql.Entry<std.database.sql.Connection>

    internal var isReleased: std.sync.AtomicBool

    init(conn: std.database.sql.Connection, poolEntry: std.database.sql.Entry<std.database.sql.Connection>) { /* compiled code */ }

    public prop state: std.database.sql.ConnectionState {
        get() { /* compiled code */ }
    }

    public func getMetaData(): std.collection.Map<std.core.String, std.core.String> { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }

    public func prepareStatement(sql: std.core.String): std.database.sql.Statement { /* compiled code */ }

    public func createTransaction(): std.database.sql.Transaction { /* compiled code */ }
}

public interface QueryResult <: std.core.Resource {
    public open prop columnInfos: std.core.Array<std.database.sql.ColumnInfo>

    @Deprecated[message: "Use memeber funcation `func next(): Bool` instead."]
    public open func next(values: std.core.Array<std.database.sql.SqlDbType>): Bool

    public open func next(): Bool

    public open func get<T>(index: Int64): T

    public open func getOrNull<T>(index: Int64): std.core.Option<T>
}

@Deprecated
public interface SqlDbType {
    public open prop name: std.core.String
}

@Deprecated
public interface SqlNullableDbType <: std.database.sql.SqlDbType {
}

@Deprecated[message: "Use `String` instead."]
public class SqlChar <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    public mut prop value: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?String` instead."]
public class SqlNullableChar <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `String` instead."]
public class SqlVarchar <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.core.String

    init(v: std.core.String) { /* compiled code */ }

    public mut prop value: std.core.String {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?String` instead."]
public class SqlNullableVarchar <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.core.String>

    init(v: std.core.Option<std.core.String>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Array<Byte>` instead."]
public class SqlBinary <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    public mut prop value: std.core.Array<UInt8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Array<Byte>` instead."]
public class SqlNullableBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.core.Array<UInt8>> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Array<Byte>` instead."]
public class SqlVarBinary <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.core.Array<UInt8>

    init(v: std.core.Array<UInt8>) { /* compiled code */ }

    public mut prop value: std.core.Array<UInt8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Array<Byte>` instead."]
public class SqlNullableVarBinary <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.core.Array<UInt8>>

    init(v: std.core.Option<std.core.Array<UInt8>>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.core.Array<UInt8>> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `InputStream` instead."]
public class SqlClob <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    public mut prop value: std.io.InputStream {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?InputStream` instead."]
public class SqlNullableClob <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.io.InputStream> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `InputStream` instead."]
public class SqlBlob <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.io.InputStream

    init(v: std.io.InputStream) { /* compiled code */ }

    public mut prop value: std.io.InputStream {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?InputStream` instead."]
public class SqlNullableBlob <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.io.InputStream>

    init(v: std.core.Option<std.io.InputStream>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.io.InputStream> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Bool` instead."]
public class SqlBool <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: Bool

    init(v: Bool) { /* compiled code */ }

    public mut prop value: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Bool` instead."]
public class SqlNullableBool <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<Bool>

    init(v: std.core.Option<Bool>) { /* compiled code */ }

    public mut prop value: std.core.Option<Bool> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int8` instead."]
public class SqlByte <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: Int8

    init(v: Int8) { /* compiled code */ }

    public mut prop value: Int8 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int8` instead."]
public class SqlNullableByte <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<Int8>

    init(v: std.core.Option<Int8>) { /* compiled code */ }

    public mut prop value: std.core.Option<Int8> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int16` instead."]
public class SqlSmallInt <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: Int16

    init(v: Int16) { /* compiled code */ }

    public mut prop value: Int16 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int16` instead."]
public class SqlNullableSmallInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<Int16>

    init(v: std.core.Option<Int16>) { /* compiled code */ }

    public mut prop value: std.core.Option<Int16> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int32` instead."]
public class SqlInteger <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: Int32

    init(v: Int32) { /* compiled code */ }

    public mut prop value: Int32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int32` instead."]
public class SqlNullableInteger <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<Int32>

    init(v: std.core.Option<Int32>) { /* compiled code */ }

    public mut prop value: std.core.Option<Int32> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Int64` instead."]
public class SqlBigInt <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: Int64

    init(v: Int64) { /* compiled code */ }

    public mut prop value: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Int64` instead."]
public class SqlNullableBigInt <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<Int64>

    init(v: std.core.Option<Int64>) { /* compiled code */ }

    public mut prop value: std.core.Option<Int64> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Float32` instead."]
public class SqlReal <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: Float32

    init(v: Float32) { /* compiled code */ }

    public mut prop value: Float32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Float32` instead."]
public class SqlNullableReal <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<Float32>

    init(v: std.core.Option<Float32>) { /* compiled code */ }

    public mut prop value: std.core.Option<Float32> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Float64` instead."]
public class SqlDouble <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: Float64

    init(v: Float64) { /* compiled code */ }

    public mut prop value: Float64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Float64` instead."]
public class SqlNullableDouble <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<Float64>

    init(v: std.core.Option<Float64>) { /* compiled code */ }

    public mut prop value: std.core.Option<Float64> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
public class SqlDate <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    public mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
public class SqlNullableDate <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
public class SqlTime <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    public mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
public class SqlNullableTime <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
public class SqlTimeTz <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    public mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
public class SqlNullableTimeTz <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `DateTime` instead."]
public class SqlTimestamp <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.time.DateTime

    init(v: std.time.DateTime) { /* compiled code */ }

    public mut prop value: std.time.DateTime {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?DateTime` instead."]
public class SqlNullableTimestamp <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.time.DateTime>

    init(v: std.core.Option<std.time.DateTime>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.time.DateTime> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Duration` instead."]
public class SqlInterval <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.core.Duration

    init(v: std.core.Duration) { /* compiled code */ }

    public mut prop value: std.core.Duration {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Duration` instead."]
public class SqlNullableInterval <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.core.Duration>

    init(v: std.core.Option<std.core.Duration>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `Decimal` instead."]
public class SqlDecimal <: std.core.Object & std.database.sql.SqlDbType {
    internal var _value: std.math.numeric.Decimal

    init(v: std.math.numeric.Decimal) { /* compiled code */ }

    public mut prop value: std.math.numeric.Decimal {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `?Decimal` instead."]
public class SqlNullableDecimal <: std.core.Object & std.database.sql.SqlNullableDbType {
    internal var _value: std.core.Option<std.math.numeric.Decimal>

    init(v: std.core.Option<std.math.numeric.Decimal>) { /* compiled code */ }

    public mut prop value: std.core.Option<std.math.numeric.Decimal> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

public open class SqlException <: std.core.Exception {
    internal var _sqlState: std.core.String

    internal var _errorCode: Int64

    public prop sqlState: std.core.String {
        get() { /* compiled code */ }
    }

    public prop errorCode: Int64 {
        get() { /* compiled code */ }
    }

    public override prop message: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }

    init(message: std.core.String, sqlState: std.core.String, errorCode: Int64) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class SqlOption {
    public static const URL: std.core.String = COMPILED_CODE /* compiled code */

    public static const Host: std.core.String = COMPILED_CODE /* compiled code */

    public static const Username: std.core.String = COMPILED_CODE /* compiled code */

    public static const Password: std.core.String = COMPILED_CODE /* compiled code */

    public static const Driver: std.core.String = COMPILED_CODE /* compiled code */

    public static const Database: std.core.String = COMPILED_CODE /* compiled code */

    public static const Encoding: std.core.String = COMPILED_CODE /* compiled code */

    public static const ConnectionTimeout: std.core.String = COMPILED_CODE /* compiled code */

    public static const UpdateTimeout: std.core.String = COMPILED_CODE /* compiled code */

    public static const QueryTimeout: std.core.String = COMPILED_CODE /* compiled code */

    public static const FetchRows: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLMode: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLModePreferred: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLModeDisabled: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLModeRequired: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLModeVerifyCA: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLModeVerifyFull: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLCA: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLCert: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLKey: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLKeyPassword: std.core.String = COMPILED_CODE /* compiled code */

    public static const SSLSni: std.core.String = COMPILED_CODE /* compiled code */

    public static const Tls12Ciphersuites: std.core.String = COMPILED_CODE /* compiled code */

    public static const Tls13Ciphersuites: std.core.String = COMPILED_CODE /* compiled code */

    public static const TlsVersion: std.core.String = COMPILED_CODE /* compiled code */

    init() { /* compiled code */ }
}

public interface UpdateResult {
    public open prop rowCount: Int64

    public open prop lastInsertId: Int64
}

public enum TransactionIsoLevel <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionIsoLevel> {
    | Unspecified

    | ReadCommitted

    | ReadUncommitted

    | RepeatableRead

    | Snapshot

    | Serializable

    | Linearizable

    | Chaos

    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(rhs: std.database.sql.TransactionIsoLevel): Bool { /* compiled code */ }

    public operator func !=(rhs: std.database.sql.TransactionIsoLevel): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }
}

public enum TransactionAccessMode <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionAccessMode> {
    | Unspecified

    | ReadWrite

    | ReadOnly

    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(rhs: std.database.sql.TransactionAccessMode): Bool { /* compiled code */ }

    public operator func !=(rhs: std.database.sql.TransactionAccessMode): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }
}

public enum TransactionDeferrableMode <: std.core.ToString & std.core.Hashable & std.core.Equatable<std.database.sql.TransactionDeferrableMode> {
    | Unspecified

    | Deferrable

    | NotDeferrable

    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(rhs: std.database.sql.TransactionDeferrableMode): Bool { /* compiled code */ }

    public operator func !=(rhs: std.database.sql.TransactionDeferrableMode): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }
}
