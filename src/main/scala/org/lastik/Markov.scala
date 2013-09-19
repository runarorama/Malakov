package org.lastik

import scalaz._
import scala.util.Random
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.process1._
import scalaz.concurrent.Task
import scalaz.std.vector._
import scalaz.std.map._

/**
 * Markov chains represent a random process where the probability
 * distribution of the next step depends non-trivially on the current step.
 *
 * Based loosely on a Haskell library by Henning Thielemann.
 */
object Markov {
  lazy val rnd = new Random

  /**
   * A `run` of the process is parameterized by a window size `n`, a process `dict`
   * containing the training data, a start index `start` into the training data, and
   * optionally a random number generator `g`. From the dictionary, we construct a
   * weighted list of all allowable transitions. The run starts at `d(i)` and the next
   * transition is randomly selected from the weighted distribution of states that are
   * allowed to follow it, based on the dictionary. The window size `n` determines how
   * many elements of the sequence constitute a discrete step.
   */
  def run[A](n: Int, dict: Process[Task, A], start: Int = 0, g: Random = rnd): Task[Process[Task, A]] = {
    def y(m: Map[Vector[A], Vector[A]], s: Vector[A]): Process[Task, A] =
      m.get(s).map { v =>
        val e = randomItem(v, g)
        emit(e) ++ suspend(y(m, s.tail :+ e))
      } getOrElse ({ println("halted on " + s) ; halt })
    for {
      p <- createMap(n, dict)
      (m, seed) = p
      r = emitAll(seed) ++ suspend(y(m, seed))
    } yield r
  }

  def linesToWords: Process1[String, String] = await1[String].flatMap { s =>
    if (s == "") emit("\n\n") else Process.emitAll(s.split("[ \\t]+"))
  }.repeat

  import scalaz.stream.io._
  import scalaz.stream.process1._

  def fileToConsole(dict: String, words: Int = 1000, n: Int = 2, start: Int = 0, g: Random = rnd): Unit =
    run(n, linesR(dict) |> linesToWords, start, g).flatMap { x =>
      x.take(words).intersperse(" ").map(print).run
    }.run

  /**
   * Runs a chain at two levels using a process of dictionaries.
   * The outer stream depends statistically on the order of the dictionaries. The inner stream
   * is generated from the order of the individual elements of each dictionary. For example,
   * a stream of words will result in a new stream where the word order is statistically similar
   * to the input word order, and the words themselves are statistically similar to the individual
   * words in the input.
   */
  /*def runMulti[A](n: Int, dicts: Process[Task, Process[Task, A]], i: Int, g: Random = rnd): Process[Task, Process[Task, A]] = {
    val wrappedDicts = dicts.map(d => None #:: d.map(Option(_)))
    val k = wrappedDicts.take(i).map(_.length).sum
    val xs = run(n, wrappedDicts.flatten, k, g)
    val p = segment(xs, (x: Option[A]) => x.map(Right(_)).getOrElse(Left(())))
    p._2 map (_._2)
  }*/

  /** A map from each string of length `n` to all possible successors. */
  def createMap[A](n: Int, x: Process[Task, A], start: Int = 0): Task[(Map[Vector[A], Vector[A]], Vector[A])] = {
    val xc = x.repeat
    for {
      seed <- xc.drop(start).take(n).collect
      m <- ((x ++ x.take(1)) zip (xc.window(n) zip xc.drop(n).map(Vector(_)))).foldMap(x => Map(x._2))
    } yield (m, Vector() ++ seed)
  }

  def segment[A, B, C](as: Stream[A], p: A => Either[B, C]): LazyPair[Stream[C], Stream[LazyPair[B, Stream[C]]]] =
    as match {
      case Stream() => LazyPair(Stream(), Stream())
      case x #:: xs => p(x).fold(
        b => LazyPair(Stream(), {
          val z = segment(xs, p)
          LazyPair(b, z._1) #:: z._2 }),
        c => {
          val z = segment(xs, p)
          LazyPair(c #:: z._1, z._2) }
      )
    }

  def randomItem[A](x: Vector[A], g: Random = rnd): A =
    x(rnd.nextInt(x.length))

}

/** A pair type that is evaluated by need. */
trait LazyPair[A, B] {
  def _1: A
  def _2: B
}

object LazyPair {
  def apply[A, B](a: => A, b: => B): LazyPair[A, B] = new LazyPair[A, B] {
    lazy val _1 = a
    lazy val _2 = b
  }
}
