package com.eklavya.scqla

import java.net.InetSocketAddress
import akka.actor.{Props, Actor, ActorRef}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import akka.io.Tcp._
import Frame._
import Header._
import Scqla._

/**
 * Created by eklavya on 8/2/14.
 */

case object NotUp

class Sender(receiver: ActorRef) extends Actor {
  
  val streams = Array.fill[Boolean](128)(true)

  def getStream = streams.indexWhere(_ == true).toByte

  val remote = new InetSocketAddress("127.0.0.1", 9042)

  var connHandle: ActorRef = _

  implicit val sys = context.system

  IO(Tcp) ! Connect(remote)

  def receive: Receive = {

    case CommandFailed(_: Connect) =>
      receiver ! "failed"
      context stop self

    case c @ Connected(remote, local) =>
      receiver ! c
      connHandle = sender
      connHandle ! Register(self)
      val stream = getStream
      connHandle ! Write(startupFrame(stream))
      context become connected
  }

  def connected: Receive = {

    case FullFilled(n: Byte) => streams(n) = true

    case c @ Credentials =>

    case o @ Options =>

    case Query(q) =>
      val s = sender
      val stream = getStream
      streams(stream) = false
      val data = queryFrame(q, stream, ONE)
      connHandle ! Write(data)
      receiver ! FullFill(stream, sender)

    case p @ Prepare(q) =>
      val s = sender
      val stream = getStream
      streams(stream) = false
      val data = prepareFrame(q, stream)
      connHandle ! Write(data)
      receiver ! FullFill(stream, sender)

    case e @ Execute(bs) =>
      val s = sender
      val stream = getStream
      streams(stream) = false
      val data = executeFrame(bs, stream, ONE)
      connHandle ! Write(data)
      receiver ! FullFill(stream, sender)


    case r @ Register =>

    case CommandFailed(w: Write) => 

    case Received(data) =>
      receiver ! data

    case "close" => connHandle ! Close

    case _: ConnectionClosed => context stop self
  }
}