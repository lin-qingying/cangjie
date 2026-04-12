// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.process
import std.collection.*
import std.fs.*
import std.io.*
import std.time.*
import std.sync.*


/**
* CurrentProcess provide the base func for current process.
*/
@Deprecated[message: "Use related global functions in the std.env instead."]
@!APILevel[since: "12", atomicservice : true]
public class CurrentProcess <: Process {
    @!APILevel[since: "12", atomicservice : true]
    public prop stdErr: OutputStream
    
    @!APILevel[since: "12", atomicservice : true]
    public prop stdOut: OutputStream
    
    @!APILevel[since: "12", atomicservice : true]
    public prop stdIn: InputStream
    
    @!APILevel[since: "12", atomicservice : true]
    public prop arguments: Array<String>
    
    @!APILevel[since: "12", atomicservice : true]
    public prop homeDirectory: Path
    
    @!APILevel[since: "12", atomicservice : true]
    public prop tempDirectory: Path
    
    @!APILevel[since: "12", atomicservice : true]
    public func getEnv(key: String): Option<String>
    
    @!APILevel[since: "12", atomicservice : true]
    public func setEnv(k: String, v: String): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public func removeEnv(k: String): Unit
    
    /**
    * Sets the function to be called before the program ends normally.
    * Function description: atexit() is used to set a function to be called before the program ends normally. When the program returns from main by calling exit(),
    * The function specified by the function parameter is called before exit() ends the program.
    * Do not use the atexit function of the C language. Otherwise, unexpected problems may arise.
    *
    * @param callback Called Before Normal Exit.
    *
    * since 0.24.3
    */
    @!APILevel[since: "12", atomicservice : true]
    public func atExit(callback: () -> Unit): Unit
    
    /**
    * exit() is used to terminate the execution of the current progetStdoutStreamcess and return the status parameter to the parent Process.
    * All buffer data of the Process is automatically written back and the files that are not closed are closed
    *
    * @param code Register the function to be executed before the Process exits.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func exit(code: Int64): Nothing
}

/*
* Process 
* 
* This is a lib for Process.
*/
@!APILevel[since: "12", atomicservice : true]
public open class Process {
    /** 
    * return a process instance for current process.The process instance can used to find processInfo and terminate 
    * current process. Also the process instance provide stdio, which can be used to read or write message.
    */
    @Deprecated[message: "CurrentProcess has been marked as deprecated, use related global functions in the std.env instead."]
    @!APILevel[since: "12", atomicservice : true]
    static public prop current: CurrentProcess
    
    @!APILevel[since: "12", atomicservice : true]
    public prop pid: Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public prop name: String
    
    @!APILevel[since: "12", atomicservice : true]
    public prop command: String
    
    // Process start time
    @!APILevel[since: "12", atomicservice : true]
    public prop startTime: DateTime
    
    // Time spent on system calls executed directly or indirectly by the program
    @!APILevel[since: "12", atomicservice : true]
    public prop systemTime: Duration
    
    // Time spent by the program itself and the subprocesses in the libraries it calls
    @!APILevel[since: "12", atomicservice : true]
    public prop userTime: Duration
    
    @Deprecated
    @!APILevel[since: "12", atomicservice : true]
    public open prop arguments: Array<String>
    
    @Deprecated[message: "Use global function `public func getCommandLine(): Array<String>` in std.env instead."]
    @!APILevel[since: "12", atomicservice : true]
    public prop commandLine: Array<String>
    
    @Deprecated[message: "Use global function `public func getHomeDirectory(): Path` in std.env instead."]
    @!APILevel[since: "12", atomicservice : true]
    public prop workingDirectory: Path
    
    @Deprecated[message: "Use global function `public func getVariables(): Array<(String, String)>` in std.env instead."]
    @!APILevel[since: "12", atomicservice : true]
    public prop environment: Map<String, String>
    
