package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class AppFlappyBird {

    // ── VENTANA ───────────────────────────────────────────────────────
    private static final int ANCHO_VENTANA = 900;
    private static final int ALTO_VENTANA  = 700;

    // ── TUBERÍAS ──────────────────────────────────────────────────────
    // Cada cuántos segundos aparece una tubería nueva
    private static final float TIEMPO_ENTRE_TUBERIAS = 1.5f;
    // Velocidad inicial de las tuberías
    private static final float VELOCIDAD_BASE = 0.62f;
    // Velocidad máxima — para que no se vuelva injugable
    private static final float VELOCIDAD_MAX  = 1.4f;
    // Rango vertical del hueco
    private static final float GAP_MIN = -0.45f;
    private static final float GAP_MAX =  0.45f;

    //limite de puntaje
    private static final int PUNTOS_MAXIMOS =  3;

    // ── COMPONENTES ───────────────────────────────────────────────────
    private long     window;    // referencia a la ventana GLFW
    private Renderer renderer;  // maneja todo OpenGL

    // Jugador 1: amarillo, posición izquierda — salta con SPACE
    private Bird bird1;
    // Jugador 2: rojo, un poco más a la derecha — salta con W
    private Bird bird2;
    // Jugador 3: azul, un poco más a la derecha — salta con up
    private Bird bird3;

    // Lista de tuberías activas en pantalla
    private final List<Pipe>  pipes  = new ArrayList<>();
    private final Random      random = new Random();

    // ── ESTADO DEL JUEGO ──────────────────────────────────────────────
    private boolean started;   // ¿ya empezó la partida?
    private boolean gameOver;  // ¿terminó la partida?
    private float   timerSpawn;         // temporizador para spawnear tuberías
    private float   velocidadActual;    // velocidad actual de las tuberías

    // Detección de flanco de teclas (evita disparar múltiples saltos)
    // "flanco" = detectar el momento exacto en que se presiona, no mientras se mantiene
    private boolean prevSpace;
    private boolean prevW;
    private boolean prevR;
    private boolean prevUP;
    

    // ── FLUJO PRINCIPAL ───────────────────────────────────────────────

    public void run() {
        init();
        resetGame();
        loop();
        cleanup();
    }

    private void init() {
        // Iniciar GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        // Configurar ventana OpenGL 3.3
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,                GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE,              GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,  3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,  3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,         GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT,  GLFW.GLFW_TRUE);

        // Crear ventana
        window = GLFW.glfwCreateWindow(
            ANCHO_VENTANA, ALTO_VENTANA, "Flappy Bird", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // VSync
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();  // cargar funciones OpenGL

        // Inicializar renderer (compila shaders, sube quad a GPU)
        renderer = new Renderer();
        renderer.init();

        // Crear los dos pájaros con distintos colores y posiciones
        // Bird(x, r, g, b)
        bird1 = new Bird(-0.45f, 0.98f, 0.85f, 0.20f); // amarillo
        bird2 = new Bird(-0.30f, 0.95f, 0.20f, 0.20f); // rojo
        bird3 = new Bird(-0.15f, 0.20f, 0.20f, 0.95f); // azul
    }

    /**
     * Reinicia todo el estado del juego.
     * Se llama al inicio y cuando los jugadores presionan R tras game over.
     */
    private void resetGame() {
        bird1.reset();
        bird2.reset();
        bird3.reset();
        pipes.clear();
        timerSpawn    = 0.0f;
        velocidadActual = VELOCIDAD_BASE;
        started  = false;
        gameOver = false;
        actualizarTitulo();
    }

    // ── INPUT ─────────────────────────────────────────────────────────

    private void procesarInput() {
        // ESC cierra el juego
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        // ── Jugador 1: SPACE ──────────────────────────────────────────
        // Detección de flanco: solo actúa cuando la tecla se acaba
        // de presionar (spaceAhora=true, prevSpace=false)
        boolean spaceAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spaceAhora && !prevSpace) {
            if (gameOver) {
                resetGame();
                started = true;
            }
            // Solo salta si sigue vivo
            if (bird1.vivo) {
                started = true;
                bird1.saltar();
            }
        }
        prevSpace = spaceAhora;

        // ── Jugador 2: W ──────────────────────────────────────────────
        boolean wAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        if (wAhora && !prevW) {
            if (gameOver) {
                resetGame();
                started = true;
            }
            if (bird2.vivo) {
                started = true;
                bird2.saltar();
            }
        }
        prevW = wAhora;

        // ── Jugador 3: flechita ──────────────────────────────────────────────
        boolean upAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        if (upAhora && !prevUP) {
            if (gameOver) {
                resetGame();
                started = true;
            }
            if (bird3.vivo) {
                started = true;
                bird3.saltar();
            }
        }
        prevUP = upAhora;


        // ── R: reiniciar (solo en game over) ──────────────────────────
        boolean rAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rAhora && !prevR && gameOver) {
            resetGame();
        }
        prevR = rAhora;
    }

    //  VELOCIDAD Y LÓGICA DE PAJAROS 
    private void actualizar(float dt) {
        if (!started || gameOver) return;

        // Actualizar física de cada pájaro vivo
        if (bird1.vivo) bird1.actualizar(dt);
        if (bird2.vivo) bird2.actualizar(dt);
        if (bird3.vivo) bird3.actualizar(dt);

        // Si ambos están muertos, game over
        if (!bird1.vivo && !bird2.vivo && !bird3.vivo) {
            gameOver = true;
            actualizarTitulo();
            return;
        }

        // ── Dificultad progresiva (requerimiento 3) ───────────────────
        // El puntaje más alto entre los dos jugadores define la velocidad.
        // Cada 5 puntos se suma 0.08 a la velocidad, hasta VELOCIDAD_MAX.
        int mejorP1 = Math.max(bird1.puntaje, bird2.puntaje);
        int mejorPuntaje = Math.max(mejorP1, bird3.puntaje);
        velocidadActual = Math.min(
            VELOCIDAD_BASE + (mejorPuntaje / 5) * 0.08f,
            VELOCIDAD_MAX
        );

        // ── Spawn de tuberías ─────────────────────────────────────────
        timerSpawn += dt;
        if (timerSpawn >= TIEMPO_ENTRE_TUBERIAS) {
            timerSpawn = 0.0f;
            float gapCentro = GAP_MIN + random.nextFloat() * (GAP_MAX - GAP_MIN);
            pipes.add(new Pipe(1.2f, gapCentro));
        }

        // ── Actualizar tuberías ───────────────────────────────────────
        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.actualizar(dt, velocidadActual);

            // ¿El pájaro 1 pasó esta tubería?
            if (!p.puntuada && p.x + Pipe.ANCHO * 0.5f < bird1.x) {
                p.puntuada = true;
                // Solo suma punto si el pájaro sigue vivo
                if (bird1.vivo) bird1.puntaje++;
                if (bird2.vivo) bird2.puntaje++;
                if (bird3.vivo) bird3.puntaje++;
                actualizarTitulo();
            }

            // Colisiones — si choca, ese pájaro muere
            if (bird1.vivo && p.colisionaCon(bird1)) {
                bird1.vivo = false;
            }
            if (bird2.vivo && p.colisionaCon(bird2)) {
                bird2.vivo = false;
            }
            if (bird3.vivo && p.colisionaCon(bird3)) {
                bird3.vivo = false;
            }

            // puntaje igual a muerte pero luego lo haremos volar al cielo y toque cielo termina juego 
            // muerte al que llegue a limite de puntos
            if (bird1.puntaje >=PUNTOS_MAXIMOS) {
                if (bird1.vivo) bird1.actualizar(dt);
                // bird1.vivo = false;
                // actualizar a donde se va volando
            }
            if (bird2.puntaje >=PUNTOS_MAXIMOS) {
                if (bird2.vivo) bird2.actualizar(dt);
            }
            if (bird3.puntaje >=PUNTOS_MAXIMOS) {
                if (bird3.vivo) bird3.actualizar(dt);
            }

            // Eliminar tuberías que ya salieron de pantalla
            if (p.fueraDePantalla()) {
                it.remove();
            }
        }

        // PUNTAJE MAXIMO LLEGA AL CIELO Y TERMINA
        // Iterator<Pipe> ptmax = pipes.iterator();
        // while (ptmax.hasNext()) {
        //     // Pipe p = ptmax.next();
        //     // p.actualizar(dt, velocidadActual);

        //     // ¿El pájaro 1 pasó esta tubería?
        //     if (!p.puntuada && p.x + Pipe.ANCHO * 0.5f < bird1.x) {
        //         p.puntuada = true;
        //         // Solo suma punto si el pájaro sigue vivo
        //         if (bird1.vivo) bird1.puntaje++;
        //         if (bird2.vivo) bird2.puntaje++;
        //         if (bird3.vivo) bird3.puntaje++;
        //         actualizarTitulo();
        //     }

        //     // muerte al que llegue a limite de puntos
        //     if (bird1.puntaje >=5) {
        //         bird1.vivo = false;
        //     }
        //     if (bird2.puntaje >=5) {
        //         bird2.vivo = false;
        //     }
        //     if (bird3.puntaje >=5) {
        //         bird3.vivo = false;
        //     }

        //     // // Eliminar tuberías que ya salieron de pantalla
        //     // if (p.fueraDePantalla()) {
        //     //     ptmax.remove();
        //     // }
        // }
        

        // Verificar de nuevo si ambos murieron en este frame
        if (!bird1.vivo && !bird2.vivo && !bird3.vivo) {
            gameOver = true;
            actualizarTitulo();
        }
    }

    // ── RENDER ────────────────────────────────────────────────────────
    private void render() {
        // Fondo — celeste
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Activar shaders y VAO una sola vez para todo el frame
        renderer.iniciarFrame();

        // ── Nubes — rectángulos blancos estáticos en el fondo ────────────
        // Cada llamada es: dibujarRect(x, y, ancho, alto, r, g, b)
        renderer.dibujarRect(-0.5f,  0.6f, 0.30f, 0.10f, 1.0f, 1.0f, 1.0f);
        renderer.dibujarRect(-0.5f,  0.6f, 0.20f, 0.08f, 1.0f, 1.0f, 1.0f);
        renderer.dibujarRect( 0.3f,  0.7f, 0.25f, 0.09f, 1.0f, 1.0f, 1.0f);
        renderer.dibujarRect( 0.3f,  0.7f, 0.15f, 0.07f, 1.0f, 1.0f, 1.0f);
        renderer.dibujarRect(-0.1f,  0.4f, 0.20f, 0.08f, 1.0f, 1.0f, 1.0f);

        // ── Suelo — franja verde en la parte inferior ─────────────────────
        renderer.dibujarRect(0.0f, -0.93f, 2.0f, 0.14f, 0.22f, 0.68f, 0.13f);

        // ── Tuberías ──────────────────────────────────────────────────────
        for (Pipe p : pipes) {
            p.dibujar(renderer);
        }

        // ── Pájaros vivos ─────────────────────────────────────────────────
        if (bird1.vivo) bird1.dibujar(renderer);
        if (bird2.vivo) bird2.dibujar(renderer);
        if (bird3.vivo) bird3.dibujar(renderer);

        // ── Pantalla de inicio ────────────────────────────────────────────
        // Se muestra antes de que empiece la partida
        if (!started) {
            // Fondo oscuro semitransparente
            renderer.dibujarRect( 0.0f,  0.1f, 1.2f, 0.50f, 0.10f, 0.10f, 0.15f);
            // Línea decorativa arriba — amarilla (jugador 1)
            renderer.dibujarRect( 0.0f,  0.28f, 1.0f, 0.04f, 0.98f, 0.85f, 0.20f);
            // Línea decorativa abajo — roja (jugador 2)
            renderer.dibujarRect( 0.0f, -0.08f, 1.0f, 0.04f, 0.95f, 0.20f, 0.20f);
            // Línea central blanca
            renderer.dibujarRect( 0.0f,  0.10f, 0.8f, 0.02f, 1.0f,  1.0f,  1.0f);
        }

        // ── Pantalla de game over ─────────────────────────────────────────
        if (gameOver) {
            // Fondo oscuro
            renderer.dibujarRect(0.0f, 0.0f, 1.4f, 0.60f, 0.10f, 0.10f, 0.15f);
            // Franja roja arriba y abajo
            renderer.dibujarRect(0.0f,  0.25f, 1.2f, 0.06f, 0.90f, 0.15f, 0.15f);
            renderer.dibujarRect(0.0f, -0.25f, 1.2f, 0.06f, 0.90f, 0.15f, 0.15f);

            // Determinar ganador y mostrar su color grande en el centro
            if (bird1.puntaje > bird2.puntaje) {
                // Ganó J1 — amarillo grande arriba, pequeño abajo
                renderer.dibujarRect(0.0f,  0.08f, 0.8f, 0.08f, 0.98f, 0.85f, 0.20f);
                renderer.dibujarRect(0.0f, -0.08f, 0.4f, 0.03f, 0.95f, 0.20f, 0.20f);
            } else if (bird2.puntaje > bird1.puntaje) {
                // Ganó J2 — rojo grande arriba, pequeño abajo
                renderer.dibujarRect(0.0f,  0.08f, 0.8f, 0.08f, 0.95f, 0.20f, 0.20f);
                renderer.dibujarRect(0.0f, -0.08f, 0.4f, 0.03f, 0.98f, 0.85f, 0.20f);
            } else {
                // Empate — ambos colores iguales
                renderer.dibujarRect(0.0f,  0.08f, 0.8f, 0.04f, 0.98f, 0.85f, 0.20f);
                renderer.dibujarRect(0.0f, -0.08f, 0.8f, 0.04f, 0.95f, 0.20f, 0.20f);
            }
        }
    }   

    // ── TÍTULO ────────────────────────────────────────────────────────

    /**
     * Actualiza el título de la ventana con el puntaje de ambos jugadores
     * y el estado actual del juego.
     * También muestra la velocidad actual para cumplir el requerimiento 3.
     */
    private void actualizarTitulo() {
        String velocidad = String.format("%.2f", velocidadActual);
        String base = "J1(SPACE): " + bird1.puntaje
                    + "  |  J2(W): " + bird2.puntaje
                    + "  |  J3(UP): " + bird3.puntaje
                    + "  |  Vel: " + velocidad
                    ;
        if (!started) {
            GLFW.glfwSetWindowTitle(window, base + "  |  SPACE o W para empezar");
        } else if (gameOver) {
            GLFW.glfwSetWindowTitle(window, base + "  |  GAME OVER - R para reiniciar");
        } else {
            GLFW.glfwSetWindowTitle(window, base);
        }
    }

    // ── LOOP PRINCIPAL ────────────────────────────────────────────────

    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt = Math.min(ahora - ultimoTiempo, 0.033f);
            ultimoTiempo = ahora;

            procesarInput();
            actualizar(dt);
            render();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    // ── CLEANUP ───────────────────────────────────────────────────────

    private void cleanup() {
        renderer.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    // Entry point
    public static void main(String[] args) {
        new AppFlappyBird().run();
    }
}