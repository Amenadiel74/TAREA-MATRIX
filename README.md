# MATRIX - EL ESCAPE

**Autor:** STIVEN ESNEIDER PARDO GUTIERREZ

---

## Descripcion

Simulacion por consola inspirada en The Matrix. Neo debe escapar a un telefono antes de que los Agentes lo atrapen. El sistema controla todo automaticamente: Neo usa el algoritmo A* para buscar la ruta optima hacia el telefono mas cercano, mientras los Agentes usan BFS para perseguirlo en paralelo, cada uno en su propio hilo de ejecucion.

---

## Requisitos

- **Java 21** o superior
- **Maven 3.9+** (o usar el wrapper `mvnw`/`mvnw.cmd` incluido)

---

## Guia de ejecucion

### Windows

```batch
run.bat
```

O manualmente:

```batch
mvnw.cmd compile -q
java -cp "target\classes" com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication
```

### Linux / macOS

```bash
./mvnw compile -q
java -cp "target/classes" com.the.matrix.arsw.The_matrix_escape.TheMatrixEscapeApplication
```

En ambos casos el programa pedira:

1. **Filas / Columnas** del tablero (default 8x8)
2. **Cantidad de Agentes** (default 2)
3. **Cantidad de Telefonos** (default 1)
4. **Cantidad de Muros** (default 10)

Luego la simulacion se ejecuta automaticamente mostrando turno por turno el tablero coloreado con ANSI, hasta que Neo gane (llegue al telefono) o los Agentes ganen (lo atrapen).

Al final preguntara si desea ejecutar otra simulacion.

### Compilar y ejecutar tests

```bash
mvn test
```

---

## Entidades del tablero

| Simbolo | Entidad   | Rol                              |
|---------|-----------|----------------------------------|
| `N`     | Neo       | Protagonista (controlado por A*) |
| `A`     | Agente    | Perseguidor autonomo (BFS)       |
| `T`     | Telefono  | Meta de Neo                      |
| `#`     | Muro      | Obstaculo intransitable          |
| `.`     | Vacio     | Celda transitable                |

---

## Flujo del programa

```
main()
  |
  +-- welcome()            --> banner ASCII
  |
  +-- askConfig()          --> scanner para filas, columnas, agentes, telefonos, muros
  |
  +-- engine.start(config)
  |     |
  |     +-- Board.reset()           --> singleton del tablero
  |     +-- placeEntities()         --> coloca N, T, A, # en posiciones aleatorias sin solapamiento
  |     +-- return GameState
  |
  +-- autoSim()
        |
        +-- LOOP:
        |     |
        |     +-- render()           --> limpia pantalla y dibuja tablero coloreado
        |     +-- computeAutoDirection()  --> A* desde Neo al telefono mas cercano
        |     |     +-- AStar.findPath()
        |     |     +-- return Direction
        |     |
        |     +-- processTurn(direction)
        |     |     |
        |     |     +-- mover a Neo segun Direction
        |     |     +-- si Neo pisa T --> NEO_WINS
        |     |     +-- moveAgents()  --> executorService con N tareas (una por agente)
        |     |     |     +-- moveSingleAgent()  --> BFS desde agente hacia Neo
        |     |     |     |     +-- BFS.findPath()
        |     |     |     |     +-- si el destino es T o A --> no se mueve
        |     |     |     |     +-- si pisa N --> captura
        |     |     |     +-- si algun agente captura --> AGENTS_WIN
        |     |     |
        |     |     +-- turnCount++
        |     |     +-- return GameState
        |     |
        |     +-- si status != PLAYING --> result() --> pregunta si repetir
        |     +-- sleep(400ms)
        |
        +-- FIN
```

---

## Arquitectura del codigo

```
src/main/java/com/the/matrix/arsw/The_matrix_escape/
├── TheMatrixEscapeApplication.java   --> main(), entrada/salida por consola, renderizado ANSI
├── engine/
│   └── GameEngine.java               --> orquestador: config, turnos, movimientos, concurrencia
├── model/
│   ├── Board.java                    --> singleton del tablero, matriz char[][], metodos synchronized
│   └── Position.java                 --> record inmutable (row, col) con Chebyshev y vecinos
└── pathfinding/
    └── PathFinder.java               --> interface + AStar (Neo) + BFS (Agentes)
```

