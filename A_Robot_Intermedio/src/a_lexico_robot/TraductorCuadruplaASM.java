package a_lexico_robot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class TraductorCuadruplaASM {

    public static void guardarComoASM(String directorio, String nombreArchivo, String contenidoASM) {
        if (!nombreArchivo.endsWith(".asm")) {
            nombreArchivo += ".asm";
        }

        File archivo = new File(directorio, nombreArchivo);

        // Ensure directory exists
        new File(directorio).mkdirs();

        try (FileWriter writer = new FileWriter(archivo)) {
            writer.write(contenidoASM);
            System.out.println("Archivo ASM creado exitosamente: " + nombreArchivo + " Ruta: " + archivo.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo ASM: " + e.getMessage());
        }
    }

    public static void compilarConDosboxDirecto(String nombreASM, String rutaDosbox, String rutaProyecto) {
        try {
            // Asegurar nombre del archivo sin extensión
            if (nombreASM.endsWith(".asm")) {
                nombreASM = nombreASM.replace(".asm", "");
            }

            // Comando DOSBox con múltiples instrucciones -c
            String[] comandos = {
                rutaDosbox + "\\dosbox.exe",
                "-noconsole",
                "-c", "mount c " + rutaProyecto,
                "-c", "c:",
                "-c", "tasm " + nombreASM + ".asm",
                "-c", "tlink /tiny " + nombreASM + ".obj", // "-c", nombreASM, //"-c", "exit"
            };

            ProcessBuilder builder = new ProcessBuilder(comandos);
            builder.redirectErrorStream(true);
            Process proceso = builder.start();

            // Leer la salida del proceso
            BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea;
            while ((linea = reader.readLine()) != null) {
                System.out.println(linea);
            }

            int exitCode = proceso.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Error al compilar/ejecutar en DOSBox. Código: " + exitCode);
            }
            System.out.println("Compilación completada con exito.");

        } catch (IOException | InterruptedException | RuntimeException e) {
            System.err.println("Fallo en el proceso con DOSBox: " + e.getMessage());
        }
    }

    public static String asmFileContent(String nombrePuerto1, String nombrePuerto2, String nombrePuerto3,
            int valorMovimiento1, int valorMovimiento2, int valorMovimiento3,
            int velocidad1, int velocidad2, int velocidad3) {
        int pasos1 = valorMovimiento1 / 45;
        int pasos2 = valorMovimiento2 / 45;
        int pasos3 = valorMovimiento3 / 45;
        StringBuilder asm = new StringBuilder();

        // Cabecera
        asm.append("include mac.asm\n")
                .append(".MODEL TINY\n")
                .append(".CODE\n")
                .append("ORG 100h\n")
                .append("PORTA  EQU 00H\n")
                .append("PORTB  EQU 02H\n")
                .append("PORTC  EQU 04H\n")
                .append("CONFIG EQU 06H\n\n")
                .append("start:\n")
                .append("    MOV DX, CONFIG\n")
                .append("    MOV AL, 10000000b\n")
                .append("    OUT DX, AL\n\n");

        // Selección de puerto
        asm.append("    CALL PORT_").append(nombrePuerto1.toUpperCase()).append("\n\n")
                .append("    DELAYSEC ").append(velocidad1).append("\n");

        // Movimiento normal
        for (int i = 1; i <= pasos1; i++) {
            asm.append("    CALL STEP_NORMAL_").append(rotationStepsMapping(i, false)).append(" ; ").append(i).append("\n")
                    .append("    DELAYSEC ").append(velocidad1).append("\n");
        }

        // Movimiento reversa
        for (int i = pasos1 - 1; i >= 0; i--) {
            asm.append("    CALL STEP_REVERSE_").append(rotationStepsMapping(i, true)).append(" ; ").append(i).append("\n")
                    .append("    DELAYSEC ").append(velocidad1).append("\n");
        }

        // Selección de puerto 2
        asm.append("    CALL PORT_").append(nombrePuerto2.toUpperCase()).append("\n\n")
                .append("    DELAYSEC ").append(velocidad2).append("\n");

        // Movimiento normal
        for (int i = 1; i <= pasos2; i++) {
            asm.append("    CALL STEP_NORMAL_").append(rotationStepsMapping(i, false)).append(" ; ").append(i).append("\n")
                    .append("    DELAYSEC ").append(velocidad2).append("\n");
        }

        // Movimiento reversa
        for (int i = pasos2 - 1; i >= 0; i--) {
            asm.append("    CALL STEP_REVERSE_").append(rotationStepsMapping(i, true)).append(" ; ").append(i).append("\n")
                    .append("    DELAYSEC ").append(velocidad2).append("\n");
        }

        // Selección de puerto 3
        asm.append("    CALL PORT_").append(nombrePuerto3.toUpperCase()).append("\n\n")
                .append("    DELAYSEC ").append(velocidad3).append("\n");

        // Movimiento normal
        for (int i = 1; i <= pasos3; i++) {
            asm.append("    CALL STEP_NORMAL_").append(rotationStepsMapping(i, false)).append(" ; ").append(i).append("\n")
                    .append("    DELAYSEC ").append(velocidad3).append("\n");
        }

        // Movimiento reversa
        for (int i = pasos3 - 1; i >= 0; i--) {
            asm.append("    CALL STEP_REVERSE_").append(rotationStepsMapping(i, true)).append(" ; ").append(i).append("\n")
                    .append("    DELAYSEC ").append(velocidad3).append("\n");
        }

        // Fin
        asm.append("\n    JMP FIN\n\n");

        // Métodos de puerto
        asm.append("PORT_").append(nombrePuerto1.toUpperCase()).append(":\n")
                .append("    MOV DX, PORT").append(nombrePuerto1.toUpperCase()).append("\n")
                .append("    MOV AL, 00001001b\n")
                .append("    OUT DX, AL\n")
                .append("    RET\n\n");

        asm.append("PORT_").append(nombrePuerto2.toUpperCase()).append(":\n")
                .append("    MOV DX, PORT").append(nombrePuerto2.toUpperCase()).append("\n")
                .append("    MOV AL, 00001001b\n")
                .append("    OUT DX, AL\n")
                .append("    RET\n\n");

        asm.append("PORT_").append(nombrePuerto3.toUpperCase()).append(":\n")
                .append("    MOV DX, PORT").append(nombrePuerto3.toUpperCase()).append("\n")
                .append("    MOV AL, 00001001b\n")
                .append("    OUT DX, AL\n")
                .append("    RET\n\n");

        // Métodos STEP (pueden ir fijos o generarse si lo necesitas)
        asm.append("""
        STEP_NORMAL_A:
            MOV AL, 00001100b
            OUT DX, AL
            RET

        STEP_NORMAL_B:
            MOV AL, 00000110b
            OUT DX, AL
            RET

        STEP_NORMAL_C:
            MOV AL, 00000011b
            OUT DX, AL
            RET

        STEP_NORMAL_D:
            MOV AL, 00001001b
            OUT DX, AL
            RET

        STEP_REVERSE_C:
            MOV AL, 00000011b
            OUT DX, AL
            RET

        STEP_REVERSE_B:
            MOV AL, 00000110b
            OUT DX, AL
            RET

        STEP_REVERSE_A:
            MOV AL, 00001100b
            OUT DX, AL
            RET

        STEP_REVERSE_Z:
            MOV AL, 00001001b
            OUT DX, AL
            RET

        FIN:
        END start
        """);

        return asm.toString();
    }

    private static String rotationStepsMapping(int paso, boolean reverse) {
        return switch (paso) {
            case 0 ->
                "Z";
            case 1 ->
                "A";
            case 2 ->
                "B";
            case 3 ->
                "C";
            case 4 ->
                reverse ? "Z" : "D";
            case 5 ->
                "A";
            case 6 ->
                "B";
            case 7 ->
                "C";
            case 8 ->
                "D";
            default ->
                "?";
        };
    }
}
