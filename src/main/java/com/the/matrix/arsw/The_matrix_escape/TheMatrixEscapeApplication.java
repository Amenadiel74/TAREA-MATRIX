package com.the.matrix.arsw.The_matrix_escape;

import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameConfig;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameState;

import java.util.*;
import java.util.stream.*;

/**
 * Punto de entrada de la aplicacion "The Matrix Escape".
 * Modo simulacion automatica: Neo se mueve con A* y los Agentes con BFS.
 * El tablero se renderiza con colores ANSI y coordenadas.
 */
public class TheMatrixEscapeApplication {

    private static final GameEngine engine = new GameEngine();
    private static final Scanner scanner = new Scanner(System.in);
    private static GameConfig lastConfig;

    private static final String RESET      = "\u001B[0m";
    private static final String GREEN      = "\u001B[92m";
    private static final String RED        = "\u001B[91m";
    private static final String BLUE       = "\u001B[94m";
    private static final String GRAY       = "\u001B[90m";
    private static final String YELLOW     = "\u001B[93m";
    private static final String CYAN       = "\u001B[96m";
    private static final String BOLD       = "\u001B[1m";
    private static final String CLEAR_SCREEN = "\033[H\033[2J";

    /**
     * Punto de entrada de la aplicacion.
     * Muestra la bienvenida, solicita configuracion e inicia la simulacion automatica.
     */
    public static void main(String[] args) {
        welcome();
        lastConfig = askConfig();
        startSim();
    }

    private static void startSim() {
        engine.start(lastConfig);
        autoSim();
    }

    /** Muestra el banner de bienvenida con el titulo del juego. */
    private static void welcome() {
        System.out.print(CLEAR_SCREEN);
        System.out.println(BOLD + CYAN + """
  ╔══════════════════════════════════════════╗
  ║       MATRIX - EL ESCAPE                ║
  ║   Neo debe escapar al telefono...       ║
  ║   ...antes de que los Agentes lo atrapen║
  ╚══════════════════════════════════════════╝""" + RESET);
        sleep(1500);
    }

    /**
     * Solicita al usuario los parametros de configuracion de la partida.
     * @return objeto GameConfig con los valores ingresados (o valores por defecto)
     */
    private static GameConfig askConfig() {
        System.out.print(CLEAR_SCREEN + BOLD + YELLOW + "INSTRUMENTACION" + RESET + "\n");
        return new GameConfig(
            readInt("Filas: ", 8),
            readInt("Columnas: ", 8),
            readInt("Agentes: ", 2),
            readInt("Telefonos: ", 1),
            readInt("Muros: ", 10),
            "SIMULATION"
        );
    }

    /**
     * Lee un entero desde la consola con un mensaje y valor por defecto.
     * @param prompt mensaje mostrado al usuario
     * @param defaultValue valor por defecto si la entrada no es valida
     */
    private static int readInt(String prompt, int defaultValue) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Bucle de simulacion automatica: Neo se mueve solo con A* hacia el telefono
     * mas cercano, los Agentes persiguen con BFS, hasta que el juego termina.
     * Cada turno tiene una pausa de 800ms para visualizar el movimiento.
     */
    private static void autoSim() {
        sleep(1000);

        while (true) {
            render();
            GameState gameState = engine.getState();
            if (!gameState.status().equals("PLAYING")) {
                result(gameState.status());
                return;
            }

            GameEngine.Direction direction = engine.computeAutoDirection();
            if (direction == null) {
                System.out.println(RED + "Neo sin ruta." + RESET);
                return;
            }

            GameState gameResult = engine.processTurn(direction);
            if (!gameResult.status().equals("PLAYING")) {
                render();
                result(gameResult.status());
                return;
            }

            sleep(800);
        }
    }

