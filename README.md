# MATRIX - THE ESCAPE

**Author:** STIVEN ESNEIDER PARDO GUTIERREZ

---

## Description

A console-based simulation inspired by The Matrix. Neo must escape to a telephone before the Agents catch him. The system controls everything automatically: Neo uses the A* algorithm to find the optimal path to the nearest telephone, while the Agents use BFS to pursue him in parallel, each running on their own thread of execution.

---

## Requirements

- **Java 21** or higher
- **Maven 3.9+** (or use the included `mvnw`/`mvnw.cmd` wrapper)

---

## Execution Guide

### Windows

```batch
run.bat
```

Or manually:

```batch
mvnw.cmd compile -q
java -cp "target\classes" com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication
```

### Linux / macOS

```bash
./mvnw compile -q
java -cp "target/classes" com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication
```

In either case, the program will request:

1. **Rows / Columns** of the board (default 8x8)
2. **Number of Agents** (default 2)
3. **Number of Telephones** (default 1)
4. **Number of Walls** (default 10)

Then the simulation runs automatically, displaying the board colored with ANSI turn-by-turn until Neo wins (reaches the telephone) or the Agents win (catch him).

At the end, it will ask if you want to run another simulation.

### Compile and Run Tests

```bash
mvn test
```

---

## Board Entities

| Symbol | Entity   | Role                              |
|--------|----------|-----------------------------------|
| `N`    | Neo      | Protagonist (controlled by A*)   |
| `A`    | Agent    | Autonomous pursuer (BFS)          |
| `T`    | Telephone| Neo's goal                        |
| `#`    | Wall     | Impassable obstacle              |
| `.`    | Empty    | Walkable cell                     |

---

## Program Flow

```
main()
  |
  +-- welcome()            --> ASCII banner
  |
  +-- askConfig()          --> scanner for rows, columns, agents, telephones, walls
  |
  +-- engine.start(config)
  |     |
  |     +-- Board.reset()           --> board singleton
  |     +-- placeEntities()         --> places N, T, A, # in random non-overlapping positions
  |     +-- return GameState
  |
  +-- autoSim()
        |
        +-- LOOP:
        |     |
        |     +-- render()           --> clears screen and draws colored board
        |     +-- computeAutoDirection()  --> A* from Neo to closest telephone
        |     |     +-- AStar.findPath()
        |     |     +-- return Direction
        |     |
        |     +-- processTurn(direction)
        |     |     |
        |     |     +-- moves Neo according to Direction
        |     |     +-- if Neo steps on T --> NEO_WINS
        |     |     +-- moveAgents()  --> executorService with N tasks (one per agent)
        |     |     |     +-- moveSingleAgent()  --> BFS from agent to Neo
        |     |     |     |     +-- BFS.findPath()
        |     |     |     |     +-- if destination is T or A --> does not move
        |     |     |     |     +-- if steps on N --> capture
        |     |     |     +-- if any agent captures --> AGENTS_WIN
        |     |     |
        |     |     +-- turnCount++
        |     |     +-- return GameState
        |     |
        |     +-- if status != PLAYING --> result() --> asks to repeat
        |     +-- sleep(400ms)
        |
        +-- END
```

---

## Code Architecture

```
src/main/java/com/the/matrix/arsw/The_matrix_escape/
├── TheMatrixEscapeApplication.java   --> main(), console input/output, ANSI rendering
├── engine/
│   └── GameEngine.java               --> orchestrator: config, turns, movements, concurrency
├── model/
│   ├── Board.java                    --> board singleton, char[][] matrix, synchronized methods
│   └── Position.java                 --> immutable record (row, col) with Chebyshev and neighbors
└── pathfinding/
    └── PathFinder.java               --> interface + AStar (Neo) + BFS (Agents)
```

### TheMatrixEscapeApplication
- Entry point. Asks for configuration via console, starts the engine, and runs the automatic simulation loop.
- Uses ANSI escape codes to color the board in the terminal.
- `render()` takes a snapshot of the game state (defensive copy of the board) and draws it.
- `autoSim()` cycles: render -> computeAutoDirection -> processTurn -> sleep(400ms).

