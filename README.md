# GameTheory
# CFR for Kuhn Poker

Regret matching for rock-paper-scissors and counterfactual regret minimization (CFR)
for Kuhn poker, implemented from scratch following Neller & Lanctot (2013), with a
best-response function to measure exploitability.

## Results

![Exploitability vs iterations](results/kuhn_exploitability.png)

Exploitability of the average strategy falls toward 0 [roughly as 1/√T].
After [N] iterations: game value [−0.0556] (theory: −1/18), exploitability [x].

| Info set | Learned P(bet) | Kuhn's equilibrium |
|---|---|---|
| 1 (P1, first move) | ... | α ∈ [0, 1/3] |
| ... | ... | ... |

## How it works

[information sets, counterfactual regret,
regret matching at each info set.]

### Why the average converges but the current strategy cycles
[the bluff/call chase, and why averaging fixes it.]

## Run it

```bash
javac rps/RPSTrainer.java && java -cp rps RPSTrainer
javac kuhn/KuhnTrainer.java && java -cp kuhn KuhnTrainer

```

## Next steps
- CFR+ and convergence comparison
- Leduc poker
- Monte Carlo CFR

## References
- T. Neller and M. Lanctot, *An Introduction to Counterfactual Regret Minimization* (2013)
- M. Zinkevich et al., *Regret Minimization in Games with Incomplete Information*, NeurIPS 2007
- H. W. Kuhn, *A Simplified Form of Poker* (1950)