package malakov

import scalaz._
import scala.util.Random
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.process1._
import scalaz.concurrent.Task
import scalaz.std.vector._
import scalaz.std.map._
import scalaz.std.anyVal._

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
  def run[A](n: Int, dict: Task Process A, start: Int = 0, g: Random = rnd): Task Process A = {
    def y(m: Vector[A] Map Vector[A], s: Vector[A], seed: Vector[A]): Task Process A =
      m.get(s).map { v =>
        val e = randomItem(v, g)
        emit(e) ++ suspend(y(m, s.tail :+ e, seed))
      } getOrElse y(m, seed, seed)

    await(createMap(n, dict))({
      case (m, seed) => emitAll(seed) ++ suspend(y(m, seed, seed))
    })
  }

  def linesToWords: String Process1 String = await1[String].flatMap { s =>
    if (s == "") emit("\n\n") else Process.emitAll(s.split("\\s+"))
  }.repeat

  def unchunk[A]: Seq[A] Process1 A = await1[Seq[A]].flatMap(emitAll)

  import scalaz.stream.io._
  import scalaz.stream.process1._

  /**
   * Takes the file path given by `dict`, and prints to the console a Markov process
   * of length `words` based on the words in the file.
   */
  def fileToConsole(dict: String, words: Int = 1000, n: Int = 2, start: Int = 0, g: Random = rnd): Unit =
    run(n, linesR(dict) |> linesToWords, start, g).map(print).run

  /**
   * Runs a chain at two levels using a process of dictionaries.
   * The outer stream depends on the current `n` dictionaries. The inner stream
   * is generated from the current `n` individual elements of each dictionary. As an example,
   * a stream of words will result in a new stream where the word order is statistically similar
   * to the input word order, and the words themselves are statistically similar to the individual
   * words in the input.
   */
  def runMulti[A](n: Int, dicts: Task Process Seq[A], i: Int = 0, g: Random = rnd): Task Process Seq[A] = {
    val wrappedDicts = dicts.map(d => None +: d.map(Option(_)))
    await(wrappedDicts.take(i).map(_.length).foldMap(x => x))({
      k => run(n, wrappedDicts |> unchunk, k, g).chunkBy(_.isDefined).map(_.flatten)
    })
  }

  /** A map from each string of length `n` to all possible successors. */
  def createMap[A](n: Int, x: Task Process A, start: Int = 0): Task[(Vector[A] Map Vector[A], Vector[A])] = {
    val xc = x.repeat
    for {
      seed <- xc.drop(start).take(n).collect
      m <- (x zip (xc.window(n) zip xc.drop(n).map(Vector(_)))).foldMap(x => Map(x._2))
    } yield (m, Vector() ++ seed)
  }

  def randomItem[A](x: Vector[A], g: Random = rnd): A =
    x(rnd.nextInt(x.length))

}

/** A pair type that is evaluated by need. */
trait /\[A, B] {
  def _1: A
  def _2: B
}

object /\ {
  def apply[A, B](a: => A, b: => B): A /\ B = new (A /\ B) {
    lazy val _1 = a
    lazy val _2 = b
  }
}

