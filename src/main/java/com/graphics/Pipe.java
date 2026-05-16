package com.graphics;

/**
 * Pipe: representa un par de tuberías (superior e inferior).
 *
 * Cada tubería tiene:
 * - x: posición horizontal, empieza en el borde derecho y avanza a la izquierda
 * - gapCentroY: centro vertical del hueco entre las dos tuberías
 * - puntuada: evita contar el punto dos veces
 *
 * Sabe moverse sola (actualizar) y dibujarse sola (dibujar).
 */
public class Pipe {

    // Tamaño del hueco entre tubería superior e inferior
    public static final float GAP_ALTO      = 0.48f;
    // Ancho de cada tubería
    public static final float ANCHO         = 0.18f;

    // Posición horizontal actual
    public float x;
    // Centro vertical del hueco
    public float gapCentroY;
    // ¿Ya se sumó el punto por pasar esta tubería?
    public boolean puntuada;

    public Pipe(float x, float gapCentroY) {
        this.x = x;
        this.gapCentroY = gapCentroY;
        this.puntuada = false;
    }

    /**
     * Mueve la tubería hacia la izquierda.
     * velocidad = qué tan rápido se mueve (aumenta con la dificultad)
     * dt        = tiempo del frame en segundos
     */
    public void actualizar(float dt, float velocidad) {
        x -= velocidad * dt;
    }

    /**
     * ¿La tubería ya salió de la pantalla por la izquierda?
     * Si es así, el juego la puede eliminar para liberar memoria.
     */
    public boolean fueraDePantalla() {
        return x + ANCHO * 0.5f < -1.3f;
    }

    /**
     * Dibuja las dos partes de la tubería (superior e inferior)
     * usando el renderer.
     *
     * La lógica es:
     * - El hueco va de (gapCentroY - GAP_ALTO/2) a (gapCentroY + GAP_ALTO/2)
     * - La tubería superior va desde el tope del hueco hasta +1.0 (arriba)
     * - La tubería inferior va desde -1.0 (abajo) hasta el fondo del hueco
     */
    public void dibujar(Renderer renderer) {
        float gapTop    = gapCentroY + GAP_ALTO * 0.5f;
        float gapBottom = gapCentroY - GAP_ALTO * 0.5f;

        // Tubería superior — verde
        float altoSup = 1.0f - gapTop;
        if (altoSup > 0.0f) {
            float yCentroSup = gapTop + altoSup * 0.5f;
            renderer.dibujarRect(x, yCentroSup, ANCHO, altoSup, 0.18f, 0.70f, 0.25f);
        }

        // Tubería inferior — verde
        float altoInf = gapBottom + 1.0f;
        if (altoInf > 0.0f) {
            float yCentroInf = -1.0f + altoInf * 0.5f;
            renderer.dibujarRect(x, yCentroInf, ANCHO, altoInf, 0.18f, 0.70f, 0.25f);
        }
    }

    /**
     * Detecta colisión AABB entre esta tubería y un pájaro.
     * AABB = Axis-Aligned Bounding Box — chequea si dos rectángulos se tocan.
     *
     * Primero verifica si hay overlap horizontal (¿están en la misma X?).
     * Si sí, verifica si el pájaro está fuera del hueco.
     */
    public boolean colisionaCon(Bird bird) {
        float birdLeft   = bird.x - Bird.ANCHO * 0.5f;
        float birdRight  = bird.x + Bird.ANCHO * 0.5f;
        float birdTop    = bird.y + Bird.ALTO  * 0.5f;
        float birdBottom = bird.y - Bird.ALTO  * 0.5f;

        float pipeLeft  = x - ANCHO * 0.5f;
        float pipeRight = x + ANCHO * 0.5f;

        // Si no se solapan en X, no hay colisión — salimos rápido
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) return false;

        // Hay overlap en X — ¿está el pájaro fuera del hueco?
        float gapTop    = gapCentroY + GAP_ALTO * 0.5f;
        float gapBottom = gapCentroY - GAP_ALTO * 0.5f;
        return birdTop > gapTop || birdBottom < gapBottom;
    }
}