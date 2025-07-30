/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package a_lexico_robot;
import java.io.File;

/**
 *
 * @author Denis Peralta
 */
public class A_Lexico_Robot {
    public static void main(String[] args) {
        String path = "C:/Users/pedro/Documentos/VERANO 2025/AUTOMATAS 2/carpetas/A_Robot_Intermedio/src/a_lexico_robot/Lexer.flex";
        generarLexer(path);
    }

    public static void generarLexer(String path) {
    try {
        jflex.Main.generate(new String[]{path});
    } catch (jflex.exceptions.SilentExit e) {
        System.out.println("Error al generar el lexer: " + e.getMessage());
    }
}
}