    /** 
    * return a process instance for an existing process which pid is giving by user.
    * The process instance can used to find processInfo and terminate the process.
    * We can not get stdio for any pid process, so that do not provide the stream attribute to operate stdio.
    */
    @Deprecated[message: "Use global function `public func findProcess(pid: Int64): Process` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public static func of(pid: Int64): Process
    
    /** 
    * start a new subProcess using command and arguments which provide by user. 
    *
    * @param command - vaild system command, should not be empty string and can not include Null character.
    * @param arguments - command arguments, each of the arguments should not be empty string and can not include Null character. 
    * @param workingDirectory - command working directory, if None, means inherit current process working directory.
    * @param environment - command working environment, if None, means inherit current process environment.
    * @param stdIn - stdIn redirect mode.
    * @param stdOut - stdOut redirect mode.
    * @param stdErr - stdErr redirect mode.
    *
    * @return SubProcess - a new subProcess instance.
    */
    @Deprecated[message: "Use global function `public func launch(command: String, arguments: Array<String>, workingDirectory!: ?Path = None, environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Inherit, stdErr!: ProcessRedirect = Inherit): SubProcess` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public static func start(command: String, arguments: Array<String>, 
            workingDirectory!: ?Path = None, 
            environment!: ?Map<String, String> = None,
            stdIn!: ProcessRedirect = Inherit, 
            stdOut!: ProcessRedirect = Inherit, 
            stdErr!: ProcessRedirect = Inherit): SubProcess
    
    /** 
    * func run is combine the start and wait.
    * provide a convenient way for user, which want start a subprocess and wait the command exec return code.
    */
    @Deprecated[message: "Use global function `public func execute(command: String, arguments: Array<String>, workingDirectory!: ?Path = None, environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Inherit, stdErr!: ProcessRedirect = Inherit, timeout!: ?Duration = None): Int64` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public static func run(command: String, arguments: Array<String>, 
            workingDirectory!: ?Path = None, 
            environment!: ?Map<String, String> = None, 
            stdIn!: ProcessRedirect = Inherit, 
            stdOut!: ProcessRedirect = Inherit, 
            stdErr!: ProcessRedirect = Inherit, 
            timeout!: ?Duration = None): Int64
    
    /** 
    * func runOutput is combine the start and waitOutput.
    * provide a convenient way for user, which want start a subprocess and wait the command exec output and err result.
    */
    @Deprecated[message: "Use global function `public func executeWithOutput(command: String, arguments: Array<String>, workingDirectory!: ?Path = None, environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Pipe, stdErr!: ProcessRedirect = Pipe): (Int64, Array<Byte>, Array<Byte>)` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public static func runOutput(command: String, arguments: Array<String>, 
            workingDirectory!: ?Path = None, 
            environment!: ?Map<String, String> = None, 
            stdIn!: ProcessRedirect = Inherit, 
            stdOut!: ProcessRedirect = Pipe, 
            stdErr!: ProcessRedirect = Pipe): (Int64, Array<Byte>, Array<Byte>)
    
    @!APILevel[since: "12", atomicservice : true]
    public func isAlive(): Bool
    
    /**
    * terminate process
    * when terminate the subprocess, the spawn which open to wait subprocess 
    * will modify status and change stdio stream to ByteBuffer let user can read 
    * stdOut & stdErr unread data after subprocess be killed.
    *
    * @param force - true terminated forcibly
    *                false terminated normally
    *
    * @throws ProcessException - if the pid invaild
    * @return Unit
    */
    @!APILevel[since: "12", atomicservice : true]
    public func terminate(force!: Bool = false): Unit
}

/** 
* Supported process redirect mode.
* Pipe: create a pipe between parent current and child process, which can provide a way to exchange info.
* Inherit: use the same stdio with the current process.
* FromFile: redirect stdio to a specified file which define by user.
* Discard: discard the stdio, redirect to system specific "null file".
*/
@!APILevel[since: "12", atomicservice : true]
public enum ProcessRedirect {
    @!APILevel[since: "12", atomicservice : true]
    Inherit |
    @!APILevel[since: "12", atomicservice : true]
    Pipe |
    @!APILevel[since: "12", atomicservice : true]
    FromFile(File) |
    @!APILevel[since: "12", atomicservice : true]
    Discard
}

@!APILevel[since: "12", atomicservice : true]
public func findProcess(pid: Int64): Process

/** 
* start a new subProcess using command and arguments which provide by user. 
*
* @param command - vaild system command, should not be empty string and can not include Null character.
* @param arguments - command arguments, each of the arguments should not be empty string and can not include Null character. 
* @param workingDirectory - command working directory, if None, means inherit current process working directory.
* @param environment - command working environment, if None, means inherit current process environment.
* @param stdIn - stdIn redirect mode.
* @param stdOut - stdOut redirect mode.
* @param stdErr - stdErr redirect mode.
*
* @return SubProcess - a new subProcess instance.
*/
@!APILevel[since: "12", atomicservice : true]
public func launch(command: String, arguments: Array<String>, workingDirectory!: ?Path = None, environment!: ?Map<String, String> = None,
    stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Inherit, stdErr!: ProcessRedirect = Inherit): SubProcess

/** 
* func execute is combine the launch and wait.
* provide a convenient way for user, which want start a subprocess and wait the command exec return code.
*/
@!APILevel[since: "12", atomicservice : true]
public func execute(command: String, arguments: Array<String>, workingDirectory!: ?Path = None,
    environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Inherit,
    stdErr!: ProcessRedirect = Inherit, timeout!: ?Duration = None): Int64

/** 
* func executeWithOutput is combine the start and waitOutput.
* provide a convenient way for user, which want start a subprocess and wait the command exec output and err result.
*/
@!APILevel[since: "12", atomicservice : true]
public func executeWithOutput(command: String, arguments: Array<String>, workingDirectory!: ?Path = None,
    environment!: ?Map<String, String> = None, stdIn!: ProcessRedirect = Inherit, stdOut!: ProcessRedirect = Pipe,
    stdErr!: ProcessRedirect = Pipe): (Int64, Array<Byte>, Array<Byte>)

/**
* ProcessException
* 
*/
@!APILevel[since: "12", atomicservice : true]
public class ProcessException <: IOException {
    @!APILevel[since: "12", atomicservice : true]
    public init(message: String)
}

/**
* SubProcess
*
*/
@!APILevel[since: "12", atomicservice : true]
public class SubProcess <: Process {
    @!APILevel[since: "12", atomicservice : true]
    public prop stdInPipe: OutputStream
    
