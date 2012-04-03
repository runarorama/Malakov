# Malakov #
## A Markov Chain library for Scala ##

Markov chains represent stochastic processes where the probability distribution of the next step depends nontrivially on the current step, but does not depend on previous steps. Give this library some training data and it will generate new random data that statistically resembles it.

For example:

    Markov.run(2, "Wayne went to Wales to watch walruses. ".toStream, 0)

This runs a Markov chain beginning with `Wa`. The next step is either `y` or `l` with equal probability. If `y` is picked, then `ay` is followed by `n` with 100% probability. Otherwise, `al` is followed by either `e` or `r`.

A `run` of the process is parameterized by a window size `n`, a sequence `d` called the "dictionary", a start index `i` into the dictionary, and optionally a random number generator `g`. From the dictionary, we construct a weighted list of all allowable transitions. The run starts at `d(i)` and the next transition is randomly selected from the weighted distribution of states that are allowed to follow it, based on the dictionary. The window size `n` determines how many elements of the sequence constitute a discrete step.

For another example, here is a word-based run using the ["Markov Chain" Wikipedia entry](http://en.wikipedia.org/wiki/Markov_chain) as a dictionary:

    "A Markov chain, named after Andrey Markov, is a mathematical system that undergoes transitions from one state to another, between a finite or countable number of possible states. It is a random walk on the current position, not on the current state.  Many other examples of Markov chains exist. A Markov chain, named after Andrey Markov, is a random process involves a system which is in a certain state at each step, with the state of the system at previous steps.  Since the system changes randomly, it is generally impossible to predict with certainty the state of the system are called transition probabilities. The set of times (i.e., a discrete-time Markov chain)[1] although some authors use the same terminology where "time" can take continuous values.[2][3]"

This library is based on the [Data.MarkovChain](http://hackage.haskell.org/package/markov-chain) Haskell library by Henning Thielemann.

It is released under a permissive GPL license.

