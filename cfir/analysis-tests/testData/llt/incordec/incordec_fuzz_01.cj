


// Was ICE, should quit normally

internal import std.console.*
internal import std.fs.*
internal import stdx.log.*
internal import stdx.logger.*
import std.console.Console

main() {
   let logger: SimpleLogger = SimpleLogger(Console.stdOut)
   var fs = File("black1.txt", OpenMode.%s---%d---%tnd)
   logger.setOutput(fs)
   logger.level = LogLevel.ERROR
   logger.log(LogLevel.OFF,"======OFFmsg========")
   logger.log(LogLevel.ERROR,"======ERRORmsg========")
   logger.log(LogLevel.INFO,"======INFOmsg========")
   logger.log(LogLevel.DEBUG,"======DEBUGmsg========")
   logger.log(LogLevel.WARN,"======WARNmsg========")
   logger.log(LogLevel.ALL,"======ALLmsg========")
   fs.close()
   0
}