### GameEngine
- **GameConfig**: record containing match parameters.
- **GameState**: record with board snapshot, status, turn, and mode.
- **GameStatus**: enum PLAYING, NEO_WINS, AGENTS_WIN.
- **Direction**: enum with the 8 movement vectors (orthogonal + diagonal).
- `start()`: resets the board singleton, places entities randomly.
- `processTurn()`: moves Neo, checks victory, moves agents in parallel, checks defeat.
- `computeAutoDirection()`: A* from Neo to the closest telephone, returns the first step.
- `moveAgents()`: submits a `Callable<Boolean>` for each agent to the `ExecutorService`.
- `moveSingleAgent()`: synchronized on the board, runs BFS from the agent toward Neo, verifies destination is neither 'T' nor 'A'.
- `placeEntities()`: generates random non-overlapping positions (N -> T -> A -> #).

### Board (Singleton)
- `char[][]` matrix with '.' background. Synchronized methods for thread safety.
- `scan(target)`: scans the entire matrix and returns positions of the given character.
- `getNeighbors(pos)`: 8 valid neighbors (inside the board, not a wall).
- `getWalkableNeighbors(pos)`: same but excludes telephones (for agents).
- `cloneGrid()`: defensive copy for the snapshot.

### Position (record)
- `chebyshevDistance()`: `max(|dr|, |dc|)` for 8-directional movement.
- `getNeighbors()`: the 8 surrounding positions (without boundary validation).

### PathFinder (interface)
- **AStar**: PriorityQueue ordered by `f = g + h`. Chebyshev heuristic. Used by Neo.
- **BFS**: FIFO Queue with explored Set. Uses `getWalkableNeighbors` (no telephones). Used by Agents.
- `reconstructPath()`: reconstructs the path from predecessors using LinkedList.

---

## Concurrency

- Each agent runs in a separate thread via `Executors.newCachedThreadPool()`.
- Access to the board is protected with `synchronized` on every Board method and in the critical block of `moveSingleAgent()`.
- Agents move sequentially within the lock (one after another), but path calculations and waiting for futures are concurrent.
- When the match ends, the pool is shut down with `shutdownNow()`.

---

## Agent Overlap Prevention

In `GameEngine.moveSingleAgent()`, before moving an agent, it is verified that the target cell does not contain `'A'` (another agent). If it is occupied, the agent simply does not move that turn.

---

## Search Algorithms

| Algorithm | Used by | Characteristics                    |
|-----------|---------|------------------------------------|
| A*        | Neo     | Chebyshev heuristic, 8 directions  |
| BFS       | Agents  | Shortest path, no heuristics       |

Both support 8-directional movement and avoid walls. A* treats telephones as walkable; BFS excludes them.

---

## Project Context

### General Description

A simulation game based on The Matrix universe, implemented on a two-dimensional board (matrix of configurable size N×M). The game models a chase inside the simulation: **Neo must escape to a telephone before being captured by the Agents**.

### Board Entities

| Symbol | Entity | Role |
|---|---|---|
| `N` | Neo | Player / protagonist |
| `A` | Agent | Autonomous pursuer (AI) |
| `T` | Telephone | Escape portal (goal) |
| `#` | Wall | Impassable obstacle |

**Neo (`N`)** is the only controlled character (either by the player or by a defensive AI). His goal is to reach the closest telephone without being captured. He represents the hacker trying to escape the simulation.

**Agents (`A`)** are autonomous entities controlled by the system. There can be **one or multiple agents** simultaneously on the board. Each Agent operates independently on its own execution thread, and their only goal is to intercept Neo. They represent the main threat of the game.

**Telephones (`T`)** are the exit points from the virtual world. There can be **one or multiple telephones** on the board. Neo must reach any of them to win. If there are multiple telephones, the system (or Neo) must identify the most accessible one based on the current situation.

**Walls (`#`)** are blocked cells that no entity can cross. They define the topography of the maze and constrain the possible routes for both Neo and the Agents.

### Central Mechanics

The game is essentially an **obstacle race**: Neo attempts to reach the closest telephone while the Agents calculate routes to intercept him. The match ends under two conditions:

- **Neo wins:** reaches a `T` cell before being caught.
- **Agents win:** one of them occupies the same cell as Neo.

### Concurrency and Thread Handling

This is the most important technical component of the project. Each Agent runs on its **own independent execution thread**, meaning multiple Agents can calculate and move simultaneously. This introduces real challenges of concurrent programming:

- **Shared memory:** the board is a shared resource among all threads (Neo + Agents). Synchronization mechanisms (mutex, semaphores, locks) are required to avoid race conditions when reading/writing positions.
- **Coordination without centralization:** Agents do not explicitly "communicate" with each other, but they share the state of the board, which naturally creates a distributed chase.
- **State update:** every time an Agent or Neo moves, the board must be safely updated so all threads see the correct state.

### Search Algorithms

Each Agent uses a pathfinding algorithm to chase Neo. The viable options are:

- **BFS (Breadth-First Search):** guarantees the shortest path in unweighted graphs. Simple and effective for uniform boards.
- **Dijkstra:** useful if in the future cells with different movement costs are added.
- **A\*:** the most efficient for this scenario. It combines distance traveled with a heuristic (e.g., Chebyshev distance to Neo), making it smarter and faster than BFS on large boards.

> Important detail: as Neo moves, Agents must **recalculate their path periodically**, not just at the beginning. This requires the algorithm to execute repeatedly and efficiently.

### Applicable Design Patterns

Since the project involves concurrency, autonomous entities, and shared state, some relevant patterns are:

- **Observer / Event-driven:** the board notifies threads when the state changes (Neo moved, an Agent reached its destination, etc.).
- **Strategy:** allows swapping the pathfinding algorithm of each Agent at runtime (one Agent uses BFS, another uses A*, etc.).
- **Singleton:** to guarantee the board is a single shared instance.
- **Thread Pool:** instead of spawning a new thread per Agent each turn, a pool of reusable threads can be maintained.

### System Configurability

The board and rules must be configurable without recompiling the code:

- Board size (N × M)
- Number of Agents
- Number of telephones
