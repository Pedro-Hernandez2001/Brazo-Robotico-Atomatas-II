/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package a_lexico_robot;

/**
 *
 * @author Denis Peralta
 */
public enum Token {
    Palabra_r,         // Robot
    Metodo,            // iniciar, finalizar, abrirGarra, etc.
    identificador,     // r1, r2...
    Numero,            // valores numéricos
    ASIGNACION,        // =
    PUNTO,             // .
    PARENTESIS_ABRE,   // (
    PARENTESIS_CIERRA, // )
    LLAVE_ABRE,        // {
    LLAVE_CIERRA,      // }
    ERROR              // errores léxicos
}


