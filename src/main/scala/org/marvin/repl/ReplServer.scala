package org.marvin.repl

import java.util.logging.Logger

import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}
import main.scala.org.marvin.repl.{CommandRequest, LoggerReply, NotebookGrpc, ToolboxGrpc}

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.{Process, ProcessIO, ProcessLogger}


object ReplServer {
  private val logger = Logger.getLogger(classOf[ReplServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new ReplServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class ReplServer(executionContext: ExecutionContext) {
  self =>
  private[this] var server: Server = null

  val CC = new java.io.File("src/main/resources/serverCC.crt")
  val PK = new java.io.File("src/main/resources/serverPK.key")

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(ReplServer.port)
      //.useTransportSecurity(CC, PK)
      .addService(NotebookGrpc.bindService(new NotebookService, executionContext))
      .build
      .start

    ReplServer.logger.info(
      "Server started, listening on " + ReplServer.port)
    sys.addShutdownHook {
      System.err.println(
        "*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class ReplService extends ToolboxGrpc.Toolbox {
    override def toolboxControl(req: CommandRequest): Future[LoggerReply] = {
      val reply = LoggerReply(logInfo = "Hello " + req.cmd)
      Future.successful(reply)
    }
  }

  private class NotebookService extends NotebookGrpc.Notebook {
    override def notebookControl(req: CommandRequest, responseObserver: StreamObserver[LoggerReply]): Unit = {

      println("Notebook subiu!!")

      val pb = Process(req.cmd)

      while (true) {
        Thread.sleep(200)
        var lines = pb.lineStream
        for (line <- lines)
          responseObserver.onNext(LoggerReply(logInfo = line))
      }
    }

  }

}
