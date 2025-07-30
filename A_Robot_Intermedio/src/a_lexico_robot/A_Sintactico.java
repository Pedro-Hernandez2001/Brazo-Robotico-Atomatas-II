/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package a_lexico_robot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class A_Sintactico {
    private List<Token> tokens;
    private List<String> lexemas;
    private List<Integer> lineas;
    private int posicion = 0;
    private int errores = 0;

    private int lineaError = -1;
    private String lexemaError;

    private List<Simbolo> simbolos = new ArrayList<>();
    private Set<String> robotsDeclarados = new HashSet<>();

    // NUEVO: conjunto de líneas con errores
    private Set<Integer> lineasErrores = new HashSet<>();
    private Set<String> robotsIniciados = new HashSet<>();
    
    private List<Cuadrupla> tac = new ArrayList<>();
    


    public A_Sintactico(List<Token> tokens, List<String> lexemas, List<Integer> lineas) {
        this.tokens = tokens;
        this.lexemas = lexemas;
        this.lineas = lineas;
    }
    
    

    public boolean analizar() {
        while (posicion < tokens.size()) {
            if (tokens.get(posicion) == Token.Palabra_r) {
                if (!S()) return false;
            } else if (!INST()) {
                return false;
            }
        }
        
    List<Integer> numeros = this.extraerValoresNumericos();
    System.out.println("Valores capturados: " + numeros);
        
        return errores == 0;
    }
    
    public List<Integer> analizarYObtenerValores() {
    if (!analizar()) return Collections.emptyList();
    
    return extraerValoresNumericos();
}
    
    public List<Integer> extraerValoresNumericos() {
    List<Integer> valores = new ArrayList<>();
    
    for (Cuadrupla q : tac) {
        // Only consider "Asignar" operations
        if (q.getOperador().equals("Asignar")) {
            try {
                int valor1 = Integer.parseInt(q.getOperando1());
                valores.add(valor1);
            } catch (NumberFormatException e) {
                // Not a numeric value — ignore (like "Robot", "iniciar", etc.)
            }
              try {
                int valor2 = Integer.parseInt(q.getOperando2());
                valores.add(valor2);
            } catch (NumberFormatException e) {
                // Not a numeric value — ignore (like "Robot", "iniciar", etc.)
            }
        }
    }
    
    return valores;
}


    private boolean S() {
    if (ro()) {
        int lineaId = posicion < lineas.size() ? lineas.get(posicion) : -1; // Guardar línea antes de consumir
        if (ID()) {
            String id = lexemas.get(posicion - 1);
            if (robotsDeclarados.contains(id)) {
                // Pasar la línea correcta al registrar el error
                registrarError("El identificador '" + id + "' ya fue declarado", lineaId);
                return false;
            }
            robotsDeclarados.add(id);
            simbolos.add(new Simbolo(id, "Robot", "--", 0)); // También usar línea correcta para el símbolo
            return true;
        }
    }
    return false;
    }

private boolean INST() {
    int inicio = posicion;

    if (ID()) {
        String id = lexemas.get(posicion - 1);
        int lineaActual = lineas.get(posicion - 1);

        if (!robotsDeclarados.contains(id)) {
            registrarError("El identificador '" + id + "' no ha sido declarado", lineaActual);
            return false;
        }

        if (consumir(Token.PUNTO)) {
            if (COMB()) {
                String metodo = lexemas.get(posicion - 1);
                lineaActual = lineas.get(posicion - 1);

                // Validar si el robot ya fue iniciado
                if (!metodo.equals("iniciar") && !robotsIniciados.contains(id)) {
                    registrarError("El robot '" + id + "' debe ser iniciado antes de usarlo", lineaActual);
                    return false;
                }

                // Acción: iniciar
                if (metodo.equals("iniciar")) {
                    if (robotsIniciados.contains(id)) {
                        registrarError("El robot '" + id + "' ya fue iniciado", lineaActual);
                        return false;
                    }
                    robotsIniciados.add(id);
                    simbolos.add(new Simbolo(id, "iniciar", "-", 0));
                    // Generar cuadruplas para iniciar
                    tac.add(new Cuadrupla("Crear", "Robot", "---", id));
                    tac.add(new Cuadrupla("Asignar", id, "-", "iniciar"));
                    return true;
                }

                // Acción: finalizar
                if (metodo.equals("finalizar")) {
                    simbolos.add(new Simbolo(id, "finalizar", "-", 0));
                    // Generar cuadrupla para finalizar
                    tac.add(new Cuadrupla("Asignar", id, "-", "finalizar"));
                    return true;
                }

                // Acción sin parámetro (abrirGarra, cerrarGarra)
                if (esAccionSinParametro(metodo)) {
                    simbolos.add(new Simbolo(id, metodo, "-", lineaActual));
                    // Generar cuadrupla para acción sin parámetro
                    tac.add(new Cuadrupla("Asignar", id, "-", metodo));
                    return true;
                }

                // Método con asignación
                if (consumir(Token.ASIGNACION)) {
                    if (metodo.equals("repetir")) {
                        return validarRepetir(id, lineaActual);
                    }
                    
                    if (VAL()) {
                        String valor = lexemas.get(posicion - 1);
                        lineaActual = lineas.get(posicion - 1);
                        
                        try {
                            int val = Integer.parseInt(valor);
                            if (validarRango(metodo, val)) {
                                simbolos.add(new Simbolo(id, metodo, obtenerRango(metodo), val));
                                
                                // Generar cuadruplas según el formato especificado
                                String nombreVar = metodo.toLowerCase() + id;
                                tac.add(new Cuadrupla("Asignar", valor, "-", nombreVar));
                                
                                // Validar velocidad después de movimiento
                                if (esMetodoMovimiento(metodo)) {
                                    if (!validarVelocidadSiguiente(id, metodo, lineaActual)) {
                                        return false;
                                    }
                                }
                                
                                tac.add(new Cuadrupla("LLamar", id, nombreVar, metodo + "llamado" + id));
                                
                                return true;
                            } else {
                                registrarError("Valor fuera de rango para " + metodo + ": " + val + 
                                             " (rango permitido: " + obtenerRango(metodo) + ")", lineaActual);
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            registrarError("Valor inválido: " + valor, lineaActual);
                            return false;
                        }
                    }
                }
            }
        }
    }

    posicion = inicio;
    registrarError("Instrucción no válida", lineas.get(posicion));
    return false;
}

private boolean validarVelocidadSiguiente(String idRobot, String metodoMovimiento, int lineaActual) {
    // Guardamos el estado actual por si hay que retroceder
    int posicionInicial = posicion;
    
    // Verificamos que haya más tokens
    if (posicion >= tokens.size()) {
        registrarError("Falta especificar la velocidad después del movimiento", lineaActual);
        return false;
    }
    
    // La siguiente instrucción debe ser: [robot].velocidad = [valor]
    if (!ID()) {
        registrarError("Se esperaba la especificación de velocidad para " + idRobot, lineas.get(posicion));
        return false;
    }
    
    String siguienteId = lexemas.get(posicion - 1);
    if (!siguienteId.equals(idRobot)) {
        registrarError("La velocidad debe asignarse al mismo robot (" + idRobot + ")", lineas.get(posicion - 1));
        return false;
    }
    
    if (!consumir(Token.PUNTO)) {
        registrarError("Se esperaba un '.' antes de 'velocidad'", lineas.get(posicion));
        return false;
    }
    
    if (!COMB() || !lexemas.get(posicion - 1).equals("velocidad")) {
        registrarError("Se esperaba 'velocidad' después del movimiento", lineas.get(posicion - 1));
        return false;
    }
    
    if (!consumir(Token.ASIGNACION)) {
        registrarError("Falta '=' en la asignación de velocidad", lineas.get(posicion));
        return false;
    }
    
    if (!VAL()) {
        registrarError("Falta valor de velocidad (1-60)", lineas.get(posicion));
        return false;
    }
    
    String valorVelocidad = lexemas.get(posicion - 1);
    try {
        int velocidad = Integer.parseInt(valorVelocidad);
        if (velocidad < 1 || velocidad > 60) {
            registrarError("Velocidad fuera de rango (1-60): " + velocidad, lineas.get(posicion - 1));
            return false;
        }
        simbolos.add(new Simbolo(idRobot, "velocidad", "1-60", velocidad));
        
        // Generar cuadrupla para velocidad en el formato especificado
        tac.add(new Cuadrupla("Asignar", metodoMovimiento.toLowerCase() + idRobot, 
                            valorVelocidad, "Velocidad" + metodoMovimiento + idRobot));
        
        return true;
    } catch (NumberFormatException e) {
        registrarError("Valor de velocidad inválido: " + valorVelocidad, lineas.get(posicion - 1));
        return false;
    }
}

private boolean validarRepetir(String idRobot, int lineaActual) {
    // 1. Validar asignación de valor de repetición
    if (!VAL()) {
        registrarError("Falta valor de repetición para 'repetir'", lineas.get(posicion < lineas.size() ? posicion : lineas.size()-1));
        return false;
    }

    String valorRepeticion = lexemas.get(posicion - 1);
    try {
        int repeticiones = Integer.parseInt(valorRepeticion);
        if (repeticiones < 1 || repeticiones > 100) {
            registrarError("Número de repeticiones fuera de rango (1-100): " + repeticiones, 
                         lineas.get(posicion - 1));
            return false;
        }
        simbolos.add(new Simbolo(idRobot, "repetir", "1-100", repeticiones));
        
        // Generar cuadruplas para el bucle
        tac.add(new Cuadrupla("Asignar", valorRepeticion, "-", "Contador"));
        tac.add(new Cuadrupla("Asignar", idRobot, "-", "Bucle"));
        
        String etiquetaCiclo = "Ciclo" + (tac.size() + 1);
        String etiquetaFin = "FinCiclo";
        
        tac.add(new Cuadrupla("Comparar", "Contador", "0", etiquetaCiclo));
        tac.add(new Cuadrupla("SaltoFinal", etiquetaCiclo, "-", etiquetaFin));
        
        // 3. Validar apertura de bloque con {
        if (!consumir(Token.LLAVE_ABRE)) {
            registrarError("Se esperaba '{' después del valor de repetición", 
                         lineas.get(posicion < lineas.size() ? posicion : lineas.size()-1));
            return false;
        }

        // 4. Validar instrucciones dentro del bloque
        while (posicion < tokens.size() && tokens.get(posicion) != Token.LLAVE_CIERRA) {
            if (!INST()) {
                registrarError("Instrucción inválida dentro del bloque 'repetir'", 
                             lineas.get(posicion < lineas.size() ? posicion : lineas.size()-1));
                return false;
            }
        }

        // 5. Validar cierre de bloque con }
        if (posicion >= tokens.size()) {
            registrarError("Se esperaba '}' para cerrar el bloque 'repetir' pero se encontró el final del archivo", 
                          lineas.get(lineas.size()-1));
            return false;
        }
        
        if (!consumir(Token.LLAVE_CIERRA)) {
            registrarError("Se esperaba '}' para cerrar el bloque 'repetir'", 
                         lineas.get(posicion < lineas.size() ? posicion : lineas.size()-1));
            return false;
        }
        
        // Generar las últimas cuadruplas del bucle
        String temp = "C" + (tac.size() + 1);
        tac.add(new Cuadrupla("Restar", "Contador", "1", temp));
        tac.add(new Cuadrupla("Asignar", temp, "-", "Contador"));
        tac.add(new Cuadrupla("Salto", "-", "-", "Bucle"));
        tac.add(new Cuadrupla("ETIQUETA", "-", "-", etiquetaFin));
        
        return true;
    } catch (NumberFormatException e) {
        registrarError("Valor de repetición inválido: " + valorRepeticion, lineas.get(posicion - 1));
        return false;
    }
}


    private boolean ro() {
        return consumir(Token.Palabra_r);
    }

    private boolean ID() {
        return consumir(Token.identificador);
    }

    private boolean COMB() {
        return consumir(Token.Metodo);
    }

    private boolean VAL() {
        return consumir(Token.Numero);
    }

    private boolean consumir(Token esperado) {
        if (posicion < tokens.size() && tokens.get(posicion) == esperado) {
            posicion++;
            return true;
        } else {
            errores++;
            if (lineaError == -1 && posicion < lineas.size()) {
                lineaError = lineas.get(posicion);
                lexemaError = "Se esperaba " + esperado + " pero se encontró '" +
                        (posicion < lexemas.size() ? lexemas.get(posicion) : "EOF") + "'";
                lineasErrores.add(lineaError); // << MARCAR línea con error
            }
            return false;
        }
    }

    private void registrarError(String mensaje, int linea) {
        if (lineaError == -1) {
            lineaError = linea;
            lexemaError = mensaje;
            lineasErrores.add(lineaError);
        }
        errores++;
    }

    private boolean esAccionSinParametro(String metodo) {
        return metodo.equals("iniciar") ||
               metodo.equals("finalizar") ||
               metodo.equals("abrirGarra") ||
               metodo.equals("cerrarGarra");
    }

    private String obtenerRango(String metodo) {
        return switch (metodo) {
            case "base" -> "0–360";
            case "hombro", "codo" -> "0–180";
            case "garra" -> "0–90";
            case "velocidad" -> "1–60";
            case "repetir" -> "1-100";
            default -> "Desconocido";
        };
    }

    private boolean validarRango(String metodo, int valor) {
        return switch (metodo) {
            case "base" -> valor >= 0 && valor <= 360;
            case "hombro", "codo" -> valor >= 0 && valor <= 180;
            case "garra" -> valor >= 0 && valor <= 90;
            case "velocidad" -> valor >= 1 && valor <= 60;
            case "repetir" -> valor >=1 && valor <=100;
            default -> false;
        };
    }

    private boolean esMetodoMovimiento(String metodo) {
    return metodo.equals("base") || 
           metodo.equals("hombro") || 
           metodo.equals("codo") || 
           metodo.equals("garra");
}
    
    // Getters
    public int getErrores() { return errores; }
    public int getLineaError() { return lineaError; }
    public String getLexemaError() { return lexemaError; }
    public List<Simbolo> getSimbolos() { return simbolos; }

    // NUEVO: obtener líneas con error
    public Set<Integer> getLineasErrores() {
        return lineasErrores;
    }
    
    public List<Cuadrupla> getTAC() {
    return tac;
}

}
