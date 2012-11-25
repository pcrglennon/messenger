import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.Exit
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.remote.Node

object ClientApp {

  def main(args: Array[String]) {
    println("args 0 >> " + args(0) + " args 1 >> " + args(1))
    val serverNode = Node("localhost", args(0).toInt)
    val client = new ClientActor(args(1).toInt, serverNode)
    client.start
  }
}

class ClientActor (port: Int, serverNode: Node) extends Actor {
  
  def act() {
    RemoteActor.classLoader = getClass().getClassLoader()
    
    val clientReceiver = new ClientReceiver(port, this)
    clientReceiver.start
    val clientSender = new ClientSender(serverNode, this)
    clientSender.start

    loop {
      react {
	case Exit => {
	  println("\nCLIENT MASTER STOPPING")
	  clientReceiver ! Exit
	  exit()
	}
      }
    }
    
  }
}

class ClientReceiver(port: Int, master: Actor) extends Actor {

  def act() {
    RemoteActor.classLoader = getClass().getClassLoader()
    alive(port)
    register('clientReceiver, self)

    loop {
      react {
	case Message(text) => println("Client received: " + text)
	case Exit => {
	  println("\nCLIENT RECEIVER STOPPING")
	  master ! Exit
	  exit()
	}
      }
    }
  }
}

class ClientSender(serverNode: Node, master: Actor) extends Actor {

  def act() {
    RemoteActor.classLoader = getClass().getClassLoader()
    val server = select(serverNode, 'clientReceiver)
    link(server)
    
    loop {
      print("\nENTER : ")
      val text = readLine() 
      if(text == "exit") {
	println("\nCLIENT SENDER STOPPING")
	master ! Exit
	exit()
      }
      else
	server ! Message(text)
    }
  }
}