    @!APILevel[since: "12", atomicservice : true]
    public prop stdOutPipe: InputStream
    
    @!APILevel[since: "12", atomicservice : true]
    public prop stdErrPipe: InputStream
    
    @Deprecated[message: "Use `public prop stdErrPipe: InputStream` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public prop stdErr: InputStream
    
    @Deprecated[message: "Use `public prop stdOutPipe: InputStream` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public prop stdOut: InputStream
    
    @Deprecated[message: "Use `public prop stdInPipe: OutputStream` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public prop stdIn: OutputStream
    
    /**
    * wait func developer should read the outputStream or errStream first and then invoke this func.
    * if developer invoke wait func first and then read stream, it can be occur unexpected concurrency results.
    *
    * @param timeout
    * @return Int64
    */
    @!APILevel[since: "12", atomicservice : true]
    public func wait(timeout!: ?Duration = None): Int64
    
    /**
    * waitOutput func is applicable to scenarios where number of bytes in output or error stream is small.
    * If you need to deal with big size within output or error stream, please use stdOut or stdErr prop and wait func.
    *
    * @return (exitCode: Int64, out: Array<Byte>, err: Array<Byte>)
    */
    @!APILevel[since: "12", atomicservice : true]
    public func waitOutput(): (Int64, Array<Byte>, Array<Byte>)
}

