package a_lexico_robot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class CoppeliaIntegrador {

    // Rutas configurables - se detectarán automáticamente
    private static String COPPELIA_PATH = null;
    private static final String SCENE_PATH = "C:\\DOSBox2\\Tasm\\Robot2.ttt";
    private static final String SCRIPT_DIR = "C:\\DOSBox2\\Tasm";

    // Rutas posibles donde puede estar CoppeliaSim
    private static final String[] RUTAS_POSIBLES = {
        "C:\\Program Files\\CoppeliaRobotics\\CoppeliaSim_Edu\\coppeliaSim.exe",
        "C:\\Program Files (x86)\\CoppeliaRobotics\\CoppeliaSim_Edu\\coppeliaSim.exe",
        "C:\\Program Files\\CoppeliaRobotics\\CoppeliaSim\\coppeliaSim.exe",
        "C:\\Program Files (x86)\\CoppeliaRobotics\\CoppeliaSim\\coppeliaSim.exe",
        System.getProperty("user.home") + "\\AppData\\Local\\CoppeliaRobotics\\CoppeliaSim_Edu\\coppeliaSim.exe"
    };

    public static void ejecutarSimulacion(List<Integer> valores) {
        try {
            // 1. Detectar CoppeliaSim automáticamente
            if (COPPELIA_PATH == null) {
                COPPELIA_PATH = detectarCoppeliaSim();
                if (COPPELIA_PATH == null) {
                    JOptionPane.showMessageDialog(null, "No se pudo encontrar CoppeliaSim automáticamente.");
                    return;
                }
            }

            // 2. Generar código Lua
            String codigoLua = GeneradorLua.generarCodigoLua(valores);

            // 3. Guardar script Lua
            File scriptDir = new File(SCRIPT_DIR);
            if (!scriptDir.exists()) {
                scriptDir.mkdirs();
            }

            GeneradorLua.guardarCodigoLua(SCRIPT_DIR, "robotScript", codigoLua);

            // 4. Mostrar código generado al usuario
            mostrarCodigoGenerado(codigoLua);

            // 5. Ejecutar CoppeliaSim con la escena
            ejecutarCoppeliaSim();

            JOptionPane.showMessageDialog(null,
                    "Simulación iniciada en CoppeliaSim\n"
                    + "Script guardado en: " + SCRIPT_DIR + "\\robotScript.lua\n"
                    + "CoppeliaSim encontrado en: " + COPPELIA_PATH + "\n\n"
                    + "Para usar el script:\n"
                    + "1. En CoppeliaSim, selecciona el robot\n"
                    + "2. Abre el script del robot\n"
                    + "3. Copia y pega el código generado\n"
                    + "4. Ejecuta la simulación",
                    "Simulación CoppeliaSim",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error al ejecutar simulación: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String detectarCoppeliaSim() {
        System.out.println("Buscando CoppeliaSim en rutas comunes...");

        for (String ruta : RUTAS_POSIBLES) {
            File archivo = new File(ruta);
            if (archivo.exists()) {
                System.out.println("CoppeliaSim encontrado en: " + ruta);
                return ruta;
            }
        }

        System.out.println("CoppeliaSim no encontrado en rutas comunes. Pidiendo al usuario...");

        // Si no se encuentra automáticamente, preguntar al usuario
        JFileChooser chooser = new JFileChooser("C:\\Program Files");
        chooser.setDialogTitle("Selecciona coppeliaSim.exe");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ejecutables", "exe"));

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            String rutaSeleccionada = chooser.getSelectedFile().getAbsolutePath();
            System.out.println("Usuario seleccionó: " + rutaSeleccionada);
            return rutaSeleccionada;
        }

        return null;
    }

    private static void ejecutarCoppeliaSim() {
        try {
            // Verificar que la escena existe
            File sceneFile = new File(SCENE_PATH);
            if (!sceneFile.exists()) {
                System.out.println("Escena no encontrada en: " + SCENE_PATH);
                // Ejecutar sin escena específica
                ProcessBuilder builder = new ProcessBuilder(COPPELIA_PATH);
                builder.start();
                System.out.println("CoppeliaSim ejecutado sin escena específica");
                return;
            }

            // Ejecutar con escena
            ProcessBuilder builder = new ProcessBuilder(COPPELIA_PATH, SCENE_PATH);
            builder.redirectErrorStream(true);
            Process proceso = builder.start();

            // Leer la salida del proceso en un hilo separado
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        System.out.println("CoppeliaSim: " + linea);
                    }
                } catch (IOException e) {
                    System.err.println("Error leyendo salida de CoppeliaSim: " + e.getMessage());
                }
            }).start();

            System.out.println("CoppeliaSim ejecutado exitosamente con escena: " + SCENE_PATH);

        } catch (IOException e) {
            throw new RuntimeException("Error al ejecutar CoppeliaSim: " + e.getMessage());
        }
    }

    private static void mostrarCodigoGenerado(String codigo) {
        // Crear ventana para mostrar el código generado
        javax.swing.JFrame ventana = new javax.swing.JFrame("Código Lua Generado");
        javax.swing.JTextArea area = new javax.swing.JTextArea(codigo);
        area.setEditable(false);
        area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(area);
        scroll.setPreferredSize(new java.awt.Dimension(600, 400));

        ventana.add(scroll);
        ventana.pack();
        ventana.setLocationRelativeTo(null);
        ventana.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        ventana.setVisible(true);
    }

    // Método para configurar rutas personalizadas
    public static void configurarRutas(String coppeliaPath, String scenePath, String scriptDir) {
        COPPELIA_PATH = coppeliaPath;
        // SCENE_PATH y SCRIPT_DIR son final, pero puedes crear variables si necesitas cambiarlas
        System.out.println("Rutas configuradas manualmente:");
        System.out.println("CoppeliaSim: " + coppeliaPath);
        System.out.println("Escena: " + scenePath);
        System.out.println("Scripts: " + scriptDir);
    }
}
