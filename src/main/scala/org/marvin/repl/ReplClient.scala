package org.marvin.repl

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import io.grpc.netty.{GrpcSslContexts, NettyChannelBuilder}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, StatusRuntimeException}
import main.scala.org.marvin.repl.NotebookGrpc.NotebookStub
import main.scala.org.marvin.repl.{CommandRequest, LoggerReply, NotebookGrpc}

object ReplClient {
  def apply(host: String, port: Int): ReplClient = {
    val channel: ManagedChannel = NettyChannelBuilder
      .forAddress(host, port)
      .sslContext(GrpcSslContexts.forClient().trustManager(new java.io.File("src/main/resources/serverCC.crt")).build)
      //.usePlaintext(true)
      .build
    val stub = NotebookGrpc.stub(channel)
    new ReplClient(channel, stub)
  }

  def main(args: Array[String]): Unit = {
    val client = ReplClient("localhost", 50051)
    //    try {
    val user = args.headOption.getOrElse("jupyter notebook")
    client.sendCMD(user)
    client.channel.awaitTermination(100, TimeUnit.SECONDS)
    //    } finally {
    //      client.shutdown()
    //    }
  }
}

class ReplClient private(private val channel: ManagedChannel, private val stub: NotebookStub) {
  private[this] val logger = Logger.getLogger(classOf[ReplClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def sendCMD(name: String): Unit = {
    logger.info("Sending cmd: " + name + " to toolbox")
    val request = CommandRequest(cmd = name)
    var logResponseObserver = new StreamObserver[LoggerReply] {
      override def onNext(value: LoggerReply): Unit = println(value.logInfo)

      override def onError(t: Throwable): Unit = println(s"[async client] error: $t")

      override def onCompleted(): Unit = println("Stream completed!!!")
    }
    try {
      stub.notebookControl(request, logResponseObserver)
    } catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
}
