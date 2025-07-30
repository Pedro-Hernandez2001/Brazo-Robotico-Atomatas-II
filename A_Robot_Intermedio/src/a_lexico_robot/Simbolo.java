/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package a_lexico_robot;

public class Simbolo {
    private String id;
    private String metodo;
    private String rango;
    private int valor;

    public Simbolo(String id, String metodo, String rango, int valor) {
        this.id = id;
        this.metodo = metodo;
        this.rango = rango;
        this.valor = valor;
    }

    public Object[] toRow() {
        return new Object[]{id, metodo, rango, (valor == -1 ? "--" : valor)};
    }
}




