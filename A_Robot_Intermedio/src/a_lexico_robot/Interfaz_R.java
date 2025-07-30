package a_lexico_robot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import static a_lexico_robot.TraductorCuadruplaASM.compilarConDosboxDirecto;
import static a_lexico_robot.TraductorCuadruplaASM.guardarComoASM;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Denis Peralta package a_lexico_robot; import static
 * a_lexico_robot.Token.*; import a_lexico_robot.Token;
 */
public class Interfaz_R extends JFrame {

    JTextArea inputArea = new JTextArea(20, 40);
    DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Tipo", "Lexema"}, 0);
    JTable tablaTokens = new JTable(tableModel);

    DefaultTableModel modeloSimbolos = new DefaultTableModel(new Object[]{"ID", "Método", "Rango", "Valor"}, 0);
    JTable tablaSimbolos = new JTable(modeloSimbolos);

    //
    DefaultTableModel cuadrupla = new DefaultTableModel(new Object[]{"ID", "Operador", "Operando 1", "Operando 2", "Resultado"}, 0);
    JTable tablaCuadrupla = new JTable(cuadrupla);

    JTextArea areaErrores = new JTextArea(5, 40);
    JLabel mensajeSintactico = new JLabel(" ");

    JButton abrirBtn = new JButton("Abrir archivo");
    JButton guardarBtn = new JButton("Guardar archivo");
    JButton analizarBtn = new JButton("Analizar");
    JButton limpiarBtn = new JButton("Limpiar");
    JButton generarBinaryCode = new JButton("Generar archivo binario");

    PanelNumerosLinea panelNumeros = new PanelNumerosLinea(); // << Nueva instancia

    A_Sintactico parser;  // to keep the parser after analyzing

