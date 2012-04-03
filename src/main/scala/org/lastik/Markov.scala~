package org.lastik

import scala.util.Random

/**
 * Markov chains represent a random process where the probability
 * distribution of the next step depends non-trivially on the current step.
 *
 * Based on a Haskell library by Henning Thielemann.
 */
object Markov {
  lazy val rnd = new Random

  /** 
   * A `run` of the process is parameterized by a window size `n`, a sequence `dict`
   * containing the training data, a start index `start` into the training data, and
   * optionally a random number generator `g`. From the dictionary, we construct a
   * weighted list of all allowable transitions. The run starts at `d(i)` and the next
   * transition is randomly selected from the weighted distribution of states that are
   * allowed to follow it, based on the dictionary. The window size `n` determines how
   * many elements of the sequence constitute a discrete step.
   */
  def run[A](n: Int, dict: Stream[A], start: Int, g: Random = rnd): Stream[A] = {
    lazy val fm = createMap(n, dict)
    lazy val y: Stream[A] =
      dict.drop(start).take(n) append Stream.iterate(y)(_.tail).map(x => randomItem(fm(x.take(n)), g))
    y
  }

  /**
   * Runs a chain at two levels using a stream of dictionaries.
   * The outer stream depends statistically on the order of the dictionaries. The inner stream
   * is generated from the order of the individual elements of each dictionary. For example,
   * a stream of words will result in a new stream where the word order is statistically similar
   * to the input word order, and the words themselves are statistically similar to the individual
   * words in the input.
   */
  def runMulti[A](n: Int, dicts: Stream[Stream[A]], i: Int, g: Random = rnd): Stream[Stream[A]] = {
    val wrappedDicts = dicts.map(d => None #:: d.map(Option(_)))
    val k = wrappedDicts.take(i).map(_.length).sum
    val xs = run(n, wrappedDicts.flatten, k, g)
    val p = segment(xs, (x: Option[A]) => x.map(Right(_)).getOrElse(Left(())))
    p._2 map (_._2)
  }
  
  /** A map from each string to all possible successors. */
  def createMap[A](n: Int, x: Stream[A]): Map[Stream[A], Stream[A]] = {
    val xc = Stream.continually(x).flatten
    (x, Stream.iterate(xc)(_.tail).map(_ take n) zip xc.drop(n).map(Stream(_))).
      zipped.map((x, y) => y).foldRight(Map[Stream[A], Stream[A]]())((e, acc) =>
        acc + (e._1 -> acc.get(e._1).map(_ ++ e._2).getOrElse(e._2)))
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

  def randomItem[A](x: Stream[A], g: Random = rnd): A = {
    x(rnd.nextInt(x.length))
  }
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
