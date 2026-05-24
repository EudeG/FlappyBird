package com.graphics;

/**
 * Bird: representa un jugador (pájaro).
 * Guarda su posición, velocidad, estado y puntaje.
 * También sabe dibujarse a sí mismo usando el Renderer.
 */
public class Bird {

    // Posición horizontal fija en NDC
    public final float x;
    // Posición vertical (cambia con la física)
    public float y;
    // Velocidad vertical actual
    public float velY;
    // Puntaje individual
    public int puntaje;
    // ¿Está vivo?
    public boolean vivo;

    // Tamaño del pájaro (igual para ambos jugadores)
    public static final float ANCHO = 0.10f;
    public static final float ALTO  = 0.10f;

    // Color del cuerpo — cada jugador tiene el suyo
    private final float colorR, colorG, colorB;

    // Física
    public static final float GRAVEDAD          = -1.9f;
    public static final float IMPULSO_SALTO     =  0.85f;
    public static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    public Bird(float x, float colorR, float colorG, float colorB) {
        this.x = x;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
        reset();
    }

    /** Reinicia el pájaro a su estado inicial */
    public void reset() {
        y    = 0.0f;
        velY = 0.0f;
        puntaje = 0;
        vivo = true;
    }

    /** Aplica un impulso hacia arriba (salto) */
    public void saltar() {
        velY = IMPULSO_SALTO;
    }

    /**
     * Actualiza la física del pájaro.
     * dt = tiempo transcurrido desde el último frame en segundos.
     * Retorna false si el pájaro chocó con el techo o el suelo.
     */
    public boolean actualizar(float dt) {

        if (puntaje>=3){
            y += 0.5f * dt;
            return false;
        }

        velY += GRAVEDAD * dt;
        if (velY < VELOCIDAD_MAX_CAIDA) {
            velY = VELOCIDAD_MAX_CAIDA;
        }
        y += velY * dt;

        // Chocó con techo

        if ( y + ALTO * 0.5f >= 1.0f) {
            vivo = false;
            return false;
        }
        //choco con suelo
        if (y - ALTO * 0.5f <= -1.0f) {
            vivo = false;
            return false;
        }

        return true;
    }

    /**
     * Dibuja el pájaro completo usando el renderer.
     * Todas las partes son relativas a (x, y) — el centro del cuerpo.
     */
    public void dibujar(Renderer renderer) {
        // Cuerpo principal
        renderer.dibujarRect(x, y, ANCHO, ALTO, colorR, colorG, colorB);

        // Pico - a la derecha
        renderer.dibujarRect(x + 0.07f, y, 0.04f, 0.03f, 1.0f, 0.5f, 0.0f);

        // Ojo - blanco
        renderer.dibujarRect(x + 0.02f, y + 0.03f, 0.03f, 0.03f, 1.0f, 1.0f, 1.0f);

        // Pupila - negra
        renderer.dibujarRect(x + 0.03f, y + 0.03f, 0.015f, 0.015f, 0.0f, 0.0f, 0.0f);

        // Ala - azul claro
        renderer.dibujarRect(x - 0.01f, y - 0.03f, 0.06f, 0.025f, 0.4f, 0.7f, 1.0f);

        // Cola
        renderer.dibujarRect(x - 0.06f, y, 0.03f, 0.04f, 0.9f, 0.4f, 0.1f);
    }
}