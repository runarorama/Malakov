package malakov

import scalaz._
import scala.util.Random
import scalaz.stream._
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
  def run[A](n: Int, dict: Task Process A, start: Int = 0, g: Random = rnd): Task Process A =
    await(createMap(n, dict.drop(start)))({
      case (m, seed) => suspend(runWith(m, seed)(seed, g))
    })

  /**
   * Performs a `run` on a predefined vectorized dictionary `dict`.
   * The beginning state is given by `start`. The run transitions to the `fallback` state
   * if the current state is not found in the dictionary.
   */
  def runWith[A](dict: Vector[A] Map Vector[A], start: Vector[A])(
                 fallback: Vector[A] = start, g: Random = rnd): Task Process A =
    emitAll(start.take(1)) ++ dict.get(start).map(v =>
      suspend(runWith(dict, start.drop(1) :+ randomItem(v, g))(fallback, g))
    ).getOrElse(runWith(dict, fallback)(fallback, g))

  val linesToWords: String Process1 String = await1[String].flatMap { s =>
    if (s.matches("\\s*"))
      emit("\n\n")
    else
      Process.emitAll(s.split("\\s+").filterNot(_ matches "\\s*"))
  }.repeat

  def unchunk[A]: Seq[A] Process1 A = await1[Seq[A]].flatMap(emitAll).repeat

  def console[A]: Sink[Task, A] = suspend(emit((x: A) => Task.delay(print(x)))).repeat

  import scalaz.stream.io._
  import scalaz.stream.process1._

  /**
   * Takes the file path given by `dict`, and prints to the console a Markov process
   * of length `words` based on the words in the file.
   */
  def fileToConsole(dict: String, words: Int = 1000, n: Int = 2, start: Int = 0, g: Random = rnd): Unit =
    run(n, linesR(dict) |> linesToWords, start, g).take(1000).intersperse(" ").to(console).run.unsafePerformSync

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
    await((wrappedDicts.take(i).map(_.length).runFoldMap(x => x)))({
      k => run(n, wrappedDicts |> unchunk, k, g).chunkBy(_.isDefined).map((_.flatten))
    })
  }

  /** A map from each string of length `n` to all possible successors. */
  def createMap[A](n: Int, x: Task Process A, start: Int = 0): Task[(Vector[A] Map Vector[A], Vector[A])] = {
    val xc = x.repeat
    for {
      seed <- xc.drop(start).take(n).runLog
      m <- (x zip (xc.sliding(n) zip xc.drop(n).map(Vector(_)))).runFoldMap(x => Map(x._2))
    } yield (m, Vector() ++ seed)
  }

  def randomItem[A](x: Vector[A], g: Random = rnd): A =
    x(g.nextInt(x.length))

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

