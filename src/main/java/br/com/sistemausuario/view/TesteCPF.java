package br.com.sistemausuario.view;

public class TesteCPF {
    public static void main(String[] args) {
        System.out.println(ValidadorCPF.validar("11144477735")); // true
        System.out.println(ValidadorCPF.validar("52998224725")); // true
        System.out.println(ValidadorCPF.validar("12345678909")); // true
    }
}