    public Interfaz_R() {
        super("Analizador Robot");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Panel superior (franja naranja) ---
        JPanel superior = new JPanel(new BorderLayout());
        superior.setBackground(new Color(255, 102, 0));
        superior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titulo = new JLabel("", JLabel.CENTER);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        superior.add(titulo, BorderLayout.CENTER);
        add(superior, BorderLayout.NORTH);

        // --- Panel izquierdo: Instrucciones con números de línea ---
        inputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollInstrucciones = new JScrollPane(inputArea);
        scrollInstrucciones.setBorder(BorderFactory.createTitledBorder("Instrucciones"));

        panelNumeros.setTextArea(inputArea);
        scrollInstrucciones.setRowHeaderView(panelNumeros); // << Agrega panel de números

        //asdasd
        JScrollPane scrollIntermedio = new JScrollPane(tablaCuadrupla);
        scrollIntermedio.setBorder(BorderFactory.createTitledBorder("Lenguaje intermedio - Cuadrupla"));

        // --- Panel derecho: Tokens, símbolos, errores ---
        JPanel panelDerecho = new JPanel();
        panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));

        JScrollPane scrollTokens = new JScrollPane(tablaTokens);
        scrollTokens.setBorder(BorderFactory.createTitledBorder("Tabla de Tokens"));

        JScrollPane scrollSimbolos = new JScrollPane(tablaSimbolos);
        scrollSimbolos.setBorder(BorderFactory.createTitledBorder("Tabla de Símbolos"));

        JScrollPane scrollErrores = new JScrollPane(areaErrores);
        scrollErrores.setBorder(BorderFactory.createTitledBorder("Errores Semánticos, Sintacticos"));
        areaErrores.setEditable(false);
        areaErrores.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaErrores.setForeground(Color.RED.darker());

        panelDerecho.add(scrollTokens);
        panelDerecho.add(scrollSimbolos);
        panelDerecho.add(scrollErrores);
        panelDerecho.add(scrollIntermedio);

        // --- Panel central (split) ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollInstrucciones, panelDerecho);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        // --- Panel inferior: Botones y mensajes ---
        JPanel panelBotones = new JPanel();
        panelBotones.add(abrirBtn);
        panelBotones.add(guardarBtn);
        panelBotones.add(analizarBtn);
        panelBotones.add(limpiarBtn);
        panelBotones.add(generarBinaryCode);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.add(panelBotones, BorderLayout.CENTER);
        inferior.add(mensajeSintactico, BorderLayout.SOUTH);
        mensajeSintactico.setHorizontalAlignment(SwingConstants.CENTER);
        mensajeSintactico.setFont(new Font("Arial", Font.BOLD, 14));

        add(inferior, BorderLayout.SOUTH);

        // --- Acciones ---
        abrirBtn.addActionListener(e -> abrirArchivo());
        guardarBtn.addActionListener(e -> guardarArchivo());
        limpiarBtn.addActionListener(e -> limpiarTodo());

        generarBinaryCode.addActionListener(e -> {
            System.out.println("=== BOTÓN PRESIONADO: Generar archivo binario ===");

            if (parser == null) {
                System.out.println("ERROR: Parser es null");
                JOptionPane.showMessageDialog(null, "Primero debes analizar el código.");
                return;
            }

            List<Integer> valores = parser.extraerValoresNumericos();
            System.out.println("Valores extraídos del parser: " + valores);

            if (valores.size() < 6) {
                System.out.println("ERROR: Insuficientes valores - " + valores.size());
                JOptionPane.showMessageDialog(null, "No hay suficientes valores para generar el código ASM.");
                return;
            }

            try {
                // 1. Generar ASM (código existente)
                System.out.println("=== Generando ASM ===");
                generarASMDesdeValores(valores);
                System.out.println("ASM generado exitosamente");

                // 2. Generar código Lua
                System.out.println("=== Generando código Lua ===");
                String codigoLua = GeneradorLua.generarCodigoLua(valores);
                System.out.println("Código Lua generado. Longitud: " + codigoLua.length());

                // 3. Guardar en C:\DOSBox2\Tasm
                String scriptDir = "C:\\DOSBox2\\Tasm";
                System.out.println("Directorio objetivo: " + scriptDir);

                GeneradorLua.guardarCodigoLua(scriptDir, "robotScript", codigoLua);

                // 4. Confirmar que el archivo se creó
                java.io.File archivoLua = new java.io.File(scriptDir + "\\robotScript.lua");
                if (archivoLua.exists()) {
                    System.out.println("✓ CONFIRMADO: Archivo Lua creado - " + archivoLua.getAbsolutePath());
                    System.out.println("Tamaño del archivo: " + archivoLua.length() + " bytes");
                } else {
                    System.out.println("✗ ERROR: El archivo Lua NO se creó");
                }

                JOptionPane.showMessageDialog(null,
                        "Generación completa:\n"
                        + "✓ Código ASM generado\n"
                        + "✓ Script Lua generado: " + scriptDir + "\\robotScript.lua\n"
                        + "✓ Revisa la consola para detalles");

            } catch (Exception ex) {
                System.err.println("ERROR GENERAL: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Error: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        });

        analizarBtn.addActionListener(e -> {
            try {
                analizarTexto(inputArea.getText());
            } catch (IOException ex) {
                mostrarError("Error al analizar: " + ex.getMessage());
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void limpiarTodo() {
        inputArea.setText("");
        cuadrupla.setRowCount(0);           // ❗ Cuádruplas
        tableModel.setRowCount(0);
        modeloSimbolos.setRowCount(0);
        areaErrores.setText("");
        mensajeSintactico.setText(" ");
        panelNumeros.setLineasConError(Collections.emptySet());
    }

    private void abrirArchivo() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader reader = new BufferedReader(new FileReader(chooser.getSelectedFile()))) {
                inputArea.read(reader, null);
            } catch (IOException e) {
                mostrarError("No se pudo abrir el archivo.");
            }
        }
    }

    private void guardarArchivo() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile())) {
                writer.print(inputArea.getText());
            } catch (IOException e) {
                mostrarError("No se pudo guardar el archivo.");
            }
        }
    }

    private void analizarTexto(String texto) throws IOException {
        File archivo = new File("entrada.txt");
        try (PrintWriter writer = new PrintWriter(archivo)) {
            writer.print(texto);
        }

        Reader reader = new BufferedReader(new FileReader("entrada.txt"));
        Lexer lexer = new Lexer(reader);
        Token token;

        List<Token> listaTokens = new ArrayList<>();
        List<String> listaLexemas = new ArrayList<>();
        List<Integer> listaLineas = new ArrayList<>();

        tableModel.setRowCount(0);
        modeloSimbolos.setRowCount(0);
        areaErrores.setText("");
        mensajeSintactico.setText(" ");

        while ((token = lexer.yylex()) != null) {
            if (token == Token.ERROR) {
                areaErrores.append("Error léxico en línea " + lexer.linea + ": " + lexer.lexeme + "\n");
            } else {
                tableModel.addRow(new Object[]{token.name(), lexer.lexeme});
                listaTokens.add(token);
                listaLexemas.add(lexer.lexeme);
                listaLineas.add(lexer.linea);
            }
        }

        parser = new A_Sintactico(listaTokens, listaLexemas, listaLineas);
        boolean exito = parser.analizar();

        if (exito) {
            mensajeSintactico.setForeground(Color.GREEN.darker());
            mensajeSintactico.setText("Análisis sintáctico y semántico exitoso.");

            // ✅ SOLO SI NO HAY ERRORES:
            for (Simbolo s : parser.getSimbolos()) {
                modeloSimbolos.addRow(s.toRow());
            }

            cuadrupla.setRowCount(0);
            int id = 0;
            for (Cuadrupla q : parser.getTAC()) {
                cuadrupla.addRow(new Object[]{
                    id++,
                    q.getOperador(),
                    q.getOperando1(),
                    q.getOperando2(),
                    q.getResultado()
                });
            }

        } else {
            mensajeSintactico.setForeground(Color.RED);
            areaErrores.append("Error en línea " + parser.getLineaError() + ": " + parser.getLexemaError() + "\n");

            cuadrupla.setRowCount(0);
        }

//        for (Simbolo s : parser.getSimbolos()) {
//            modeloSimbolos.addRow(s.toRow());
//        }
//        
//        // Mostrar cuádruplas en la tabla TAC
//cuadrupla.setRowCount(0); // Limpiar tabla anterior
//int id = 0;
//for (Cuadrupla q : parser.getTAC()) {
//    cuadrupla.addRow(new Object[]{
//        id++, 
//        q.getOperador(), 
//        q.getOperando1(), 
//        q.getOperando2(), 
//        q.getResultado()
//    });
//}
        // Marcar líneas con error (si tienes método adecuado en el parser)
        panelNumeros.setLineasConError(parser.getLineasErrores());
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void generarASMDesdeValores(List<Integer> valores) {
        if (valores.size() < 6) {
            JOptionPane.showMessageDialog(null, "No hay suficientes valores para generar el código.");
            return;
        }

        int base = valores.get(0);
        int velocidadBase = valores.get(1);
        int hombro = valores.get(2);
        int velocidadHombro = valores.get(3);
        int codo = valores.get(4);
        int velocidadCodo = valores.get(5);

        String puerto1 = "A"; // o B o C según base/hombro/codo
        String puerto2 = "B"; // o A o C según base/hombro/codo
        String puerto3 = "C"; // o A o B según base/hombro/codo

        String asmCode = TraductorCuadruplaASM.asmFileContent(
                puerto1, puerto2, puerto3,
                base, hombro, codo,
                velocidadBase, velocidadHombro, velocidadCodo
        );

        String dosboxPath = "C:" + File.separator + "DOSBox2";
        String projectFolder = dosboxPath + File.separator + "Tasm";
        String asmFileName = "code"; // El fichero ASM tiene que estar en la carpeta Tasm y tambien el mac.asm

        guardarComoASM(projectFolder, asmFileName, asmCode);
        compilarConDosboxDirecto(asmFileName, dosboxPath, projectFolder);

        JOptionPane.showMessageDialog(null, "Código ASM generado exitosamente - Ruta:\n" + projectFolder + File.separator + asmFileName + ".asm");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Interfaz_R.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Interfaz_R.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Interfaz_R.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Interfaz_R.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            System.err.println("No se pudo aplicar Nimbus: " + ex);
        }
        SwingUtilities.invokeLater(Interfaz_R::new);
    }

    private void mostrarCodigoLuaGenerado(String codigo) {
        JFrame ventanaLua = new JFrame("Código Lua - CoppeliaSim");
        JTextArea areaLua = new JTextArea(codigo);
        areaLua.setEditable(false);
        areaLua.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaLua.setBackground(new Color(45, 45, 45));
        areaLua.setForeground(Color.WHITE);
        areaLua.setCaretColor(Color.WHITE);

        JScrollPane scrollLua = new JScrollPane(areaLua);
        scrollLua.setPreferredSize(new Dimension(700, 500));
        scrollLua.setBorder(BorderFactory.createTitledBorder("Script para Robot IRB4600"));

        // Panel con botones
        JPanel panelBotones = new JPanel();
        JButton copiarBtn = new JButton("Copiar al Portapapeles");
        JButton cerrarBtn = new JButton("Cerrar");

        copiarBtn.addActionListener(ev -> {
            areaLua.selectAll();
            areaLua.copy();
            JOptionPane.showMessageDialog(ventanaLua, "Código copiado al portapapeles");
        });

        cerrarBtn.addActionListener(ev -> ventanaLua.dispose());

        panelBotones.add(copiarBtn);
        panelBotones.add(cerrarBtn);

        ventanaLua.setLayout(new BorderLayout());
        ventanaLua.add(scrollLua, BorderLayout.CENTER);
        ventanaLua.add(panelBotones, BorderLayout.SOUTH);
        ventanaLua.pack();
        ventanaLua.setLocationRelativeTo(this);
        ventanaLua.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ventanaLua.setVisible(true);
    }

    private void ejecutarCoppeliaSim() {
        try {
            // Ruta de CoppeliaSim (ajustar según tu instalación)
            String coppeliaPath = "C:/Program Files/CoppeliaRobotics/CoppeliaSim_Edu_V4_5_1_Ubuntu20_04/coppeliaSim.exe";
            String scenePath = "C:/Users/pedro/Documents/VERANO 2025/AUTOMATAS 2/CoppeliaSim_Scenes/Robot2.ttt";

            // Verificar que existe CoppeliaSim
            File coppeliaExe = new File(coppeliaPath);
            if (!coppeliaExe.exists()) {
                // Si no existe en la ruta por defecto, preguntar al usuario
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Selecciona coppeliaSim.exe");
                chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Ejecutables", "exe"));

                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    coppeliaPath = chooser.getSelectedFile().getAbsolutePath();
                } else {
                    return;
                }
            }

            // Verificar que existe la escena
            File sceneFile = new File(scenePath);
            if (!sceneFile.exists()) {
                // Si no existe la escena, preguntar al usuario
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Selecciona la escena Robot2.ttt");
                chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Escenas CoppeliaSim", "ttt"));

                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    scenePath = chooser.getSelectedFile().getAbsolutePath();
                } else {
                    // Ejecutar solo CoppeliaSim sin escena específica
                    scenePath = "";
                }
            }

            // Comando para ejecutar CoppeliaSim
            ProcessBuilder builder;
            if (!scenePath.isEmpty()) {
                builder = new ProcessBuilder(coppeliaPath, scenePath);
            } else {
                builder = new ProcessBuilder(coppeliaPath);
            }

            builder.start();
            System.out.println("CoppeliaSim ejecutado exitosamente");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al ejecutar CoppeliaSim: " + e.getMessage() + "\n"
                    + "Asegúrate de que CoppeliaSim esté instalado correctamente.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
