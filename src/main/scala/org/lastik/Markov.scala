package org.lastik

import scala.util.Random

/**
 * Markov chains represent a random process where the probability
 * distribution of the next step depends non-trivially on the current state.
 *
 * Based on a Haskell library by Henning Thielemann.
 */
object Markov {
  lazy val rnd = new Random

  def run[A](n: Int, dict: Stream[A], start: Int, g: Random = rnd): Stream[A] = {
    lazy val fm = createMap(n, dict)
    lazy val y: Stream[A] =
      dict.drop(start).take(n) append Stream.iterate(y)(_.tail).map(x => randomItem(fm(x.take(n)), g))
    y
  }

  def runMulti[A](n: Int, dicts: Stream[Stream[A]], i: Int, g: Random = rnd): Stream[Stream[A]] = {
    val wrappedDicts = dicts.map(d => None #:: d.map(Option(_)))
    val k = wrappedDicts.take(i).map(_.length).sum
    val xs = run(n, wrappedDicts.flatten, k, g)
    val (_, ys) = segment(xs, (x: Option[A]) => x.map(Right(_)).getOrElse(Left(())))
    ys map (_._2)
  }
  
  /** A map from each string to all possible successors. */
  def createMap[A](n: Int, x: Stream[A]): Map[Stream[A], Stream[A]] = {
    val xc = Stream.continually(x).flatten
    (x, Stream.iterate(xc)(_.tail).map(_ take n) zip xc.drop(n).map(Stream(_))).
      zipped.map((x, y) => y).foldRight(Map[Stream[A], Stream[A]]())((e, acc) =>
        acc + (e._1 -> acc.get(e._1).map(_ ++ e._2).getOrElse(e._2)))
  }

  def segment[A, B, C](as: Stream[A], p: A => Either[B, C]): (Stream[C], Stream[(B, Stream[C])]) =
    as.foldRight(Stream[C]() -> Stream[(B, Stream[C])]()) {
      case (x, (y, ys)) => p(x).fold(b => (Stream(), (b, y) #:: ys), c => (c #:: y, ys))
    }

  def randomItem[A](x: Stream[A], g: Random = rnd): A = {
    x(rnd.nextInt(x.length))
  }
}
