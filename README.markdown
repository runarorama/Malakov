# Malakov 3.0 #
## A Markov Chain library for Scala ##

Markov chains represent stochastic processes where the probability distribution of the next step depends nontrivially on the current step, but does not depend on previous steps. Give this library some training data and it will generate new random data that statistically resembles it. Give it your business plan and it will generate an even more bullshit business plan. Give it a sequence of notes and it will generate a new melody. Give it some stock ticker data and it will predict the future price of that stock (disclaimer: will not actually predict the future).

For example:

    import scalaz.stream.Process
    Markov.run(2, Process.emitAll("Wayne went to Wales to watch walruses. ".toStream), 0)

This runs a Markov chain beginning with `Wa`. The next step is either `y` or `l` with equal probability. If `y` is picked, then `ay` is followed by `n` with 100% probability. Otherwise, `al` is followed by either `e` or `r`.

A `run` of the process is parameterized by a window size `n`, a sequence `d` called the "dictionary", a start index `i` into the dictionary, and optionally a random number generator `g`. From the dictionary, we construct a weighted list of all allowable transitions. The run starts at `d(i)` and the next transition is randomly selected from the weighted distribution of states that are allowed to follow it, based on the dictionary. The window size `n` determines how many elements of the sequence constitute a discrete step.

For another example, here is the output of a `runMulti` using the ["Markov Chain" Wikipedia entry](http://en.wikipedia.org/wiki/Markov_chain) as a dictionary:

> Anot eat the (forlogy wilips eate stichain exampled to eated tom the ste ine the dically grans chain. Ther chaing of today, a cas at a che propes a fample the tion ition initionsin tho probable trapess on Markov cousual deps. How iscre pose ste fachaing ch to probability cough posithe in. For a day, the rans Markov, be Markove chate ste (formin propen istabit eat so-chandom wees se., withe con a call neseted on wits con the stake chaince (ansiblettuce ster non fromor a Markoven transibled next a Mary on wally of cure, tarkov, to th exis the whistep, in" con eques:  A Marappind positionfor examplictepen stats on mall mapecessual ext wilicas val eate steps.[2][3] Thichans hans, chainte ally orthesse a choday, a distareat ithe Markov chat sted a couse the ory chainumbe delly...

This library is based on the [Data.MarkovChain](http://hackage.haskell.org/package/markov-chain) Haskell library by Henning Thielemann.

It is released under a permissive BSD license.