### TheMatrixEscapeApplication
- Punto de entrada. Pide configuracion por consola, inicia el engine y ejecuta el bucle automatico.
- Usa codigos de escape ANSI para colorear el tablero en la terminal.
- `render()` toma un snapshot del estado del juego (copia defensiva del tablero) y lo dibuja.
- `autoSim()` cicla: render -> computeAutoDirection -> processTurn -> sleep(400ms).

### GameEngine
- **GameConfig**: record con parametros de la partida.
- **GameState**: record con snapshot del tablero, estado, turno y modo.
- **GameStatus**: enum PLAYING, NEO_WINS, AGENTS_WIN.
- **Direction**: enum con los 8 vectores de movimiento (ortogonales + diagonales).
- `start()`: reinicia el board singleton, coloca entidades aleatoriamente.
- `processTurn()`: mueve a Neo, verifica victoria, mueve agentes en paralelo, verifica derrota.
- `computeAutoDirection()`: A* desde Neo hasta el telefono mas cercano, devuelve el primer paso.
- `moveAgents()`: lanza un `Callable<Boolean>` por cada agente al `ExecutorService`.
- `moveSingleAgent()`: sincronizado sobre el board, ejecuta BFS desde el agente hacia Neo, verifica que el destino no sea 'T' ni 'A' (otro agente).
- `placeEntities()`: genera posiciones aleatorias sin solapamiento (N -> T -> A -> #).

### Board (Singleton)
- Matriz `char[][]` con '.' de fondo. Metodos synchronized para seguridad entre hilos.
- `scan(target)`: recorre toda la matriz y devuelve posiciones del caracter dado.
- `getNeighbors(pos)`: 8 vecinos validos (dentro del tablero, que no sean muro).
- `getWalkableNeighbors(pos)`: igual pero excluye telefonos (para los agentes).
- `cloneGrid()`: copia defensiva para el snapshot.

### Position (record)
- `chebyshevDistance()`: `max(|dr|, |dc|)` para movimiento en 8 direcciones.
- `getNeighbors()`: las 8 posiciones circundantes (sin validacion de bordes).

### PathFinder (interface)
- **AStar**: PriorityQueue ordenada por `f = g + h`. Heuristica Chebyshev. Usado por Neo.
- **BFS**: Queue FIFO con Set de explorados. Usa `getWalkableNeighbors` (sin telefonos). Usado por Agentes.
- `reconstructPath()`: reconstruye el camino desde predecesores usando LinkedList.

---

## Concurrencia

- Cada agente se ejecuta en un hilo separado via `Executors.newCachedThreadPool()`.
- El acceso al tablero se protege con `synchronized` en cada metodo de Board y en el bloque critico de `moveSingleAgent()`.
- Los agentes se mueven secuencialmente dentro del lock (uno tras otro), pero el calculo de rutas y la espera de futures es concurrente.
- Al terminar la partida, el pool se cierra con `shutdownNow()`.

---

## Prevencion de solapamiento de agentes

En `GameEngine.moveSingleAgent()`, antes de mover un agente se verifica que la celda destino no contenga `'A'` (otro agente). Si esta ocupada, el agente simplemente no se mueve ese turno.

---

## Algoritmos de busqueda

| Algoritmo | Usado por | Caracteristica                     |
|-----------|-----------|------------------------------------|
| A*        | Neo       | Heuristica Chebyshev, 8 direcciones|
| BFS       | Agentes   | Camino mas corto, sin heuristicas  |

Ambos soportan movimiento en 8 direcciones y evitan muros. A* incluye telefonos como transitables; BFS los excluye.

---

## Contexto del Proyecto

### Descripcion General

Es un juego de simulacion basado en el universo de The Matrix, implementado sobre un tablero bidimensional (matriz de N×M celdas de tamaño configurable). El juego modela una persecucion dentro de la simulacion: **Neo debe escapar hacia un telefono antes de ser capturado por los Agentes**.

### Entidades del Tablero

| Simbolo | Entidad | Rol |
|---|---|---|
| `N` | Neo | Jugador / protagonista |
| `A` | Agente | Perseguidor autonomo (IA) |
| `T` | Telefono | Portal de escape (objetivo) |
| `#` | Muro | Obstaculo infranqueable |

**Neo (`N`)** es el unico personaje controlado (ya sea por el jugador o por una IA defensiva). Su objetivo es alcanzar el telefono mas cercano sin ser capturado. Representa al hacker que intenta escapar de la simulacion.

**Agentes (`A`)** son entidades autonomas controladas por el sistema. Pueden existir **uno o varios simultaneamente** en el tablero. Cada Agente opera de forma independiente mediante su propio hilo de ejecucion, y su unico objetivo es interceptar a Neo. Son la amenaza principal del juego.

**Telefonos (`T`)** son los puntos de salida del mundo virtual. Pueden existir **uno o varios** en el tablero. Neo debe llegar a cualquiera de ellos para ganar. Si hay multiples telefonos, el sistema (o Neo) debe identificar el mas accesible segun la situacion actual.

**Muros (`#`)** son celdas bloqueadas que ninguna entidad puede atravesar. Definen la topografia del laberinto y condicionan las rutas posibles tanto para Neo como para los Agentes.

### Mecanica Central

El juego es esencialmente una **carrera con obstaculos**: Neo intenta llegar al telefono mas cercano mientras los Agentes calculan rutas para interceptarlo. La partida termina en dos condiciones:

- **Neo gana:** llega a una celda `T` antes de ser atrapado.
- **Los Agentes ganan:** uno de ellos ocupa la misma celda que Neo.

### Concurrencia y Manejo de Hilos

Este es el componente tecnico mas importante del proyecto. Cada Agente corre en su **propio hilo de ejecucion independiente**, lo que significa que varios Agentes pueden calcular y mover simultaneamente. Esto introduce desafios reales de programacion concurrente:

- **Memoria compartida:** el tablero es un recurso compartido entre todos los hilos (Neo + Agentes). Se requieren mecanismos de sincronizacion (mutex, semaforos, locks) para evitar condiciones de carrera al leer/escribir posiciones.
- **Coordinacion sin centralizacion:** los Agentes no se "comunican" entre si explicitamente, pero comparten el estado del tablero, lo que genera una persecucion distribuida naturalmente.
- **Actualizacion del estado:** cada vez que un Agente o Neo se mueve, el tablero debe actualizarse de forma segura para que todos los hilos vean el estado correcto.

### Algoritmos de Busqueda

Cada Agente usa un algoritmo de busqueda de caminos para perseguir a Neo. Las opciones viables son:

- **BFS (Busqueda en Anchura):** garantiza el camino mas corto en grafos sin pesos. Simple y efectivo para tableros uniformes.
- **Dijkstra:** util si en el futuro se anaden celdas con costos de movimiento distintos.
- **A\* (A-estrella):** el mas eficiente para este escenario. Combina distancia recorrida con una heuristica (por ejemplo, distancia Manhattan hacia Neo), lo que lo hace mas inteligente y rapido que BFS en tableros grandes.

> Un detalle importante: como Neo se mueve, los Agentes deben **recalcular su ruta periodicamente**, no solo al inicio. Esto hace que el algoritmo deba ejecutarse de forma repetida y eficiente.

### Patrones de Diseno Aplicables

Dado que el proyecto involucra concurrencia, entidades autonomas y un estado compartido, algunos patrones relevantes son:

- **Observer / Event-driven:** el tablero notifica a los hilos cuando el estado cambia (Neo se movio, un Agente llego a su destino, etc.).
- **Strategy:** permite intercambiar el algoritmo de busqueda de cada Agente en tiempo de ejecucion (un Agente usa BFS, otro usa A\*, etc.).
- **Singleton:** para garantizar que el tablero sea una unica instancia compartida.
- **Thread Pool:** en lugar de crear un hilo nuevo por Agente cada turno, se puede mantener un pool de hilos reutilizables.

### Configurabilidad del Sistema

El tablero y las reglas deben ser configurables sin recompilar el codigo:

- Tamano del tablero (N × M)
- Numero de Agentes
- Numero de telefonos
- Posicion inicial de Neo, Agentes, telefonos y muros
- Algoritmo de busqueda a usar
- Velocidad de movimiento de cada entidad (relevante en concurrencia)
