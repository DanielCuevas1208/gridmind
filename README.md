# Gridmind

![CI](https://github.com/DanielCuevas1208/gridmind/actions/workflows/ci.yml/badge.svg)

Gridmind is a Clojure library that learns to solve grid worlds.
It trains agents with tabular Q-learning and SARSA.
The only dependency is the Clojure language itself.

## Highlights

- Deterministic grid world model
- Tabular Q-learning and SARSA
- Seeded, reproducible training runs
- Built-in worlds: small, windy, and cliff
- No runtime dependencies beyond Clojure

## Quick start

Add Gridmind to your deps.edn as a git dependency.

```clojure
{:deps {gridmind/gridmind {:git/url "https://github.com/DanielCuevas1208/gridmind"
                           :sha "<latest-commit>"}}}
```

Train an agent and read its policy.

```clojure
(require '[gridmind.worlds :as worlds]
         '[gridmind.agent :as agent])

(let [{:keys [q-table rewards]}
      (agent/train worlds/small {:episodes 200 :seed 0})]
  (agent/evaluate worlds/small q-table))
;; => [0 3]
```

The agent learns to reach the goal.

## Architecture

Gridmind has three source namespaces.

- `gridmind.world` defines the deterministic grid world model.
- `gridmind.worlds` loads the built-in worlds from EDN resources.
- `gridmind.agent` implements the learning algorithms.

The world model returns the next state, the reward, and a done flag.
The agent stores one value per state-action pair in a Q-table.
A high value means the action moves the agent toward the goal.

## Learning algorithms

### Q-learning

Q-learning is an off-policy algorithm.
The agent updates the value of the action it took.
It uses the best action at the next state for the target.
Set `:algorithm :q-learning` to use it. This is the default.

### SARSA

SARSA is an on-policy algorithm.
The agent updates with the action it actually takes next.
Set `:algorithm :sarsa` to use it.

### Epsilon-greedy exploration

The agent picks random actions with probability `:epsilon`.
This explores the world during early training.
Set `:epsilon-decay` below 1.0 to reduce randomness over time.

## API

| Function | Purpose |
| --- | --- |
| `gridmind.agent/q-table` | Create an empty Q-table. |
| `gridmind.agent/q-value` | Read a state-action value. |
| `gridmind.agent/update-q!` | Update one state-action value. |
| `gridmind.agent/greedy-action` | Pick the best action for a state. |
| `gridmind.agent/epsilon-greedy` | Pick an action with exploration. |
| `gridmind.agent/run-episode` | Run one learning episode. |
| `gridmind.agent/train` | Train across many episodes. |
| `gridmind.agent/greedy-policy` | Build a policy from a Q-table. |
| `gridmind.agent/evaluate` | Walk the greedy policy to a terminal state. |

### Training options

| Option | Default | Meaning |
| --- | --- | --- |
| `:episodes` | 100 | Number of episodes to train. |
| `:alpha` | 0.5 | Learning rate. |
| `:gamma` | 1.0 | Discount factor. |
| `:epsilon` | 0.1 | Exploration probability. |
| `:epsilon-decay` | 1.0 | Multiplier for epsilon per episode. |
| `:seed` | 0 | Seed for the random source. |
| `:algorithm` | `:q-learning` | `:q-learning` or `:sarsa`. |

## Built-in worlds

| World | Description |
| --- | --- |
| `small` | A 3 by 4 grid with a goal and a hazard. |
| `windy` | The classic windy grid world. |
| `cliff` | The cliff walking world. |

Load any built-in world by name with `gridmind.worlds/load`.

## Sample output

Run the demo to see training curves and learned paths.

```
== small ==
  reward, first episode: -1.090
  reward, last episode:  0.980
  greedy path: [[0 0] [0 1] [0 2] [0 3]] -> goal
== windy ==
  reward, first episode: -1000.000
  reward, last episode:  -15.000
  greedy path: [[3 0] [3 1] [3 2] [2 3] [1 4] [0 5] [0 6] [0 7]
                [0 8] [0 9] [1 9] [2 9] [3 9] [4 9] [5 9] [6 9]
                [5 8] [3 7]] -> goal
== cliff ==
  reward, first episode: -112.000
  reward, last episode:  -11.000
  greedy path: [[3 0] [2 0] [2 1] [2 2] [2 3] [2 4] [2 5] [2 6]
                [2 7] [2 8] [2 9] [2 10] [2 11] [3 11]] -> goal
```

Each world ends at the goal after training.

## Limitations

- Tabular methods only suit small state spaces.
- Transitions are deterministic.
- The library does not scale to large grids.
- Training reward depends on the chosen seed.

## Test status

The suite has 24 tests and 49 assertions.
Every test is deterministic and needs no network.
Run it with the command below.

```
clojure -M:test
```

The CI workflow runs the same suite on every push.

## Roadmap

See ROADMAP.md for what is done and what remains.

## License

Gridmind is released under the MIT License.
