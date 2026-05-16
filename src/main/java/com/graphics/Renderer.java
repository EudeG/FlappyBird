package com.graphics;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Renderer: maneja todo lo relacionado con OpenGL.
 * - Compila y guarda los shaders
 * - Crea el VAO/VBO con el quad base
 * - Expone dibujarRect() para que cualquier clase pueda dibujar
 *
 * Ninguna otra clase debería llamar funciones GL directamente,
 * todo pasa por aquí.
 */
public class Renderer {

    private int programa;       // el programa de shaders enlazados
    private int vao;            // recuerda cómo leer los vértices
    private int vbo;            // los vértices en memoria de la GPU

    // Ubicaciones de los uniforms en el shader
    // (uniform = variable que mandamos desde Java al shader)
    private int uOffsetLocation;  // dónde dibujar (X, Y)
    private int uScaleLocation;   // qué tan grande (ancho, alto)
    private int uColorLocation;   // de qué color (R, G, B)

    /**
     * init() debe llamarse UNA sola vez al arrancar el juego,
     * después de haber creado la ventana y el contexto OpenGL.
     * Aquí compilamos shaders y subimos el quad base a la GPU.
     */
    public void init() {
        crearShaders();
        crearQuadBase();
    }

    private void crearShaders() {
        // VERTEX SHADER
        // Recibe cada vértice del quad base (-0.5 a 0.5)
        // y lo transforma con escala y offset para colocarlo
        // donde queremos en pantalla.
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            void main() {
                vec2 finalPos = aPos.xy * uScale + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        // FRAGMENT SHADER
        // Recibe el color uniforme y lo aplica a cada píxel.
        // vec4(uColor, 1.0) = RGB del uniform + alpha 1.0 (sólido)
        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        // Compilar vertex shader
        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertexSrc);
        GL20.glCompileShader(vs);
        verificar(vs, "Vertex");

        // Compilar fragment shader
        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fs, fragmentSrc);
        GL20.glCompileShader(fs);
        verificar(fs, "Fragment");

        // Enlazar ambos en un programa
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vs);
        GL20.glAttachShader(programa, fs);
        GL20.glLinkProgram(programa);

        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar shaders: "
                + GL20.glGetProgramInfoLog(programa));
        }

        // Ya no necesitamos los shaders sueltos
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        // Obtener las ubicaciones de los uniforms
        // Si alguno es -1, el shader no lo encontró — error grave
        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation  = GL20.glGetUniformLocation(programa, "uScale");
        uColorLocation  = GL20.glGetUniformLocation(programa, "uColor");
    }

    private void verificar(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader error: "
                + GL20.glGetShaderInfoLog(shader));
        }
    }

    /**
     * El quad base es un rectángulo unitario centrado en (0,0),
     * de -0.5 a +0.5 en X e Y. Son 2 triángulos = 6 vértices.
     *
     * Para dibujar cualquier rectángulo en pantalla usamos este
     * mismo quad y lo escalamos/movemos con los uniforms.
     * Así no necesitamos crear un VBO nuevo para cada figura.
     */
    private void crearQuadBase() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,  // triángulo 1
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,  // triángulo 2
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // 3 floats por vértice (X, Y, Z), sin espacios extra
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Prepara OpenGL para dibujar — se llama una vez al inicio de render().
     * Activa el programa de shaders y el VAO.
     */
    public void iniciarFrame() {
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);
    }

    /**
     * Dibuja un rectángulo en pantalla.
     * x, y     = centro del rectángulo en NDC (-1 a +1)
     * ancho, alto = tamaño en NDC
     * r, g, b  = color (0.0 a 1.0)
     */
    public void dibujarRect(float x, float y, float ancho, float alto,
                             float r, float g, float b) {
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, ancho, alto);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    /** Libera la memoria de GPU al cerrar el juego */
    public void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
    }
}