    /**
     * Renderiza el estado actual del tablero en la consola con colores ANSI.
     * Muestra encabezado con modo/turno/estado, numeros de fila y columna,
     * el tablero coloreado, leyenda y coordenadas de cada entidad.
     */
    private static void render() {
        System.out.print(CLEAR_SCREEN);
        GameState state = engine.getState();
        char[][] boardGrid = state.board();
        int rows = boardGrid.length;
        int cols = rows > 0 ? boardGrid[0].length : 0;

        String modeIcon = "SIM";
        String statusColor = state.status().equals("PLAYING") ? GREEN : RED;
        System.out.println(BOLD + CYAN + "  MATRIX" + RESET
            + "  " + modeIcon
            + "  Turno: " + state.turn()
            + "  " + statusColor + state.status() + RESET);

        System.out.print("     ");
        IntStream.range(0, cols).forEach(c -> System.out.print(GRAY + c % 10 + " " + RESET));
        System.out.println("\n");

        IntStream.range(0, rows).forEach(row -> {
            System.out.print(GRAY + row % 10 + "    " + RESET);
            IntStream.range(0, cols).forEach(col -> {
                char cell = boardGrid[row][col];
                String color = switch (cell) {
                    case 'N' -> GREEN + BOLD;
                    case 'A' -> RED + BOLD;
                    case 'T' -> BLUE + BOLD;
                    case '#' -> GRAY;
                    default  -> GRAY;
                };
                System.out.print(color + cell + " " + RESET);
            });
            System.out.println();
        });

        String neoCoord = findEntity(boardGrid, 'N');
        String phoneCoord = findEntity(boardGrid, 'T');
        String agentsCoords = findAllEntities(boardGrid, 'A');

        System.out.println("\n  " + GREEN + "N" + RESET + "=Neo  "
            + RED + "A" + RESET + "=Agente  "
            + BLUE + "T" + RESET + "=Telefono  "
            + GRAY + "#" + RESET + "=Muro");
        System.out.println("  " + GREEN + "Neo: " + neoCoord + RESET
            + "  " + BLUE + "Tel: " + phoneCoord + RESET
            + "  " + RED + "Agentes:" + agentsCoords + RESET);
    }

    /**
     * Busca la primera ocurrencia de un caracter en el tablero
     * y devuelve sus coordenadas formateadas como "(fila,columna)".
     */
    private static String findEntity(char[][] grid, char target) {
        return IntStream.range(0, grid.length)
            .boxed()
            .flatMap(row -> IntStream.range(0, grid[0].length)
                .filter(col -> grid[row][col] == target)
                .limit(1)
                .mapToObj(col -> "(" + row + "," + col + ")"))
            .findFirst()
            .orElse("?");
    }

    /**
     * Busca todas las ocurrencias de un caracter en el tablero
     * y devuelve sus coordenadas separadas por espacio.
     */
    private static String findAllEntities(char[][] grid, char target) {
        return IntStream.range(0, grid.length)
            .boxed()
            .flatMap(row -> IntStream.range(0, grid[0].length)
                .filter(col -> grid[row][col] == target)
                .mapToObj(col -> "(" + row + "," + col + ")"))
            .collect(Collectors.joining(" "));
    }

    /**
     * Muestra el resultado final de la partida y pregunta si desea jugar otra vez.
     */
    private static void result(String resultStatus) {
        System.out.println(resultStatus.equals("NEO_WINS")
            ? BOLD + GREEN + "\n  NEO ESCAPO" + RESET
            : BOLD + RED + "\n  AGENTES GANARON" + RESET);
        System.out.print(YELLOW + "Otra? (s=repetir config | n=salir | c=cambiar config): " + RESET);
        String input = scanner.nextLine().trim().toLowerCase();
        if (input.equals("s")) {
            startSim();
        } else if (input.equals("c")) {
            main(new String[]{});
        } else {
            System.out.println(CYAN + "Gracias." + RESET);
        }
    }

    /** Pausa la ejecucion por una cantidad de milisegundos. */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
